package pointOfUse.CartCompass;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class defaultActivity extends TabActivity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TabHost tabHost = getTabHost();       
      
        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("AP Reads").setContent(new Intent(this, apviewActivity.class)));
      
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Settings").setContent(new Intent(this, resultActivity.class)));
        
        tabHost.setCurrentTab(0); 
    }
}