package com.bachelor.vui_ba;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;

public class WiFiCheckerService extends Service {

    public Handler handler = null;
    public static Runnable runnable = null;

    private boolean isReactivated = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        handler = new Handler();
        runnable = new Runnable() {
            public void run() {

                if(checkWiFiState() && isReactivated){
                    MainActivity.openWiFiDiscovery();
                    isReactivated = false;
                } else {
                    isReactivated = true;
                }

                handler.postDelayed(runnable, 3000);
            }
        };

        handler.postDelayed(runnable, 15000);
    }

    private boolean checkWiFiState(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
}
