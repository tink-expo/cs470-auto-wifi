package autowifi.experimental;

import java.util.*;

public class SsidPwPicker {

    private final String[] SSID_TAGS = {"id"};
    private final String[] PW_TAGS = {"pw", "password", "비밀번호", "비번"};

    private class TextBlockGraphComponent {
        // Sorted in x increasing order.
        final List<TextBlock> textBlockNodes;

        final String concatenatedDescription;

        final float minX;
        final float maxX;
        final float minY;
        final float maxY;

        TextBlockGraphComponent(List<TextBlock> textBlockNodes) {
            textBlockNodes.sort(new Comparator<TextBlock>() {
                @Override
                public int compare(TextBlock block1, TextBlock block2) {
                    float diff = block1.getNormalizedPointList().get(0).x - block2.getNormalizedPointList().get(0).x;
                    if (diff > 0) return 1;
                    else if (diff == 0) return 0;
                    return -1;
                }
            });
            this.textBlockNodes = textBlockNodes;

            StringBuilder stringBuilder = new StringBuilder();
            for (TextBlock textBlock : textBlockNodes) {
                stringBuilder.append(textBlock.getDescription());
            }
            concatenatedDescription = stringBuilder.toString();

            List<Float> xList = new ArrayList<>();
            List<Float> yList = new ArrayList<>();
            for (TextBlock textBlock : textBlockNodes) {
                xList.add(textBlock.getMaxX());
                xList.add(textBlock.getMinX());
                yList.add(textBlock.getMaxY());
                yList.add(textBlock.getMinY());
            }
            minX = Collections.min(xList);
            maxX = Collections.max(xList);
            minY = Collections.min(yList);
            maxY = Collections.max(yList);
        }
    }

    private final List<TextBlock> textBlockList;
    private final List<TextBlockGraphComponent> textBlockGraphComponentList;
    private final List<WifiInfo> wifiInfoList;
    private List<String> ssidTriedList;
    private Queue<String> pwCandidateQueue;

    private final TextBlockGraphComponent ssidTagComponent;
    private final TextBlockGraphComponent pwTagComponent;

