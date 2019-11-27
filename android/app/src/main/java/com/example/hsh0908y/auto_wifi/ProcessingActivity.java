package com.example.hsh0908y.auto_wifi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.File;

public class ProcessingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("imageFile");
        File imageFile = new File(imagePath);
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

        imageFile.delete();

        // Temporary: show image
        ImageView imageView = (ImageView) findViewById(R.id.processingImageView);
        imageView.setImageBitmap(bitmap);
    }
}
