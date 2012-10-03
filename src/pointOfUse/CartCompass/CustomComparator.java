package pointOfUse.CartCompass;

import java.util.Comparator;

public class CustomComparator implements Comparator<AccessPoint> {
	public int compare(AccessPoint o1, AccessPoint o2) {
        return ((Integer) o1.getSS()).compareTo((Integer) o2.getSS());
    }
}