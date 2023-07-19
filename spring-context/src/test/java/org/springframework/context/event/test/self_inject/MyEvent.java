package org.springframework.context.event.test.self_inject;

import org.springframework.context.ApplicationEvent;

public class MyEvent extends ApplicationEvent {
	private String message;

	public MyEvent(Object source, String message) {
		super(source);
		this.message = message;
	}
}
