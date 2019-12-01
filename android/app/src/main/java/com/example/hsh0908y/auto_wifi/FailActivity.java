package com.example.hsh0908y.auto_wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.hsh0908y.auto_wifi.common.SsidPw;

import java.util.Timer;

public class FailActivity extends AppCompatActivity {
    static final int REQUEST_CODE = 0;
    static final String TAG = "FailActivity";

    private String recognizedId;
    private String recognizedPw;

    private String currentId;
    private String currentPw;

    private Timer timer = null;
    private BroadcastReceiver wifiConnectReceiver = null;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail);

        Intent intent = getIntent();

        recognizedId = intent.getStringExtra("id");
        setCurrentId(recognizedId);

        recognizedPw = intent.getStringExtra("pw");
        setCurrentPw(recognizedPw);


        Button idButton = (Button) findViewById(R.id.idButton);
        idButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FailActivity.this, SelectIdActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        Button pwButton = (Button) findViewById(R.id.pwButton);
        pwButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(FailActivity.this);
                dialogBuilder.setTitle("Password");
                final EditText input = new EditText(FailActivity.this);
                input.setText(currentPw);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                dialogBuilder.setView(input);
                dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setCurrentPw(input.getText().toString());
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialogBuilder.show();
            }
        });

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Button retryButton = (Button) findViewById(R.id.retryButton);
        retryButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer = new Timer();
                wifiConnectReceiver = WifiScanConnect.connectAndRegisterReceiver(FailActivity.this, wifiManager, new IntentFilter(), new SsidPw(currentId, currentPw), timer);
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (timer != null) {
            timer.cancel();
        }

        try {
            unregisterReceiver(wifiConnectReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            String id = intent.getStringExtra("id");
            setCurrentId(id);
        }
    }

    private void setCurrentId(String id) {
        currentId = id;
        TextView idTextView = (TextView) findViewById(R.id.idTextView);
        idTextView.setText(currentId);
    }

    private void setCurrentPw(String pw) {
        currentPw = pw;
        TextView pwTextView = (TextView) findViewById(R.id.pwTextView);
        pwTextView.setText(currentPw);
    }
}
