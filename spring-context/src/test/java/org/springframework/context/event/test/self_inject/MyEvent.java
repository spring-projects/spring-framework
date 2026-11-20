/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test.self_inject;

import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public class MyEvent extends ApplicationEvent {

	@SuppressWarnings("unused")
	private String message;

	public MyEvent(Object source, String message) {
		super(source);
		this.message = message;
	}

}
