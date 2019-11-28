package com.example.hsh0908y.auto_wifi;

import com.example.hsh0908y.auto_wifi.common.SsidPw;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.example.hsh0908y.auto_wifi.common.WifiData;
import com.example.hsh0908y.auto_wifi.tasks.SsidPwPickTask;
import com.example.hsh0908y.auto_wifi.utils.AlgorithmUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

class Evaluation {
    private Map<Integer, String> groundTruthSsidMap;
    private Map<Integer, String> groundTruthPwMap;

    public Evaluation() {
        groundTruthSsidMap = new HashMap<>();
        groundTruthPwMap = new HashMap<>();
    }

    public boolean LoadGroundTruth(String csvPath) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(csvPath),"UTF8"));
            String line = "";

            boolean allLoaded = true;
            while((line = reader.readLine()) != null){
                String[] data = line.split(",", -1);
                if (data[0].isEmpty() || !Character.isDigit(data[0].charAt(0))) {
                    continue;
                }

                int fileNamePrefix = Integer.parseInt(data[0]);

                // Load ssid.
                if (!data[1].equals("")) {
                    groundTruthSsidMap.put(fileNamePrefix, data[1]);
                }

                // Load pw.
                if (!data[2].equals("")) {
                    groundTruthPwMap.put(fileNamePrefix, data[2]);
                }  else {
                    // Each line has to be guaranteed that at least PW exists.
                    allLoaded = false;
                }
            }
            return allLoaded;

        } catch (IOException e) {
            return false;
        }
    }

    public float evaluateAll(String saveDir, int wifiListSize, int numConfusingWifis) {
        File dir = new File(saveDir);
        File[] files = dir.listFiles();
        List<File> fileList = new ArrayList<>();
        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (fileName.endsWith(".save")) {
                fileList.add(file);
            }
        }
        fileList.sort(new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return getIntPrefix(file1.getName()) - getIntPrefix(file2.getName());
            }
        });

        float scoreSum = 0f;
        int count = 0;
        for (File file : fileList) {
            int prefix = getIntPrefix(file.getName());
            String groundTruthSsid = groundTruthSsidMap.get(prefix);
            String groundTruthPw = groundTruthPwMap.get(prefix);
            if (groundTruthSsid != null || groundTruthPw != null) {
                System.out.printf("[ %s ]\n", file.getName());
                List<TextBlock> textBlockList = TextDetect.getTextBlockListFromSaved(file.getPath());
                scoreSum += evaluate(
                        textBlockList, groundTruthSsid, groundTruthPw,
                        wifiListSize, numConfusingWifis);
                ++count;
            }
        }
        return scoreSum / count;
    }

    public float evaluate(String savePath,
                          int wifiListSize, int numConfusingWifis) {
        int prefix = getIntPrefix(Paths.get(savePath).getFileName().toString());
        String groundTruthSsid = groundTruthSsidMap.get(prefix);
        String groundTruthPw = groundTruthPwMap.get(prefix);

        if (groundTruthSsid != null || groundTruthPw != null) {
            List<TextBlock> textBlockList = TextDetect.getTextBlockListFromSaved(savePath);
            return evaluate(textBlockList, groundTruthSsid, groundTruthPw,
                    wifiListSize, numConfusingWifis);
        }
        return -1f;
    }

    private float evaluate(List<TextBlock> textBlockList,
                          String groundTruthSsid, String groundTruthPw,
                          int wifiListSize, int numConfusingWifis) {
        if (groundTruthSsid != null) {
            return evaluateSsidPw(wifiListSize, numConfusingWifis,
                    groundTruthSsid, groundTruthPw, textBlockList);
        }
        return evaluatePw(groundTruthPw, textBlockList);
    }

    private float evaluateSsidPw(
            int wifiListSize, int numConfusingWifis,
            String groundTruthSsid, String groundTruthPw, List<TextBlock> textBlockList) {
        List<WifiData> wifis = new ArrayList<>();
        Random random = new Random();
        int maxCount = Math.min(numConfusingWifis, textBlockList.size());
        int count = 0;
        for (int trial = 0; trial < textBlockList.size() && count < maxCount; ++trial) {
            String description =
                    textBlockList.get(random.nextInt(textBlockList.size())).getDescription();
            if (AlgorithmUtil.getLengthOfLongestCommonSubsequence(
                    description, groundTruthSsid) < 4) {
                wifis.add(new WifiData(description, 2));
                ++count;
            }
        }
        wifis.add(new WifiData(groundTruthSsid, 1));

        int numRemainWifis = wifiListSize - wifis.size();
        for (int i = 0; i < numRemainWifis; ++i) {
            wifis.add(new WifiData(generateRandomString(7), 2));
        }

        SsidPwPickTask pickTask = new SsidPwPickTask(textBlockList, wifis);
        SsidPw ssidPw = pickTask.extractSsidPw();

        float score;
        if (ssidPw == null) {
            score = 0f;
        } else if (groundTruthPw == null) {
            score = getStringScore(ssidPw.ssid, groundTruthSsid);
        } else {
            score = getStringScore(ssidPw.ssid, groundTruthSsid)* 0.5f +
                    getStringScore(ssidPw.pw, groundTruthPw) * 0.5f;
        }

        System.out.printf("%s  /  %s\n%s  /  %s\n\n",
                groundTruthSsid, ssidPw.ssid, groundTruthPw, ssidPw.pw);

        return Math.min(1f, Math.max(0f, score));
    }

    private float evaluatePw(String groundTruthPw, List<TextBlock> textBlockList) {
        SsidPwPickTask pickTask = new SsidPwPickTask(textBlockList);
        String pw = pickTask.extractPw();
        float score = getStringScore(pw, groundTruthPw);

        System.out.printf("%s  /  %s\n\n", groundTruthPw, pw);

        return Math.min(1f, Math.max(0f, score));
    }

    private static float getStringScore(String answer, String groundTruth) {
        if (answer == null) {
            return 0.0f;
        }
        if (groundTruth.equals(answer)) {
            return 1.0f;
        }
        return 0.5f * (1 -
                (float) AlgorithmUtil.getEditDistance(groundTruth, answer) / groundTruth.length());
    }

    private static String generateRandomString(int length) {
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }

    private static int getIntPrefix(String fileName) {
        return Integer.parseInt((fileName.split("\\."))[0]);
    }
}
