package com.example.hsh0908y.auto_wifi;

import com.example.hsh0908y.auto_wifi.common.Point;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class TextDetect {

    static List<TextBlock> getTextBlockListFromSaved(String savePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(savePath);
            BatchAnnotateImagesResponse response = BatchAnnotateImagesResponse.parseFrom(fileInputStream);
            fileInputStream.close();

            return getTextBlockListFromResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static String getJsonFromSaved(String savePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(savePath);
            BatchAnnotateImagesResponse response = BatchAnnotateImagesResponse.parseFrom(fileInputStream);
            fileInputStream.close();

            String s = TextFormat.printToUnicodeString(response);

            JsonFormat.Printer printer = JsonFormat.printer();
            return printer.print(response);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static List<TextBlock> getTextBlockListFromResponse(BatchAnnotateImagesResponse response) {
        int imageHeight = 0;
        int imageWidth = 0;
        AnnotateImageResponse res = response.getResponsesList().get(0);
        try {
            Page page = res.getFullTextAnnotation().getPagesList().get(0);
            imageHeight = page.getHeight();
            imageWidth = page.getWidth();

            List<TextBlock> textBlockList = new ArrayList<>();
            List<EntityAnnotation> entityAnnotationList = res.getTextAnnotationsList();

            // Exclude the first entityAnnotation. It is the concatenation of found descriptions. (Following indices)
            for (int index = 1; index < entityAnnotationList.size(); ++index) {
                EntityAnnotation annotation = entityAnnotationList.get(index);
                List<Point> pointList = new ArrayList<>();
                for (Vertex vertex : annotation.getBoundingPoly().getVerticesList()) {
                    pointList.add(new Point(vertex.getX(), vertex.getY()));
                }
                textBlockList.add(new TextBlock(annotation.getDescription(), imageHeight, imageWidth, pointList));
            }
            return textBlockList;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }
    }

    // [NOTE] Following methods can't be run on current Android studio project dependencies
    // Begin ---
    static List<TextBlock> getTextBlockListFromImage(String imagePath) {
        try {
            BatchAnnotateImagesResponse response = getBatchResponseFromImage(new File(imagePath));
            return getTextBlockListFromResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static boolean saveDetectionFromImageAll(String imageDirPath, String saveDirPath, int maxNum) {
        try {
            File[] files = new File(imageDirPath).listFiles();
            for (int index = 0; index < Math.min(maxNum, files.length); ++index) {
                File file = files[index];
                saveDetectionFromImageInternal(file, saveDirPath);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    static String saveDetectionFromImage(String imagePath, String saveDir) {
        try {
            return saveDetectionFromImageInternal(new File(imagePath), saveDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Returns saved file path
    private static String saveDetectionFromImageInternal(File imageFile, String saveDir) throws IOException {
        BatchAnnotateImagesResponse response = getBatchResponseFromImage(imageFile);

        String savePath = Paths.get(saveDir, imageFile.getName() + ".save").toString();
        FileOutputStream fileOutputStream = new FileOutputStream(savePath);
        response.writeTo(fileOutputStream);
        fileOutputStream.close();

        return savePath;
    }

    private static BatchAnnotateImagesResponse getBatchResponseFromImage(File imageFile) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(imageFile);
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(fileInputStream);
        fileInputStream.close();

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        ImageAnnotatorClient client = ImageAnnotatorClient.create();
        BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
        client.close();
        return response;
    }

    // End --- Can't be run
}

