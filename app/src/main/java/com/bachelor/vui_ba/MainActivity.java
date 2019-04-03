package com.bachelor.vui_ba;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.nuance.speechanywhere.CommandSet;
import com.nuance.speechanywhere.Session;
import com.nuance.speechanywhere.SessionEventListener;
import com.nuance.speechanywhere.VuiController;
import com.nuance.speechanywhere.VuiControllerEventListener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SessionEventListener, VuiControllerEventListener {
    private VuiController theVuiController; // Reference to the VuiController object
    private boolean recording_flag = false;
    private EditText spokenText;
    private TextView dictatedText;
    private TextView batteryText;
    private TextView tvDate;
    private final String ip_address = "147.87.16.79";
    private final int port = 6000;
    private String tempStr;

    private List<String> dictatedList;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        registerOnNuance();
        configNuance();
        setDate();

        if ((savedInstanceState != null) && (savedInstanceState.containsKey("recording"))) {
            recording_flag = savedInstanceState.getBoolean("recording");
        }

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void init(){
        spokenText = findViewById(R.id.spokenText);
        dictatedText = findViewById(R.id.dictatedText);
        dictatedText.setMovementMethod(new ScrollingMovementMethod());
        batteryText = findViewById(R.id.batteryText);
        tvDate = findViewById(R.id.tvDate);

        spokenText.requestFocus();
        spokenText.setTextIsSelectable(true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        theVuiController = findViewById(R.id.vuicontroller);

        CommandSet startDictationCommandSet = new CommandSet("Start with Dictation", "By using this command, you will start dictating");
        startDictationCommandSet.createCommand("startElias", "Elias", "", "Start with Dictation");
        theVuiController.assignCommandSets(new CommandSet[]{startDictationCommandSet});
        theVuiController.synchronize();

        dictatedList = new ArrayList<String>();
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

    private void setDate(){
        // Sets the current date
        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int month = (cal.get(Calendar.MONTH)) + 1;
        int year = cal.get(Calendar.YEAR);
        String dayOfMonthString = String.valueOf(dayOfMonth);
        String monthString = String.valueOf(month);
        String yearString = String.valueOf(year);
        tvDate.setText(dayOfMonthString + "." + monthString + "." + yearString);
    }

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
    protected void onResume(){
        super.onResume();
        theVuiController.synchronize();
        Session.getSharedSession().startRecording();
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
        onResume();
    }

    @Override
    public void onProcessingStarted() {

    }

    @Override
    public void onProcessingFinished() {
        System.out.println("test");
    }

    @Override
    public void onProcessingStarted(View view) {
        recording_flag = true;
    }

    @Override
    public void onProcessingFinished(View view) {
        recording_flag = false;
        tempStr = spokenText.getText().toString();
        if (tempStr.contains("Elias")) {
            tempStr = tempStr.replaceAll("Elias", "");
            //dictatedText.setText(tempStr);
            dictatedList.add(tempStr);
            Log.d("dictatedList", dictatedList + "");

            dictatedText.setText("");
            for (String el : dictatedList) {
                dictatedText.setText(dictatedText.getText() + el + "\n");
            }

            BackgroundTask bt = new BackgroundTask();
            bt.execute();
        }

        Log.d("onProcessingFinished", spokenText.getText().toString());
        spokenText.setText("");
    }

    @Override
    public void onCommandRecognized(String s, String s1, String s2, HashMap<String, String> hashMap) {
        if (s.equals("startElias")){
            spokenText.append("Elias");
            //BackgroundTask bt = new BackgroundTask();
            //bt.execute();
        }
    }

    class BackgroundTask extends AsyncTask<String, Void, Void> {

        Socket s;
        DataOutputStream dos;


        @Override
        protected Void doInBackground(String... strings) {
            try {
                s = new Socket(ip_address, port);
                dos = new DataOutputStream(s.getOutputStream());
                dos.writeUTF(tempStr);

                dos.close();
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
