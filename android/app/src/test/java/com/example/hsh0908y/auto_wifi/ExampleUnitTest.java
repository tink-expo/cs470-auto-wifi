package com.example.hsh0908y.auto_wifi;

import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.example.hsh0908y.auto_wifi.common.WifiData;
import com.example.hsh0908y.auto_wifi.tasks.SsidPwPickTask;
import com.example.hsh0908y.auto_wifi.utils.AlgorithmUtil;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void printEvaluation() {
        String saveDir = "/Users/woohyunhan/Desktop/output-test";
        Evaluation evaluation = new Evaluation();
        evaluation.LoadGroundTruth("/Users/woohyunhan/Desktop/ground_truth.csv");
        System.out.println(evaluation.evaluateAll(saveDir, 10, 0));
        System.out.println(evaluation.evaluateAll(saveDir, 10, 1));
    }

    @Test
    public void printJson() {
        String saveDir = "/Users/woohyunhan/Desktop/output-test";
        System.out.println(TextDetect.getJsonFromSaved(Paths.get(saveDir, "52.jpeg.save").toString()));
    }
}