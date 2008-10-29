/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jms.support.destination;

import javax.jms.ConnectionFactory;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 */
public class JmsDestinationAccessorTests extends TestCase {

	public void testChokesIfDestinationResolverIsetToNullExplcitly() throws Exception {
		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		mockConnectionFactory.replay();

		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				JmsDestinationAccessor accessor = new StubJmsDestinationAccessor();
				accessor.setConnectionFactory(connectionFactory);
				accessor.setDestinationResolver(null);
				accessor.afterPropertiesSet();
			}
		}.runTest();

		mockConnectionFactory.verify();
	}

	public void testSessionTransactedModeReallyDoesDefaultToFalse() throws Exception {
		JmsDestinationAccessor accessor = new StubJmsDestinationAccessor();
		assertFalse("The [pubSubDomain] property of JmsDestinationAccessor must default to " +
				"false (i.e. Queues are used by default). Change this test (and the " +
				"attendant Javadoc) if you have changed the default.",
				accessor.isPubSubDomain());
	}


	private static class StubJmsDestinationAccessor extends JmsDestinationAccessor {

	}

}
