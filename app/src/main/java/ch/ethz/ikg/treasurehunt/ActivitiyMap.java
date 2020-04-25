package ch.ethz.ikg.treasurehunt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
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
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * The TreasureHunt activity is the main screen of the treasure hunt application. It features a
 * spinner to select the current treasure, a compass that shows the direction to this treasure,
 * and several UI elements that indicate the player's speed, the current temperature, the coins
 * a player already has collected, etc.
 */
public class ActivitiyMap extends AppCompatActivity {

             private MapView mMapView;
             private Callout mCallout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();
//        String uriString = getIntent().getDataString();
//        double lat = Double.parseDouble(uriString.substring(uriString.indexOf(':') + 1, uriString.indexOf(',')));
//        double lng = Double.parseDouble(uriString.substring(uriString.indexOf(',') + 1, uriString.indexOf('?')));
//        int zoom = Integer.parseInt(uriString.substring(uriString.indexOf('=') + 1));

        mMapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, 47.408367, 8.507640, 16);

        // Adding Baselayer World_Topo_Map
        ArcGISTiledLayer layer = new ArcGISTiledLayer("http://services.arcgisonline.com/arcgis/rest/services/DARK_GRAY_CANVAS_VECTOR/MapServer");
        map.getOperationalLayers().add(layer);

        // Add track Feature Layer
        ServiceFeatureTable track = new ServiceFeatureTable("https://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/treassure/FeatureServer/0");
        FeatureLayer featureLayer = new FeatureLayer(track);
        map.getOperationalLayers().add(featureLayer);

        // Add Graphic Layer
        GraphicsOverlay overlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(overlay);


        SpatialReference SPATIAL_REFERENCE = SpatialReferences.getWgs84();
        Point geometry1 = new Point(8.507847,47.408992,  SPATIAL_REFERENCE);  // x : lng, y : lat
        SimpleMarkerSymbol s = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, Color.BLUE, 10);
        Graphic g1 = new Graphic(geometry1, s);
        overlay.getGraphics().add(g1);

        Point geometry2 = new Point( 8.507309,47.408288, SPATIAL_REFERENCE);
        Graphic g2 = new Graphic(geometry2, s);
        overlay.getGraphics().add(g2);


        // set an on touch listener to listen for click events
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // remove any existing callouts
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }
                // get the point that was clicked and convert it to a point in map coordinates
                final Point clickPoint = mMapView
                        .screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                // create a selection tolerance
                int tolerance = 10;
                double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
                // use tolerance to create an envelope to query
                Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance,
                        clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, map.getSpatialReference());
                QueryParameters query = new QueryParameters();
                query.setGeometry(envelope);
                // request all available attribute fields
                final ListenableFuture<FeatureQueryResult> future = track.queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
                // add done loading listener to fire when the selection returns


                future.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //call get on the future to get the result
                            FeatureQueryResult result = future.get();
                            // create an Iterator
                            Iterator<Feature> iterator = result.iterator();
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
                            while (iterator.hasNext()) {
                                feature = iterator.next();
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
                return super.onSingleTapConfirmed(e);
            }
        });

        //setMap
        mMapView.setMap(map);
        // get the callout that shows attributes
        mCallout = mMapView.getCallout();
        //mCallout.show();



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

