package org.springframework.context.event.test.self_inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MyEventPublisher {
	@Autowired
	private ApplicationEventPublisher eventPublisher;

	public void publishMyEvent(String message) {
		eventPublisher.publishEvent(new MyEvent(this, message));
	}
}
