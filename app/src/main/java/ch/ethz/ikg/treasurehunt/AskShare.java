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
 * The Ask Share give the possibility when the user want to quit the application to share the score.
 * -> By clicking on the Yes-button, the user can shows the won coins & the userID and choose the sharing application.
 * -> By clicking on the No-button, the app will be closed.
 */
public class AskShare extends AppCompatActivity {

    // Initialization of sharing values.
    private int userId = 0;
    private int currentCoins = 0;

    // Initialization of buttons.
    private Button myYesButton;
    private Button myNoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        // Get sharing data from TreasureHuntActivity.
        Intent intent = getIntent();
        int id = intent.getIntExtra("userId", 0);
        int coins = intent.getIntExtra("currentCoins", 0);

        // Actualize values of sharing.
        userId = id;
        currentCoins =coins;

        // Yes-button definition.
        myYesButton = (Button)findViewById(R.id.yesButton);
        myYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myYesButtonAction();
            }
        });

        //No-button definition.
        myNoButton = (Button)findViewById(R.id.noButton);
        myNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myNoButtonAction();
            }
        });
    }

    // Yes-button activate social sharing.
    private void myYesButtonAction() {

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "My score on TreasureGame is: " + currentCoins + " Coins. My user ID is: " + userId);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    // No-button finish the app activity.
    private void myNoButtonAction() {
        this.finish();

    }

}

