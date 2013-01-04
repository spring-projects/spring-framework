/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.*;

import javax.jms.Destination;
import javax.jms.Session;
import javax.naming.NamingException;

import org.easymock.MockControl;
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

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		mockSession.replay();

		JndiDestinationResolver resolver = new OneTimeLookupJndiDestinationResolver();
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);
		assertNotNull(destination);
		assertSame(DESTINATION, destination);

		mockSession.verify();
	}

	@Test
	public void testDoesNotUseCacheIfCachingIsTurnedOff() throws Exception {

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		mockSession.replay();

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

		mockSession.verify();
	}

	@Test
	public void testDelegatesToFallbackIfNotResolvedInJndi() throws Exception {
		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		mockSession.replay();

		MockControl mockDestinationResolver = MockControl.createControl(DestinationResolver.class);
		DestinationResolver dynamicResolver = (DestinationResolver) mockDestinationResolver.getMock();
		dynamicResolver.resolveDestinationName(session, DESTINATION_NAME, true);
		mockDestinationResolver.setReturnValue(DESTINATION);
		mockDestinationResolver.replay();

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

		mockSession.verify();
		mockDestinationResolver.verify();
	}

	@Test
	public void testDoesNotDelegateToFallbackIfNotResolvedInJndi() throws Exception {
		MockControl mockSession = MockControl.createControl(Session.class);
		final Session session = (Session) mockSession.getMock();
		mockSession.replay();

		MockControl mockDestinationResolver = MockControl.createControl(DestinationResolver.class);
		DestinationResolver dynamicResolver = (DestinationResolver) mockDestinationResolver.getMock();
		mockDestinationResolver.replay();

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
		} catch (DestinationResolutionException ex) { /* expected */ }

		mockSession.verify();
		mockDestinationResolver.verify();
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
