package orz.macrobull.luckymoney;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;
import android.util.Log;

import java.util.List;

/**
 * Created by macrobull on 12/28/15.
 * ...
 */
public class AService extends AccessibilityService {

	static State state = State.CHAT_WALK;
	static Integer openStackSize = 0;
	static Integer newSize = 0;
	static Integer lastNode = 0;
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
			Log.d("click", "GET" + Integer.valueOf(node.hashCode()).toString());
			node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
			lastNode = node.hashCode();
		}

		return mNodes.size();
	}

	Integer getFromLastNode(AccessibilityNodeInfo root, Integer size, boolean dup){
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		size = Math.min(size, mNodes.size());
		for (Integer i=mNodes.size()-size; i<mNodes.size(); i++){
			AccessibilityNodeInfo node = mNodes.get(i);
			Log.d("node", node.toString());
			Log.d("node.parent", node.getParent().toString());
			if (dup || (lastNode != node.hashCode())) {
				Log.d("click", "GET" + Integer.valueOf(node.hashCode()).toString());
				node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
				lastNode = node.hashCode();
			} else {
				Log.d("node duplicate", Integer.valueOf(node.hashCode()).toString());
				size -= 1;
			}
		}

		return size;
	}

	void logState(){
		Log.d("state", state.toString() + "/" + openStackSize.toString() + "/" + newSize.toString());
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
				performGlobalAction(GLOBAL_ACTION_BACK); // Not available when screen is locked.
				openStackSize -= 1;

				if (openStackSize > 0) {
					state = State.OPEN;
				} else {
					NLService.releaseLock();
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
					if (record.getText() == null) return;

					Log.d("chat record", record.toString());
					Log.d("chat source", source.toString());

					boolean maybeMoney = false;

					if (record.getText().size()>3) {
						for (CharSequence cText : record.getText()) {
							if (cText.toString().matches(getResources().getString(R.string.chat_pattern))) {
								maybeMoney = true;
								break;
							}
						}
					}

					if (record.getText().toString().matches("\\[\\d+\\]")) maybeMoney = true;

					if (maybeMoney) {
//						source = source.getParent();
						Log.d("source", source.toString());
						openStackSize += getFromLastNode(source, 1, false);
						if (openStackSize>0) state = State.OPEN;
					}

				}
				break;
			case CHAT_NEW:
				openStackSize += getFromLastNode(source, newSize, true);
				newSize -= openStackSize;

				if (openStackSize>0) state = State.OPEN;
				break;
		}

	}

	enum State {
		CHAT_WALK,
		CHAT_IDLE,
		CHAT_NEW,
		OPEN,
		DETAIL,
	}
}
