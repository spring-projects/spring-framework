/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms;

import javax.jms.Topic;

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
