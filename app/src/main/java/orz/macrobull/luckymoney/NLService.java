package orz.macrobull.luckymoney;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by macrobull on 12/28/15.
 * ...
 */

public class NLService extends NotificationListenerService {

	static private boolean mBinded = false;
	static private boolean mInGame = false;

	@Override
	public IBinder onBind(Intent intent) {
		IBinder mIBinder =  super.onBind(intent);
		mBinded = true;
		return mIBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		boolean mOnUnbind = super.onUnbind(intent);
		mBinded = false;
		return mOnUnbind;
	}

	static public boolean getBindStatus(){
		return mBinded;
	}

	static public boolean catchTheGame(){
		boolean ret = mInGame;
		mInGame = false;
		return ret;
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		if (!sbn.getPackageName().equals(getResources().getString(R.string.target_pname))) return;

		Notification notification = sbn.getNotification();
		String fullText = notification.extras.getString("android.text");

//		Log.i("tickerText", n.tickerText.toString());
//		Log.i("extras", n.extras.toString());
		Log.d("fullText", fullText);

		if (!fullText.matches(getResources().getString(R.string.notify_pattern))) return;

		Log.d("contentIntent", notification.contentIntent.toString());
		try {
			sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
			mInGame = true;
			notification.contentIntent.send(this, 0, new Intent());
		} catch (PendingIntent.CanceledException e) {
			Log.w("pendingIntent", "Sending pendingIntent failed.");
		}
	}

}
