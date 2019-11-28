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
}
