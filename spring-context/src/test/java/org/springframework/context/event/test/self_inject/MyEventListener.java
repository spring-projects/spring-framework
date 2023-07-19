package org.springframework.context.event.test.self_inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MyEventListener implements ApplicationListener<MyEvent> {
	public static int eventCount;

	@Autowired  // use '-Dspring.main.allow-circular-references=true' in Spring Boot >= 2.6.0
	//@Lazy     // with '@Lazy', the problem does not occur
	private MyEventListener eventDemoListener;

	@Override
	public void onApplicationEvent(MyEvent event) {
		//System.out.println("Event: " + event);
		eventCount++;
	}
}
