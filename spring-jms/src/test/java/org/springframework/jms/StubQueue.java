/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

import jakarta.jms.Queue;

/**
 * Stub implementation of the {@link jakarta.jms.Queue} interface.
 *
 * @author Rick Evans
 */
public class StubQueue implements Queue {

	public static final String DEFAULT_QUEUE_NAME = "banjo";


	private String queueName = DEFAULT_QUEUE_NAME;


	public StubQueue() {
	}

	public StubQueue(String queueName) {
		this.queueName = queueName;
	}


	@Override
	public String getQueueName() {
		return this.queueName;
	}

}
