package pointOfUse.CartCompass;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class defaultActivity extends Activity {
	 /** Called when the activity is first created. */
	 WifiManager mainWifi;
	 WifiReceiver receiverWifi;
	 List<ScanResult> wifiList;
	 String myIP = "";
	 
	 public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
		  requestWindowFeature(Window.FEATURE_NO_TITLE);
		  
		  //get IP address
		  myIP = getLocalIpAddress();
		  
		  receiverWifi = new WifiReceiver();
		  
		  WebView webview = new WebView(this);
		  webview.getSettings().setJavaScriptEnabled(true);
		  webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		  webview.addJavascriptInterface(new JavaScriptInterface(this, receiverWifi), "Android");
		  webview.setWebChromeClient(new WebChromeClient() {
				@Override
				public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			                //Required functionality here
			                return super.onJsAlert(view, url, message, result);
			       }
		  });
		  //webview.getSettings().setAppCacheEnabled(false);
		  setContentView(webview);

		  float r = (float) (Math.random() * 10000);
		  webview.loadUrl("http://www.pointofusesolutions.com/kroger/index_live.php?r=" + r + "&local_IP=" + myIP);
		  
		 mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		 registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		 readAPs();
		
	 }
	 
	 public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			//Log.e(LOG_TAG, ex.toString());
			Toast.makeText(getApplicationContext(), "Error getting IP: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
		}
		return null;
	}

	 public class JavaScriptInterface {
		Context mContext;
		private WifiReceiver wi;
		
		JavaScriptInterface(Context c, WifiReceiver w) {
			mContext = c;
			wi = w;
		}
		public void showToast(String toast) {
			Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
		}
		public void exitApp() {
			int pid = android.os.Process.myPid();
			android.os.Process.killProcess(pid);
		}
		public String getReads() {
			return wi.getReads();
		}
		 
	 }

	 public void readAPs() {
		  mainWifi.startScan();
	 }

	 class WifiReceiver extends BroadcastReceiver {
		 
		 private String reads;
		 
		 public String getReads() {
				return reads;
		 }
				
		 public void setReads(String sent_reads) {
				this.reads = sent_reads;
		 }
		 
		  public void onReceive(Context c, Intent intent) {
				List<ScanResult> wifiList = mainWifi.getScanResults();
				//float f = (float) (Math.random() * 10000);
				
				String full_post = "";
				for(int i = 0; i < wifiList.size(); i++){
					ScanResult s = wifiList.get(i);
					
					full_post += s.BSSID + "_" + s.level + "~~"; 
					 //Log.i(getPackageName(), "Got AP:" + s.BSSID + " Signal Strength=" + s.level);
					 //Toast.makeText(c, "Got AP:" + s.BSSID + " Signal Strength=" + s.level, Toast.LENGTH_SHORT).show();
					 //send to site
					 //int done = (i == (wifiList.size()-1)?1:0);
				}
				
				this.setReads(full_post);
				
				/*try {
					String rr = "http://www.pointofusesolutions.com/kroger/update_device2.php?fp=" + full_post + "&ip=" + myIP + "&rui=" + f;
					URL u = new URL(rr);
					u.openStream();
					//Toast.makeText(c, "did post " + rr, Toast.LENGTH_SHORT).show();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block

					Toast.makeText(c, "bad url: " + e.getMessage(), Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Toast.makeText(c, "post failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				*/
				//do it again
				Timer t = new Timer();
				t.schedule(new TimerTask() {
				@Override
				public void run() {
					readAPs();
				}}, 2000);
		  }
	 }
}