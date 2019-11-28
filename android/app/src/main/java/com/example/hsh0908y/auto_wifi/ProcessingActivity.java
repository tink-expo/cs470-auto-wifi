package com.example.hsh0908y.auto_wifi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ProcessingActivity extends AppCompatActivity {
    private final String TAG = "ProcessingActivity";

    private Bitmap bitmap;
    private boolean hasId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        Intent intent = getIntent();

        String imagePath = intent.getStringExtra("imageFile");
        File imageFile = new File(imagePath);
        bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imageFile.delete();

        hasId = intent.getBooleanExtra("hasId", true);

        // Temporary: show image and hasId
        ImageView imageView = (ImageView) findViewById(R.id.processingImageView);
        imageView.setImageBitmap(bitmap);
        TextView textView = (TextView) findViewById(R.id.processingTextView);
        textView.setText(String.valueOf(hasId));
    }
}
