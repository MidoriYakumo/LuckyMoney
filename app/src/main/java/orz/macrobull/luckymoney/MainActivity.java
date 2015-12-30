package orz.macrobull.luckymoney;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * 主界面
 */
public class MainActivity extends AppCompatActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
//		getSupportActionBar().setDisplayShowHomeEnabled(true);
//		getSupportActionBar().setIcon(R.mipmap.ic_launcher);

		updateStatus();
	}

	@Override
	protected void onResume() {
		super.onResume();

		updateStatus();
	}

	/**
	 * 更新信息
	 */
	private void updateStatus() {
		Boolean nl_status, as_status;
		as_status = false;

		nl_status = NLService.getBindStatus(); // 通知监听服务状态由服务绑定状态标识
		if (!nl_status) try {
//			startService(new Intent(this, NLService.class));
//			nl_status = NLService.getBindStatus(); // #FIXME 怎样程序启动这个服务?
			Toast.makeText(this, "May recheck notification listener!", Toast.LENGTH_SHORT).show(); // 那只有手动启动啦
//			openNLSetting(null);
		} catch (Exception e) {
			Log.w("Start NLService fail:", e.getMessage());
		}

		AccessibilityManager accessibilityManager =
				(AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
		List<AccessibilityServiceInfo> accessibilityServices =
				accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
		for (AccessibilityServiceInfo info : accessibilityServices) {
			if (info.getId().equals(getPackageName() + "/.AService")) { // 检索辅助服务, 确认运行状态
				as_status = true;
				break;
			}
		}

		Button b_nl = (Button) findViewById(R.id.b_nl);
		Button b_as = (Button) findViewById(R.id.b_as);
		b_nl.setText("通知监听服务: " + (nl_status ? "已启动" : "未启动或未启用"));
		b_as.setText("点击辅助服务: " + (as_status ? "已启动" : "未启用"));


		TextView t_stat = (TextView) findViewById(R.id.t_stat);
		t_stat.setText(AService.getStatistics()); // 显示统计数据
	}

	/**
	 * 打开设置中的通知监听选项
	 *
	 * @param v
	 */
	public void openNLSetting(View v) {
//		Intent intent=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS); // Android Lint说API22+再使用这个
		Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
		startActivity(intent);
	}

	/**
	 * 打开设置中的辅助服务选项
	 *
	 * @param v
	 */
	public void openASSetting(View v) {
		Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		startActivity(intent);
	}

}
