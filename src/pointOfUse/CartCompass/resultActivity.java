package pointOfUse.CartCompass;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class resultActivity extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settingsview);
	
		  //drop-down menu for saving number of APs
		  final Spinner ap_spinner = (Spinner) findViewById(R.id.num_aps_spinner);
		  //drop-down menu for saving number of reads
		  final Spinner read_spinner = (Spinner) findViewById(R.id.num_reads_spinner);
		  //drop-down menu for saving duration of read cycle
		  final Spinner dur_spinner = (Spinner) findViewById(R.id.dur_cycle_spinner);
		  
		  ArrayAdapter<CharSequence> adapter_1 = ArrayAdapter.createFromResource(this, R.array.num_aps_array, android.R.layout.simple_spinner_item);
		  adapter_1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		  
		  ap_spinner.setAdapter(adapter_1);

		  ArrayAdapter<CharSequence> adapter_2 = ArrayAdapter.createFromResource(this, R.array.num_reads_array, android.R.layout.simple_spinner_item);
		  adapter_2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		  
		  read_spinner.setAdapter(adapter_2);
		  
		  ArrayAdapter<CharSequence> adapter_3 = ArrayAdapter.createFromResource(this, R.array.dur_cycle_array, android.R.layout.simple_spinner_item);
		  adapter_3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		  
		  dur_spinner.setAdapter(adapter_3);
		  
		  //Location save button and listener
		  Button save = (Button) findViewById(R.id.save_settings);

		  save.setOnClickListener(new View.OnClickListener() {
		  
			public void onClick(View v) {
				SavedSettings.setNumAP( Integer.valueOf(ap_spinner.getSelectedItem().toString()) );
				SavedSettings.setNumReads( Integer.valueOf(read_spinner.getSelectedItem().toString()) );
				SavedSettings.setDurCycle( Integer.valueOf(dur_spinner.getSelectedItem().toString()) );
			}
			
		  });
		
	}
	
}