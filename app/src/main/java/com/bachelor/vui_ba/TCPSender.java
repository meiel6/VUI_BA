package com.bachelor.vui_ba;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This Class is used to send TCP-Messages to the ePatientenprotokoll.
 */
public class TCPSender extends AsyncTask<String, Void, String> {

    //Connection
    private Socket s;
    private DataOutputStream dos;
    private String ip_address;
    private int port;
    private static int currentId = 0;
    private Context context;

    //Logging
    private File appDirectory;
    private File backupLog;
    private File standardLog;
    private FileOutputStream fos;

    //Payload
    private String spokenText = "";

    public TCPSender(String ip, int port, Context ctx){
        this.ip_address = ip;
        this.port = port;
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
        try {
            s = new Socket(ip_address, port);
            dos = new DataOutputStream(s.getOutputStream());

            String payload = getPayload();
            dos.writeUTF(payload);

            dos.close();
            s.close();

            incrementId();

            return payload;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String payload) {
        super.onPostExecute(payload);
        writeToStandardLogFile(payload);
    }

    public void setSpokenText(String text){
        spokenText = text;
    }

    /**
     * This method returns the Json Object in a string representation.
     * @return String of the Json Object
     */
    private String getPayload(){
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

        if(spokenText.contains("Anamnese")) {
            component = "Anamnese";
        } else if(spokenText.contains("GCS") || spokenText.contains("Glasgow") || spokenText.contains("Coma")) {        //maybe there are some additions needed
            component = "GCS";
        } else if(spokenText.contains("Lagerung")) {
            component = "Lagerung";
        } else if(spokenText.contains("Puls")) {
            component = "Puls";
        } else if(spokenText.contains("Blutdruck")) {
            component = "Blutdruck";
        } else if(spokenText.contains("Medikament") || spokenText.contains("Adrenalin")
                || spokenText.contains("Glukose") || spokenText.contains("Glucose")
                || spokenText.contains("Fentanyl")){
            component = "Medikament";
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
}
