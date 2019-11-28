package com.example.hsh0908y.auto_wifi;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

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
}
