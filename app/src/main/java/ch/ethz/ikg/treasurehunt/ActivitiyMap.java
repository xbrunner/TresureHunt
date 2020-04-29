package ch.ethz.ikg.treasurehunt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.SyncStateContract;
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
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;


import java.text.SimpleDateFormat;
import java.util.*;


/**
 * The TreasureHunt activity is the main screen of the treasure hunt application. It features a
 * spinner to select the current treasure, a compass that shows the direction to this treasure,
 * and several UI elements that indicate the player's speed, the current temperature, the coins
 * a player already has collected, etc.
 */
public class ActivitiyMap extends AppCompatActivity {

            private static final String TAG = ActivitiyMap.class.getSimpleName();


    // Button.
    private Button mySearchButton;

    private MapView mMapView;
    private Callout mCallout;
    private SpatialReference SPATIAL_REFERENCE = SpatialReferences.getWgs84();
    private Spinner spinner = null;
    private String selectedPerson = "";
    private Integer selectedId = 19; // Per default definition ID 19: all the Users.

    private Envelope backEnvelope = null;



    //Define Switch and check state.
    private Switch switchTrackFeature;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Set content map.
        setContentView(R.layout.activity_map);

        // Add treasure Feature Layer.
        ServiceFeatureTable treasureTable = new ServiceFeatureTable("https://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/treassure/FeatureServer/0");
        treasureTable.loadAsync();
        FeatureLayer treasureFeatureLayer = new FeatureLayer(treasureTable);


        // Add track Feature Layer.
        ServiceFeatureTable trackTable = new ServiceFeatureTable("https://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/track/FeatureServer/0");
        trackTable.loadAsync();
        FeatureLayer trackFeatureLayer = new FeatureLayer(trackTable);

        mMapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, 47.408367, 8.507640, 16);

        // Adding Basemap.
        //ArcGISTiledLayer layer = new ArcGISTiledLayer("http://services.arcgisonline.com/arcgis/rest/services/DARK_GRAY_CANVAS_VECTOR/MapServer");
        //map.getOperationalLayers().add(layer);
        // Adding Layer
        //searchForGeometry(selectedId, treasureTable,treasureFeatureLayer,map);
        //map.getOperationalLayers().add(treasureFeatureLayer);


        //SearchButton
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



        // Spinner to choose ID.
        spinner = findViewById(R.id.spinner);
        final List<String> list = new ArrayList<String>();
        addId(list);
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



        // Switch action.
        switchTrackFeature = findViewById(R.id.switchTrack);
        switchTrackFeature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                if (switchTrackFeature.isChecked()){
                    displayLayer(selectedId, trackTable, trackFeatureLayer, map);
                    map.getOperationalLayers().add(trackFeatureLayer);
                }
                else {
                    map.getOperationalLayers().remove(trackFeatureLayer);
                }
            }

        });



        // Set an on touch listener to listen for click events.
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // remove any existing callouts
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }
                // get the point that was clicked and convert it to a point in map coordinates
                final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                // create a selection tolerance
                int tolerance = 10;
                double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
                // use tolerance to create an envelope to query
                Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance,clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, map.getSpatialReference());
                QueryParameters query = new QueryParameters();
                query.setGeometry(envelope);

                // Define Envelope to zoom back in displayLayer.
                backEnvelope = envelope;

                // request all available attribute fields
                final ListenableFuture<FeatureQueryResult> futureTreasure = treasureTable.queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
                final ListenableFuture<FeatureQueryResult> futureTrack = trackTable.queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
                // add done loading listener to fire when the selection returns

                // Listener for Treasure feature.
                futureTreasure.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //call get on the future to get the result.
                            FeatureQueryResult resultTreasure = futureTreasure.get();
                            // create an Iterator
                            Iterator<Feature> iteratorTreasure = resultTreasure.iterator();
                            // create a TextView to display field values
                            TextView calloutContent = new TextView(getApplicationContext());
                            calloutContent.setTextColor(Color.BLACK);
                            calloutContent.setSingleLine(false);
                            calloutContent.setVerticalScrollBarEnabled(true);
                            calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                            calloutContent.setMovementMethod(new ScrollingMovementMethod());
                            calloutContent.setLines(5);
                            // cycle through selections
                            int counter = 0;
                            Feature feature;
                            while (iteratorTreasure.hasNext()) {
                                feature = iteratorTreasure.next();
                                // create a Map of all available attributes as name value pairs
                                Map<String, Object> attr = feature.getAttributes();
                                Set<String> keys = attr.keySet();
                                for (String key : keys) {
                                    Object value = attr.get(key);
                                    // format observed field value as date
                                    if (value instanceof GregorianCalendar) {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                        value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                    }
                                    // append name value pairs to TextView
                                    calloutContent.append(key + " | " + value + "\n");
                                }
                                counter++;
                                // center the mapview on selected feature
                                Envelope envelope = feature.getGeometry().getExtent();
                                mMapView.setViewpointGeometryAsync(envelope, 200);

                            }
                        } catch (Exception e) {
                            Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                        }
                    }
                });

                // Listener for Track feature.
                futureTrack.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //call get on the future to get the result
                            FeatureQueryResult resultTrack = futureTrack.get();
                            // create an Iterator
                            Iterator<Feature> iteratorTrack = resultTrack.iterator();
                            // create a TextView to display field values
                            TextView calloutContent = new TextView(getApplicationContext());
                            calloutContent.setTextColor(Color.BLACK);
                            calloutContent.setSingleLine(false);
                            calloutContent.setVerticalScrollBarEnabled(true);
                            calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                            calloutContent.setMovementMethod(new ScrollingMovementMethod());
                            calloutContent.setLines(5);
                            // cycle through selections
                            int counter = 0;
                            Feature feature;
                            while (iteratorTrack.hasNext()) {
                                feature = iteratorTrack.next();
                                // create a Map of all available attributes as name value pairs
                                Map<String, Object> attr = feature.getAttributes();
                                Set<String> keys = attr.keySet();
                                for (String key : keys) {
                                    Object value = attr.get(key);
                                    // format observed field value as date
                                    if (value instanceof GregorianCalendar) {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                        value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                    }
                                    // append name value pairs to TextView
                                    calloutContent.append(key + " | " + value + "\n");
                                }
                                counter++;
                                // center the mapview on selected feature
//                                Envelope envelope = feature.getGeometry().getExtent();
//                                mMapView.setViewpointGeometryAsync(envelope, 200);
////                                // show CallOut
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

        //setMap

        //mCallout.show();

        // Add Graphic Layer.
        GraphicsOverlay overlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(overlay);
        mMapView.setMap(map);
        // get the callout that shows attributes
        mCallout = mMapView.getCallout();

    }


    private void displayLayer(final Integer id, ServiceFeatureTable mServiceFeatureTable, FeatureLayer mFeatureLayer, ArcGISMap map) {
        map.getOperationalLayers().remove(mFeatureLayer);
        // clear any previous selections
        mFeatureLayer.clearSelection();
        mFeatureLayer.resetRenderer();
        // create objects required to do a selection with a query
        QueryParameters query = new QueryParameters();
        // make search case insensitive
        if (id == 19) {
            query.setWhereClause("user_id < 0");
        }
        else if (id >= 20 && id <=35 ) {
            query.setWhereClause("user_id <> " + id);
        }
        else {query.setWhereClause("user_id < 35 AND user_id > 19");
        }

        // call select features
        final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query);
        // add done loading listener to fire when the selection returns
        future.addDoneListener(() -> {
            try {
                // call get on the future to get the result
                FeatureQueryResult result = future.get();
                mFeatureLayer.resetFeaturesVisible();
                mFeatureLayer.setFeaturesVisible(result,false);
                // check there are some results
                Iterator<Feature> resultIterator = result.iterator();
                if (resultIterator.hasNext()) {
                    // get the extent of the first feature in the result to zoom to
                    Feature feature = resultIterator.next();

                    // select the feature
                    mMapView.setViewpointGeometryAsync(backEnvelope, 200);
                    //mFeatureLayer.selectFeature(feature);


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


    private void addId(List<String> listId) {
        listId.add("All users"); //All users.
        listId.add("Mudathir Awadaljeed"); // Mobile-GIS class users.
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
        listId.add("Other users"); // Others users.
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

