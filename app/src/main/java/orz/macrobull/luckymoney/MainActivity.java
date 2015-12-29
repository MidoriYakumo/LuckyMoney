package orz.macrobull.luckymoney;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		updateStatus();
	}

	@Override
	protected void onResume() {
		super.onResume();

		updateStatus();
	}

	private void updateStatus() {
		Boolean nl_status, as_status;
		as_status = false;

		nl_status = NLService.getBindStatus();
		if (!nl_status) try {
//			startService(new Intent(this, NLService.class));
//			nl_status = NLService.getBindStatus();
			Toast.makeText(this, "May recheck notification listener!", Toast.LENGTH_LONG).show();
//			openNLSetting(null);
		} catch (Exception e){
			Log.w("Start NLService fail:", e.getMessage());
		}

		AccessibilityManager accessibilityManager =
				(AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
		List<AccessibilityServiceInfo> accessibilityServices =
				accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
		for (AccessibilityServiceInfo info : accessibilityServices) {
			if (info.getId().equals(getPackageName() + "/.AService")) {
				as_status = true;
			}
		}

		Button b_nl = (Button) findViewById(R.id.b_nl);
		Button b_as = (Button) findViewById(R.id.b_as);
		b_nl.setText("Notification Listener Service: " + (nl_status?"ON":"OFF"));
		b_as.setText("Accessibility Service: " + (as_status?"ON":"OFF"));
	}

	public void openNLSetting(View v){
		Intent intent=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
		startActivity(intent);
	}

	public void openASSetting(View v){
		Intent intent=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		startActivity(intent);
	}

}
