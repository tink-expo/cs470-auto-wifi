package autowifi.experimental;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Experiment {
    public static void main(String[] args) {
        String imageDir = "/Users/woohyunhan/Documents/1-courses-19fall/ai-cs470/term-project/dataset-wifi-images";
        String saveDir = "/Users/woohyunhan/Desktop/output-test";

    }

    static void testPicker(String saveDir, String imageFileName, String ssid, String[] wrongSsids) {
        String savePath = Paths.get(saveDir, imageFileName + ".save").toString();
        List<TextBlock> textBlockList = TextDetect.getTextBlockListFromSaved(savePath);

        System.out.println("<API OCR>");
        for (TextBlock block : textBlockList) {
            System.out.print(block.getDescription() + "     ");
            for (Point point : block.getNormalizedPointList()) {
                System.out.printf("(%f, %f) ", point.x, point.y);
            }
            System.out.println();
        }
        System.out.println();

        List<WifiInfo> wifis = new ArrayList<>();
        for (String id : wrongSsids) {
            wifis.add(new WifiInfo(id, 2));
        }
        wifis.add(new WifiInfo(ssid, 1));
        SsidPwPicker picker = new SsidPwPicker(textBlockList, wifis);
        SsidPw ssidPw = picker.ExtractSsidPw();
        System.out.println(ssidPw.ssid);
        System.out.println(ssidPw.pw);
    }

    static void RequestAndReadSingle(String imageDir, String saveDir, String imageFileName) {
        String imagePath = Paths.get(imageDir, imageFileName).toString();
        String savePath = TextDetect.saveDetectionFromImage(imagePath, saveDir);

        List<TextBlock> textBlockList = TextDetect.getTextBlockListFromSaved(savePath);
        for (TextBlock block : textBlockList) {
            System.out.println(block.getDescription());
//            for (TextBlock.Point point : block.getNormalizedPointList()) {
//                System.out.printf("(%f, %f) ", point.x, point.y);
//            }
//            System.out.println();
        }
    }

    static void ReadAll(String saveDir) {
        File dir = new File(saveDir);
        File[] files = dir.listFiles();
        List<File> fileList = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".save")) {
                fileList.add(file);
            }
        }
        fileList.sort(new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                String prefix1 = (file1.getName().split("\\."))[0];
                String prefix2 = (file2.getName().split("\\."))[0];
                int parsed1 = Integer.parseInt(prefix1);
                int parsed2 = Integer.parseInt(prefix2);
                return Integer.parseInt(prefix1) - Integer.parseInt(prefix2);
            }
        });
        for (File file : fileList) {
            System.out.println("[" + file.getName() + "]");
            List<TextBlock> textBlockList = TextDetect.getTextBlockListFromSaved(file.getPath());
            for (TextBlock block : textBlockList) {
                System.out.print(block.getDescription() + "     ");
                for (Point point : block.getNormalizedPointList()) {
                    System.out.printf("(%f, %f) ", point.x, point.y);
                }
                System.out.println();
            }
            SsidPwPicker picker = new SsidPwPicker(textBlockList, new ArrayList<>());
            System.out.println();
        }
    }

    static void ReadTextFormatSingle(String imageDir, String imageFileName) {
        String filePath = Paths.get(imageDir, imageFileName).toString();
        List<TextBlock> textBlockList = TextDetect.getTextBlockListFromSaved(filePath);

    }
}
