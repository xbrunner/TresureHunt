package ch.ethz.ikg.treasurehunt;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.MapView;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The Activity map shows treasures and tracks records of each individual Mobile-GIS class and other users.
 * ->The Map is displayed using ArcGIS online.
 * ->The found treasures and tracks features of the users are downloaded from two different ArcGIS feature services using URLs.
 * ->The user can choose with a spinner if the features of all the users and the Mobile-GIS class users or the others users.
 * ->The user has to press on the button "SEARCH" to display the user features. (The others will be set as invisible.)
 * ->To show Track layer, the switch has to be on.
 * ->To show tracks & treasures attributes of the selected user, the user can click (tap) on a feature.
 */

public class ActivitiyMap extends AppCompatActivity {

    // Tag of the activity.
    private static final String TAG = ActivitiyMap.class.getSimpleName();

    // Button switch and spinner definition.
    private Button mySearchButton;
    private Switch switchTrackFeature;
    private Spinner spinner = null;

    // Map elements definition.
    private MapView mMapView;
    private Callout mCallout;
    private Envelope backEnvelope = null; // Enveloppe to come back to extent.

    // Selected ID for the switch.
    private Integer selectedId = 19; // To set the first position of spinner at 19 (all Users). The first ID begins at 20.

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add treasure Feature Layer.
        ServiceFeatureTable treasureTable = new ServiceFeatureTable("https://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/treassure/FeatureServer/0");
        treasureTable.loadAsync();
        FeatureLayer treasureFeatureLayer = new FeatureLayer(treasureTable);

        // Add track Feature Layer.
        ServiceFeatureTable trackTable = new ServiceFeatureTable("https://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/track/FeatureServer/0");
        trackTable.loadAsync();
        FeatureLayer trackFeatureLayer = new FeatureLayer(trackTable);

