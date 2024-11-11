/*
 * Copyright 2002-2023 the original author or authors.
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
