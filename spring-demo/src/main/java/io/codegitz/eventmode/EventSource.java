package io.codegitz.eventmode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 张观权
 * @date 2020/8/14 20:27
 **/
public class EventSource {
	private List<EventListener> eventListenerList = new ArrayList<>();

	public void register(EventListener eventListener){
		eventListenerList.add(eventListener);
	}

	public void publish(Event event){
		for (EventListener eventListener : eventListenerList){
			eventListener.eventProcessor(event);
		}
	}
}
