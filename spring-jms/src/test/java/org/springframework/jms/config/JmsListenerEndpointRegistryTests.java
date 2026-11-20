/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
class JmsListenerEndpointRegistryTests {

	private final JmsListenerEndpointRegistry registry = new JmsListenerEndpointRegistry();

	private final JmsListenerContainerTestFactory containerFactory = new JmsListenerContainerTestFactory();


	@Test
	void createWithNullEndpoint() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				registry.registerListenerContainer(null, containerFactory));
	}

	@Test
	void createWithNullEndpointId() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				registry.registerListenerContainer(new SimpleJmsListenerEndpoint(), containerFactory));
	}

	@Test
	void createWithNullContainerFactory() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				registry.registerListenerContainer(createEndpoint("foo", "myDestination"), null));
	}

	@Test
	void createWithDuplicateEndpointId() {
		registry.registerListenerContainer(createEndpoint("test", "queue"), containerFactory);

		assertThatIllegalStateException().isThrownBy(() ->
				registry.registerListenerContainer(createEndpoint("test", "queue"), containerFactory));
	}


	private SimpleJmsListenerEndpoint createEndpoint(String id, String destinationName) {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setId(id);
		endpoint.setDestination(destinationName);
		return endpoint;
	}

}
