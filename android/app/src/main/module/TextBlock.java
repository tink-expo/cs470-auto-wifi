import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextBlock {

    private final String description;
    private final List<Point> normalizedPointList;
    private final float minX;
    private final float maxX;
    private final float minY;
    private final float maxY;

    public TextBlock(String description, int imageHeight, int imageWidth, List<Point> unNormalizedPointList) {
        this.description = description;
        normalizedPointList = new ArrayList<Point>();
        for (Point unNormalizedPoint : unNormalizedPointList) {
            normalizedPointList.add(new Point(
                    unNormalizedPoint.x / imageHeight, unNormalizedPoint.y / imageWidth));
        }

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
    }

    public String getDescription() { return description; }
    public List<Point> getNormalizedPointList() { return normalizedPointList; }
    public float getMinX() { return minX; }
    public float getMaxX() { return maxX; }
    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }
    
}
