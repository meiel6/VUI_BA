package com.bachelor.vui_ba;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nuance.speechanywhere.CommandSet;
import com.nuance.speechanywhere.Session;
import com.nuance.speechanywhere.SessionEventListener;
import com.nuance.speechanywhere.VuiController;
import com.nuance.speechanywhere.VuiControllerEventListener;

import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements SessionEventListener, VuiControllerEventListener {

    private VuiController theVuiController;

    private boolean recording_flag = false;
    private boolean dayMode = true;
    private LinearLayout background;
    private EditText spokenText;
    private TextView dictatedText;
    private TextView batteryText;
    private TextView tvDate;
    private Vibrator v;

    private boolean isDictatingActive = false;
    private boolean isDictatingDone = false;
    private String finalText = "";
    private int startIndex;
    private static Context context;

    //Connection
    private int port;
    private String ip;
    private static WiFiServiceDiscovery wsd;
    private TCPSender tcps;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

//        startWiFiCheckerService();
        openWiFiDiscovery();

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
     * Creates a new instance from WiFiServiceDiscovery to discover the open Sub-Wlan from the
     * ePatientenprotokoll.
     */
    public static void openWiFiDiscovery(){
        wsd = new WiFiServiceDiscovery(context);
    }

    /**
     * Creates a background service that checks the wifi state of the user device.
     */
    private void startWiFiCheckerService(){
        startService(new Intent(this, WiFiCheckerService.class));
    }

    private void openTCPConnection(final String finalText){
        System.out.println("JAN: openTCPConnection() started");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket s = new Socket(ip, port);
                    tcps = new TCPSender(s, context);
                    tcps.setSpokenText(finalText);
                    System.out.println("JAN: openTCPConnection() before execute()");
                    tcps.execute();
                } catch (IOException e) {
                    System.out.println("JAN: openTCPConnection() IOException before TCPSender Creation");
                    tcps = new TCPSender(context);
                    tcps.setSpokenText(finalText);
                    String payload = tcps.getPayload();
                    System.out.println("JAN: openTCPCOnnection: Backup Payload: " + payload);
                    tcps.writeToBackupLogFile(payload);
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * Initializes all the controls for this UI.
     */
    private void init(){
        background = findViewById(R.id.background);
        spokenText = findViewById(R.id.spokenText);
        dictatedText = findViewById(R.id.dictatedText);
        dictatedText.setMovementMethod(new ScrollingMovementMethod());
        batteryText = findViewById(R.id.batteryText);
        tvDate = findViewById(R.id.tvDate);


        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
        startDictationCommandSet.createCommand("stopElias1", "Stopp Elias", "", "Stop with Dictation");

        startDictationCommandSet.createCommand("background", "Hintergrundfarbe", "", "Changes background color");
        startDictationCommandSet.createCommand("background2", "Couleur de fond", "", "Changes background color");

        startDictationCommandSet.createCommand("activateTouch", "Berührung aktivieren", "", "Activates user touch and interactions");
        startDictationCommandSet.createCommand("activateTouch2", "Activer le toucher", "", "Activates user touch and interactions");
        startDictationCommandSet.createCommand("deactivateTouch", "Berührung deaktivieren", "", "Deactivates user touch and interactions");
        startDictationCommandSet.createCommand("deactivateTouch2", "Deactiver le toucher", "", "Deactivates user touch and interactions");

        startDictationCommandSet.createCommand("languageGerman", "Dokumentation in deutsch", "", "Enables the user to document in the German language");
        startDictationCommandSet.createCommand("languageFrench", "documentation en français", "", "Enables the user to document in the French language");
        startDictationCommandSet.createCommand("languageGerman2", "documentation en allemand", "", "Enables the user to document in the German language");
        startDictationCommandSet.createCommand("languageFrench2", "Dokumentation in französisch", "", "Enables the user to document in the French language");
        theVuiController.assignCommandSets(new CommandSet[]{startDictationCommandSet});
        theVuiController.synchronize();
    }

    /**
     * Configure parameter for Nuance.
     * - "GeneralMedicine" = Vocabulary of common medical words and phrases,
     * - "DE" = German Language.
     * - "FR" = French Language.
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

            finalText = currEditTextContent.substring(startIndex + 1);
        }

        if(isDictatingDone){
            this.ip = wsd.getDiscoveredIp();
            this.port = wsd.getDiscoveredPort();

            sendToProtocol();
            flushTextHolder();
            isDictatingDone = false;
        }
    }

    @Override
    public void onCommandRecognized(String s, String s1, String s2, HashMap<String, String> hashMap) {

        switch(s){
            case "startElias":
                spokenText.append(";");
                isDictatingActive = true;
                isDictatingDone = false;
                v.vibrate(600);
                break;
            case "stopElias1":
            case "stopElias":
                spokenText.append(":");

                if(isDictatingActive){
                    writeToHistory();
                }

                isDictatingActive = false;
                isDictatingDone = true;
                v.vibrate(300);
                break;
            case "background":
            case "background2":
                if(dayMode) {
                    background.setBackgroundColor(getResources().getColor(R.color.black));
                    dictatedText.setTextColor(getResources().getColor(R.color.darkGrey));
                    spokenText.setTextColor(getResources().getColor(R.color.white));
                    dayMode = false;
                } else {
                    background.setBackgroundColor(0x00000000);
                    dictatedText.setTextColor(getResources().getColor(R.color.black));
                    spokenText.setTextColor(getResources().getColor(R.color.darkGrey));
                    dayMode = true;
                }
                break;
            case "activateTouch":
            case "activateTouch2":
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                v.vibrate(500);
                break;
            case "deactivateTouch":
            case "deactivateTouch2":
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                v.vibrate(500);
                break;
            case "languageGerman":
            case "languageGerman2":
                theVuiController.setLanguage("de");
                theVuiController.synchronize();
                finalText = "Dokumentation in Deutsch";
                sendToProtocol();
                break;
            case "languageFrench":
            case "languageFrench2":
                theVuiController.setLanguage("fr");
                theVuiController.synchronize();
                finalText = "Dokumentation in Französisch";
                sendToProtocol();
                break;
        }
    }

    /**
     * Writes the spoken, final text to the History to stay visible for the user.
     */
    private void writeToHistory(){
        String newHistoryEntry = removeTags(finalText);

        if(!dictatedText.getText().toString().equals("")){
            String alreadyDictatedText = dictatedText.getText().toString();
            alreadyDictatedText += "\n";
            alreadyDictatedText += newHistoryEntry;
            dictatedText.setText(alreadyDictatedText);
        } else {
            dictatedText.setText(newHistoryEntry);
        }
    }

    /**
     * Removes all semicolons and points in the text which are to split the spoken text into parts.
     * @param text - the spoken, final text
     * @return cleaned text
     */
    private String removeTags(String text){
        text = text.replace(";", "");
        text = text.replace(":", "");

        return text;
    }

    /**
     * Sends the finaltext, which holds the whole dictate phrase of the user, via TCP-Connection to the ePatientenprotokoll.
     */
    private void sendToProtocol(){
        openTCPConnection(finalText);
    }

    /**
     * This method cleans the two string placeholder when the command is sent to the ePatientenprotokoll.
     */
    private void flushTextHolder(){
        spokenText.setText("");
        finalText = "";
    }


}
