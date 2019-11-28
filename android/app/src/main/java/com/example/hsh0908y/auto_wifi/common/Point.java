package com.example.hsh0908y.auto_wifi.common;

public class Point {
    public final float x;
    public final float y;
    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }
    public Point (int x, int y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    public static float getDistance(Point p1, Point p2) {
        return (float) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}
