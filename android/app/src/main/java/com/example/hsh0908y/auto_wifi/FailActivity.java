package com.example.hsh0908y.auto_wifi;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail);

        Intent intent = getIntent();

        String id = intent.getStringExtra("id");
        TextView idTextView = (TextView) findViewById(R.id.idTextView);
        idTextView.setText(id);

        String pw = intent.getStringExtra("pw");
        TextView pwTextView = (TextView) findViewById(R.id.pwTextView);
        pwTextView.setText(pw);


        Button idButton = (Button) findViewById(R.id.idButton);
        idButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        Button pwButton = (Button) findViewById(R.id.pwButton);
        pwButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }
}
