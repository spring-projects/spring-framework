/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
