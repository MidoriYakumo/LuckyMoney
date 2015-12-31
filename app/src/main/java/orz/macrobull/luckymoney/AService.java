package orz.macrobull.luckymoney;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.os.Vibrator;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;
import android.widget.Toast;

import java.util.List;

/**
 * Created by macrobull on 12/28/15.
 * 辅助点击服务
 */
public class AService extends AccessibilityService {

	static State state = State.CHAT_WALK;                // 服务状态
	static boolean mutex = false;                        // 互斥锁
	static Integer lastNode = 0;                        // 简单记录上一红包节点hashcode去重复

	/*
	 * 为了确保获得金额信息, 设置详情标志
	 * 1: 红包有效
	 * 2: 左上角"详情"出现
	 * 4: 金额出现
	 */
	static Integer flags_detail = 0;

	static Integer size_open = 0;                        // 已点开的红包数
	static Integer size_new = 0;                        // 待处理的新红包数

	// 统计信息
	static Integer cnt_get = 0;                        // 点开的红包数
	static Integer cnt_open = 0;                        // 拆开的红包数
	static Integer cnt_detail = 0;                        // 进入详情数
	static Integer cnt_new = 0;                        // 捕获通知次数

	static Float amount_total = 0.0f;        // 红包总金额
	static Float amount_success = 0.0f;    // 成功抢到的红包总金额

	/**
	 * 供主界面显示统计信息
	 *
	 * @return 统计信息
	 */
	public static String getStatistics() {
		return String.format(
				"点了%d个红包, 开了%d个\n抢到了%d个红包\n从通知抢了%d次"
						+ "\n路过%.2f元, 抢到%.2f元"
				, cnt_get, cnt_detail, cnt_open, cnt_new, amount_total, amount_success);
	}

