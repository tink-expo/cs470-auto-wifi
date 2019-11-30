package com.example.hsh0908y.auto_wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.hsh0908y.auto_wifi.common.SsidPw;
import com.example.hsh0908y.auto_wifi.common.WifiData;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiScanConnect {
    final static String TAG = "WifiScanConnect";

    public static BroadcastReceiver scanAndRegisterReceiver(
            final Activity activity, final WifiManager wifiManager, IntentFilter intentFilter, BroadcastReceiver scanReceiver) {
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        intentFilter.addAction(wifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        activity.registerReceiver(scanReceiver, intentFilter);
        wifiManager.startScan();

        return scanReceiver;
    }

    public static BroadcastReceiver connectAndRegisterReceiver(
            final AppCompatActivity activity, final WifiManager wifiManager,
            IntentFilter intentFilter, final SsidPw ssidPw, Timer timer) {
        if (ssidPw == null || ssidPw.ssid == null) {
            return null;
        }

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        final WifiConfiguration config = new WifiConfiguration();

        config.SSID = "\"" + ssidPw.ssid + "\"";
        if (ssidPw.pw != null) {
            config.preSharedKey = "\"" + ssidPw.pw + "\"";
        }

//        config.SSID = "\"" + "SPARCS-AP" + "\"";
//        config.preSharedKey = "\"" + "tnfqkrtmtnfqkrtm" + "\"";
        Log.d(TAG, config.SSID + " " + config.preSharedKey);

        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

        final int netId = wifiManager.addNetwork(config);
        wifiManager.disconnect();

        BroadcastReceiver connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(TAG, "ConnectReceiver");

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                        networkInfo.isConnected() && wifiManager.getConnectionInfo().getSSID().equals(config.SSID)) {
                    Intent successIntent = new Intent(activity, SuccessActivity.class);
                    successIntent.putExtra("id", wifiManager.getConnectionInfo().getSSID());
                    activity.startActivity(successIntent);
                }
            }
        };

        activity.registerReceiver(connectReceiver, intentFilter);
        wifiManager.enableNetwork(netId, true);
        // wifiManager.reconnect();

        Log.d(TAG, "Start timer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "Run timer");
                if (activity != null && !activity.isFinishing()) {
                    Intent failIntent = new Intent(activity, FailActivity.class);
                    failIntent.putExtra("id", ssidPw.ssid);
                    failIntent.putExtra("pw", ssidPw.pw);
                    activity.startActivity(failIntent);
                }
            }
        }, 3500);

        return connectReceiver;
    }
}
