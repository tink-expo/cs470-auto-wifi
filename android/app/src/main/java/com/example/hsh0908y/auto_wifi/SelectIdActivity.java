package com.example.hsh0908y.auto_wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.hsh0908y.auto_wifi.common.WifiData;

import java.util.ArrayList;
import java.util.List;

public class SelectIdActivity extends AppCompatActivity {

    final static String TAG = "SelectIdActivity";

    private BroadcastReceiver wifiScanReceiver;
    private WifiManager wifiManager;

    private String[] idList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_id);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        idList = new String[]{};

        ListView listView = (ListView) findViewById(R.id.selectIdListView);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, idList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemId = (String) parent.getItemAtPosition(position);
                Log.d(TAG, itemId);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        wifiScanReceiver = WifiScanConnect.scanAndRegisterReceiver(this, wifiManager, new IntentFilter(),
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        List<ScanResult> scanResultList = wifiManager.getScanResults();
                        ArrayList<String> resultIdList = new ArrayList<>();
                        for (ScanResult scanResult : scanResultList) {
                            resultIdList.add(scanResult.SSID);
                        }
                        idList = resultIdList.toArray(new String[resultIdList.size()]);
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        }
    }
}
