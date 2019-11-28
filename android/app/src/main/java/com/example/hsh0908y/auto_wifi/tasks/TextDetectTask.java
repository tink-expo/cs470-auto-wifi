package com.example.hsh0908y.auto_wifi.tasks;

import android.graphics.Bitmap;

import com.example.hsh0908y.auto_wifi.common.Point;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Page;
import com.google.api.services.vision.v1.model.Vertex;

import java.util.ArrayList;
import java.util.List;

public class TextDetectTask {
    
    public List<TextBlock> getTextBlockListFromImage(Bitmap bitmap) {
        // Call GCP API and get BatchAnnotateImagesResponse.

        // Temporary
        return new ArrayList<>();
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
                List<Point> pointList = new ArrayList<>();
                for (Vertex vertex : annotation.getBoundingPoly().getVertices()) {
                    pointList.add(new Point(vertex.getX(), vertex.getY()));
                }
                textBlockList.add(new TextBlock(annotation.getDescription(), imageHeight, imageWidth, pointList));
            }
            return textBlockList;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }
    }
}
