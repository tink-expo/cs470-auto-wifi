import java.util.Arrays;
import java.util.List;


public class IdPwPicker {
    public static List<List<String>> pickIdPw(List<String> strings) {
        List<List<String>> idPwCandidates = new ArrayList<List<String>>();

        idPwCandidates.add(Arrays.asList(strings.get(1), strings.get(3)));
        
        return idPwCandidates;
    }
}