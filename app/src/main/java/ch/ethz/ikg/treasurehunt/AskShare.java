package ch.ethz.ikg.treasurehunt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;


/**
 * The TreasureHunt activity is the main screen of the treasure hunt application. It features a
 * spinner to select the current treasure, a compass that shows the direction to this treasure,
 * and several UI elements that indicate the player's speed, the current temperature, the coins
 * a player already has collected, etc.
 */
public class AskShare extends AppCompatActivity {

    private int userId = 0;
    private int currentCoins = 0;


    private Button myYesButton;
    private Button myNoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Intent intent = getIntent();
        int id = intent.getIntExtra("userId", 0);
        int coins = intent.getIntExtra("currentCoins", 0);
        userId = id;
        currentCoins =coins;

        myYesButton = (Button)findViewById(R.id.yesButton);
        myYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myYesButtonAction();
            }
        });

        myNoButton = (Button)findViewById(R.id.noButton);
        myNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myNoButtonAction();
            }
        });

    }

    private void myYesButtonAction() {

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "My score on TreasureGame is: " + currentCoins + " Coins. My user ID is: " + userId);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }


    private void myNoButtonAction() {
        this.finish();

    }


}

