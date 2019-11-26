public class WifiInfo {
    private final String ssid;
    private final int signalLevel;

    public WifiInfo(String ssid, int signalLevel) {
        this.ssid = ssid;
        this.signalLevel = signalLevel;
    }

    public String getSsid() { return ssid; }
    public int getSignalLevel() { return signalLevel; }
 }
