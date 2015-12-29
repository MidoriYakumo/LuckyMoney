package orz.macrobull.luckymoney;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by macrobull on 12/28/15.
 * ...
 */

public class NLService extends NotificationListenerService {

	static private boolean mBinding = false;
	static private boolean mInGame = false;
	static private PowerManager.WakeLock wakeLock;

	static public boolean getBindStatus() {
		return mBinding;
	}

	static public boolean catchTheGame() {
		boolean ret = mInGame;
		mInGame = false;
		return ret;
	}

	static public void releaseWakeLock() {
		Log.d("wakelock", String.valueOf(wakeLock.isHeld()));
		wakeLock.release();
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder mIBinder = super.onBind(intent);
		mBinding = true;

		wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(
				PowerManager.FULL_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP
						| PowerManager.ON_AFTER_RELEASE, "WakeLock");

		return mIBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		boolean mOnUnbind = super.onUnbind(intent);
		mBinding = false;
		return mOnUnbind;
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		if (!sbn.getPackageName().equals(getResources().getString(R.string.target_pname))) return;

		Notification notification = sbn.getNotification();
		String fullText = notification.extras.getString("android.text");

		if (fullText == null) return;

//		Log.i("tickerText", n.tickerText.toString());
//		Log.i("extras", n.extras.toString());
		Log.d("fullText", fullText);

		if (!fullText.matches(getResources().getString(R.string.notify_pattern))) return;

		Log.d("contentIntent", notification.contentIntent.toString());
		try {
			Log.d("wakelock", String.valueOf(wakeLock.isHeld()));
			wakeLock.acquire();
			sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
			mInGame = true;
			notification.contentIntent.send(this, 0, new Intent());
		} catch (PendingIntent.CanceledException e) {
			Log.w("pendingIntent", "Sending pendingIntent failed.");
		}
	}

}
