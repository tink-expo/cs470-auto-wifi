package com.example.hsh0908y.auto_wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.example.hsh0908y.auto_wifi.common.SsidPw;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.example.hsh0908y.auto_wifi.common.WifiData;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ProcessingActivity extends AppCompatActivity {

    final static String TAG = "ProcessingActivity";
    final static int IMAGE_ANIMATE_PERIOD = 2000;

    private boolean hasTriedWifiConnect = false;
    private String selectedSsid;
    private BroadcastReceiver wifiScanReceiver = null;
    private BroadcastReceiver wifiConnectReceiver = null;
    private Bitmap unscaledBitmap;
    private List<WifiData> receivedWifiDataList = null;
    private List<TextBlock> receivedTextBlockList = null;
    private WifiManager wifiManager;
    private Timer timer;
    private WeakReference<ProcessingActivity> mWeakReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timer = new Timer();
        mWeakReference = new WeakReference<>(this);
        setContentView(R.layout.activity_processing);

        Intent intent = getIntent();

        String imagePath = intent.getStringExtra("imageFile");
        File imageFile = new File(imagePath);
        unscaledBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imageFile.delete();

        selectedSsid = intent.getStringExtra("selectedSsid");

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Log.d(TAG, String.valueOf(wifiManager.getConnectionInfo() == null));

        // Animate loading image
        final ImageView imageView = (ImageView) findViewById(R.id.processingImageView);
        final Handler handler = new Handler();
        final int[] images = new int[] {R.drawable.icon_wifi_1, R.drawable.icon_wifi_2, R.drawable.icon_wifi_3};
        final int[] currentIdx = new int[] {0};
        Runnable imageAnimateRunnable = new Runnable() {
            @Override
            public void run() {
                imageView.setImageResource(images[currentIdx[0]]);
                currentIdx[0] = (currentIdx[0] + 1) % images.length;
                handler.postDelayed(this, IMAGE_ANIMATE_PERIOD);
            }
        };
        handler.postDelayed(imageAnimateRunnable, IMAGE_ANIMATE_PERIOD);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        wifiScanReceiver = WifiScanConnect.scanAndRegisterReceiver(this, wifiManager, new IntentFilter(),
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        ProcessingActivity activity = ProcessingActivity.this;
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
                });
        TextDetect.detectAndRegisterCallback(this, unscaledBitmap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();

        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        }

        try {
            unregisterReceiver(wifiConnectReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public boolean getHasTriedWifiConnect() { return hasTriedWifiConnect; }
    public List<WifiData> getReceivedWifiDataList() { return receivedWifiDataList; }
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
            final SsidPw ssidPw = getSsidPw();

            wifiConnectReceiver = WifiScanConnect.connectAndRegisterReceiver(this, wifiManager, new IntentFilter(), ssidPw, timer);
        }
    }

    private SsidPw getSsidPw() {
        SsidPwPick ssidPwPick = new SsidPwPick(receivedTextBlockList, receivedWifiDataList);
        if (selectedSsid == null) {
            return ssidPwPick.extractSsidPw();
        } else {
            String pw = ssidPwPick.extractPw();
            return new SsidPw(selectedSsid, pw);
        }
    }
}
