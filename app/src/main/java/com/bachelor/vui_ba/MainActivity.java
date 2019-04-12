package com.bachelor.vui_ba;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.nuance.speechanywhere.CommandSet;
import com.nuance.speechanywhere.Session;
import com.nuance.speechanywhere.SessionEventListener;
import com.nuance.speechanywhere.VuiController;
import com.nuance.speechanywhere.VuiControllerEventListener;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements SessionEventListener, VuiControllerEventListener {

    private VuiController theVuiController;

    private boolean recording_flag = false;
    private EditText spokenText;
    private TextView dictatedText;
    private TextView batteryText;
    private TextView tvDate;

    private boolean isDictatingActive = false;
    private boolean isDictatingDone = false;
    private String finalText = "";
    private int startIndex;

    //Connection
    private int port;
    private String ip;
    private WiFiServiceDiscovery wsd;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wsd = new WiFiServiceDiscovery(this);

        init();
        initCommandSets();

        registerOnNuance();
        configNuance();

        setDate();

        if ((savedInstanceState != null) && (savedInstanceState.containsKey("recording"))) {
            recording_flag = savedInstanceState.getBoolean("recording");
        }

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    /**
     * Initializes all the controls for this UI.
     */
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
    }

    /**
     * Initializes the individual designed command sets used for the Nuance speech recognition.
     */
    private void initCommandSets(){
        CommandSet startDictationCommandSet = new CommandSet("Start with Dictation", "By using this command, you will start dictating");
        startDictationCommandSet.createCommand("startElias", "Ok Elias", "", "Start with Dictation");
        startDictationCommandSet.createCommand("stopElias", "Stop Elias", "", "Stop with Dictation");
        theVuiController.assignCommandSets(new CommandSet[]{startDictationCommandSet});
        theVuiController.synchronize();
    }

    /**
     * Configure parameter for Nuance.
     * - "GeneralMedicine" = Vocabulary of common medical words and phrases,
     * - "DE" = German Language.
     */
    private void configNuance(){
        theVuiController.setTopic("GeneralMedicine");
        theVuiController.setLanguage("de");
    }

    /**
     * Register on the Nuance Cloud Server to use speech recognition functionality.
     */
    private void registerOnNuance(){
        Registration r = new Registration();
        r.openNuanceSession();
    }

    /**
     * This method is used to receive the battery level of the device and show it to the user.
     */
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryText.setText(String.valueOf(level) + "%");
        }
    };

    /**
     * Sets the current date with day, month and year on the UI.
     */
    private void setDate(){
        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int month = (cal.get(Calendar.MONTH)) + 1;
        int year = cal.get(Calendar.YEAR);

        String dayOfMonthString = String.valueOf(dayOfMonth);
        String monthString = String.valueOf(month);
        String yearString = String.valueOf(year);

        tvDate.setText(dayOfMonthString + "." + monthString + "." + yearString);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
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
        Session.getSharedSession().removeSessionEventListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
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
    }

    @Override
    public void onProcessingStarted(View view) {
        recording_flag = true;
    }

    @Override
    public void onProcessingFinished(View view) {
        recording_flag = false;

        if(isDictatingActive){
            String currEditTextContent = spokenText.getText().toString();
            startIndex = currEditTextContent.indexOf(";");

            finalText += currEditTextContent.substring(startIndex + 1);
        }

        if(isDictatingDone){
            sendToProtocol();
            flushTextHolder();
            isDictatingDone = false;
        }
    }

    @Override
    public void onCommandRecognized(String s, String s1, String s2, HashMap<String, String> hashMap) {
        if (s.equals("startElias")){
            spokenText.append(";");
            isDictatingActive = true;
            isDictatingDone = false;
        } else if(s.equals("stopElias")){
            spokenText.append(":");
            isDictatingActive = false;
            isDictatingDone = true;
        }
    }

    /**
     * Sends the finaltext, which holds the whole dictate phrase of the user, via TCP-Connection to the ePatientenprotokoll.
     */
    private void sendToProtocol(){
        this.ip = wsd.getDiscoveredIp();
        this.port = wsd.getDiscoveredPort();

        TCPSender tcps = new TCPSender(ip, port);
        tcps.setSpokenText(finalText);
        tcps.execute();
    }

    /**
     * This method cleans the two string placeholder when the command is sent to the ePatientenprotokoll.
     */
    private void flushTextHolder(){
        spokenText.setText("");
        finalText = "";
    }
}
