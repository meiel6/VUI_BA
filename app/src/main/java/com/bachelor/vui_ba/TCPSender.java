package com.bachelor.vui_ba;

import android.os.AsyncTask;

import com.google.gson.JsonObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This Class is used to send TCP-Messages to the ePatientenprotokoll.
 */
public class TCPSender extends AsyncTask<String, Void, Void> {

    //Connection
    private Socket s;
    private DataOutputStream dos;
    private String ip_address;
    private int port;

    //Payload
    private String spokenText = "";

    public TCPSender(String ip, int port){
        this.ip_address = ip;
        this.port = port;
    }

    @Override
    protected Void doInBackground(String... strings) {
        try {

            s = new Socket(ip_address, port);
            dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(getPayload());

            dos.close();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
        json.addProperty("id", 1);
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
}
