package com.example.hsh0908y.auto_wifi.tasks;

import com.example.hsh0908y.auto_wifi.common.SsidPw;
import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.example.hsh0908y.auto_wifi.common.TextBlockGraphComponent;
import com.example.hsh0908y.auto_wifi.common.WifiData;
import com.example.hsh0908y.auto_wifi.utils.Algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.getCenterDistance;
import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.indexOfMaxSizeComponent;
import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.isAlignedBlockHorizontal;
import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.isAlignedComponentHorizontal;
import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.isAlignedComponentVertical;
import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.isOrderX;
import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.isOrderY;
import static com.example.hsh0908y.auto_wifi.utils.TextBlockGeometry.rotationNormalize;

public class SsidPwPickTask {

    private final String[] SSID_TAGS = {"아이디", "id"};
    private final String[] PW_TAGS = {"pw", "password", "비밀번호", "비번", "ps", "패스워드", "p/w", "p.w", "pu"};
    private final String[] WIFI_TAGS = {"wifi", "wi-fi", "와이파이", "와이-파이"};
    private final int NUM_TRY_PW_PER_COMPONENT = 2;

    private final List<TextBlock> textBlockList;
    private final List<TextBlockGraphComponent> textBlockGraphComponentList;
    private final List<WifiData> wifiDataList;
    private List<String> ssidTriedList;
    private Queue<String> pwCandidateQueue;

    private final TextBlockGraphComponent ssidTagComponent;
    private final List<String> pwCandidateListFromTag;

    public SsidPwPickTask(List<TextBlock> textBlockList, List<WifiData> wifiDataList) {
        this.textBlockList = textBlockList;
        rotationNormalize(this.textBlockList);
        this.textBlockGraphComponentList = buildTextBlockGraphComponentList();

        ssidTriedList = new ArrayList<>();
        pwCandidateQueue = new LinkedList<>();

        ssidTagComponent = getSsidTagComponent();
        pwCandidateListFromTag = getPwCandidateListFromTag(false);
        this.wifiDataList = wifiDataList;
    }

    public SsidPwPickTask(List<TextBlock> textBlockList) {
        this.textBlockList = textBlockList;
        rotationNormalize(this.textBlockList);
        this.textBlockGraphComponentList = buildTextBlockGraphComponentList();

        ssidTriedList = new ArrayList<>();
        pwCandidateQueue = new LinkedList<>();

        ssidTagComponent = getSsidTagComponent();
        pwCandidateListFromTag = getPwCandidateListFromTag(true);
        this.wifiDataList = null;
    }

    public SsidPw extractSsidPw() {
        if (wifiDataList == null ||
                wifiDataList.isEmpty() || textBlockGraphComponentList.isEmpty()) {
            return null;
        }

        if (!pwCandidateQueue.isEmpty()) {
            // ASSERT ssidTriedList is not empty.
            return new SsidPw(ssidTriedList.get(ssidTriedList.size() - 1), pwCandidateQueue.poll());
        }

        String extractedSsid = null;
        TextBlockGraphComponent extractedSsidComponent = null;
        int maximumScore = 0;
        for (WifiData wifiData : wifiDataList) {
            for (TextBlockGraphComponent component : textBlockGraphComponentList) {
                int score = ComputeScoreSsid(wifiData, component);
                if (score > maximumScore) {
                    maximumScore = score;
                    extractedSsid = wifiData.getSsid();
                    extractedSsidComponent = component;
                }
            }
        }
        ssidTriedList.add(extractedSsid);

        for (String pw : pwCandidateListFromTag) {
            pwCandidateQueue.offer(pw);
        }
        if (pwCandidateQueue.isEmpty()) {
            for (String pw : getPwCandidateListFromSsidComponent(extractedSsidComponent)) {
                pwCandidateQueue.offer(pw);
            }
        }
        if (pwCandidateQueue.isEmpty()) {
            return new SsidPw(extractedSsid, null);
        }
        return new SsidPw(extractedSsid, pwCandidateQueue.poll());
    }

