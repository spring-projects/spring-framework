/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.destination;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
class JmsDestinationAccessorTests {

	@Test
	void chokesIfDestinationResolverIsetToNullExplicitly() {
		ConnectionFactory connectionFactory = mock();

		JmsDestinationAccessor accessor = new StubJmsDestinationAccessor();
		accessor.setConnectionFactory(connectionFactory);
		assertThatIllegalArgumentException().isThrownBy(() ->
				accessor.setDestinationResolver(null));
	}

	@Test
	void sessionTransactedModeReallyDoesDefaultToFalse() {
		JmsDestinationAccessor accessor = new StubJmsDestinationAccessor();
		assertThat(accessor.isPubSubDomain()).as("The [pubSubDomain] property of JmsDestinationAccessor must default to " +
				"false (i.e. Queues are used by default). Change this test (and the " +
				"attendant Javadoc) if you have changed the default.").isFalse();
	}


	private static class StubJmsDestinationAccessor extends JmsDestinationAccessor {

	}

}
