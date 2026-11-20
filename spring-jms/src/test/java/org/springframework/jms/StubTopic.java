/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

import jakarta.jms.Topic;

/**
 * Stub implementation of the {@link Topic} interface.
 *
 * @author Rick Evans
 */
public class StubTopic implements Topic {

	public static final String DEFAULT_TOPIC_NAME = "banjo";


	private String topicName = DEFAULT_TOPIC_NAME;


	public StubTopic() {
	}

	public StubTopic(String topicName) {
		this.topicName = topicName;
	}


	@Override
	public String getTopicName() {
		return this.topicName;
	}

}
