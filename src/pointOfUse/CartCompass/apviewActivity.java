package pointOfUse.CartCompass;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
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
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class apviewActivity extends Activity {
	 /** Called when the activity is first created. */
	 WifiManager mainWifi;
	 WifiReceiver receiverWifi;
	 List<ScanResult> wifiList;
	 
	 //delay before app scans for access points
	 int read_latency = 1000;
	 //# of reads since last save
	 int reads_since_save = 0;
	 
	 //local ip
	 String myIP = "";
	 
	 //stores saved location data
	 HashMap<String, ArrayList<ArrayList<AccessPoint>>> locs = new HashMap<String, ArrayList<ArrayList<AccessPoint>>>();
	 
	 //stores current access point data
	 ArrayList<ArrayList<AccessPoint>> latest_reads = new ArrayList<ArrayList<AccessPoint>>();
	 //max number of continuously stored access point data based on time
	 int max_list_stored = 3;
	 
	 //HashMap of all saved locations and their delta to current read
	 //HashMap<String, Float> winner = new HashMap<String, Float>();
	 
	 public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  setContentView(R.layout.apview);
		  
		  //get IP address
		  myIP = getLocalIpAddress();
		  
		  receiverWifi = new WifiReceiver();
		  
		  mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		  registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		  readAPs();
		  
		  //drop-down menu for selecting location
		  final Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		  
		  ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.locations_array, android.R.layout.simple_spinner_item);
		  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		  spinner.setAdapter(adapter);
		  
		  //Location save button and listener
		  Button save = (Button) findViewById(R.id.save_location);
		  
		  //final Context ctx = this;
		  
		  save.setOnClickListener(new View.OnClickListener() {
		  
			public void onClick(View v) {
				//ProgressDialog dialog = ProgressDialog.show(ctx, "Loading", "Reading data, please refrain from moving for... " + (max_list_stored) + " seconds", true);

				locs.put(spinner.getSelectedItem().toString(), new ArrayList<ArrayList<AccessPoint>>(latest_reads));
				reads_since_save = 0;
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
			ap_success_rate = count / saved_avg.size();
			//doesn't work if there is 100% success rate so i'm going to make 
			if (ap_success_rate != 1) {
				float diff = 2 - ap_success_rate;
				avg = avg * diff;
			} else { avg = (float) (avg * 1.01); }
		}
		
		return avg;
	 }
	 
	 //over ride onStart, which is part of an app's lifecycle
	 protected void onStart() {
		 super.onStart();
		 readAPs();
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
	 
	 //grabs current wifi state based on the method getWifiState()
	 public String getWifiStateStr() {
		    switch (mainWifi.getWifiState()) {
		      case WifiManager.WIFI_STATE_DISABLING:
		        return "disabling";
		      case WifiManager.WIFI_STATE_DISABLED:
		        return "disabled";
		      case WifiManager.WIFI_STATE_ENABLING:
		        return "enabling";
		      case WifiManager.WIFI_STATE_ENABLED:
		        return "enabled";
		      case WifiManager.WIFI_STATE_UNKNOWN:
		        return "unknown";
		      default:
		        return null;  //or whatever you want for an error string
		    }
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
				
				for(int i = 0; i < wifiList.size(); i++){
					AccessPoint ap = new AccessPoint();
					ScanResult s = wifiList.get(i);
					calcLevel = WifiManager.calculateSignalLevel(s.level, num_levels);
					
					ap.setSS(calcLevel);
					ap.setBSSID(s.BSSID);
					
					current_aps.add(ap);
					
					display_post += "\nSSID: " + s.SSID + "\n -- BSSID: " + s.BSSID + "\n -- Signal: " + calcLevel;
				}

				//add the array list referencing all the access point objects to hashmap for latest reads
				latest_reads.add(current_aps);
				reads_since_save++;
				
				//TODO: only keep latest "max_list_stored" in latest_reads hashmap
				if (latest_reads.size() > max_list_stored) {
					latest_reads.remove(0);
				}
				
				//pass data to resultActivity
				//Intent i = getParent().getIntent(); //new Intent(getApplicationContext(), resultActivity.class);
				//i.putExtra("cur_ap", build_post);
				//startActivity(i);
				
				//if hashmap data that has saved location i.e. LOC1 access point data has data then we compare current
				//ap data and the saved location ap data
				if (!locs.isEmpty() && locs.size() > 1) {
					
					//View listview =  findViewById(R.id.loc_winner);
					
					ListView listView = (ListView) findViewById(android.R.id.list);
					String winner_out = "";
					float value = 0;
						
						for (Map.Entry<String, ArrayList<ArrayList<AccessPoint>>> entry : locs.entrySet()) {
							value = compareAP(entry.getValue(), latest_reads);
							
							TextView loc = new TextView(getApplicationContext());
							//winner.put(entry.getKey().toString(), value);
							winner_out += entry.getKey() + ": " + value + "\n";
						}
					
					//loc_winner.setText(winner_out);
					
				}
				
				final TextView show_aps = (TextView) findViewById(R.id.ap_reads);
				
				//String wifiState = getWifiStateStr();
				show_aps.setMovementMethod(new ScrollingMovementMethod());
				show_aps.setText("Stored Reads: " + latest_reads.size() + "\nReads Since Last Save: " + reads_since_save + "\n" + info + "\n" + display_post);
				
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