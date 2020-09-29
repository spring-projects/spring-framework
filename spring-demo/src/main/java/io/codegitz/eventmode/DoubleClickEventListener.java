package io.codegitz.eventmode;

/**
 * @author 张观权
 * @date 2020/8/14 20:25
 **/
public class DoubleClickEventListener implements EventListener {
	@Override
	public void eventProcessor(Event event) {
		if ("doubleClick".equals(event.getType())){
			System.out.println("active doubleClick event");
		}
	}
}
