package com.example.hsh0908y.auto_wifi.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextBlock {

    private final String description;
    private List<Point> normalizedPointList;
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private float letterX;

    public TextBlock(String description, int imageHeight, int imageWidth, List<Point> unNormalizedPointList) {
        this.description = description;
        normalizedPointList = new ArrayList<Point>();
        for (Point unNormalizedPoint : unNormalizedPointList) {
            normalizedPointList.add(new Point(
                    unNormalizedPoint.x / imageHeight - 0.5f, -(unNormalizedPoint.y / imageWidth - 0.5f)));
        }

        resetGeometryValues();
    }

    public String getDescription() { return description; }

    public float getMinX() { return minX; }
    public float getMaxX() { return maxX; }
    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }
    public float getLetterX() { return letterX; }

    public List<Point> getNormalizedPointList() { return normalizedPointList; }

    public void rotate(float radAngle) {
        for (int index = 0; index < normalizedPointList.size(); ++index) {
            double sinVal = Math.sin(radAngle);
            double cosVal = Math.cos(radAngle);
            Point oldPoint = normalizedPointList.get(index);
            double newX = cosVal * oldPoint.x - sinVal * oldPoint.y;
            double newY = sinVal * oldPoint.x + cosVal * oldPoint.y;
            // System.out.printf("%.2f (%.2f %.2f) (%.2f %.2f)\n", radAngle * 180 / Math.PI, oldPoint.x * 1000, oldPoint.y * 1000, newX * 1000, newY * 1000);
            normalizedPointList.set(index, new Point((float) newX, (float) newY));
        }
        resetGeometryValues();
    }

    private void resetGeometryValues() {
        List<Float> xList = new ArrayList<>();
        List<Float> yList = new ArrayList<>();
        for (Point normalizedPoint : normalizedPointList) {
            xList.add(normalizedPoint.x);
            yList.add(normalizedPoint.y);
        }
        minX = Collections.min(xList);
        maxX = Collections.max(xList);
        minY = Collections.min(yList);
        maxY = Collections.max(yList);
        letterX = (maxX - minX) / description.length();
    }
}
