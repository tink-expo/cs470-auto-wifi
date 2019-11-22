import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class IdPwPickerTester {
    public static void main(String[] args) {
        String json = null;
        try {
            json = readFileToSingleString(args[0], Charset.defaultCharset());
        } catch (IOException e) {
            return;
        }

        GcpJsonUtil gcpJsonUtil = new GcpJsonUtil(json);
        int imageWidth = gcpJsonUtil.GetImageWidth();
        int imageHeight = gcpJsonUtil.GetImageHeight();
        TextAnnotation[] annotations = gcpJsonUtil.GetTextAnnotations();
        if (imageWidth == 0 || imageHeight == 0 || annotations == null) {
            return;
        }
        IdPwPicker idPwPicker = new IdPwPicker(annotations, imageWidth, imageHeight);

        List<String> wifiSsids = null;
        try {
            wifiSsids = readFileLines(args[1], Charset.defaultCharset());
        } catch (IOException e) {
            return;
        }
        for (String ssid : wifiSsids) {
            idPwPicker.AddOrUpdateWifi(ssid, 1);
        }

        for (int i = 0; i < Integer.parseInt(args[2]); ++i) {
            IdPwPicker.SsidPw ssidPw = idPwPicker.ExtractSsidPw();
            System.out.println(ssidPw.ssid);
            System.out.println(ssidPw.pw);
        }
    }

    static String readFileToSingleString(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    static List<String> readFileLines(String path, Charset encoding) throws IOException {
        List<String> readLines = new LinkedList<>();
        File file = new File(path);
        FileReader filereader = new FileReader(file);
        BufferedReader bufReader = new BufferedReader(filereader);
        String line;
        while((line = bufReader.readLine()) != null){
            readLines.add(line);
        }
        bufReader.close();
        return readLines;
    }
}