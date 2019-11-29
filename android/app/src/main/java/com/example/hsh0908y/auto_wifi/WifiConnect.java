package com.example.hsh0908y.auto_wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.util.List;

public class WifiConnect {
    public static boolean connectWifi(Activity activity, String id, String pw) {
        WifiManager wifi = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifi.isWifiEnabled())
            wifi.setWifiEnabled(true);

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = id;
        config.preSharedKey = pw;

        int networkId = wifi.addNetwork(config);

        boolean success = wifi.enableNetwork(networkId, true);

        return success;
    }

//    public static void ConnectAsdf() {
//        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//
//        if (!wifi.isWifiEnabled())
//            wifi.setWifiEnabled(true);
//
//        registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                List<ScanResult> results = wifi.getScanResults();
//                // Do Something
//            }
//        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
//
//        wifi.startScan();
//    }
}
