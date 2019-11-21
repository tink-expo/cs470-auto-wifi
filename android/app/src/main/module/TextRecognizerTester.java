import java.awt.Image;
import java.util.Arrays;
import java.util.List;


public class TextRecognizerTester {
    private final List<String> gtStrings;
    private final List<String> resultStrings;


    private double getScore() {
        for (int i=0; i<gtStrings.size() && i<resultStrings.size(); i++) {
            if (!(gtStrings.get(i).equals(resultStrings.get(i))))
                return 0;
        }
        return 1;
    }


    public TextRecognizerTester(String imageFileName, String resultFileName) {
        // Load Strings from result file
        this.gtStrings = Arrays.asList("name", "Starbucks_wifi", "password", "1234");

        // Load image and run through TextRecognizer
        Image image = null;
        this.resultStrings = TextRecognizer.recognizeText(image);
    }


    public static void main(String[] args) {
        for (int i=0; i<20; i++) {
            TextRecognizerTester testRecognizerTester = new TextRecognizerTester(String.format("image%d.jpg", i), String.format("strings#d.jpg", i));
            double score = testRecognizerTester.getScore();
            System.out.println(score);
        }
    }
}