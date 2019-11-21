import java.util.Arrays;
import java.util.List;


public class IdPwPickerTester {
    private final List<String> gtIdPw;
    private final List<List<String>> resultIdPwCandidates;


    private double getScore() {
        for (int i=0; i<resultIdPwCandidates.size(); i++) {
            List<String> candidate = resultIdPwCandidates.get(i);

            if (gtIdPw.get(0).equals(candidate.get(0)) && gtIdPw.get(1).equals(candidate.get(1)))
                return 0.7f + 0.3f / resultIdPwCandidates.size();
        }
        return 0;
    }


    public IdPwPickerTester(String stringsFileName, String idPwFileName) {
        // Load ID and PW from result file
        this.gtIdPw = Arrays.asList("Starbucks_wifi", "1234");

        // Load strings and run through IdPwPicker
        List<String> strings = Arrays.asList("name", "Starbucks_wifi", "password", "1234");
        this.resultIdPwCandidates = IdPwPicker.pickIdPw(strings);
    }


    public static void main(String[] args) {
        for (int i=0; i<20; i++) {
            IdPwPickerTester idpwPickerTester = new IdPwPickerTester(String.format("strings%d.jpg", i), String.format("idpw#d.jpg", i));
            double score = idpwPickerTester.getScore();
            System.out.println(score);
        }
    }
}