package io.codegitz.eventmode;

/**
 * @author 张观权
 * @date 2020/8/14 20:30
 **/
public class EventModeDemo {
	public static void main(String[] args) {
		EventSource eventSource = new EventSource();
		SingleClickEventListener singleClickEventListener = new SingleClickEventListener();
		DoubleClickEventListener doubleClickEventListener = new DoubleClickEventListener();
		eventSource.register(singleClickEventListener);
		eventSource.register(doubleClickEventListener);
		Event event = new Event();
		event.setType("singleClick");
		eventSource.publish(event);

	}
}
