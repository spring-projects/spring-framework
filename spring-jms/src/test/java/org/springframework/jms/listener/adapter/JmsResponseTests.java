/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jms.listener.adapter;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

import org.springframework.jms.support.destination.DestinationResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
class JmsResponseTests {

	@Test
	void destinationDoesNotUseDestinationResolver() throws JMSException {
		Destination destination = mock();
		Destination actual = JmsResponse.forDestination("foo", destination).resolveDestination(null, null);
		assertThat(actual).isSameAs(destination);
	}

	@Test
	void resolveDestinationForQueue() throws JMSException {
		Session session = mock();
		DestinationResolver destinationResolver = mock();
		Destination destination = mock();

		given(destinationResolver.resolveDestinationName(session, "myQueue", false)).willReturn(destination);
		JmsResponse<String> jmsResponse = JmsResponse.forQueue("foo", "myQueue");
		Destination actual = jmsResponse.resolveDestination(destinationResolver, session);
		assertThat(actual).isSameAs(destination);
	}

	@Test
	void createWithNullResponse() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				JmsResponse.forQueue(null, "myQueue"));
	}

	@Test
	void createWithNullQueueName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				JmsResponse.forQueue("foo", null));
	}

	@Test
	void createWithNullTopicName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				JmsResponse.forTopic("foo", null));
	}

	@Test
	void createWithNulDestination() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				JmsResponse.forDestination("foo", null));
	}

}