	/**
	 * 监视UI变更事件
	 *
	 * @param event AccessibilityEvent
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
	 *
	 * @param root 根UI节点
	 * @return 成功点击的红包数
	 */
	Integer getFromNode(AccessibilityNodeInfo root) {
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		for (AccessibilityNodeInfo node : mNodes) {
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
	 *
	 * @param root      根UI节点
	 * @param size      点击最后size个
	 * @param ignoreDup 是否无视重复检测
	 * @return 成功点击的红包数
	 */
	Integer getFromLastNode(AccessibilityNodeInfo root, Integer size, boolean ignoreDup) {
		List<AccessibilityNodeInfo> mNodes =
				root.findAccessibilityNodeInfosByText(getResources().getString(R.string.chat_pattern));

		size = Math.min(size, mNodes.size()); // 先设成功点击数为预计点击的红包数目
		for (Integer i = mNodes.size() - size; i < mNodes.size(); i++) {
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

	void logState() {
		Log.d("state", state.toString() + "/" + size_open.toString() + "/" + size_new.toString());
	}



	AccessibilityNodeInfo source;
	AccessibilityRecord record;

	Integer debug_cnt_open = -1;

	/**
	 * 处理UI变动
	 *
	 * @param event AccessibilityEvent
	 */
	void process(AccessibilityEvent event) {
		source = event.getSource();
		if (source == null) return;
		logState(); // 舍弃无效的source

		switch (state) {
			case CHAT_WALK: // 遍历聊天界面的红包
				size_open += getFromNode(source);
				if (size_open > 0) {
					state = State.OPEN;
					debug_cnt_open = 0;
				} else {
					state = State.CHAT_IDLE;
				}

				break;
			case OPEN: // 已打开红包
				debug_cnt_open += 1;
				Log.d("open debug_cnt_open", debug_cnt_open.toString());
				Log.d("open", source.toString());
				// 寻找拆红包按钮
				// #FIXME 6.3.8在某些设备上找不到按钮, 点击了所有新组件都没有效果
				if (source.getClassName().toString().equals("android.widget.Button")) {
					Log.d("click", "OPEN");
					source.performAction(AccessibilityNodeInfo.ACTION_CLICK); // 拆红包
					cnt_open += 1;
					flags_detail = 1; // 红包有效
					state = State.DETAIL;
					break;
				} else if (debug_cnt_open>6) { // 只好震动提示手动拆包
					Toast.makeText(this, "Button OPEN cannot get touched, do it yourself!", Toast.LENGTH_SHORT).show();
					Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
					vibrator.vibrate(new long[]{300, 100, 300, 100}, -1);
				}

				// #TODO 处理没抢到的红包
				// 已拆的红包会进入详情界面

			case DETAIL: // 红包详情界面
//				Log.d("detail", source.toString());
//				if (!source.getClassName().toString().equals("android.widget.LinearLayout")) {
				// 抓LinearLayout似乎不好用, 虽然无关语言
				if (source.getText() == null) break; //无视无文本的组件
				Log.d("detail text", source.getText().toString());

				if (!(source.getText().toString().equals("Details")
						|| source.getText().toString().equals("红包详情")
				)) flags_detail |= 2; // 抓左上角详情文本, #TODO i18n支持

				if (source.getText().toString().matches("\\d+\\.\\d\\d")) { // 抓金额, 采用第一个出现的值
					try {
						Log.d("amount", "got value:" + source.getText().toString());
						amount_total += Float.valueOf(source.getText().toString());
						if ((flags_detail & 1) > 0)
							amount_success += Float.valueOf(source.getText().toString());
						flags_detail |= 4;
					} catch (Exception e) { // 潜在的转换异常
						Log.w("amount", e.getMessage());
					}
				}
//				}

				if ((flags_detail & 6) != 6) return; // 等待抓到所有必需的UI
				flags_detail = 0; // 清除flag

				Log.d("click", "BACK");
				performGlobalAction(GLOBAL_ACTION_BACK);
				// 点击返回, 虽然锁屏界面下辅助服务能够操作应用UI, 但是返回不可用!!!
				// #TODO 改用点击详情的左上角返回, 实现完全后台操作
				cnt_detail += 1;
				size_open -= 1;

				if (size_open > 0) {
					state = State.OPEN;
					debug_cnt_open = 0;
				} else {
					NLService.releaseLock(); // 结束抢红包后解除wakelock和恢复锁屏
					state = State.CHAT_IDLE;
				}

				break;
			case CHAT_IDLE: // 聊天界面
				if (NLService.catchTheGame()) { // 通知表明有新红包(后台或其他聊天界面有红包)
					cnt_new += 1;
					size_new += 1;
					state = State.CHAT_NEW;
				} else {  // 在更新的气泡里找新红包(当前聊天界面)
					if (event.getRecordCount() <= 0) return;
					record = event.getRecord(0); // 微信每次只增加一条record
					if (record.getText() == null) return; // 只关注有文本的UI

					Log.d("chat record", record.toString());
					Log.d("chat source", source.toString());

					boolean maybeMoney = false;

					if (record.getText().size() > 3) { // 典型的红包包含4段文本, 且关注点为chat_pattern
						for (CharSequence cText : record.getText()) {
							if (cText.toString().matches(getResources().getString(R.string.chat_pattern))) {
								maybeMoney = true;
								break;
							}
						}
					}

					if (record.getText().toString().matches("\\[\\d+\\]")) maybeMoney = true;
					// 有时只产生通知数UI更新, 也加以关注

					if (maybeMoney) {
						Log.d("source", source.toString());
						size_open += getFromLastNode(source, 1, false); // 只点最后一个红包, 并检测重复
						if (size_open > 0) {
							state = State.OPEN;
							debug_cnt_open = 0;
						}
					}

				}
				break;
			case CHAT_NEW: // 由通知进入聊天界面, 点最后size_new个红包
				size_open += getFromLastNode(source, size_new, true); // 点最后size_new个红包, 不检测重复(UI节点重用情况)
				size_new -= size_open;

				if (size_open > 0) {
					state = State.OPEN;
					debug_cnt_open = 0;
				}
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
