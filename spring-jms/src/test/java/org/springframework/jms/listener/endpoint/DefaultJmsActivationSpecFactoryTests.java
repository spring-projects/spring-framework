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

package org.springframework.jms.listener.endpoint;

import jakarta.jms.Destination;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

import org.springframework.jca.StubResourceAdapter;
import org.springframework.jms.StubQueue;
import org.springframework.jms.support.destination.DestinationResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Agim Emruli
 * @author Juergen Hoeller
 */
class DefaultJmsActivationSpecFactoryTests {

	private final JmsActivationSpecConfig activationSpecConfig = new JmsActivationSpecConfig() {{
		setMaxConcurrency(5);
		setPrefetchSize(3);
		setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
		setClientId("clientid");
		setDestinationName("destinationname");
		setDurableSubscriptionName("durableSubscriptionName");
		setMessageSelector("selector");
	}};


	@Test
	void activeMQResourceAdapterSetup() {
		activationSpecConfig.setAcknowledgeMode(Session.SESSION_TRANSACTED);
		JmsActivationSpecFactory activationSpecFactory = new DefaultJmsActivationSpecFactory();
		StubActiveMQActivationSpec spec = (StubActiveMQActivationSpec) activationSpecFactory.createActivationSpec(
				new StubActiveMQResourceAdapter(), activationSpecConfig);

		assertThat(spec.getMaxSessions()).isEqualTo(5);
		assertThat(spec.getMaxMessagesPerSessions()).isEqualTo(3);
		assertThat(spec.isUseRAManagedTransaction()).isTrue();
	}

	@Test
	void webSphereResourceAdapterSetup() throws Exception {
		Destination destination = new StubQueue();

		DestinationResolver destinationResolver = mock();
		given(destinationResolver.resolveDestinationName(null, "destinationname", false)).willReturn(destination);

		DefaultJmsActivationSpecFactory activationSpecFactory = new DefaultJmsActivationSpecFactory();
		activationSpecFactory.setDestinationResolver(destinationResolver);

		StubWebSphereActivationSpecImpl spec = (StubWebSphereActivationSpecImpl) activationSpecFactory
				.createActivationSpec(new StubWebSphereResourceAdapterImpl(), activationSpecConfig);

		assertThat(spec.getDestination()).isEqualTo(destination);
		assertThat(spec.getMaxConcurrency()).isEqualTo(5);
		assertThat(spec.getMaxBatchSize()).isEqualTo(3);
	}


	private static class StubActiveMQResourceAdapter extends StubResourceAdapter {
	}


	private static class StubWebSphereResourceAdapterImpl extends StubResourceAdapter {
	}


	@SuppressWarnings("unused")
	private static class StubActiveMQActivationSpec extends StubJmsActivationSpec {

		private int maxSessions;

		private int maxMessagesPerSessions;

		private String destination;

		private boolean useRAManagedTransaction;

		public void setMaxSessions(int maxSessions) {
			this.maxSessions = maxSessions;
		}

		public void setMaxMessagesPerSessions(int maxMessagesPerSessions) {
			this.maxMessagesPerSessions = maxMessagesPerSessions;
		}

		public int getMaxSessions() {
			return maxSessions;
		}

		public int getMaxMessagesPerSessions() {
			return maxMessagesPerSessions;
		}

		public String getDestination() {
			return destination;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		public boolean isUseRAManagedTransaction() {
			return useRAManagedTransaction;
		}

		public void setUseRAManagedTransaction(boolean useRAManagedTransaction) {
			this.useRAManagedTransaction = useRAManagedTransaction;
		}
	}


	@SuppressWarnings("unused")
	private static class StubWebSphereActivationSpecImpl extends StubJmsActivationSpec {

		private Destination destination;

		private int maxConcurrency;

		private int maxBatchSize;

		public void setDestination(Destination destination) {
			this.destination = destination;
		}

		public Destination getDestination() {
			return destination;
		}

		public int getMaxConcurrency() {
			return maxConcurrency;
		}

		public void setMaxConcurrency(int maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
		}

		public int getMaxBatchSize() {
			return maxBatchSize;
		}

		public void setMaxBatchSize(int maxBatchSize) {
			this.maxBatchSize = maxBatchSize;
		}
	}

}
