package orz.macrobull.luckymoney;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;

import java.util.List;

/**
 * Created by macrobull on 12/28/15.
 */
public class AService extends AccessibilityService {

	enum State {
		WALK,
		CHAT,
		NEW,
		OPEN,
		DETAIL,
	}

	static State state = State.WALK;
	static Integer openStackSize = 0;
	static boolean lock = false;

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (lock) return;
		lock = true;

		try {
//			Log.i("getPackageName", event.getPackageName().toString());
//			Log.i("getRecord", (event.getRecordCount()>0)?event.getRecord(0).toString():"null");
//			Log.i("getSource", (event.getSource() != null)?event.getSource().toString():"null");
//			Log.i("getText[]", (!event.getText().isEmpty()) ? event.getText().toString() : "[]");
			process(event);
		} finally {
			lock = false;
		}

	}

	@Override
	public void onInterrupt() {
		Log.d("onInterrupt", "!");
	}

	Integer getFromNode(AccessibilityNodeInfo root){
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		for (AccessibilityNodeInfo node: mNodes){
			Log.d("node", node.toString());
			Log.d("node.parent", node.getParent().toString());
			Log.d("Click", "GET");
			node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
		}

		return mNodes.size();
	}


	Integer getFromLastNode(AccessibilityNodeInfo root){
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		if (mNodes.size() == 0) return 0;
		AccessibilityNodeInfo node = mNodes.get(mNodes.size() - 1);
		Log.d("node", node.toString());
		Log.d("node.parent", node.getParent().toString());
		Log.d("Click", "GET");
		node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);

		return 1;
	}

	void logState(){
		Log.d("state", state.toString() + "/" + openStackSize.toString());
//		if (state.equals(State.OPEN) || state.equals(State.DETAIL)) {
//			Toast.makeText(this, state.toString() + "/" + openStackSize.toString(), Toast.LENGTH_SHORT).show();
//		}
	}

	void process(AccessibilityEvent event){
		AccessibilityNodeInfo source;
		AccessibilityRecord record;

		if (event.getSource() == null) return; logState();
		source = event.getSource();

		switch (state) {
			case WALK:
				openStackSize = getFromNode(source);
				if (openStackSize > 0) {
					state = State.OPEN;
				} else {
					state = State.CHAT;
				}

				break;
			case OPEN:
				Log.d("open", source.toString());
				if (source.getClassName().toString().equals("android.widget.Button")) {
					Log.d("Click", "OPEN");
					source.performAction(AccessibilityNodeInfo.ACTION_CLICK);
					state = State.DETAIL;
					break;
				}

			case DETAIL:
				if (!source.getClassName().toString().equals("android.widget.LinearLayout")) {
					if (source.getText() == null) break;
					if (!( source.getText().toString().equals("Details")
						|| source.getText().toString().equals("红包详情")
						)) break;
				}

				Log.d("Click", "BACK");
				performGlobalAction(GLOBAL_ACTION_BACK);
				openStackSize -= 1;

				if (openStackSize > 0) {
					state = State.OPEN;
				} else {
//					Log.d("Click", "BACK");
//					performGlobalAction(GLOBAL_ACTION_BACK);
					state = State.CHAT;
				}

				break;
			case CHAT:
				if (NLService.catchTheGame()){
					state = State.NEW;
				} else {
//					Log.d("chat", source.toString());
				}
				break;
			case NEW:
				openStackSize = getFromLastNode(source);

//				if (event.getRecordCount()<=0) return; logState();
//				record = event.getRecord(0); // Wechat only add 1 node once.
//
//				Log.i("record", record.toString());
//				if (record.getText().toString().equals("[1]")) {
//					Log.i("record", record.toString());
//					source = record.getSource();
//					source = source.getChild(1);
//
//					for (Integer i=0; i<source.getChildCount(); i++){
//						Log.i("node" + i.toString(), source.getChild(i).toString());
//						Log.i("Click", "GET");
//						source.getChild(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//					}

//					source = source.getParent();
//					Log.i("record.source", source.toString());
//					openStackSize = getFromNode(source);
//				}

				if (openStackSize>0) state = State.OPEN;
				break;
		}

	}
}
