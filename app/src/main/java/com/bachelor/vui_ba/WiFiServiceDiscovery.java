package com.bachelor.vui_ba;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

/**
 * This Class represents the WiFiServiceDiscovery functionality. It is used to discover the service
 * from the ePatienteprotokoll in the local WLAN. If a service with the Name "_speechreceiver._tcp"
 * is found a connection with the opponents port and ip address is resolved.
 */
public class WiFiServiceDiscovery {

    //Common
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mServiceInfo;
    private String discoveredIp;
    private int discoveredPort = 0;

    //Context
    private Context context;

    //Const
    private static final String SERVICE_TYPE = "_nuance._tcp.";

    public WiFiServiceDiscovery(Context ctx){
        this.context = ctx;

        initializeResolveListener();
        initializeDiscoveryListener();

        if (nsdManager == null)
            nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    /**
     * Initializes the DiscoveryListener. This Listener checks the WLAN for open services from other
     * devices.
     */
    private void initializeDiscoveryListener(){
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String s, int i) {
                System.out.println("WiFiServiceDiscovery: onStartDiscovery failed");
            }

            @Override
            public void onStopDiscoveryFailed(String s, int i) {
                System.out.println("WiFiServiceDiscovery: onStopDiscovery failed");
            }

            @Override
            public void onDiscoveryStarted(String s) {
                System.out.println("WiFiServiceDiscovery: Discovery started");
            }

            @Override
            public void onDiscoveryStopped(String s) {
                System.out.println("WiFiServiceDiscovery: Discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                System.out.println("WiFiServiceDiscovery: Serivce found");
                //if(nsdServiceInfo.getServiceType().equals(SERVICE_TYPE)){
                    nsdManager.resolveService(nsdServiceInfo, mResolveListener);
                //}
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                System.out.println("WiFiServiceDiscovery: Service lost");
            }
        };
    }

    /**
     * Initializes the ResolveListener. If an open Service is found (onServiceFound() from DiscoveryListener)
     * the onServiceResolved() method is fired. It includes the opponents ip address and the port to use
     * for future data transactions.
     */
    private void initializeResolveListener(){
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                System.out.println("WiFiServiceDiscovery: resolve failed");
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                mServiceInfo = nsdServiceInfo;
                discoveredPort = mServiceInfo.getPort();
                discoveredIp = mServiceInfo.getHost().toString().split("/")[1];

                System.out.println("WiFiServiceDiscovery: discoveredPort " + discoveredPort);
                System.out.println("WiFiServiceDiscovery: discoveredIp " + discoveredIp);
            }
        };
    }

    /**
     * Returns the discovered port.
     * @return port - int
     */
    public int getDiscoveredPort(){
        return discoveredPort;
    }

    /**
     * Returns the discovered IP.
     * @return IP - String
     */
    public String getDiscoveredIp(){
        return discoveredIp;
    }
}
