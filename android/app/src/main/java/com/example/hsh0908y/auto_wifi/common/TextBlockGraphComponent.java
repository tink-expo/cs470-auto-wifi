package com.example.hsh0908y.auto_wifi.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TextBlockGraphComponent {
    // Sorted in x increasing order.
    final private List<TextBlock> textBlockNodes;

    final private String concatenatedDescription;

    final private float minX;
    final private float maxX;
    final private float minY;
    final private float maxY;

    final private float letterX;

    public TextBlockGraphComponent(List<TextBlock> textBlockNodes) {
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

        float sumLetterX = 0f;
        for (TextBlock textBlock : textBlockNodes) {
            sumLetterX += textBlock.getLetterX();
        }
        letterX = sumLetterX / textBlockNodes.size();
    }

    public String getConcatenatedDescription() { return concatenatedDescription; }

    public float getMaxX() { return maxX; }

    public float getMaxY() { return maxY; }

    public float getMinX() { return minX; }

    public float getMinY() { return minY; }

    public float getLetterX() { return letterX; }
}
