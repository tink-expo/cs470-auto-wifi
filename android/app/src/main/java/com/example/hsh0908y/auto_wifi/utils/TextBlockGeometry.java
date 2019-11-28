package com.example.hsh0908y.auto_wifi.utils;

import com.example.hsh0908y.auto_wifi.common.TextBlock;
import com.example.hsh0908y.auto_wifi.common.TextBlockGraphComponent;

import java.util.ArrayList;
import java.util.List;

public class TextBlockGeometry {
    // Below 3 static methods are safe to call only if both args are not null.

    public static boolean isAlignedBlockHorizontal(TextBlock block1, TextBlock block2) {
        final float RATIO_SIZE_Y_OVERLAP_Y = 0.7f;
        final float RATIO_LETTER_X_GAP_X = 2.5f;

        float sizeY1 = block1.getMaxY() - block1.getMinY();
        float sizeY2 = block2.getMaxY() - block2.getMinY();
        float overlapY = Math.min(block1.getMaxY() - block2.getMinY(), block2.getMaxY() - block1.getMinY());
        float gapX = Math.max(block1.getMinX() - block2.getMaxX(), block2.getMinX() - block1.getMaxX());

        return overlapY > Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_OVERLAP_Y &&
                gapX < Math.max(block1.getLetterX(), block2.getLetterX()) * RATIO_LETTER_X_GAP_X;
    }

    public static boolean isAlignedComponentHorizontal(
            TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        final float RATIO_SIZE_Y_OVERLAP_Y = 0.3f;
        final float RATIO_LETTER_X_GAP_X = 4.0f;

        float sizeY1 = component1.getMaxY() - component1.getMinY();
        float sizeY2 = component2.getMaxY() - component2.getMinY();
        float overlapY = Math.min(component1.getMaxY() - component2.getMinY(), component2.getMaxY() - component1.getMinY());
        float gapX = Math.max(component1.getMinX() - component2.getMaxX(), component2.getMinX() - component1.getMaxX());

        return overlapY > Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_OVERLAP_Y &&
                0 <= gapX && gapX < Math.max(component1.getLetterX(), component2.getLetterX()) * RATIO_LETTER_X_GAP_X;
    }

    public static boolean isAlignedComponentVertical(
            TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        final float RATIO_SIZE_Y_DIFF_X = 2.0f;
        final float RAITO_SIZE_Y_GAP_Y = 2.0f;

        float sizeY1 = component1.getMaxY() - component1.getMinY();
        float sizeY2 = component2.getMaxY() - component2.getMinY();
        float diffMinX = Math.abs(component1.getMinX() - component2.getMinX());
        float diffCenterX = Math.abs((component1.getMaxX() + component1.getMinX()) / 2 - (component2.getMaxX() + component2.getMinX()) / 2);
        float gapY = Math.max(component1.getMinY() - component2.getMaxY(), component2.getMinY() - component1.getMaxY());

        return (diffMinX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_DIFF_X ||
                diffCenterX < Math.min(sizeY1, sizeY2) * RATIO_SIZE_Y_DIFF_X) &&
                0 <= gapY && gapY < Math.max(sizeY1, sizeY2) * RAITO_SIZE_Y_GAP_Y;
    }

    public static int indexOfMaxSizeComponent(List<TextBlockGraphComponent> components) {
        int indexOfMaxSize = 0;
        float maxSize = 0;
        for (int index = 0; index < components.size(); ++index) {
            TextBlockGraphComponent component = components.get(index);
            float size = (component.getMaxX() - component.getMinX()) * (component.getMaxY() - component.getMinY());
            if (size > maxSize) {
                indexOfMaxSize = index;
                maxSize = size;
            }
        }
        return indexOfMaxSize;
    }

    public static float getCenterDistance(TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        float centerX1 = (component1.getMinX() + component1.getMaxX()) / 2f;
        float centerX2 = (component2.getMinX() + component2.getMaxX()) / 2f;
        float centerY1 = (component1.getMinY() + component1.getMaxY()) / 2f;
        float centerY2 = (component2.getMinY() + component2.getMaxY()) / 2f;
        return (float) Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));
    }

    public static boolean isOrderY(TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        float sizeY1 = component1.getMaxY() - component1.getMinY();
        float sizeY2 = component2.getMaxY() - component2.getMinY();
        float gapY = component2.getMinY() - component1.getMaxY();
        if (gapY >= 0) {
            return true;
        }
        return Math.abs(gapY) < 0.2 * Math.min(sizeY1, sizeY2);
    }

    public static boolean isOrderX(TextBlockGraphComponent component1, TextBlockGraphComponent component2) {
        float gapX = component2.getMinX() - component1.getMaxX();
        if (gapX >= 0) {
            return true;
        }
        return Math.abs(gapX) < 0.2 * Math.min(component1.getLetterX(), component2.getLetterX());
    }
}
