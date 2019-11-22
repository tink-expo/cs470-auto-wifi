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

        IdPwPicker extractor = GcpJsonUtil.GetIdPwPickerFromGcpJsonResponse(json);
        if (json == null) {
            return;
        }

        List<String> wifiSsids = null;
        try {
            wifiSsids = readFileLines(args[1], Charset.defaultCharset());
        } catch (IOException e) {
            return;
        }
        for (String ssid : wifiSsids) {
            extractor.AddOrUpdateWifi(ssid, 1);
        }

        for (int i = 0; i < Integer.parseInt(args[3]); ++i) {
            IdPwPicker.SsidPw ssidPw = extractor.ExtractSsidPw();
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