        // Set map content and basemap.
        setContentView(R.layout.activity_map);
        mMapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, 47.408367, 8.507640, 16);

        //SearchButton definition.
        mySearchButton = (Button)findViewById(R.id.buttonSearch);
        mySearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLayer(selectedId, treasureTable, treasureFeatureLayer, map);
                map.getOperationalLayers().add(treasureFeatureLayer);

                if (switchTrackFeature.isChecked()){
                    displayLayer(selectedId, trackTable, trackFeatureLayer, map);
                    map.getOperationalLayers().add(trackFeatureLayer);
                }
            }
        });

        // Spinner definition (to choose user ID).
        spinner = findViewById(R.id.spinner);
        final List<String> list = new ArrayList<String>();
        addId(list); // Add all the users.
        ArrayAdapter<String> adp1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        adp1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adp1);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedId = spinner.getSelectedItemPosition() + 19; //Id List begin at 20. -> 19 is all the users
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Switch action to show/hide the track feature layer.
        switchTrackFeature = findViewById(R.id.switchTrack);
        switchTrackFeature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                if (switchTrackFeature.isChecked()){
                    displayLayer(selectedId, trackTable, trackFeatureLayer, map); // Select track of selected user ID.
                    map.getOperationalLayers().add(trackFeatureLayer); // Show tracks.
                }
                else {
                    map.getOperationalLayers().remove(trackFeatureLayer); // Hide tracks.
                }
            }

        });

        // Set an on touch listener to listen for click events.
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // Remove any existing callouts.
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }
                // Get the point that was clicked and convert it to a point in map coordinates.
                final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));

                // Create a selection tolerance.
                int tolerance = 10;
                double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();

                // Use tolerance to create an envelope to query.
                Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance,clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, map.getSpatialReference());

                // Set back-envelope to zoom back in displayLayer.
                backEnvelope = envelope;

                // Request all available attribute fields.
                QueryParameters queryCurrentUser = new QueryParameters();
                queryCurrentUser.setGeometry(envelope);

                // make search case insensitive
                if (selectedId >= 20 && selectedId <= 35) {
                queryCurrentUser.setWhereClause("user_id =" + selectedId);
                } else {
                    queryCurrentUser.setWhereClause("user_id >= 0");
                }
                final ListenableFuture<FeatureQueryResult> futureTreasure = treasureTable.queryFeaturesAsync(queryCurrentUser, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL); // Treasure Feature.
                final ListenableFuture<FeatureQueryResult> futureTrack = trackTable.queryFeaturesAsync(queryCurrentUser, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL); //Track Feature.

                // Listener for Treasure layer (points).
                futureTreasure.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Call get on the future to get the result.
                            FeatureQueryResult resultTreasure = futureTreasure.get();

                            // Create an Iterator.
                            Iterator<Feature> iteratorTreasure = resultTreasure.iterator();

                            // Create a TextView to display field values.
                            TextView calloutContent = new TextView(getApplicationContext());
                            calloutContent.setTextColor(Color.BLACK);
                            calloutContent.setSingleLine(false);
                            calloutContent.setVerticalScrollBarEnabled(true);
                            calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                            calloutContent.setMovementMethod(new ScrollingMovementMethod());
                            calloutContent.setLines(5);

                            // Cycle through selections.
                            int counter = 0;
                            Feature feature;
                            while (iteratorTreasure.hasNext()) {
                                feature = iteratorTreasure.next();

                                // Create a Map of all available attributes as name value pairs.
                                Map<String, Object> attr = feature.getAttributes();
                                Set<String> keys = attr.keySet();
                                for (String key : keys) {
                                    Object value = attr.get(key);
                                    // Format observed field value as date.
                                    if (value instanceof GregorianCalendar) {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                        value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                    }
                                    // Append name value pairs to TextView.
                                    calloutContent.append(key + " | " + value + "\n");
                                }
                                counter++;

                                // Center the mapview on selected feature.
                                Envelope envelope = feature.getGeometry().getExtent();
                                // show CallOut
                                mCallout.setLocation(clickPoint);
                                mCallout.setContent(calloutContent);
                                mCallout.show();
                            }
                        } catch (Exception e) {
                            Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                        }
                    }
                });

                // Listener for Track layer (lines).
                futureTrack.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Call get on the future to get the result.
                            FeatureQueryResult resultTrack = futureTrack.get();

                            // Create an Iterator.
                            Iterator<Feature> iteratorTrack = resultTrack.iterator();

                            // Create a TextView to display field values.
                            TextView calloutContent = new TextView(getApplicationContext());
                            calloutContent.setTextColor(Color.BLACK);
                            calloutContent.setSingleLine(false);
                            calloutContent.setVerticalScrollBarEnabled(true);
                            calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                            calloutContent.setMovementMethod(new ScrollingMovementMethod());
                            calloutContent.setLines(5);

                            // Cycle through selections.
                            int counter = 0;
                            Feature feature;
                            while (iteratorTrack.hasNext()) {
                                feature = iteratorTrack.next();
                                // Create a Map of all available attributes as name value pairs.
                                Map<String, Object> attr = feature.getAttributes();
                                Set<String> keys = attr.keySet();
                                for (String key : keys) {
                                    Object value = attr.get(key);
                                    // Format observed field value as date.
                                    if (value instanceof GregorianCalendar) {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                        value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                    }
                                    // Append name value pairs to TextView.
                                    calloutContent.append(key + " | " + value + "\n");
                                }
                                counter++;
                                // Center the mapview on selected feature.
                                Envelope envelope = feature.getGeometry().getExtent();
                                mMapView.setViewpointGeometryAsync(envelope, 2);
                                // Show CallOut.
                                mCallout.setLocation(clickPoint);
                                mCallout.setContent(calloutContent);
                                mCallout.show();
                            }
                        } catch (Exception e) {
                            Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                        }
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });

        // Add Graphic Layers.
        GraphicsOverlay overlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(overlay);
        mMapView.setMap(map);

        // get the callout that shows attributes
        mCallout = mMapView.getCallout();
    }

    // Display only features (lines & points) of the selected user using query.
    private void displayLayer(final Integer id, ServiceFeatureTable mServiceFeatureTable, FeatureLayer mFeatureLayer, ArcGISMap map) {
        // Hide all the layers
        map.getOperationalLayers().remove(mFeatureLayer);
        // Clear any previous selections.
        mFeatureLayer.clearSelection();
        mFeatureLayer.resetRenderer();

        // Create objects required to do a selection with a query.
        QueryParameters query = new QueryParameters();
        // Make search case insensitive.
        if (id == 19) {
            query.setWhereClause("user_id < 0"); // Case for "all the users".
        }
        else if (id >= 20 && id <=35 ) {
            query.setWhereClause("user_id <> " + id); // Case for Mobile-GIS class users (IDs 20 to 35).
        }
        else {query.setWhereClause("user_id < 35 AND user_id > 19"); // Case for the "not Mobile-GIS class users" (all IDs except 20 to 35).
        }
        // Call select features.
        final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query);

        // Add done loading listener to fire when the selection returns.
        future.addDoneListener(() -> {
            try {
                // Get the result.
                FeatureQueryResult result = future.get();
                mFeatureLayer.resetFeaturesVisible();
                mFeatureLayer.setFeaturesVisible(result,false);
                // Check there are some results.
                Iterator<Feature> resultIterator = result.iterator();
                if (resultIterator.hasNext()) {
                    // Back zoom.
                    mMapView.setViewpointGeometryAsync(backEnvelope, 200);
                } else {
                    Toast.makeText(this, "No Layer found for ID : " + id, Toast.LENGTH_LONG).show();

                }
            } catch (Exception e) {
                String error = "Feature search failed for: " + id + ". Error: " + e.getMessage();
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, error);
            }
        });
    }

    // Add all the users.
    private void addId(List<String> listId) {
        listId.add("All users"); //All users (ID 19).
        listId.add("Mudathir Awadaljeed"); // Mobile-GIS class users (IDs 20 to 35 according to PDF Assignment).
        listId.add("Isabelle Bai");
        listId.add("Simone Brönnimann");
        listId.add("Xavier Brunner");
        listId.add("Patrik Eugster");
        listId.add("Xi Fan");
        listId.add("Valérie Hellmüller");
        listId.add("Yuchang Jiang");
        listId.add("Selim Kälin");
        listId.add("Maria Pérez Ortega");
        listId.add("Laura Schalbetter");
        listId.add("Yihang She");
        listId.add("Reto Spannagel");
        listId.add("Raphael Stauffer");
        listId.add("Han Sun");
        listId.add("Evelyn Weiss");
        listId.add("Other users"); // Others users. ID 36.
    }

    @Override
             protected void onPause() {
                 mMapView.pause();
                 super.onPause();
             }

             @Override
             protected void onResume() {
                 super.onResume();
                 mMapView.resume();
             }

             @Override
             protected void onDestroy() {
                 super.onDestroy();
                 mMapView.dispose();
             }

}

