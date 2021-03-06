package pointOfUse.CartCompass;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class apviewActivity extends Activity {
	 /** Called when the activity is first created. */
	 WifiManager mainWifi;
	 WifiReceiver receiverWifi;
	 List<ScanResult> wifiList;
	 
	 //delay before app scans for access points
	 int read_latency = SavedSettings.dur_read_cycle * 1000;
	 //max number of continuously stored access point data based on time
	 int max_list_stored = SavedSettings.num_of_saved_reads;
	 //max number of access points saved during a cycle
	 int max_aps_saved = SavedSettings.num_of_aps;
	 
	 //# of reads since last save
	 int reads_since_save = 0;
	 
	 //local ip
	 String myIP = "";
	 
	 //stores saved location data
	 HashMap<String, ArrayList<ArrayList<AccessPoint>>> locs = new HashMap<String, ArrayList<ArrayList<AccessPoint>>>();
	 
	 //stores current access point data
	 ArrayList<ArrayList<AccessPoint>> latest_reads = new ArrayList<ArrayList<AccessPoint>>();
	 
	 //HashMap of all saved locations and their delta to current read
	 //HashMap<String, Integer> winner = new HashMap<String, Integer>();
	 
	 public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  setContentView(R.layout.apview);
		  
		  //get IP address
		  myIP = getLocalIpAddress();
		  
		  receiverWifi = new WifiReceiver();
		  
		  mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		  registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		  //start scan
		  readAPs();
		  
		  //drop-down menu for selecting location
		  final Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		  
		  ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.locations_array, android.R.layout.simple_spinner_item);
		  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		  spinner.setAdapter(adapter);
		  
		  //Location save button and listener
		  Button save = (Button) findViewById(R.id.save_location);
		  
		  save.setOnClickListener(new View.OnClickListener() {
		  
			public void onClick(View v) {
				
				locs.put(spinner.getSelectedItem().toString(), new ArrayList<ArrayList<AccessPoint>>(latest_reads));
				reads_since_save = 0;
				//Rest.connect("http://pointofusesolutions.com/kroger/processors/admin_mode.php?action=get_location_and_ap");

			}
			
		  });
		  
		  //This button essentially attempts to reconnect the wifi in the case of a timeout
		  /*Button update_aps = (Button) findViewById(R.id.update_aps);
				  
			  update_aps.setOnClickListener(new View.OnClickListener() {
				
				  public void onClick(View v) {
					  String wifiState = getWifiStateStr();
					  
					  if (wifiState == "enabled" || wifiState == "enabling") {
						  mainWifi.reassociate();
					  } else if (wifiState == "disabled" || wifiState == "disabling" || wifiState == "unknown") {
						  mainWifi.reconnect();
					  }
					  
					  readAPs();
					  //show_aps.setText("APs: " + receiverWifi.getReads());	
				  }
				  
			  });*/
	 }
	 
	 //Grabs two data reads with all associated access points and find the difference between them
	 //and returns the average delta of those access points
	 //So confusing don't even try to figure it out, arraylist-ception!
	 public float compareAP(ArrayList<ArrayList<AccessPoint>> current_aps, ArrayList<ArrayList<AccessPoint>> saved_aps) {
		float total = 0;
		int count = 0;
		float avg = 0;
		float ap_success_rate = 0;
		
		HashMap<String, Float> saved_avg = new HashMap<String, Float>();
		HashMap<String, Float> current_avg = new HashMap<String, Float>();
		
		for (ArrayList<AccessPoint> c : current_aps) {
			
			for (AccessPoint curr : c) {
				if (current_avg.containsKey(curr.getBSSID())) {
					float tmp_val = (current_avg.get(curr.getBSSID()) + curr.getSS()) / 2;
					current_avg.put(curr.getBSSID(), tmp_val);
				} else {
					current_avg.put(curr.getBSSID(), (float) curr.getSS());
				}

			}
			
		}

		for (ArrayList<AccessPoint> s : saved_aps) {
			
			for (AccessPoint sav : s) {

				if (saved_avg.containsKey(sav.getBSSID())) {
					float tmp_val = (saved_avg.get(sav.getBSSID()) + sav.getSS()) / 2;
					saved_avg.put(sav.getBSSID(), tmp_val);
				} else {
					saved_avg.put(sav.getBSSID(), (float) sav.getSS());
				}
			}
			
		}
		
		for (Map.Entry<String, Float> sa : saved_avg.entrySet()) {
			for (Map.Entry<String, Float> cu : current_avg.entrySet()) {
				
				if (sa.getKey().equals(cu.getKey())) {
					float delta = Math.abs(sa.getValue() - cu.getValue());
					total += delta;
					count++;
					break;
				} 
			}
		}
		
		if (count > 0) {
			avg = total / count;
			//Concept: ratio of correct access point hits out of the saved location data based on current scan data
			//Take the alternative ratio of that success rate and multiply it by the average.
			//The more correct hits means the avg will be lower based on its success rate.
			//e.g. LOC1.) 93% success rate with avg delta of 2 : 1.07*2 = 2.14
			//e.g. LOC2.) 65% success rate with avg delta of 2 : 1.35*2 = 2.7
			// 2.14 < 2.70 = LOC1 wins even though the delta between the two locations is 0.
			ap_success_rate = (float) count / (float) saved_avg.size();
			//doesn't work if there is 100% success rate so i'm going to make 
			if (ap_success_rate != 1) {
				float diff = 2 - ap_success_rate;
				avg = avg * diff;
			 } else { avg = (float) (avg * 1.01); }
		}
		
		return avg;
		//return avg + " -- " + (ap_success_rate*100) + "% success; cur: " + count + " saved: " + saved_avg.size();
	 }
	 
	 //over ride onStart, which is part of an app's lifecycle
	 protected void onStart() {
		 super.onStart();
		 readAPs();
	 }
	 
	 //After activity gets focus again
	 protected void onResume() {
		 super.onResume();
		 read_latency = SavedSettings.dur_read_cycle * 1000;
		 max_list_stored = SavedSettings.num_of_saved_reads;
		 max_aps_saved = SavedSettings.num_of_aps;
	 }
	 
	 //over ride onStop, which is part of an app's lifecycle
	 protected void onStop() {
		 super.onStop();
		 //Kills the process when app isn't visible, should only be used for development purposes
		 android.os.Process.killProcess(android.os.Process.myPid());
	 }
	 
	 //grabs local ip address
	 public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Toast.makeText(getApplicationContext(), "Error getting IP: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
		}
		return null;
	}

	//starts the scanning process which then kicks off onReceive
	 public void readAPs() {
		  mainWifi.startScan();
	 }
	 
	 class WifiReceiver extends BroadcastReceiver {
		 
		  public void onReceive(Context c, Intent intent) {
				List<ScanResult> wifiList = mainWifi.getScanResults();
				WifiInfo info = mainWifi.getConnectionInfo();
				
				String display_post = "";
				//dBm -> ratio out of num_levels
				int num_levels = 41;
				int calcLevel;
				//array list for temporary key storage of last reads
				ArrayList<AccessPoint> current_aps = new ArrayList<AccessPoint>();
				String win_name = "";
				//float win_val = 0;
				
				for(int i = 0; i < wifiList.size(); i++){
					AccessPoint ap = new AccessPoint();
					ScanResult s = wifiList.get(i);
					calcLevel = WifiManager.calculateSignalLevel(s.level, num_levels);
					
					ap.setSS(calcLevel);
					ap.setBSSID(s.BSSID);
					
					current_aps.add(ap);
					
					display_post += "\nSSID: " + s.SSID + "\n -- BSSID: " + s.BSSID + "\n -- Signal: " + calcLevel;
				}
				
				//only save the top allowed access points
				Collections.sort(current_aps, new CustomComparator());
				
				if (current_aps.size() > max_aps_saved) {
					System.out.println("CURRENT AP SIZE TOO BIG");
					int tmp_diff = current_aps.size() - max_aps_saved;
					for (int i = 0; i < tmp_diff; i++) {
						current_aps.remove(0);
						System.out.println("CURRENT AP REMOVED");
					}
				}
				
				//add the array list referencing all the access point objects to hashmap for latest reads
				latest_reads.add(current_aps);
				reads_since_save++;
				
				//only keep latest "max_list_stored" in latest_reads hashmap
				if (latest_reads.size() > max_list_stored) {
					int tmp_diff = latest_reads.size() - max_list_stored;
					for (int i = 0; i < tmp_diff; i++) {
						latest_reads.remove(0);
					}
				}
				
				//if hashmap data that has saved location i.e. LOC1 access point data has data then we compare current
				//ap data and the saved location ap data
				if (!locs.isEmpty() && locs.size() > 1) {
					
					String winner_out = "";
					float value = 0;
					TextView loc_winner = (TextView) findViewById(R.id.loc_winner);
					TextView loc_results = (TextView) findViewById(R.id.loc_results);
					HashMap<String, Float> winner = new HashMap<String, Float>();
					
					for (Map.Entry<String, ArrayList<ArrayList<AccessPoint>>> entry : locs.entrySet()) {
						value = compareAP(latest_reads, entry.getValue());

						winner.put(entry.getKey(), value);
						winner_out += entry.getKey() + ": " + value + "\n";
						
					}
					
					ArrayList<Float> find_loc = new ArrayList<Float>();
					for (Map.Entry<String, Float> sort : winner.entrySet()) {
						find_loc.add(sort.getValue());
					}
					
					Collections.sort(find_loc);
					
					for (Map.Entry<String, Float> sort : winner.entrySet()) {
						if (find_loc.get(0) == sort.getValue()) {
							win_name = sort.getKey();
							//win_val = sort.getValue();
						}
					}
					
					loc_winner.setTextColor(Color.RED);
					loc_winner.setText("Winner: " + win_name + "\n");
					loc_results.setText(winner_out);
					
				}
				
				final TextView show_aps = (TextView) findViewById(R.id.ap_reads);
				
				//String wifiState = getWifiStateStr();
				show_aps.setMovementMethod(new ScrollingMovementMethod());
				show_aps.setText("Stored Reads: " + latest_reads.size() + "\nReads Since Last Save: " + reads_since_save + "\nSaved APs Per Read: " + current_aps.size() + "\n" + info + "\n" + display_post);
				
				//do it again
				Timer t = new Timer();
				t.schedule(new TimerTask() {
				@Override
				public void run() {
					readAPs();
				}}, read_latency);
				
		  }
	 }
}