package com.example.hsh0908y.auto_wifi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.example.hsh0908y.auto_wifi.common.Point;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

// Reference code : https://github.com/GoogleCloudPlatform/cloud-vision/tree/master/android/CloudVision

public class TextDetect {

    private static final String CLOUD_VISION_API_KEY = BuildConfig.API_KEY;
    private static final int MAX_TEXT_RESULTS = 20;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = "TextDetect";

    public static void detectAndRegisterCallback(
            ProcessingActivity activity, final Bitmap unscaledBitmap) {
        if (unscaledBitmap == null) {
            return;
        }
        // scale the image to save on bandwidth
        Bitmap bitmap =
                scaleBitmapDown(unscaledBitmap, MAX_DIMENSION);

        callCloudVision(activity, bitmap);
    }

    static private boolean callCloudVision(
            final ProcessingActivity activity, final Bitmap bitmap) {
        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, BatchAnnotateImagesResponse> textDetectionTask =
                    new LableDetectionTask(activity, prepareAnnotationRequest(bitmap));
            textDetectionTask.execute();
            return true;
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
            return false;
        }
    }

    static private Vision.Images.Annotate prepareAnnotationRequest(final Bitmap cameraBitmap) throws IOException {
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
            if (activity == null || activity.isFinishing() || activity.getHasTriedWifiConnect()) {
                return;
            }
            List<TextBlock> textBlockList = getTextBlockListFromResponse(response);
            if (textBlockList != null) {
                activity.setReceivedTextBlockList(textBlockList);
                activity.tryWifiConnect();
            } else {
                Intent retryIntent = new Intent(activity, CameraActivity.class);
                activity.startActivity(retryIntent);
            }
        }
    }

    private static Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

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
        try {
            AnnotateImageResponse res = response.getResponses().get(0);
            Page page = res.getFullTextAnnotation().getPages().get(0);
            int imageHeight = page.getHeight();
            int imageWidth = page.getWidth();

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
