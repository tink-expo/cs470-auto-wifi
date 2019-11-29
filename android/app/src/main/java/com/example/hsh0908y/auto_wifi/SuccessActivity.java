package com.example.hsh0908y.auto_wifi;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class SuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        Intent intent = getIntent();

        String id = intent.getStringExtra("id");
        TextView textView = (TextView) findViewById(R.id.successTextView);
        textView.setText(id);
    }
}
