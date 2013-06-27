/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.listener.endpoint;

import javax.jms.Destination;
import javax.jms.Session;

import junit.framework.TestCase;

import org.springframework.jca.StubResourceAdapter;
import org.springframework.jms.StubQueue;
import org.springframework.jms.support.destination.DestinationResolver;

import static org.mockito.BDDMockito.*;

/**
 * @author Agim Emruli
 * @author Juergen Hoeller
 */
public class DefaultJmsActivationSpecFactoryTests extends TestCase {

	private JmsActivationSpecConfig activationSpecConfig;

	@Override
	protected void setUp() throws Exception {
		activationSpecConfig = new JmsActivationSpecConfig();
		activationSpecConfig.setMaxConcurrency(5);
		activationSpecConfig.setPrefetchSize(3);
		activationSpecConfig.setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
		activationSpecConfig.setClientId("clientid");
		activationSpecConfig.setDestinationName("destinationname");
		activationSpecConfig.setDurableSubscriptionName("durableSubscriptionName");
		activationSpecConfig.setMessageSelector("selector");
	}

	public void testActiveMQResourceAdapterSetup() {
		activationSpecConfig.setAcknowledgeMode(Session.SESSION_TRANSACTED);
		JmsActivationSpecFactory activationSpecFactory = new DefaultJmsActivationSpecFactory();
		StubActiveMQActivationSpec spec = (StubActiveMQActivationSpec) activationSpecFactory.createActivationSpec(
				new StubActiveMQResourceAdapter(), activationSpecConfig);

		assertEquals(5, spec.getMaxSessions());
		assertEquals(3, spec.getMaxMessagesPerSessions());
		assertTrue(spec.isUseRAManagedTransaction());
	}

	public void testWebSphereResourceAdapterSetup() throws Exception {
		Destination destination = new StubQueue();

		DestinationResolver destinationResolver = mock(DestinationResolver.class);

		given(destinationResolver.resolveDestinationName(null, "destinationname",
				false)).willReturn(destination);

		DefaultJmsActivationSpecFactory activationSpecFactory = new DefaultJmsActivationSpecFactory();
		activationSpecFactory.setDestinationResolver(destinationResolver);

		StubWebSphereActivationSpecImpl spec = (StubWebSphereActivationSpecImpl) activationSpecFactory
				.createActivationSpec(new StubWebSphereResourceAdapterImpl(), activationSpecConfig);

		assertEquals(destination, spec.getDestination());
		assertEquals(5, spec.getMaxConcurrency());
		assertEquals(3, spec.getMaxBatchSize());
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
