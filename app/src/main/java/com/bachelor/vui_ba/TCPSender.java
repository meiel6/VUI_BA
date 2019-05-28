package com.bachelor.vui_ba;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * This Class is used to send TCP-Messages to the ePatientenprotokoll. It also logs all the commands
 * to two different log files (Standard and Backup). If no connection is available to the
 * ePatienteprotokoll, the VUI logs all commands in the backup-log. If the connection is active
 * again, the delta between backup-log and standard-log (with all the commands) will be send to the
 * ePatienteprotokoll. Additionally, it creates the Json-String (Payload to the ePatientenprotokoll) in the correct format.
 */
public class TCPSender extends AsyncTask<String, Void, String> {

    //Connection
    private Socket s;
    private DataOutputStream dos;
    private static int currentId = 0;
    private Context context;
    private static boolean wasDisconnected = false;

    //Logging
    private File appDirectory;
    private File backupLog;
    private File standardLog;
    private FileOutputStream fos;

    //Payload
    private String spokenText = "";

    public TCPSender(Socket socket, Context ctx){
        this.s = socket;
        this.context = ctx;

        initLoggingDirectories();
    }

    public TCPSender(Context ctx){
        this.context = ctx;
        initLoggingDirectories();
    }

    /**
     * Initializes the two log-files "backup" and "standard" for logging the spoken commands.
     */
    private void initLoggingDirectories(){
        appDirectory = context.getFilesDir();
        backupLog = new File(appDirectory, "backupLog.txt");
        standardLog = new File(appDirectory, "standardLog.txt");
    }

    @Override
    protected String doInBackground(String... strings) {

        String payload = getPayload();

        try {
            dos = new DataOutputStream(s.getOutputStream());

            if(backupLog.length() != 0){

                if(wasDisconnected){
                    MainActivity.openWiFiDiscovery();
                }

                String[] logList = readBackupLog();

                for(int i = 0; i <= logList.length - 1; ++i){
                    String backupPayload = invertLogStringToJson(logList[i]);
                    dos.writeUTF(backupPayload);
                    writeToStandardLogFile(backupPayload);
                }
                dos.writeUTF(payload);
                clearBackUpLog();
            } else {
                dos.writeUTF(payload);
                writeToStandardLogFile(payload);
            }

            dos.flush();
            dos.close();
            s.close();

            return payload;

        } catch (Exception e) {
            writeToBackupLogFile(payload);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String payload) {
        super.onPostExecute(payload);
    }

    /**
     * Sets the text, spoken by the user, to the variable "spokenText" to use it in this Class.
     * @param text
     */
    public void setSpokenText(String text){
        spokenText = text;
    }

    /**
     * This method returns the Json Object in a string representation.
     * @return String of the Json Object
     */
    public String getPayload(){
        return createJson().toString();
    }

    /**
     * Creates the Json Object with the payload.
     *
     * id       = unique identifier
     * ts       = timestamp in ms
     * comp     = the topic of the following payload
     * payload  = spoken text
     *
     * @return JsonObject as Payload
     */
    private JsonObject createJson(){

        spokenText = spokenText.toLowerCase();

        JsonObject json = new JsonObject();
        json.addProperty("id", currentId);
        json.addProperty("ts", System.currentTimeMillis());
        json.addProperty("comp", getComponent() != null ? getComponent() : "");
        json.addProperty("payload", spokenText);

        return json;
    }

    /**
     * This method is used to notify the ePatientenprotokoll in which fragment the spokenText should appear.
     * @return the component string
     */
    private String getComponent(){
        String component = null;

        if(spokenText.contains("anamnese") || spokenText.contains("anamnèse")) {
            component = "Anamnese";
        } else if(spokenText.contains("gcs") || spokenText.contains("glasgow") || spokenText.contains("coma")) {
            component = "GCS";
        } else if(spokenText.contains("puls") || spokenText.contains("pouls") || spokenText.contains("battement")) {
            component = "Puls";
        } else if(spokenText.contains("blutdruck") || spokenText.contains("tension artérielle") || spokenText.contains("artérielle")) {
            component = "Blutdruck";
        } else if(spokenText.contains("medikament") || spokenText.contains("drogue") || spokenText.contains("médicament")
                || spokenText.contains("adrenalin") || spokenText.contains("adrénaline")
                || spokenText.contains("glukose") || spokenText.contains("glucose")
                || spokenText.contains("fentanyl")) {
            component = "Medikament";
        } else if(spokenText.contains("dokumentation") || spokenText.contains("documentation")){
            component = "Dokumentation";
        }

        return component;
    }

    /**
     * Increments the command Id. Identifies the Command.
     */
    private void incrementId(){
        currentId++;
    }

    /**
     * Writes the normal "standard" log. EVERY command that is created by the user is logged
     * in this file.
     *
     * @param payload - the spoken text
     */
    private void writeToStandardLogFile(String payload){
        try {
            fos = new FileOutputStream(standardLog, true);
            incrementId();
            fos.write(buildLogString(payload).getBytes());
            fos.write("\r\n".getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the backup log. EVERY command that is created AFTER OR DURING a connection issue is logged
     * in this file.
     *
     * @param payload - the spoken text
     */
    public void writeToBackupLogFile(String payload){
        try {
            wasDisconnected = true;

            fos = new FileOutputStream(backupLog, true);
            incrementId();
            fos.write(buildLogString(payload).getBytes());
            fos.write("\r\n".getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds the log string. The log string follows the following pattern:
     * ID | TIMESTAMP | COMPONENT (ex. ANAMNESE) | PAYLOAD (spoken text)
     *
     * @param payload - the spoken text
     * @return Log String
     */
    private String buildLogString(String payload){
        JsonObject json = getJson(payload);
        String logString;

        logString = json.get("id").getAsString() + "|"
                + json.get("ts").getAsString() + "|"
                + json.get("comp").getAsString() + "|"
                + json.get("payload").getAsString() + "|";

        logString = logString.trim();

        return logString;
    }

    /**
     * Creates a Json Object from the payload.
     *
     * @param payload - the spoken text
     * @return JsonObject
     */
    private JsonObject getJson(String payload){
        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(payload);
    }

    /**
     * Read all lines from the backup.txt file on the internal storage. The Variable logs contains
     * all logs who were in the backup.txt file after executing this method.
     *
     * @return String[] with log lines
     */
    private String[] readBackupLog(){
        StringBuilder logs = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(backupLog));
            String line;

            while ((line = br.readLine()) != null) {
                logs.append(line);
                logs.append('\n');
            }
            br.close();

            return logs.toString().split("\n");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This method inverts the LogString from the XX|XX|XX|XX-Form in a Json-Form and converts it
     * to a string.
     *
     * @param log - one row of the backup log
     * @return Log in json as a String
     */
    private String invertLogStringToJson(String log){
        JsonObject json = new JsonObject();
        String[] logParts = log.split("\\|");

        json.addProperty("id", logParts[0]);
        json.addProperty("ts", logParts[1]);
        json.addProperty("comp", logParts[2]);
        json.addProperty("payload", logParts[3]);

        return json.toString();
    }

    /**
     * Deletes all entries from the backup file.
     */
    public void clearBackUpLog(){
        try {
            new PrintWriter(backupLog).close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
