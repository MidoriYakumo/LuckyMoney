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
		CHAT_WALK,
		CHAT_IDLE,
		CHAT_NEW,
		OPEN,
		DETAIL,
	}

	static State state = State.CHAT_WALK;
	static Integer openStackSize = 0;
	static Integer newSize = 0;
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
			Log.d("click", "GET");
			node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
		}

		return mNodes.size();
	}


	Integer getFromLastNode(AccessibilityNodeInfo root, Integer size){
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		size = Math.min(size, mNodes.size());
		for (Integer i=mNodes.size()-size; i<mNodes.size(); i++){
			AccessibilityNodeInfo node = mNodes.get(i);
			Log.d("node", node.toString());
			Log.d("node.parent", node.getParent().toString());
			Log.d("click", "GET");
			node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
		}

		return size;
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
			case CHAT_WALK:
				openStackSize += getFromNode(source);
				if (openStackSize > 0) {
					state = State.OPEN;
				} else {
					state = State.CHAT_IDLE;
				}

				break;
			case OPEN:
				Log.d("open", source.toString());
				if (source.getClassName().toString().equals("android.widget.Button")) {
					Log.d("click", "OPEN");
					source.performAction(AccessibilityNodeInfo.ACTION_CLICK);
					state = State.DETAIL;
					break;
				}

			case DETAIL:
//				if (!source.getClassName().toString().equals("android.widget.LinearLayout")) {
					if (source.getText() == null) break;
					if (!( source.getText().toString().equals("Details")
						|| source.getText().toString().equals("红包详情")
						)) break;
//				}

				Log.d("click", "BACK");
				performGlobalAction(GLOBAL_ACTION_BACK);
				openStackSize -= 1;

				if (openStackSize > 0) {
					state = State.OPEN;
				} else {
//					Log.d("click", "BACK");
//					performGlobalAction(GLOBAL_ACTION_BACK);
					state = State.CHAT_IDLE;
				}

				break;
			case CHAT_IDLE:
				if (NLService.catchTheGame()){
					newSize += 1;
					state = State.CHAT_NEW;
				} else {
					if (event.getRecordCount()<=0) return;
					record = event.getRecord(0); // Wechat only add 1 node once.
					Log.d("chat record", record.toString());
					Log.d("chat source", source.toString());

					for (CharSequence cText: record.getText()){
						if (cText.toString().matches(getResources().getString(R.string.chat_pattern))) {
							Log.d("source", source.toString());
							openStackSize += getFromLastNode(source, 1);
							if (openStackSize>0) state = State.OPEN;
							break;
						}
					}

				}
				break;
			case CHAT_NEW:
				openStackSize += getFromLastNode(source, newSize);
				newSize -= openStackSize;

				if (openStackSize>0) state = State.OPEN;
				break;
		}

	}
}
