/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import org.springframework.messaging.Message;

public class TestMessagePostProcessor implements MessagePostProcessor {

	private Message<?> message;


	Message<?> getMessage() {
		return this.message;
	}

	@Override
	public Message<?> postProcessMessage(Message<?> message) {
		this.message = message;
		return message;
	}

}
