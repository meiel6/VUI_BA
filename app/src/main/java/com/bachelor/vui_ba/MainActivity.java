package com.bachelor.vui_ba;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.nuance.speechanywhere.Session;
import com.nuance.speechanywhere.SessionEventListener;
import com.nuance.speechanywhere.VuiController;
import com.nuance.speechanywhere.VuiControllerEventListener;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements SessionEventListener, VuiControllerEventListener {
    private VuiController theVuiController; // Reference to the VuiController object
    private EditText spokenText;
    private TextView batteryText;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        registerOnNuance();
        configNuance();
        Session.getSharedSession().startRecording();

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void init(){
        spokenText = findViewById(R.id.spokenText);
        batteryText = findViewById(R.id.batteryText);
        spokenText.requestFocus();
        spokenText.setTextIsSelectable(true);
        theVuiController = findViewById(R.id.vuicontroller);
    }

    private void configNuance(){
        theVuiController.setTopic("GeneralMedicine");
        theVuiController.setLanguage("de");
    }

    private void registerOnNuance(){
        Registration r = new Registration();
        r.openNuanceSession();
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryText.setText(String.valueOf(level) + "%");
        }
    };

    // Save recording state across destruction-recreation (for example, on device rotation)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    // Activity lifecycle callbacks to register and unregister for session event callbacks properly
    @Override
    protected void onStart() {
        // When the Activity starts (it is already created), register for session event callbacks
        Session.getSharedSession().addSessionEventListener(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // When the Activity stops, unregister for session event callbacks
        Session.getSharedSession().removeSessionEventListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // When the Activity is about to be destroyed, close session.
        // From the application logic we know that speech recognition is not needed beyond this point.
        if (isFinishing()) Session.getSharedSession().close();
        super.onDestroy();
    }


        @Override
    public void onRecordingStarted() {

    }

    @Override
    public void onRecordingStopped() {

    }

    @Override
    public void onProcessingStarted() {

    }

    @Override
    public void onProcessingFinished() {

    }

    @Override
    public void onProcessingStarted(View view) {

    }

    @Override
    public void onProcessingFinished(View view) {

    }

    @Override
    public void onCommandRecognized(String s, String s1, String s2, HashMap<String, String> hashMap) {

    }
}
