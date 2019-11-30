package com.example.hsh0908y.auto_wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hsh0908y.auto_wifi.common.SsidPw;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.example.hsh0908y.auto_wifi.common.WifiData;

import java.io.File;
import java.util.List;

public class ProcessingActivity extends AppCompatActivity {

    final static String TAG = "ProcessingActivity";

    private boolean hasTriedWifiConnect = false;
    private String selectedSsid;
    private BroadcastReceiver wifiScanReceiver = null;
    private BroadcastReceiver wifiConnectReceiver = null;
    private Bitmap unscaledBitmap;
    private List<WifiData> receivedWifiDataList = null;
    private List<TextBlock> receivedTextBlockList = null;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        Intent intent = getIntent();

        String imagePath = intent.getStringExtra("imageFile");
        File imageFile = new File(imagePath);
        unscaledBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imageFile.delete();

        selectedSsid = intent.getStringExtra("selectedSsid");

        // Temporary: show image and hasId
        ImageView imageView = (ImageView) findViewById(R.id.processingImageView);
        imageView.setImageBitmap(unscaledBitmap);
        TextView textView = (TextView) findViewById(R.id.processingTextView);
        textView.setText("hasId : " + String.valueOf(selectedSsid == null));

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        wifiScanReceiver = WifiScanConnect.scanAndRegisterReceiver(this, wifiManager, new IntentFilter());
        TextDetect.detectAndRegisterCallback(this, wifiManager, unscaledBitmap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        } finally {
            try {
                unregisterReceiver(wifiConnectReceiver);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    public boolean getHasTriedWifiConnect() { return hasTriedWifiConnect; }
    public void setReceivedWifiDataList(List<WifiData> wifiDataList) {
        receivedWifiDataList = wifiDataList;
    }
    public void setReceivedTextBlockList(List<TextBlock> textBlockList) {
        receivedTextBlockList = textBlockList;
    }

    public void tryWifiConnect() {
        Log.d(TAG, String.valueOf(receivedWifiDataList == null) + "," + String.valueOf(receivedTextBlockList == null));
        if (!hasTriedWifiConnect && receivedTextBlockList != null && receivedWifiDataList != null) {
            hasTriedWifiConnect = true;
            SsidPwPick ssidPwPick = new SsidPwPick(receivedTextBlockList, receivedWifiDataList);
            SsidPw ssidPw;
            if (selectedSsid == null) {
                ssidPw = ssidPwPick.extractSsidPw();
            } else {
                String pw = ssidPwPick.extractPw();
                ssidPw = new SsidPw(selectedSsid, pw);
            }

            wifiConnectReceiver = WifiScanConnect.connectAndRegisterReceiver(this, wifiManager, new IntentFilter(), ssidPw);
        }
    }
}
