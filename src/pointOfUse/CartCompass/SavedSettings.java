package pointOfUse.CartCompass;


public class SavedSettings {
	//default settings
	public static int num_of_aps = 10;
	public static int num_of_saved_reads = 3;
	public static int dur_read_cycle = 1;
	
	public static void setNumAP(int s) {
		num_of_aps = s;
	}
	
	public static int getNumAP() {
		return num_of_aps;
	}
	
	public static void setNumReads(int r) {
		num_of_saved_reads = r;
	}
	
	public static int getNumReads() {
		return num_of_saved_reads;
	}
	
	public static void setDurCycle(int d) {
		dur_read_cycle = d;
	}
	
	public static int getDurCycle() {
		return dur_read_cycle;
	}
	
}
