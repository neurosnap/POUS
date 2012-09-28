package pointOfUse.CartCompass;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class resultActivity extends Activity {
	TextView t = new TextView(this);
	Intent i = getParent().getIntent();
	String value = "";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Bundle extras = getIntent().getExtras();
		//if (extras != null) {
		    value = i.getStringExtra("cur_ap");
			t.setText(value);
			setContentView(t);
		//}
	
	}
	
	protected void onStart() {
		value = i.getStringExtra("cur_ap");
		t.setText(value);
		//setContentView(t);
	}
}