    public SsidPwPicker(List<TextBlock> textBlockList, List<WifiInfo> wifiInfoList) {
        this.textBlockList = textBlockList;
        this.textBlockGraphComponentList = buildTextBlockGraphComponentList();
        this.wifiInfoList = wifiInfoList;

        ssidTriedList = new ArrayList<>();
        pwCandidateQueue = new LinkedList<>();

        List<TextBlockGraphComponent> ssidTagComponents = new ArrayList<>();
        List<TextBlockGraphComponent> pwTagComponents = new ArrayList<>();
        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
            if (containsAnyOf(component.concatenatedDescription, SSID_TAGS)) {
                ssidTagComponents.add(component);
            }
            if (containsAnyOf(component.concatenatedDescription, PW_TAGS)) {
                pwTagComponents.add(component);
            }
        }
        ssidTagComponent = findMaxSizeComponent(ssidTagComponents);
        pwTagComponent = findMaxSizeComponent(pwTagComponents);

//        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
//            System.out.println(component.concatenatedDescription);
//        }
//        System.out.println();
    }

    public SsidPw ExtractSsidPw() {
        if (wifiInfoList.isEmpty() || textBlockGraphComponentList.isEmpty()) {
            return null;
        }

        String extractedSsid;
        if (!pwCandidateQueue.isEmpty()) {
            // ASSERT ssidTriedList is not empty.
            extractedSsid = ssidTriedList.get(ssidTriedList.size() - 1);
        } else {
            extractedSsid = null;
            TextBlockGraphComponent extractedSsidComponent = null;
            int maximumScore = 0;
            for (WifiInfo wifiInfo : wifiInfoList) {
                for (TextBlockGraphComponent component : textBlockGraphComponentList) {
                    int score = ComputeScoreSsid(wifiInfo, component);
                    if (score > maximumScore) {
                        maximumScore = score;
                        extractedSsid = wifiInfo.getSsid();
                        extractedSsidComponent = component;
                    }
                    // System.out.printf("%s\n%s\n%d\n\n", wifiInfo.getSsid(), component.concatenatedDescription, score);
                }
            }

            ssidTriedList.add(extractedSsid);
            ExtractAndOfferPws(extractedSsidComponent);
        }
        return new SsidPw(extractedSsid, pwCandidateQueue.poll());
    }

    public String ExtractPw(String ssid) {
        if (wifiInfoList.isEmpty() || textBlockGraphComponentList.isEmpty()) {
            return null;
        }

        WifiInfo wifiInfoMatched = null;
        for (WifiInfo wifiInfo : wifiInfoList) {
            if (wifiInfo.getSsid().equals(ssid)) {
                wifiInfoMatched = wifiInfo;
                break;
            }
        }
        if (wifiInfoMatched == null) {
            return null;
        }

        if (pwCandidateQueue.isEmpty() || !ssid.equals(ssidTriedList.get(ssidTriedList.size() - 1))) {
            pwCandidateQueue.clear();
            TextBlockGraphComponent extractedSsidComponent = null;
            int maximumScore = 0;
            for (TextBlockGraphComponent component : textBlockGraphComponentList) {
                int score = ComputeScoreSsid(wifiInfoMatched, component);
                if (score > maximumScore) {
                    maximumScore = score;
                    extractedSsidComponent = component;
                }
            }

            ssidTriedList.add(ssid);
            ExtractAndOfferPws(extractedSsidComponent);
        }
        return pwCandidateQueue.poll();
    }

    // TODO: Now call of this method is redundant for different ssid, same ssidComponent. Optimize this.
    private void ExtractAndOfferPws(TextBlockGraphComponent ssidComponent) {
        // ASSERT textBlockGraphComponentList is not empty.

        final int NUM_TRY_PW_PER_COMPONENT = 2;

        List<TextBlockGraphComponent> pwComponentCandidateList = getPwComponentCandidateList(ssidComponent);

        for (TextBlockGraphComponent pwComponent : pwComponentCandidateList) {
            List<String> pwCandidateList = getPwCandidateList(pwComponent, NUM_TRY_PW_PER_COMPONENT);
            for (String pw : pwCandidateList) {
                pwCandidateQueue.offer(pw);
            }
        }
    }

    private int ComputeScoreSsid(WifiInfo wifiInfo, TextBlockGraphComponent component) {
        float polygonScore = ssidTagComponent != null &&
                (component == ssidTagComponent ||
                isAlignedComponentHorizontal(component, ssidTagComponent) ||
                isAlignedComponentVertical(component, ssidTagComponent))
                ? 2.0f : 1.0f;

        String ssid = wifiInfo.getSsid();
        String description = component.concatenatedDescription;
        int lengthLCS = Util.getLengthOfLongestCommonSubsequence(ssid, description);
        float ssidScore = (float) lengthLCS / ssid.length() * 1000f + lengthLCS * 50f;
        
        // In case there are multiple WifiInfos with identical ssid, add signalLevel in a way that it wouldn't affect
        // the likelihood calculated by polygon and ssid.
        return (int) (polygonScore * ssidScore + wifiInfo.getSignalLevel());
    }

    private List<TextBlockGraphComponent> getPwComponentCandidateList(TextBlockGraphComponent ssidComponent) {
        TextBlockGraphComponent[] pwComponentCandidates = new TextBlockGraphComponent[4];
        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
            // 0. 
        }

        List<TextBlockGraphComponent> pwComponentCandidateList = new ArrayList<>();
        pwComponentCandidateList.add(textBlockGraphComponentList.get(0));
        return pwComponentCandidateList;
    }

    private List<String> getPwCandidateList(TextBlockGraphComponent pwComponent, int maxNumPws) {
        List<String> pwCandidateList = new ArrayList<>();
        pwCandidateList.add("a");
        pwCandidateList.add("b");
        return pwCandidateList;
    }

    private List<TextBlockGraphComponent> buildTextBlockGraphComponentList() {
        List<List<Integer>> indexGraph = new ArrayList<>();
        for (int i = 0; i < textBlockList.size(); ++i) {
            indexGraph.add(new ArrayList<>());
        }

        for (int i = 0; i < textBlockList.size(); ++i) {
            for (int j = i + 1; j < textBlockList.size(); ++j) {
                if (isAlignedBlockHorizontal(textBlockList.get(i), textBlockList.get(j))) {
                    indexGraph.get(i).add(j);
                    indexGraph.get(j).add(i);
                }
            }
        }

        List<List<Integer>> indexGraphComponentList = Util.getIndexGraphComponentList(indexGraph);

        List<TextBlockGraphComponent> textBlockGraphComponentList = new ArrayList<>();
        for (List<Integer> indexGraphComponent : indexGraphComponentList) {
            List<TextBlock> textBlockGraphComponent = new ArrayList<>();
            for (int index : indexGraphComponent) {
                textBlockGraphComponent.add(textBlockList.get(index));
            }
            textBlockGraphComponentList.add(new TextBlockGraphComponent(textBlockGraphComponent));
        }
        return textBlockGraphComponentList;
    }

    // Below 3 methods are safe to call only if both args are not null.

    private static boolean isAlignedBlockHorizontal(TextBlock block1, TextBlock block2) {
        final float RATIO_SIZE_Y_OVERLAP_Y = 0.7f;
        final float RATIO_SIZE_Y_GAP_X = 1.5f;

        float sizeY1 = block1.getMaxY() - block1.getMinY();
        float sizeY2 = block2.getMaxY() - block2.getMinY();
        float overlapY = Math.min(block1.getMaxY() - block2.getMinY(), block2.getMaxY() - block1.getMinY());
        float gapX = Math.max(block1.getMinX() - block2.getMaxX(), block2.getMinX() - block1.getMaxX());

        return overlapY > Math.max(sizeY1, sizeY2) * RATIO_SIZE_Y_OVERLAP_Y &&
                gapX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_GAP_X;
    }

    private static boolean isAlignedComponentHorizontal(
            TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        final float RATIO_SIZE_Y_OVERLAP_Y = 0.7f;
        final float RATIO_SIZE_Y_GAP_X = 1.5f;

        float sizeY1 = component1.maxY - component1.minY;
        float sizeY2 = component2.maxY - component2.minY;
        float overlapY = Math.min(component1.maxY - component2.minY, component2.maxY - component1.minY);
        float gapX = Math.max(component1.minX - component2.maxX, component2.minX - component1.maxX);

        return overlapY > Math.max(sizeY1, sizeY2) * RATIO_SIZE_Y_OVERLAP_Y &&
                gapX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_GAP_X;
    }

    private static boolean isAlignedComponentVertical(
            TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        final float RATIO_SIZE_Y_DIFF_X = 1.0f;
        final float RAITO_SIZE_Y_GAP_Y = 1.0f;

        float sizeY1 = component1.maxY - component1.minY;
        float sizeY2 = component2.maxY - component2.minY;
        float diffMinX = Math.abs(component1.minX - component2.minX);
        float diffCenterX = Math.abs((component1.maxX + component1.minX) / 2 - (component2.maxX + component2.minX) / 2);
        float gapY = Math.max(component1.minY - component2.maxY, component2.minY - component1.maxY);

        return (diffMinX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_DIFF_X ||
                diffCenterX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_DIFF_X) &&
                gapY < Math.min(sizeY1, sizeY2) * RAITO_SIZE_Y_GAP_Y;
    }

    static private boolean containsAnyOf(String searched, String[] targets) {
        for (String target : targets) {
            if (searched.toLowerCase().contains(target.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    static private TextBlockGraphComponent findMaxSizeComponent(List<TextBlockGraphComponent> components) {
        TextBlockGraphComponent maxSizeComponent = null;
        float maxSize = 0;
        for (TextBlockGraphComponent component : components) {
            float size = (component.maxX - component.minX) * (component.maxY - component.minY);
            if (size > maxSize) {
                maxSizeComponent = component;
                maxSize = size;
            }
        }
        return maxSizeComponent;
    }
}
