package orz.macrobull.luckymoney;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
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
	static private PowerManager powerMan;
	static private PowerManager.WakeLock wakeLock;
	static private KeyguardManager keyMan;
	static private KeyguardManager.KeyguardLock keyLock;

	static public boolean getBindStatus() {
		return mBinding;
	}

	static public boolean catchTheGame() {
		boolean ret = mInGame;
		mInGame = false;
		return ret;
	}

	static public void releaseLock() {
		Log.d("wakelock", String.valueOf(wakeLock.isHeld()));
		if (wakeLock.isHeld()) wakeLock.release();
		keyLock.reenableKeyguard();
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder mIBinder = super.onBind(intent);
		mBinding = true;

		powerMan = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerMan.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeLock");

		keyMan = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		keyLock = keyMan.newKeyguardLock("KeyLock");

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
			sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
			Log.d("wakelock", String.valueOf(wakeLock.isHeld()));
			if (!wakeLock.isHeld()) {
				keyLock.disableKeyguard();
				wakeLock.acquire();
				try {
					while (!powerMan.isInteractive()) {
						Log.d("keyguard", String.valueOf(keyMan.inKeyguardRestrictedInputMode()));
						Log.d("keyguard", String.valueOf(powerMan.isScreenOn()));
						Log.d("keyguard", String.valueOf(powerMan.isInteractive()));
						Log.d("keyguard", "locked");
						Thread.sleep(100); // bad workaround;
					}
				} catch (Exception e){
					//
				}
			}
			mInGame = true;
			notification.contentIntent.send(this, 0, new Intent());
		} catch (PendingIntent.CanceledException e) {
			Log.w("pendingIntent", "Sending pendingIntent failed.");
		}
	}

}
