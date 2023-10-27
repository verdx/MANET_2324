package d2d.testing.gui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import d2d.testing.R;
import d2d.testing.gui.setting.ExitActivity;
import info.guardianproject.panic.PanicResponder;

public class ModeActivity extends AppCompatActivity {
    private static final String TAG = "ModeActivity";
    ImageButton witness;
    ImageButton humanitarian;
    TextView textWitness;
    TextView textHumanitarian;

    public static final String PREF_LOCK_AND_EXIT = "pref_lock_and_exit";
    public static final String PREF_CLEAR_APP_DATA = "pref_clear_app_data";
    public static final String PREF_UNINSTALL_THIS_APP = "pref_uninstall_this_app";


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PanicResponder.checkForDisconnectIntent(this)) {
            finish(); return;
        }

        askPermits();
        setContentView(R.layout.activity_mode);
        checkWifiAwareAvailability();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();

        @SuppressLint("ResourceType")
        Animation right = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animate_slide_right);
        Animation left = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animate_slide_left);

        witness = findViewById(R.id.witnessButton);
        /*
        witness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        */
        textWitness = findViewById(R.id.textWitness);
        witness.startAnimation(right);
        textWitness.startAnimation(right);

        humanitarian = findViewById(R.id.humanitarianButton);
        /*
        humanitarian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMainActivity(getString(R.string.mode_humanitarian));
            }
        });
        */
        textHumanitarian = findViewById(R.id.textHumanitarian);
        humanitarian.startAnimation(left);
        textHumanitarian.startAnimation(left);
    }

    public void witnessMode(View v){
        openMainActivity(getString(R.string.mode_witness));
    }

    public void humanitarianMode(View v){
        openMainActivity(getString(R.string.mode_humanitarian));
    }

    /*
    * If mode = w then witnes
    * If mode = h then humanitarian
    */
    public void openMainActivity(String mode) {
        Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
        mainActivityIntent.putExtra("MODE", mode);
        this.startActivity(mainActivityIntent);
    }

    private void askPermits(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 2);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 3);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 4);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0){
            for(int granted: grantResults){
                if(granted != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"Ve a ajustes para otorgar los permisos necesarios", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void checkWifiAwareAvailability(){
        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Snackbar.make(findViewById(android.R.id.content), "No dispones de Wifi Aware, la aplicación no funcionará correctamente", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        final boolean lockAndExit = prefs.getBoolean("PREF_LOCK_AND_EXIT", true);
        final boolean clearAppData = prefs.getBoolean("PREF_CLEAR_APP_DATA", false);
        final boolean uninstallThisApp = prefs.getBoolean("PREF_UNINSTALL_THIS_APP", false);

        //String context = PanicResponder.getConnectIntentSender(this);
        PanicResponder.setTriggerPackageName(this);

        if (PanicResponder.receivedTriggerFromConnectedApp(this)) {
            if (uninstallThisApp) {
                Log.i(TAG, PREF_UNINSTALL_THIS_APP + " " + getApplication().getPackageName());
                Intent uninstall = new Intent(Intent.ACTION_DELETE);
                uninstall.setData(Uri.parse("package:" + getApplication().getPackageName()));
                uninstall.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                startActivityForResult(uninstall, 100);
                ExitActivity.exitAndRemoveFromRecentApps(this);

            } else if (clearAppData) {
                Log.i(TAG, PREF_CLEAR_APP_DATA);
                PanicResponder.deleteAllAppData(getApplicationContext());
                ExitActivity.exitAndRemoveFromRecentApps(this);

            } else if (lockAndExit) {
                Log.i(TAG, PREF_LOCK_AND_EXIT);
                ExitActivity.exitAndRemoveFromRecentApps(this);
            }
            // add other responses here, paying attention to if/else order
        } else if (PanicResponder.shouldUseDefaultResponseToTrigger(this)) {
            if (prefs.getBoolean(PREF_LOCK_AND_EXIT, true)) {
                Log.i(TAG, PREF_LOCK_AND_EXIT);
                ExitActivity.exitAndRemoveFromRecentApps(this);
            }
        }

        //finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK){
            //Delete in release app
            Toast.makeText(getApplicationContext(), "App unistall successfully...", Toast.LENGTH_SHORT).show();
        }
    }
}
