package orz.macrobull.luckymoney;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;
import android.util.Log;

import java.util.List;

/**
 * Created by macrobull on 12/28/15.
 * 辅助点击服务
 */
public class AService extends AccessibilityService {

	static State state = State.CHAT_WALK; 				// 服务状态
	static Integer size_open = 0; 						// 已点开的红包数
	static Integer size_new = 0; 		 				// 待处理的新红包数
	static Integer lastNode = 0; 		 				// 简单记录上一红包节点hashcode去重复
	static boolean mutex = false; 		 				// 互斥锁

	// 统计信息
	static Integer cnt_get = 0; 						// 点开的红包数
	static Integer cnt_open = 0; 						// 拆开的红包数
	static Integer cnt_detail = 0; 						// 进入详情数
	static Integer cnt_new = 0; 						// 捕获通知次数

	static Float amount_total = Float.valueOf(0); 		// 红包总金额
	static Float amount_success = Float.valueOf(0); 	// 成功抢到的红包总金额

	/*
	 * 为了确保获得金额信息, 设置详情标志
	 * 1: 红包有效
	 * 2: 左上角"详情"出现
	 * 4: 金额出现
	 */
	static Integer detail_flags = 0;

	/**
	 * 供主界面显示统计信息
	 * @return
	 */
	public static String getStatistics(){
		return String.format(
				"点了%d个红包, 开了%d个\n抢到了%d个红包\n从通知抢了%d次"
						+ "\n在路过的%.2f元中抢到了%.2f元"
				, cnt_get, cnt_detail, cnt_open, cnt_new, amount_total, amount_success);
	}

	/**
	 * 监视UI变更事件
	 * @param event
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (mutex) {
			Log.w("onAccessibilityEvent", "MUTEX!");
			return;
		}
		mutex = true;

		try {
//			Log.i("getPackageName", event.getPackageName().toString());
//			Log.i("getRecord", (event.getRecordCount()>0)?event.getRecord(0).toString():"null");
//			Log.i("getSource", (event.getSource() != null)?event.getSource().toString():"null");
//			Log.i("getText[]", (!event.getText().isEmpty()) ? event.getText().toString() : "[]");
			process(event); // 测试表明source和record有参考价值
		} finally {
			mutex = false;
		}

	}

	/**
	 * 按要求重载
	 */
	@Override
	public void onInterrupt() {
		Log.d("onInterrupt", "!");
	}

	/**
	 * 搜索包含红包的UI节点, 点击所有
	 * @param root 根UI节点
	 * @return 成功点击的红包数
	 */
	Integer getFromNode(AccessibilityNodeInfo root){
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		for (AccessibilityNodeInfo node: mNodes){
			Log.d("node", node.toString());
			Log.d("node.parent", node.getParent().toString()); // 有时候没有父节点, 蜜汁bug
			Log.d("click", "GET" + Integer.valueOf(node.hashCode()).toString());
			node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK); // TextView不能点, 点的是ListView, 详情查看clickable
			cnt_get += 1;
			lastNode = node.hashCode();
		}

		return mNodes.size(); // 即搜索结果数目
	}

	/**
	 * 搜索包含红包的UI节点, 点击末几个
	 * @param root 根UI节点
	 * @param size 点击最后size个
	 * @param ignoreDup  是否无视重复检测
	 * @return 成功点击的红包数
	 */
	Integer getFromLastNode(AccessibilityNodeInfo root, Integer size, boolean ignoreDup){
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		size = Math.min(size, mNodes.size()); // 先设成功点击数为预计点击的红包数目
		for (Integer i=mNodes.size()-size; i<mNodes.size(); i++){
			AccessibilityNodeInfo node = mNodes.get(i);
			Log.d("node", node.toString());
			Log.d("node.parent", node.getParent().toString());
			if (ignoreDup || (lastNode != node.hashCode())) { // 非重复红包, 点击
				Log.d("click", "GET" + Integer.valueOf(node.hashCode()).toString());
				node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
				cnt_get += 1;
				lastNode = node.hashCode();
			} else {
				Log.d("node duplicate", Integer.valueOf(node.hashCode()).toString());
				size -= 1; // 重复红包, 减少成功计数
			}
		}

		return size;
	}

	void logState(){
		Log.d("state", state.toString() + "/" + size_open.toString() + "/" + size_new.toString());
	}

	AccessibilityNodeInfo source;
	AccessibilityRecord record;

	/**
	 * 处理UI变动
	 * @param event
	 */
	void process(AccessibilityEvent event){

		if (event.getSource() == null) return; logState();
		source = event.getSource();

		switch (state) {
			case CHAT_WALK:
				size_open += getFromNode(source);
				if (size_open > 0) {
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
					cnt_open += 1;
					detail_flags = 1;
					state = State.DETAIL;
					break;
				}

			case DETAIL:
//				Log.d("detail", source.toString());
//				if (!source.getClassName().toString().equals("android.widget.LinearLayout")) {
				if (source.getText() == null) break;
				Log.d("detail text", source.getText().toString());

				if (!( source.getText().toString().equals("Details")
						|| source.getText().toString().equals("红包详情")
				)) detail_flags |= 2;

				if (source.getText().toString().matches("\\d+\\.\\d\\d")){
					try {
						Log.d("amount", "got value:" + source.getText().toString());
						amount_total += Float.valueOf(source.getText().toString());
						if ((detail_flags & 1)>0) amount_success += Float.valueOf(source.getText().toString());
						detail_flags |= 4;
					} catch (Exception e){
						Log.w("amount", e.getMessage());
					}
				}
//				}
				if ((detail_flags & 6) != 6) return;
				detail_flags = 0;

				Log.d("click", "BACK");
				performGlobalAction(GLOBAL_ACTION_BACK); // Not available when screen is locked.
				cnt_detail += 1;
				size_open -= 1;

				if (size_open > 0) {
					state = State.OPEN;
				} else {
					NLService.releaseLock();
					state = State.CHAT_IDLE;
				}

				break;
			case CHAT_IDLE:
				if (NLService.catchTheGame()){
					cnt_new += 1;
					size_new += 1;
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
						size_open += getFromLastNode(source, 1, false);
						if (size_open >0) state = State.OPEN;
					}

				}
				break;
			case CHAT_NEW:
				size_open += getFromLastNode(source, size_new, true);
				size_new -= size_open;

				if (size_open >0) state = State.OPEN;
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