    public String extractPw() {
        if (textBlockGraphComponentList.isEmpty()) {
            return null;
        }

        if (pwCandidateQueue.isEmpty()) {
            for (String pw : pwCandidateListFromTag) {
                pwCandidateQueue.offer(pw);
            }
        }
        if (pwCandidateQueue.isEmpty()) {
            for (String pw : getPwCandidateListFromGeometry()) {
                pwCandidateQueue.offer(pw);
            }
        }
        if (pwCandidateQueue.isEmpty()) {
            return null;
        }
        return pwCandidateQueue.poll();
    }

    // Constructor helpers.

    private TextBlockGraphComponent getSsidTagComponent() {
        List<TextBlockGraphComponent> ssidTagComponents = new ArrayList<>();
        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
            boolean contains = false;
            for (String ssidTag : SSID_TAGS) {
                if (isTagDetected(component, ssidTag)) {
                    contains = true;
                    break;
                }
            }
            if (contains) {
                ssidTagComponents.add(component);
            }
        }
        return ssidTagComponents.isEmpty()
                ? null
                : ssidTagComponents.get(indexOfMaxSizeComponent(ssidTagComponents));
    }

    private List<String> getPwCandidateListFromTag(boolean regardWifiTag) {
        List<TextBlockGraphComponent> pwTagComponents = new ArrayList<>();
        List<String> pwTags = new ArrayList<>();
        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
            String containedPwTag = null;
            for (String pwTag : PW_TAGS) {
                if (isTagDetected(component, pwTag)) {
                    containedPwTag = pwTag;
                    break;
                }
            }
            if (containedPwTag != null) {
                pwTagComponents.add(component);
                pwTags.add(containedPwTag);
            }
        }
        if (pwTagComponents.isEmpty() && regardWifiTag) {
            for (TextBlockGraphComponent component : textBlockGraphComponentList) {
                String containedWifiTag = null;
                for (String wifiTag : WIFI_TAGS) {
                    if (isTagDetected(component, wifiTag)) {
                        containedWifiTag = wifiTag;
                        break;
                    }
                }
                if (containedWifiTag != null) {
                    pwTagComponents.add(component);
                    pwTags.add(containedWifiTag);
                }
            }
        }
        if (pwTagComponents.isEmpty()) {
            return new ArrayList<>();
        }

        int selectedIndex = -1;
        if (ssidTagComponent != null) {
            float minDistance = Float.MAX_VALUE;
            for (int index = 0; index < pwTagComponents.size(); ++index) {
                float distance = getCenterDistance(ssidTagComponent, pwTagComponents.get(index));
                if (distance < minDistance) {
                    selectedIndex = index;
                    minDistance = distance;
                }
            }
        } else {
            selectedIndex = indexOfMaxSizeComponent(pwTagComponents);
        }
        // ASSERT selectedIndex >= 0
        return getPwCandidateListFromPwComponent(
                pwTagComponents.get(selectedIndex), pwTags.get(selectedIndex));
    }

    private List<String> getPwCandidateListFromPwComponent(
            TextBlockGraphComponent pwTagComponent, String pwTag) {
        String description = pwTagComponent.getConcatenatedDescription();
        String pwCandidate;
        int tagEndIndex = getTagEndIndex(pwTagComponent, pwTag);
        if (tagEndIndex >= 0 && tagEndIndex + 1 < description.length() * 0.65f) {
            pwCandidate = description.substring(tagEndIndex);
        } else {
            TextBlockGraphComponent alignedToPwTagComponent = pwTagComponent;
            do {
                alignedToPwTagComponent =
                        getAlignedComponentHorizontalThenVertical(alignedToPwTagComponent);
                if (alignedToPwTagComponent == null) {
                    return new ArrayList<>();
                }
            } while (anyTagDetected(alignedToPwTagComponent, WIFI_TAGS) &&
                    anyTagDetected(alignedToPwTagComponent, PW_TAGS) &&
                    alignedToPwTagComponent.getConcatenatedDescription().length() <= 2);
            pwCandidate = alignedToPwTagComponent.getConcatenatedDescription();
        }

        int index = 0;
        while (index < pwCandidate.length() &&
                !Character.isLetter(pwCandidate.charAt(index)) &&
                !Character.isDigit(pwCandidate.charAt(index))) {
            ++index;
        }
        if (index == pwCandidate.length()) {
            return new ArrayList<>();
        }
        return getCharAlternates(pwCandidate.substring(index));
    }

    // Group TextBlocks that are aligned horizontally close to each other to a single TextBlockGraphComponent.
    // Each component has concatenated string.
    private List<TextBlockGraphComponent> buildTextBlockGraphComponentList() {
        List<List<Integer>> indexGraph = new ArrayList<>();
        for (int i = 0; i < textBlockList.size(); ++i) {
            indexGraph.add(new ArrayList<Integer>());
        }

        for (int i = 0; i < textBlockList.size(); ++i) {
            for (int j = i + 1; j < textBlockList.size(); ++j) {
                if (isAlignedBlockHorizontal(textBlockList.get(i), textBlockList.get(j))) {
                    indexGraph.get(i).add(j);
                    indexGraph.get(j).add(i);
                }
            }
        }

        List<List<Integer>> indexGraphComponentList = Algorithm.getIndexGraphComponentList(indexGraph);

        List<TextBlockGraphComponent> textBlockGraphComponentList = new ArrayList<>();
        for (List<Integer> indexGraphComponent : indexGraphComponentList) {
            List<TextBlock> textBlockGraphComponent = new ArrayList<>();
            for (int index : indexGraphComponent) {
                textBlockGraphComponent.add(textBlockList.get(index));
            }
            textBlockGraphComponentList.add(new TextBlockGraphComponent(textBlockGraphComponent));
        }

//        for (TextBlockGraphComponent c : textBlockGraphComponentList) {
////            System.out.println(c.getConcatenatedDescription());
////        }

        return textBlockGraphComponentList;
    }

    // End Constructor helpers.

    // This method is used only when pwCandidateListFromPwTag is empty.
    // (PW tag not found, or found but failed to find following component.)
    private List<String> getPwCandidateListFromSsidComponent(TextBlockGraphComponent ssidComponent) {

        TextBlockGraphComponent alignedToSsidComponent = ssidComponent;

        do {
            alignedToSsidComponent =
                    getAlignedComponentHorizontalThenVertical(alignedToSsidComponent);
            if (alignedToSsidComponent == null) {
                return new ArrayList<>();
            }
        } while (anyTagDetected(alignedToSsidComponent, WIFI_TAGS) &&
                anyTagDetected(alignedToSsidComponent, PW_TAGS) &&
                alignedToSsidComponent.getConcatenatedDescription().length() <= 2);
        return getCharAlternates(alignedToSsidComponent.getConcatenatedDescription());
    }

    // This method is used only when pwCandidateListFromPwTag is empty
    // (PW tag not found, or found but failed to find following component.),
    // and no ssidComponent hint exists.
    private List<String> getPwCandidateListFromGeometry() {
        // Currently just pick the largest component.
        // This could be improved by measuring its location. (e.g. center)
        TextBlockGraphComponent selectedComponent = null;

        float maxComponentSize = 0;
        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
            float componentSize = (component.getMaxX() - component.getMinX()) * (component.getMaxY() - component.getMinY());
            if (componentSize > maxComponentSize) {
                selectedComponent = component;
                maxComponentSize = componentSize;
            }
        }
        // ASSERT selectedComponent != null
        return getCharAlternates(selectedComponent.getConcatenatedDescription());
    }

    // Measure how much `component` can represent `wifiData`'s ssid, and it is a real ssid that should be found in the
    // picture.
    private int ComputeScoreSsid(WifiData wifiData, TextBlockGraphComponent component) {
        float polygonScore = ssidTagComponent != null &&
                (component == ssidTagComponent ||
                isAlignedComponentHorizontal(component, ssidTagComponent) ||
                isAlignedComponentVertical(component, ssidTagComponent))
                ? 2.0f : 1.0f;

        String ssid = wifiData.getSsid();
        String description = component.getConcatenatedDescription();
        int lengthLCS = Algorithm.getLengthOfLongestCommonSubsequence(ssid, description);
        float ssidScore = (float) lengthLCS / ssid.length() * 1000f + lengthLCS * 300f;

        // In case there are multiple WifiInfos with identical ssid, add signalLevel in a way that it wouldn't affect
        // the likelihood calculated by polygon and ssid.
        return (int) (polygonScore * ssidScore + wifiData.getSignalLevel());
    }

    // First try to find component aligned close horizontally.
    // If fail, try to find component aligned close vertically.
    // If fail, try to find component located below the benchmark, and pick the top among the belows.
    private TextBlockGraphComponent getAlignedComponentHorizontalThenVertical(
            TextBlockGraphComponent benchmarkComponent) {
        List<TextBlockGraphComponent> rightAlignedComponents = new ArrayList<>();
        List<TextBlockGraphComponent> belowAlignedComponents = new ArrayList<>();
        List<TextBlockGraphComponent> belowUnalignedComponents = new ArrayList<>();
        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
            if (isAlignedComponentHorizontal(benchmarkComponent, component) &&
                    isOrderX(benchmarkComponent, component)) {
                rightAlignedComponents.add(component);
            }

            if (isOrderY(benchmarkComponent, component)) {
                if (isAlignedComponentVertical(benchmarkComponent, component)) {
                    belowAlignedComponents.add(component);
                } else {
                    belowUnalignedComponents.add(component);
                }
            }
        }

        Comparator<TextBlockGraphComponent> minXComparator = new Comparator<TextBlockGraphComponent>() {
            @Override
            public int compare(TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
                return (int) (component1.getMinX() - component2.getMinX());
            }
        };
        Comparator<TextBlockGraphComponent> maxYComparator = new Comparator<TextBlockGraphComponent>() {
            @Override
            public int compare(TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
                return (int) (component1.getMaxY() - component2.getMaxY());
            }
        };
        if (!rightAlignedComponents.isEmpty()) {
            return Collections.min(rightAlignedComponents, minXComparator);
        }
        if (!belowAlignedComponents.isEmpty()) {
            return Collections.min(belowAlignedComponents, maxYComparator);
        }
        if (!belowUnalignedComponents.isEmpty()) {
            return Collections.min(belowUnalignedComponents, maxYComparator);
        }
        return null;
    }

    static private List<String> getCharAlternates(String s) {
        List<String> alternates = new ArrayList<>();
        alternates.add(s);
        return alternates;
    }

    static private boolean anyTagDetected(TextBlockGraphComponent component, String[] tags) {
        for (String tag : tags) {
            if (isTagDetected(component, tag)) {
                return true;
            }
        }
        return false;
    }

    static private boolean isTagDetected(TextBlockGraphComponent component, String tag) {
        String componentString = component.getConcatenatedDescription().toLowerCase();
        return componentString.contains(tag.toLowerCase()) ||
                Algorithm.getLengthOfLongestCommonSubstring(componentString, tag.toLowerCase()) >= 3;
    }

    static private int getTagEndIndex(TextBlockGraphComponent component, String tag) {
        return Algorithm.getLengthAndEndOfLongestCommonSubstring(
                component.getConcatenatedDescription().toLowerCase(), tag.toLowerCase()).get(1);
    }
}
