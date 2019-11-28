package com.example.hsh0908y.auto_wifi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ProcessingActivity extends AppCompatActivity {
    private final String TAG = "ProcessingActivity";

    private Bitmap bitmap;
    private boolean hasId;

    private RecognizeTask recognizeTask;

    private String id;
    private String pw;

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
        textView.setText("hasId : " + String.valueOf(hasId));

        recognizeTask = new RecognizeTask(this);
        recognizeTask.execute();
    }

    static class RecognizeTask extends AsyncTask<Integer , Integer , String[]> {
        private ProcessingActivity mActivity;

        public RecognizeTask(ProcessingActivity activity) {
            mActivity = activity;
        }

        @Override
        protected String[] doInBackground(Integer ... values) {

            // To simulate processing time. TODO:Remove it later
            try {
                Thread.sleep(3000);
            }
            catch (java.lang.InterruptedException e) {
            }

            // TODO: implement recognizing ID and PW from image
            String id = "TEMPORARY_ID";
            String pw = "TEMPORARY_PW";

            return new String[]{id, pw};
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);

            mActivity.id = result[0];
            mActivity.pw = result[1];

            mActivity.connectWifi();
        }
    }

    private void connectWifi() {
        boolean success = WifiConnect.connectWifi(this, id, pw);

        // Temporary: show ID, PW, and success
        TextView textView2 = (TextView) findViewById(R.id.processingTextView2);
        textView2.setText("success : " + String.valueOf(success));
        TextView textView3 = (TextView) findViewById(R.id.processingTextView3);
        textView3.setText("id : " + id);
        TextView textView4 = (TextView) findViewById(R.id.processingTextView4);
        textView4.setText("pw : " + pw);
    }
}
