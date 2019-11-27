package autowifi.experimental;

import java.util.*;

public class SsidPwPicker {

    private final String[] SSID_TAGS = {"아이디", "id"};
    private final String[] PW_TAGS = {"pw", "password", "비밀번호", "비번", "ps", "패스워드"};
    private final int NUM_TRY_PW_PER_COMPONENT = 2;

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
    private final List<String> pwCandidateListFromTag;

    SsidPwPicker(List<TextBlock> textBlockList, List<WifiInfo> wifiInfoList) {
        this.textBlockList = textBlockList;
        this.textBlockGraphComponentList = buildTextBlockGraphComponentList();
        this.wifiInfoList = wifiInfoList;

        ssidTriedList = new ArrayList<>();
        pwCandidateQueue = new LinkedList<>();

        ssidTagComponent = getSsidTagComponent();
        pwCandidateListFromTag = getPwCandidateListFromTag();
    }

    public SsidPw ExtractSsidPw() {
        if (wifiInfoList.isEmpty() || textBlockGraphComponentList.isEmpty()) {
            return null;
        }

        if (!pwCandidateQueue.isEmpty()) {
            // ASSERT ssidTriedList is not empty.
            return new SsidPw(ssidTriedList.get(ssidTriedList.size() - 1), pwCandidateQueue.poll());
        }

        String extractedSsid = null;
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

        if (!pwCandidateQueue.isEmpty() && ssid.equals(ssidTriedList.get(ssidTriedList.size() - 1))) {
            return pwCandidateQueue.poll();
        }

        ssidTriedList.add(ssid);
        for (String pw : pwCandidateListFromTag) {
            pwCandidateQueue.offer(pw);
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
                if (component.concatenatedDescription.toLowerCase().contains(ssidTag.toLowerCase())) {
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

    private List<String> getPwCandidateListFromTag() {
        List<TextBlockGraphComponent> pwTagComponents = new ArrayList<>();
        List<String> pwTags = new ArrayList<>();
        for (TextBlockGraphComponent component : textBlockGraphComponentList) {
            String containedPwTag = null;
            for (String pwTag : PW_TAGS) {
                if (component.concatenatedDescription.toLowerCase().contains(pwTag.toLowerCase())) {
                    containedPwTag = pwTag;
                    break;
                }
            }
            if (containedPwTag != null) {
                pwTagComponents.add(component);
                pwTags.add(containedPwTag);
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
        String description = pwTagComponent.concatenatedDescription;
        String pwCandidate;
        int pwTagPosition = description.indexOf(pwTag);
        if (pwTagPosition + pwTag.length() < description.length() * 0.5) {
            pwCandidate = description.substring(description.toLowerCase().indexOf(pwTag.toLowerCase()) + pwTag.length());
        } else {
            TextBlockGraphComponent alignedToPwTagComponent = pwTagComponent;
            do {
                alignedToPwTagComponent = getAlignedComponentHorizontalThenVertical(alignedToPwTagComponent);
                if (alignedToPwTagComponent == null) {
                    return new ArrayList<>();
                }
            } while (alignedToPwTagComponent.concatenatedDescription.length() <= 2);
            pwCandidate = alignedToPwTagComponent.concatenatedDescription;
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

    // End Constructor helpers.

    // This method is used only when pwCandidateListFromPwTag is empty. (No pw tag found)
    private List<String> getPwCandidateListFromSsidComponent(TextBlockGraphComponent ssidComponent) {
        TextBlockGraphComponent alignedToSsidComponent = getAlignedComponentHorizontalThenVertical(ssidComponent);
        if (alignedToSsidComponent != null) {
            return getCharAlternates(alignedToSsidComponent.concatenatedDescription);
        }
        return new ArrayList<>();
    }

    // Measure how much `component` can represent `wifiInfo`'s ssid, and it is a real ssid that should be found in the
    // picture.
    private int ComputeScoreSsid(WifiInfo wifiInfo, TextBlockGraphComponent component) {
        float polygonScore = ssidTagComponent != null &&
                (component == ssidTagComponent ||
                isAlignedComponentHorizontal(component, ssidTagComponent) ||
                isAlignedComponentVertical(component, ssidTagComponent))
                ? 2.0f : 1.0f;

        String ssid = wifiInfo.getSsid();
        String description = component.concatenatedDescription;
        int lengthLCS = Util.getLengthOfLongestCommonSubsequence(ssid, description);
        float ssidScore = (float) lengthLCS / ssid.length() * 1000f + lengthLCS * 100f;

        // In case there are multiple WifiInfos with identical ssid, add signalLevel in a way that it wouldn't affect
        // the likelihood calculated by polygon and ssid.
        return (int) (polygonScore * ssidScore + wifiInfo.getSignalLevel());
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
                    benchmarkComponent.maxX <= component.minX) {
                rightAlignedComponents.add(component);
            }

            if (benchmarkComponent.maxY <= component.minY) {
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
                return (int) (component1.minX - component2.minX);
            }
        };
        Comparator<TextBlockGraphComponent> minYComparator = new Comparator<TextBlockGraphComponent>() {
            @Override
            public int compare(TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
                return (int) (component1.minY - component2.minY);
            }
        };
        if (!rightAlignedComponents.isEmpty()) {
            return Collections.min(rightAlignedComponents, minXComparator);
        }
        if (!belowAlignedComponents.isEmpty()) {
            return Collections.min(belowAlignedComponents, minYComparator);
        }
        if (!belowUnalignedComponents.isEmpty()) {
            return Collections.min(belowUnalignedComponents, minYComparator);
        }
        return null;
    }

    // Below 3 static methods are safe to call only if both args are not null.

    private static boolean isAlignedBlockHorizontal(TextBlock block1, TextBlock block2) {
        final float RATIO_SIZE_Y_OVERLAP_Y = 0.7f;
        final float RATIO_SIZE_Y_GAP_X = 1.5f;

        float sizeY1 = block1.getMaxY() - block1.getMinY();
        float sizeY2 = block2.getMaxY() - block2.getMinY();
        float overlapY = Math.min(block1.getMaxY() - block2.getMinY(), block2.getMaxY() - block1.getMinY());
        float gapX = Math.max(block1.getMinX() - block2.getMaxX(), block2.getMinX() - block1.getMaxX());

        return overlapY > Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_OVERLAP_Y &&
                gapX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_GAP_X;
    }

    private static boolean isAlignedComponentHorizontal(
            TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        final float RATIO_SIZE_Y_OVERLAP_Y = 0.5f;
        final float RATIO_SIZE_Y_GAP_X = 2.0f;

        float sizeY1 = component1.maxY - component1.minY;
        float sizeY2 = component2.maxY - component2.minY;
        float overlapY = Math.min(component1.maxY - component2.minY, component2.maxY - component1.minY);
        float gapX = Math.max(component1.minX - component2.maxX, component2.minX - component1.maxX);

        return overlapY > Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_OVERLAP_Y &&
               0 <= gapX && gapX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_GAP_X;
    }

    private static boolean isAlignedComponentVertical(
            TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        final float RATIO_SIZE_Y_DIFF_X = 2.0f;
        final float RAITO_SIZE_Y_GAP_Y = 2.0f;

        float sizeY1 = component1.maxY - component1.minY;
        float sizeY2 = component2.maxY - component2.minY;
        float diffMinX = Math.abs(component1.minX - component2.minX);
        float diffCenterX = Math.abs((component1.maxX + component1.minX) / 2 - (component2.maxX + component2.minX) / 2);
        float gapY = Math.max(component1.minY - component2.maxY, component2.minY - component1.maxY);

        return (diffMinX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_DIFF_X ||
                diffCenterX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_DIFF_X) &&
                0 <= gapY && gapY < Math.min(sizeY1, sizeY2) * RAITO_SIZE_Y_GAP_Y;
    }

    static private int indexOfMaxSizeComponent(List<TextBlockGraphComponent> components) {
        int indexOfMaxSize = 0;
        float maxSize = 0;
        for (int index = 0; index < components.size(); ++index) {
            TextBlockGraphComponent component = components.get(index);
            float size = (component.maxX - component.minX) * (component.maxY - component.minY);
            if (size > maxSize) {
                indexOfMaxSize = index;
                maxSize = size;
            }
        }
        return indexOfMaxSize;
    }

    static private float getCenterDistance(TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        float centerX1 = (component1.minX + component1.maxX) / 2f;
        float centerX2 = (component2.minX + component2.maxX) / 2f;
        float centerY1 = (component1.minY + component1.maxY) / 2f;
        float centerY2 = (component2.minY + component2.maxY) / 2f;
        return (float) Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));
    }

    static private List<String> getCharAlternates(String s) {
        List<String> alternates = new ArrayList<>();
        alternates.add(s);
        return alternates;
    }
}
