package com.example.hsh0908y.auto_wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hsh0908y.auto_wifi.common.Point;
import com.example.hsh0908y.auto_wifi.common.SsidPw;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.example.hsh0908y.auto_wifi.common.WifiData;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Page;
import com.google.api.services.vision.v1.model.Vertex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ProcessingActivity extends AppCompatActivity {

    private static final String CLOUD_VISION_API_KEY = "AIzaSyBeqND-h_JplQ_uV3_OyNTgORZLfRqMZOs";
    private static final int MAX_TEXT_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = "ProcessingActivity";

    private boolean hasId;

    private List<TextBlock> achievedTextBlockList = null;
    private List<WifiData> achievedWifiDataList = null;
    private boolean connectedTried = false;

    private static WifiManager wifiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (wifiManager == null) {
            wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }

        setContentView(R.layout.activity_processing);

        Intent intent = getIntent();

        ScanAvailableWifis();

        String imagePath = intent.getStringExtra("imageFile");
        File imageFile = new File(imagePath);
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imageFile.delete();

        hasId = intent.getBooleanExtra("hasId", true);

        // Temporary: show image and hasId
        ImageView imageView = (ImageView) findViewById(R.id.processingImageView);
        imageView.setImageBitmap(bitmap);
        TextView textView = (TextView) findViewById(R.id.processingTextView);
        textView.setText("hasId : " + String.valueOf(hasId));

        detectAndConnectWifi(bitmap);
    }

    public void ScanAvailableWifis() {
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (connectedTried) {
                    return;
                }
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                List<ScanResult> scanResultList = wifiManager.getScanResults();
                Log.d(TAG, String.valueOf(scanResultList.size()));
                WeakReference<ProcessingActivity> pActivityWeakReference =
                        new WeakReference<>((ProcessingActivity)context);

                achievedWifiDataList = new ArrayList<>();
                for (ScanResult scanResult : scanResultList) {
                    int signalLevel = wifiManager.calculateSignalLevel(scanResult.level, 5);
                    achievedWifiDataList.add(new WifiData(scanResult.SSID, signalLevel));
                    Log.d(TAG, scanResult.SSID);
                }
                if (!connectedTried && achievedTextBlockList != null) {
                    connectedTried = true;
                    boolean connectSuccess = ConnectWifi();
                    Log.d(TAG, connectSuccess ? "Connected" : "Not Connected");
                }
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiManager.startScan();
    }

    public void detectAndConnectWifi(final Bitmap unscaledBitmap) {
        if (unscaledBitmap == null) {
            Log.d(TAG, "null bitmap");
        }
        // scale the image to save on bandwidth
        Bitmap bitmap =
                scaleBitmapDown(unscaledBitmap, MAX_DIMENSION);

        callCloudVision(bitmap);
    }

    private Vision.Images.Annotate prepareAnnotationRequest(final Bitmap cameraBitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            cameraBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                textDetection.setMaxResults(MAX_TEXT_RESULTS);
                add(textDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private static class LableDetectionTask extends AsyncTask<Object, Void, BatchAnnotateImagesResponse> {
        private final WeakReference<ProcessingActivity> pActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(ProcessingActivity activity, Vision.Images.Annotate annotate) {
            pActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected BatchAnnotateImagesResponse doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                return mRequest.execute();

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return null;
        }

        protected void onPostExecute(BatchAnnotateImagesResponse response) {
            ProcessingActivity activity = pActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
                activity.achievedTextBlockList = getTextBlockListFromResponse(response);
                if (!activity.connectedTried &&
                        activity.achievedTextBlockList != null &&
                        activity.achievedWifiDataList != null) {
                    activity.connectedTried = true;
                    boolean success = activity.ConnectWifi();
                    Log.d(TAG, success ? "Connected" : "Not Connected");
                }
            }
        }
    }

    private boolean ConnectWifi() {
        // ASSERT textBlockList
        SsidPwPickTask pickTask = new SsidPwPickTask(achievedTextBlockList, achievedWifiDataList);
        SsidPw ssidPw = pickTask.extractSsidPw();
        Log.d(TAG, ssidPw.ssid + "{}{}" + ssidPw.pw);
        if (ssidPw.ssid != null && ssidPw.pw != null) {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + ssidPw.ssid + "\"";
            config.preSharedKey = "\"" + ssidPw.pw + "\"";
            Log.d(TAG, ssidPw.ssid + "{}{}" + ssidPw.pw);
            config.SSID = "\"devsisters_kaist_5\"";
            config.preSharedKey = "\"!epqm@zkdltmxm\"";
            int netId = wifiManager.addNetwork(config);

            wifiManager.disconnect();

            wifiManager.enableNetwork(netId, true);

            boolean success = wifiManager.reconnect();

            // Temporary: show ID, PW, and success
            TextView textView2 = (TextView) findViewById(R.id.processingTextView2);
            textView2.setText("success : " + String.valueOf(success));
            TextView textView3 = (TextView) findViewById(R.id.processingTextView3);
            textView3.setText("id : " + ssidPw.ssid);
            TextView textView4 = (TextView) findViewById(R.id.processingTextView4);
            textView4.setText("pw : " + ssidPw.pw);

            return success;
        }
        return false;
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, BatchAnnotateImagesResponse> textDetectionTask =
                    new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            textDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private static List<TextBlock> getTextBlockListFromResponse(BatchAnnotateImagesResponse response) {
        int imageHeight = 0;
        int imageWidth = 0;
        AnnotateImageResponse res = response.getResponses().get(0);
        try {
            Page page = res.getFullTextAnnotation().getPages().get(0);
            imageHeight = page.getHeight();
            imageWidth = page.getWidth();

            List<TextBlock> textBlockList = new ArrayList<>();
            List<EntityAnnotation> entityAnnotationList = res.getTextAnnotations();

            // Exclude the first entityAnnotation. It is the concatenation of found descriptions. (Following indices)
            for (int index = 1; index < entityAnnotationList.size(); ++index) {
                EntityAnnotation annotation = entityAnnotationList.get(index);
                Log.d(TAG, annotation.getDescription());
                List<Point> pointList = new ArrayList<>();
                for (Vertex vertex : annotation.getBoundingPoly().getVertices()) {
                    Integer vx = vertex.getX();
                    Integer vy = vertex.getY();
                    pointList.add(new Point(vx == null ? 0 : vx, vy == null ? 0 : vy));
                }
                textBlockList.add(new TextBlock(annotation.getDescription(), imageHeight, imageWidth, pointList));
            }
            Log.d(TAG, String.valueOf(textBlockList.size()));
            return textBlockList;

        } catch (IndexOutOfBoundsException | NullPointerException e) {
            Log.d(TAG, e.toString());
            return null;
        }
    }
}
