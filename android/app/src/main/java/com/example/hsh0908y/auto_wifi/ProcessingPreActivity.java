package com.example.hsh0908y.auto_wifi;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ProcessingPreActivity extends AppCompatActivity {
    static final int REQUEST_CODE = 0;

    private boolean hasId;
    private String imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing_pre);

        Intent intent = getIntent();
        hasId = intent.getBooleanExtra("hasId", false);
        imageFile = intent.getStringExtra("imageFile");


        Intent selectIdIntent = new Intent(this, SelectIdActivity.class);
        startActivityForResult(selectIdIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (!(requestCode == REQUEST_CODE && resultCode == RESULT_OK)) {
            return;
        }

        String selectedSsid = intent.getStringExtra("id");

        Intent processingIntent = new Intent(this, ProcessingActivity.class);
        processingIntent.putExtra("imageFile", imageFile);
        processingIntent.putExtra("hasId", hasId);
        processingIntent.putExtra("selectedSsid", selectedSsid);
        this.startActivity(processingIntent);
    }
}
