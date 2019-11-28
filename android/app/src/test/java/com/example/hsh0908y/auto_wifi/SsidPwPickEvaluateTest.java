package com.example.hsh0908y.auto_wifi;

import org.junit.Test;

import java.nio.file.Paths;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SsidPwPickEvaluateTest {

    @Test
    public void printEvaluation() {
        String testDataDir =
                Paths.get(System.getProperty("user.dir"), "sampledata").toString();
        String saveDir = Paths.get(testDataDir, "output-test").toString();
        Evaluation evaluation = new Evaluation();
        evaluation.LoadGroundTruth(Paths.get(testDataDir, "ground_truth.csv").toString());
        System.out.println(evaluation.evaluateAll(saveDir, 10, 0));
        System.out.println(evaluation.evaluateAll(saveDir, 10, 1));
    }
}