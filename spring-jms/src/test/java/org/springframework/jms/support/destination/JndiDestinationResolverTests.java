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

package org.springframework.jms.support.destination;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.jms.Destination;
import javax.jms.Session;
import javax.naming.NamingException;

import org.junit.Test;
import org.springframework.jms.StubTopic;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class JndiDestinationResolverTests {

	private static final String DESTINATION_NAME = "foo";

	private static final Destination DESTINATION = new StubTopic();


	@Test
	public void testHitsCacheSecondTimeThrough() throws Exception {

		Session session = mock(Session.class);

		JndiDestinationResolver resolver = new OneTimeLookupJndiDestinationResolver();
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);
		assertNotNull(destination);
		assertSame(DESTINATION, destination);
	}

	@Test
	public void testDoesNotUseCacheIfCachingIsTurnedOff() throws Exception {

		Session session = mock(Session.class);

		CountingCannedJndiDestinationResolver resolver
				= new CountingCannedJndiDestinationResolver();
		resolver.setCache(false);
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);
		assertNotNull(destination);
		assertSame(DESTINATION, destination);
		assertEquals(1, resolver.getCallCount());

		destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);
		assertNotNull(destination);
		assertSame(DESTINATION, destination);
		assertEquals(2, resolver.getCallCount());
	}

	@Test
	public void testDelegatesToFallbackIfNotResolvedInJndi() throws Exception {
		Session session = mock(Session.class);

		DestinationResolver dynamicResolver = mock(DestinationResolver.class);
		given(dynamicResolver.resolveDestinationName(session, DESTINATION_NAME,
				true)).willReturn(DESTINATION);

		JndiDestinationResolver resolver = new JndiDestinationResolver() {
			@Override
			protected Object lookup(String jndiName, Class requiredClass) throws NamingException {
				throw new NamingException();
			}
		};
		resolver.setFallbackToDynamicDestination(true);
		resolver.setDynamicDestinationResolver(dynamicResolver);
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);

		assertNotNull(destination);
		assertSame(DESTINATION, destination);
	}

	@Test
	public void testDoesNotDelegateToFallbackIfNotResolvedInJndi() throws Exception {
		final Session session = mock(Session.class);
		DestinationResolver dynamicResolver = mock(DestinationResolver.class);

		final JndiDestinationResolver resolver = new JndiDestinationResolver() {
			@Override
			protected Object lookup(String jndiName, Class requiredClass) throws NamingException {
				throw new NamingException();
			}
		};
		resolver.setDynamicDestinationResolver(dynamicResolver);

		try {
			resolver.resolveDestinationName(session, DESTINATION_NAME, true);
			fail("expected DestinationResolutionException");
		}
		catch (DestinationResolutionException ex) {
			// expected
		}
	}


	private static class OneTimeLookupJndiDestinationResolver extends JndiDestinationResolver {

		private boolean called;

		@Override
		protected Object lookup(String jndiName, Class requiredType) throws NamingException {
			if (called) {
				fail("Must not be delegating to lookup(..), must be resolving from cache.");
			}
			assertEquals(DESTINATION_NAME, jndiName);
			called = true;
			return DESTINATION;
		}
	}

	private static class CountingCannedJndiDestinationResolver extends JndiDestinationResolver {

		private int callCount;

		public int getCallCount() {
			return this.callCount;
		}

		@Override
		protected Object lookup(String jndiName, Class requiredType) throws NamingException {
			++this.callCount;
			return DESTINATION;
		}
	}
}
