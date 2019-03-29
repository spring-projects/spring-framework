/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jms.support.destination;

import javax.jms.ConnectionFactory;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class JmsDestinationAccessorTests {

	@Test
	public void testChokesIfDestinationResolverIsetToNullExplicitly() throws Exception {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

		try {
			JmsDestinationAccessor accessor = new StubJmsDestinationAccessor();
			accessor.setConnectionFactory(connectionFactory);
			accessor.setDestinationResolver(null);
			accessor.afterPropertiesSet();
			fail("expected IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

	}

	@Test
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
