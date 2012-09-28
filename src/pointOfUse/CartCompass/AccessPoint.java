package pointOfUse.CartCompass;

public class AccessPoint {
	int signal_level;
	String bssid;
	
	public void setSS(int ss) {
		this.signal_level = ss;
	}
	
	public int getSS() {
		return this.signal_level;
	}
	
	public void setBSSID(String bs) {
		this.bssid = bs;
	}
	
	public String getBSSID() {
		return this.bssid;
	}
}
