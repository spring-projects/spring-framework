package com.pengbin.spring.event;

import org.springframework.context.ApplicationEvent;

public class TestEvent extends ApplicationEvent {
	private String message;

	public TestEvent(Object source) {
		super(source);
	}

	public void getMessage() {
		System.out.println(message);
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
