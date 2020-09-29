package io.codegitz.eventmode;

/**
 * @author 张观权
 * @date 2020/8/14 20:25
 **/
public class SingleClickEventListener implements EventListener {
	@Override
	public void eventProcessor(Event event) {
		if ("singleClick".equals(event.getType())){
			System.out.println("active singleClick event");
		}
	}
}
