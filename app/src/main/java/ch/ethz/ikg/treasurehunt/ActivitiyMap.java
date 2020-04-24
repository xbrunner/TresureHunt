package ch.ethz.ikg.treasurehunt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;


/**
 * The TreasureHunt activity is the main screen of the treasure hunt application. It features a
 * spinner to select the current treasure, a compass that shows the direction to this treasure,
 * and several UI elements that indicate the player's speed, the current temperature, the coins
 * a player already has collected, etc.
 */
public class ActivitiyMap extends AppCompatActivity {

             private MapView mMapView;

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

        // Add Feature Layer
//        ServiceFeatureTable table = new ServiceFeatureTable("http://services1.arcgis.com/i9MtZ1vtgD3gTnyL/arcgis/rest/services/buildings5/FeatureServer/0");
//        FeatureLayer featureLayer = new FeatureLayer(table);
//        map.getOperationalLayers().add(featureLayer);

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

        //setMap
        mMapView.setMap(map);


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

