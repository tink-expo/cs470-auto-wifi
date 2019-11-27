package com.example.hsh0908y.auto_wifi;

import android.support.v7.app.AppCompatActivity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class CameraActivity extends AppCompatActivity {
    private SurfaceView surfaceView;
    private CameraPreview cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        surfaceView = findViewById(R.id.cameraSurfaceView);
        cameraPreview = new CameraPreview(this, this, Camera.CameraInfo.CAMERA_FACING_BACK, surfaceView);

        Button button = findViewById(R.id.cameraButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraPreview.takePicture();
            }
        });
    }
}
