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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

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

        // Temporary: show image and hasId
        ImageView imageView = (ImageView) findViewById(R.id.processingImageView);
        imageView.setImageBitmap(unscaledBitmap);
        TextView textView = (TextView) findViewById(R.id.processingTextView);
        textView.setText("hasId : " + String.valueOf(selectedSsid == null));

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Log.d(TAG, String.valueOf(wifiManager.getConnectionInfo() == null));
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

            wifiConnectReceiver = WifiScanConnect.connectAndRegisterReceiver(this, wifiManager, new IntentFilter(), ssidPw);
            Log.d(TAG, "Start timer");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d(TAG, "Run timer");
                    ProcessingActivity activity = mWeakReference.get();
                    if (activity != null && !activity.isFinishing()) {
                        Intent failIntent = new Intent(activity, FailActivity.class);
                        failIntent.putExtra("id", ssidPw.ssid);
                        failIntent.putExtra("pw", ssidPw.pw);
                        startActivity(failIntent);
                    }
                }
            }, 3500);
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
