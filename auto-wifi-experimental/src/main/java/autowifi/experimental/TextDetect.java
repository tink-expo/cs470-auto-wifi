package autowifi.experimental;

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

public class TextDetect {
    public static List<TextBlock> getTextBlockListFromImage(String imagePath) {
        try {
            BatchAnnotateImagesResponse response = getBatchResponseFromImage(new File(imagePath));
            return getTextBlockListFromResponse(response);
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    public static boolean saveDetectionFromImageAll(String imageDirPath, String saveDirPath, int maxNum) {
        try {
            File[] files = new File(imageDirPath).listFiles();
            for (int index = 0; index < Math.min(maxNum, files.length); ++index) {
                File file = files[index];
                saveDetectionFromImageInternal(file, saveDirPath);
            }
            return true;
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
    }

    public static String saveDetectionFromImage(String imagePath, String saveDir) {
        try {
            return saveDetectionFromImageInternal(new File(imagePath), saveDir);
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    public static List<TextBlock> getTextBlockListFromSaved(String savePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(savePath);
            BatchAnnotateImagesResponse response = BatchAnnotateImagesResponse.parseFrom(fileInputStream);
            fileInputStream.close();

            return getTextBlockListFromResponse(response);
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    public static void printJsonFromSaved(String savePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(savePath);
            BatchAnnotateImagesResponse response = BatchAnnotateImagesResponse.parseFrom(fileInputStream);
            fileInputStream.close();

            String s = TextFormat.printToUnicodeString(response);

            JsonFormat.Printer printer = JsonFormat.printer();
            System.out.println(printer.print(response));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

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
}
