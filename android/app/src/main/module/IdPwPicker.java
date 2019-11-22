import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.IntStream;

public class IdPwPicker {

    public static class SsidPw {
        public final String ssid;
        public final String pw;
        public SsidPw(String ssid, String pw) {
            this.ssid = ssid;
            this.pw = pw;
        }
    }

    private class WifiInfo {
        public final String ssid;
        public int signalLevel;
        public final int similarity[];

        public WifiInfo(String ssid, int signalLevel) {
            this.ssid = ssid;
            this.signalLevel = signalLevel;

            similarity = new int[annotations.length];
            for (int index = 0; index < annotations.length; ++index) {
                // similarity[index] holds similarity between
                // the WifiInfo's ssid and annotations[index]'s description.
                similarity[index] = ComputeSimilarity(annotations[index].description, ssid);
            }
        }
    }

    // Since the expected sizes of below collection members are very small,
    // it is not required to avoid linear iteration.

    private final TextAnnotation[] annotations;
    private final int imageWidth;
    private final int imageHeight;
    private HashMap<String, WifiInfo> wifis;

    private HashMap<String, LinkedList<String>> triedSsidPws;


    public IdPwPicker(TextAnnotation[] annotations, int imageWidth, int imageHeight) {
        this.annotations = annotations;
        // TODO: Iterate over annotations and if "PW", "ID", ... etc is find,
        // memorize annotations that are positioned next to them.

        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.wifis = new HashMap<>();
        this.triedSsidPws = new HashMap<>();
    }

    public void AddOrUpdateWifi(String ssid, int signalLevel) {
        WifiInfo wifiInfo = wifis.get(ssid);
        if (wifiInfo == null) {
            wifis.put(ssid, new WifiInfo(ssid, signalLevel));
        } else {
            wifiInfo.signalLevel = signalLevel;
        }
    }

    public SsidPw ExtractSsidPw() {
        if (wifis.isEmpty() || annotations.length == 0) {
            return null;
        }

        String extractedSsid = null;
        TextAnnotation extractedSsidAnnotation = null;
        int maximumScore = 0;
        for (String ssid : wifis.keySet()) {
            WifiInfo wifiInfo = wifis.get(ssid);
            for (int index = 0; index < annotations.length; ++index) {
                // ComputeScore always returns > 0.
                int score = ComputeScoreSsid(
                        wifiInfo.similarity[index], annotations[index],
                        triedSsidPws.containsKey(ssid), wifiInfo.signalLevel);
                if (score > maximumScore) {
                    maximumScore = score;
                    extractedSsid = ssid;
                    extractedSsidAnnotation = annotations[index];
                }
            }
        }
        return new SsidPw(extractedSsid, ExtractPwInternal(extractedSsid, extractedSsidAnnotation));
    }

    public String ExtractPw(String ssid) {
        WifiInfo wifiInfo = wifis.get(ssid);
        if (wifiInfo == null) {
            return null;
        }

        TextAnnotation ssidAnnotation = null;
        int maximumSimilarity = 0;
        for (int index = 0; index < annotations.length; ++index) {
            int similarity = ComputeSimilarity(ssid, annotations[index].description);
            if (similarity > maximumSimilarity) {
                maximumSimilarity = similarity;
                ssidAnnotation = annotations[index];
            }
        }

        return ExtractPwInternal(ssid, ssidAnnotation);
    }

    private String ExtractPwInternal(String ssid, TextAnnotation ssidAnnotation) {
        if (annotations.length == 0) {
            return null;
        }

        int[] scores = new int[annotations.length];
        for (int i = 0; i < annotations.length; ++i) {
            scores[i] = ComputeScorePw(annotations[i], ssid, ssidAnnotation);
        }
        int[] sortedIndices = IntStream.range(0, scores.length)
                .boxed().sorted((i, j) -> scores[i] < scores[j] ? 1 : 0)
                .mapToInt(ele -> ele).toArray();
        LinkedList<String> triedPws = triedSsidPws.get(ssid);

        String extractedPw = null;
        for (int i = 0; i < sortedIndices.length; ++i) {
            int index = sortedIndices[i];
            if (triedPws == null || !triedPws.contains(annotations[index].description)) {
                extractedPw = annotations[index].description;
                break;
            }
        }

        if (extractedPw != null) {
            UpdateTriedSsidPws(ssid, extractedPw);
        }
        return extractedPw;
    }

    private void UpdateTriedSsidPws(String ssid, String pw) {
        LinkedList<String> pws = triedSsidPws.get(ssid);
        if (pws == null) {
            triedSsidPws.put(ssid, new LinkedList<String>());
            pws = triedSsidPws.get(ssid);
        }
        pws.add(pw);
    }

    private static int ComputeSimilarity(String target, String candidate) {
        // TODO: Check similar characters
        return target.equals(candidate) ? 1 : 0;
    }

    private int ComputeScoreSsid(
            int similarity, TextAnnotation annotation, boolean isIdTried, int wifiSignalLevel) {
        // TODO: Is annotation big, and is its position at the center of the image? Use imageWidth, imageHeight.
        // similarity > annotation > isIdTried > wifiSignalLevel, and smaller field can't turn the tide.

        // TODO: Give very high score to annotation if it is positioned right next to "ID", ... etc.
        // Indices will be stored at construction.

        return 100 * similarity;
    }

    private int ComputeScorePw(TextAnnotation pwAnnotation, String ssid, TextAnnotation ssidAnnotation) {
        // TODO: Is pwAnnotation stands right next to ssidAnnotation?

        // TODO: Give very high score to pwAnnotation if it is positioned right next to "PW", ... etc.
        // Indices will be stored at construction.

        return 1;
    }
}