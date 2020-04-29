package ch.ethz.ikg.treasurehunt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.content.Intent;

import java.util.*;
import java.util.concurrent.ExecutionException;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.InputStream;

import android.widget.Toast;

import ch.ethz.ikg.treasurehunt.model.Treasure;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.loadable.LoadStatus;

/**
 * The TreasureHunt activity is the main screen of the treasure hunt application.
 * *** Assignment 1 Solution
 * It features a spinner to select the current treasure, a compass that shows the direction to this treasure,
 * and several UI elements that indicate the player's speed, the current temperature, the coins
 * a player already has collected, etc.
 * ->By clicking on the Android back button to quit the app, the AskShare activity starts (Social sharing options).
 * ->By clicking on the "SEE MAP" button, the ActivityMap activity starts (Arc GIS SDK to show results).
 *
 * *** Assignment 2 Functionalities
 * The track of the user and the collected treasures are uploaded to two differents ArcGIS features using URLs.
 * The score can be shared by leaving the app (android back arrow of the device) on other apps via the "AskShare" Activity.
 * The user can see the track and found treasures on the ActivityMap by clicking on the "SAVE TRACK AND SEE MAP" button.
 * ->The track ID  change each time a treasure is found.
 * ->The found treasure are immediately uploaded on the server.
 * ->The tracks are uploaded on the serve by clicking on the "SAVE TRACK AND SEE MAP" button.
 * -> The current time is calculated using the device (Emulator) time.
 */

