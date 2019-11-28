package com.example.hsh0908y.auto_wifi.common;

public class WifiData {
    private final String ssid;
    private final int signalLevel;

    public WifiData(String ssid, int signalLevel) {
        this.ssid = ssid;
        this.signalLevel = signalLevel;
    }

    public String getSsid() { return ssid; }
    public int getSignalLevel() { return signalLevel; }
 }
