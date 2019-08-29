package com.atlwj.demo.ioc.event.annotation;

import org.springframework.context.ApplicationEvent;

/**
 * @author lengweijian
 */

public class BlackListEvent extends ApplicationEvent {

	private static final long serialVersionUID = -6280259453347969528L;
	private final String address;
	private final String content;

	public BlackListEvent(Object source, String address, String content) {
		super(source);
		this.address = address;
		this.content = content;
	}

	public String getAddress() {
		return address;
	}

	public String getContent() {
		return content;
	}
}