public class TreasureHunt extends AppCompatActivity
        implements LocationListener, SensorEventListener {

    // Application tag.
    private static final String TAG = TreasureHunt.class.getSimpleName();

    // Some application constants.
    private static final double COLLECT_COIN_DISTANCE = 10.0;

    // Variables that store the different treasures.
    private List<Treasure> treasures;
    private Treasure currentTreasure;

    // Variables that store current sensor readings and derived game values.
    private Location currentLocation;
    private Location previousLocation = currentLocation;
    private float[] currentAcceleration;
    private float[] currentCompassReading;
    private boolean temperatureAvailable = false;
    private double currentTemperature = 20.0;
    private double currentHeading;
    private int currentCoins = 0;
    private int userId = 23; // User: Xavier Brunner
    private int trackId = 0; //Is incremented each time a Treasure is found.

    // Button.
    private Button myButton;

    // Managers.
    private SensorManager sensorManager;
    private LocationManager locationManager;

    // UI elements.
    private TextView txtCoins;
    private TextView txtTemperature;
    private TextView txtSpeed;
    private TextView txtDistance;
    private ImageView imgCompass;
    private Spinner selectTreasure;

    // Set Feature Layers URL.
    private ServiceFeatureTable treasureFeature;
    private ServiceFeatureTable trackFeature;

    //Current Date and time definition.
    private String currentMoment = null;

    // Create a polyline builder, polyline track and polyline for the track uploading.
    PolylineBuilder polylineBuilder = new PolylineBuilder(SpatialReferences.getWgs84()); //Spatial reference.
    Part trackPart = new Part(SpatialReferences.getWgs84());
    private Polyline polyline = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treasure_hunt);

        // Set up all UI bindings.
        txtCoins = findViewById(R.id.txtCoins);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtSpeed = findViewById(R.id.txtSpeed);
        txtDistance = findViewById(R.id.txtDistance);
        imgCompass = findViewById(R.id.imgCompass);
        selectTreasure = findViewById(R.id.selectTreasure);
        myButton = (Button)findViewById(R.id.button);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myButtonAction();
            }
        });

        // Create service feature table from URL.
        // Track feature.
        trackFeature = new ServiceFeatureTable("https://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/track/FeatureServer/0");
        trackFeature.loadAsync();

        // Treasure feature.
        treasureFeature = new ServiceFeatureTable("https://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/treassure/FeatureServer/0");
        treasureFeature.loadAsync();

        // Set up all manager references.
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Set up all required components.
        readTreasures();
        updateSpinner();
        updateGameAndUI();
    }

    private void currentTime() {
        // Time format.
        String pattern = "MM/dd/yyyy HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);

        // Get the today date using Calendar object.
        Date today = Calendar.getInstance(Locale.FRANCE).getTime();

        // String Conversion.
        String todayAsString = df.format(today);

        // Change current Moment.
        currentMoment = todayAsString;
    }

    private void myButtonAction() {
        // Calculate current time for server.
        currentTime();

        // Add track (polyline) feature on the server.
        trackFeature.addDoneLoadingListener(new Runnable(){
            @Override
            public void run() {
                if(trackFeature.getLoadStatus() == LoadStatus.LOADED) {
                    polyline = polylineBuilder.toGeometry();
                    addPolylineFeature(polyline, trackFeature);
                } else {
                    treasureFeature.retryLoadAsync();
                }
            }
        });

        // Go to ActivityMap to see results on ArcGIS map.
        Intent Intent = new Intent(this, ActivitiyMap.class);
        startActivityForResult(Intent, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // In onResume, we try to set up all sensors (including the location manager), and give
        // indications to the user in case something goes wrong.
        setUpLocationManager();
        Sensor ambientTemperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (ambientTemperatureSensor != null) {
            temperatureAvailable = true;
            sensorManager.registerListener(this, ambientTemperatureSensor,
                    SensorManager.SENSOR_DELAY_GAME);
        } else {
            // No temperature sensor is available. Notify the user!
            Toast.makeText(this, R.string.err_temperature, Toast.LENGTH_LONG).show();
        }

        // Register accelerometer and magnetic field to make the compass more smooth.
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister all listeners.
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Switch to Askshare Activity using Intent.
        Intent Intent = new Intent(this, AskShare.class);
        Intent.putExtra("userId", userId); // To share user ID.
        Intent.putExtra("currentCoins", currentCoins); // To share score.
        startActivityForResult(Intent, 0);
    }

    /**
     * Helper method that reads the treasure CSV file from the raw folder. In case reading is not
     * possible, this method will notify the user, but not do anything else.
     */
    private void readTreasures() {
        InputStream inputStream = getResources().openRawResource(R.raw.treasures);
        treasures = new ArrayList<>();
        try {
            for (String[] row : CSVReader.readFile(inputStream, ";", true)) {
                treasures.add(new Treasure(row[0], Double.parseDouble(row[1]),
                        Double.parseDouble(row[2]), Integer.parseInt(row[3])));
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.err_reading_treasures),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Helper method that populates the spinner (dropdown menu) in the UI. This method also registers
     * a listener that responds to changes in the spinner.
     */
    private void updateSpinner() {
        List<String> spinnerElements = new ArrayList<>();
        for (Treasure treasure : treasures) {
            spinnerElements.add(getString(R.string.spinner_element, treasure.getName(), treasure.getCoins()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, spinnerElements);
        selectTreasure.setAdapter(adapter);
        selectTreasure.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTreasure = treasures.get(position);
                updateGameAndUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    /**
     * Helper method that updates the game state and the user interface. This also checks if
     * we're close to the current treasure, and collects it in case.
     */
    private void updateGameAndUI() {
        if (currentTreasure == null) {
            // No treasure has been selected yet. Notify the user using the txtDistance box.
            txtDistance.setText(R.string.distance_unk);
            return;
        }

        if (currentLocation != null) {
            double distance = Util.distance(currentLocation, currentTreasure.getLocation());

            // Check if we're close to any treasure.
            checkTreasure(distance);

            // Update the compass and other UI elements.
            txtDistance.setText(getString(R.string.distance, distance));
            txtSpeed.setText(getString(R.string.speed, currentLocation.getSpeed() * 3.6));
        } else {

            // If the current location is not yet available, notify the user.
            txtSpeed.setText(R.string.speed_unk);
            txtDistance.setText(R.string.wait_for_gps);
        }

        if (temperatureAvailable) {
            txtTemperature.setText(getString(R.string.temperature, currentTemperature));
        } else {
            txtTemperature.setText(R.string.err_temperature);
        }
        imgCompass.setRotation((float) currentHeading);

        // Update the current game info.
        txtCoins.setText(getString(R.string.coins, currentCoins));
    }

    /**
     * Helper function that checks if we're close to the current treasure. If so, points are awarded.
     * <p>
     * The coin award function works as follows:
     * If the speed is above 10 km/h, all points are awarded. If the speed is below 2.5 km/h, 1/4 of
     * the maximal points are awarded. In between, a linear interpolation is applied.
     * If the temperature is above 25°C, all points are awarded. If the temperature is below 6.25°C,
     * 1/4 of the points are awarded. In between, a linear interpolation is applied.
     * Both these weights are multiplied, i.e., a player can get between 0.25*0.25 and 1.0 times
     * the maximally available coins in a treasure.
     *
     * @param distance The distance to the current treasure.
     */
    private void checkTreasure(double distance) {
        if (distance < COLLECT_COIN_DISTANCE) {
            // Apply the above described coin award function. If the temperature sensor is not
            // available, we simply give the maximal number of coins.
            double speedWeight = Math.min(1.0,
                    Math.max(0.25, 1.0 - (10.0 - currentLocation.getSpeed() * 3.6) / 10.0));
            double temperatureWeight = 1.0;
            if (temperatureAvailable) {
                temperatureWeight = Math.min(1.0,
                        Math.max(0.25, 1.0 - (25.0 - currentTemperature) / 25.0));
            }
            int coinAward = (int) (currentTreasure.getCoins() * speedWeight * temperatureWeight);
            currentCoins += coinAward;

            // Notify the user that he or she collected the treasure! And also remove the
            // treasure from the treasures list.
            treasures.remove(currentTreasure);
            Toast.makeText(this, getString(R.string.congrats_collected,
                    currentTreasure.getName(), coinAward), Toast.LENGTH_LONG).show();

            // Refresh Track ID when treasure is found.
            trackId += 1;

            // Save current location for the server.
            Point foundTreasurePoint = new Point( 7.081406, 47.419509,  SpatialReferences.getWgs84());

            // Calculate current time for server.
            currentTime();

            // Add Treasure Feature to Server.
            treasureFeature.addDoneLoadingListener(new Runnable(){
                @Override
                public void run() {
                    if(treasureFeature.getLoadStatus() == LoadStatus.LOADED) {

                        addPointFeature(foundTreasurePoint, treasureFeature);
                    } else {
                        treasureFeature.retryLoadAsync();
                    }
                }
            });

            currentTreasure = null;
            updateSpinner();
        }
    }

    /**
     * Sets up the location manager, and requests the permissions if they have not been given yet.
     */
    private void setUpLocationManager() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            // The permissions already have been granted, request the location updates here.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    // The permissions have been granted, request the location updates here.
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                } else {
                    Toast.makeText(this, getString(R.string.no_location_enabled),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        updateGameAndUI();

        // Add new points to polylineTrack.
        refreshTrack();
    }

    //  Add new points to polylineTrack.
    private void refreshTrack() {
        polylineBuilder.getParts().add(trackPart);
        trackPart.addPoint(currentLocation.getLongitude(), currentLocation.getLatitude());
        trackPart.addPoint(currentLocation.getLongitude(), currentLocation.getLatitude());
    }

    // Function to upload points on the ArcGIS URL.
    private void addPointFeature(Point mapPoint, final ServiceFeatureTable featureTable) {
        // Create default attributes for the feature.
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user_id", userId);
        attributes.put("track_id", trackId);
        attributes.put("timestamp", currentMoment);
        attributes.put("collected_coins", Double.valueOf(currentCoins));
        attributes.put("treasure_name", currentTreasure.getName());
        // Creates a new feature using default attributes and point.
        Feature feature = featureTable.createFeature(attributes, mapPoint);

        // Add the new feature to the feature table and to server.
        // Check if feature can be added to feature table.
        if (featureTable.canAdd()) {
            // Add the new feature to the feature table and to server.
            featureTable.addFeatureAsync(feature).addDoneListener(() -> applyEdits(featureTable));
        } else {
            runOnUiThread(() -> logToUser(true, "Error cannot add to feature table"));
        }
    }

    // Function to upload polylines on the ArcGIS URL.
    private void addPolylineFeature(Polyline polyline, final ServiceFeatureTable featureTable) {

        // Create default attributes for the feature.
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user_id", userId);
        attributes.put("track_id", trackId);
        attributes.put("timestamp", currentMoment);
        // Creates a new feature using default attributes and point.
        Feature feature = featureTable.createFeature(attributes, polyline);

        // Add the new feature to the feature table and to server.
        // Check if feature can be added to feature table.
        if (featureTable.canAdd()) {
            // Add the new feature to the feature table and to server.
            featureTable.addFeatureAsync(feature).addDoneListener(() -> applyEdits(featureTable));
        } else {
            runOnUiThread(() -> logToUser(true, "Error cannot add to feature table"));
        }
    }


    private void applyEdits(ServiceFeatureTable featureTable) {
        // apply the changes to the server
        final ListenableFuture<List<FeatureEditResult>> editResult = featureTable.applyEditsAsync();
        editResult.addDoneListener(() -> {
            try {
                List<FeatureEditResult> editResults = editResult.get();
                // check if the server edit was successful
                if (editResults != null && !editResults.isEmpty()) {
                    if (!editResults.get(0).hasCompletedWithErrors()) {
                        runOnUiThread(() -> logToUser(false, "New Layer added"));
                    } else {
                        throw editResults.get(0).getError();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                runOnUiThread(() -> logToUser(true, "Error applying edits"));
            }
        });
    }


    private void logToUser(boolean isError, String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (isError) {
            Log.e(TAG, message);
        } else {
            Log.d(TAG, message);
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // Check if it's the temperature sensor.
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            currentTemperature = event.values[0];
        }

        // Accelerometer and magnetic field are required for the compass.
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            currentAcceleration = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            currentCompassReading = event.values;
        }

        // If we have all required values for the compass, compute it's heading.
        if (currentAcceleration != null && currentCompassReading != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, currentAcceleration, currentCompassReading)) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                double headingNorth = (float) (Math.toDegrees(orientation[0]) + 360) % 360;
                // If we know our location and the location of the currently selected treasure,
                // we can compute the compass' position.
                if (currentTreasure != null && currentLocation != null) {
                    // We filter the heading so that the compass doesn't move too quickly.
                    currentHeading = 0.7 * currentHeading +
                            0.3 * (360.0 - (headingNorth - currentLocation.bearingTo(currentTreasure.getLocation()))) % 360;
                } else {
                    currentHeading = 0.0;
                }
            }
        }

        updateGameAndUI();
    }

    // Empty overrides. ============================================================================
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}