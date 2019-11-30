package com.example.hsh0908y.auto_wifi;

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

public class WifiScanConnect {
    final static String TAG = "WifiScanConnect";

    public static BroadcastReceiver scanAndRegisterReceiver(
            final ProcessingActivity activity, final WifiManager wifiManager, IntentFilter intentFilter) {
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        BroadcastReceiver scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (activity == null || activity.isFinishing() || activity.getHasTriedWifiConnect()) {
                    return;
                }

                List<ScanResult> scanResultList = wifiManager.getScanResults();
                List<WifiData> wifiDataList = new ArrayList<>();
                for (ScanResult scanResult : scanResultList) {
                    int signalLevel = wifiManager.calculateSignalLevel(scanResult.level, 5);
                    wifiDataList.add(new WifiData(scanResult.SSID, signalLevel));
                    Log.d(TAG, scanResult.SSID + ", " + String.valueOf(signalLevel));
                }
                activity.setReceivedWifiDataList(wifiDataList);
                activity.tryWifiConnect();
            }
        };

        intentFilter.addAction(wifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        activity.registerReceiver(scanReceiver, intentFilter);
        wifiManager.startScan();

        return scanReceiver;
    }

    public static BroadcastReceiver connectAndRegisterReceiver(
            final AppCompatActivity activity, final WifiManager wifiManager,
            IntentFilter intentFilter, final SsidPw ssidPw) {
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

        BroadcastReceiver connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                        networkInfo.isConnected() && wifiManager.getConnectionInfo().getSSID().equals(config.SSID)) {
                    Intent successIntent = new Intent(activity, SuccessActivity.class);
                    successIntent.putExtra("id", wifiManager.getConnectionInfo().getSSID());
                    activity.startActivity(successIntent);
                } else {
                    if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                        if (intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)) {
                            Log.d(TAG, String.valueOf(intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1)));
                            Intent failIntent = new Intent(activity, FailActivity.class);
                            failIntent.putExtra("id", ssidPw.ssid);
                            failIntent.putExtra("pw", ssidPw.pw);
                            activity.startActivity(failIntent);
                        }
                    }
                }
            }
        };

        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);



        int netId = wifiManager.addNetwork(config);
        // wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        // wifiManager.reconnect();
        return connectReceiver;
    }
}
