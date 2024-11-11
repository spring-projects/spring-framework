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

package org.springframework.jms.support.destination;

import javax.naming.NamingException;

import jakarta.jms.Destination;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

import org.springframework.jms.StubTopic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
class JndiDestinationResolverTests {

	private static final String DESTINATION_NAME = "foo";

	private static final Destination DESTINATION = new StubTopic();


	@Test
	void testHitsCacheSecondTimeThrough() throws Exception {

		Session session = mock();

		JndiDestinationResolver resolver = new OneTimeLookupJndiDestinationResolver();
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);
		assertThat(destination).isNotNull();
		assertThat(destination).isSameAs(DESTINATION);
	}

	@Test
	void testDoesNotUseCacheIfCachingIsTurnedOff() throws Exception {

		Session session = mock();

		CountingCannedJndiDestinationResolver resolver
				= new CountingCannedJndiDestinationResolver();
		resolver.setCache(false);
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);
		assertThat(destination).isNotNull();
		assertThat(destination).isSameAs(DESTINATION);
		assertThat(resolver.getCallCount()).isEqualTo(1);

		destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);
		assertThat(destination).isNotNull();
		assertThat(destination).isSameAs(DESTINATION);
		assertThat(resolver.getCallCount()).isEqualTo(2);
	}

	@Test
	void testDelegatesToFallbackIfNotResolvedInJndi() throws Exception {
		Session session = mock();

		DestinationResolver dynamicResolver = mock();
		given(dynamicResolver.resolveDestinationName(session, DESTINATION_NAME,
				true)).willReturn(DESTINATION);

		JndiDestinationResolver resolver = new JndiDestinationResolver() {
			@Override
			protected <T> T lookup(String jndiName, Class<T> requiredClass) throws NamingException {
				throw new NamingException();
			}
		};
		resolver.setFallbackToDynamicDestination(true);
		resolver.setDynamicDestinationResolver(dynamicResolver);
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, true);

		assertThat(destination).isNotNull();
		assertThat(destination).isSameAs(DESTINATION);
	}

	@Test
	void testDoesNotDelegateToFallbackIfNotResolvedInJndi() {
		final Session session = mock();
		DestinationResolver dynamicResolver = mock();

		final JndiDestinationResolver resolver = new JndiDestinationResolver() {
			@Override
			protected <T> T lookup(String jndiName, Class<T> requiredClass) throws NamingException {
				throw new NamingException();
			}
		};
		resolver.setDynamicDestinationResolver(dynamicResolver);

		assertThatExceptionOfType(DestinationResolutionException.class).isThrownBy(() ->
				resolver.resolveDestinationName(session, DESTINATION_NAME, true));
	}


	private static class OneTimeLookupJndiDestinationResolver extends JndiDestinationResolver {

		private boolean called;

		@Override
		protected <T> T lookup(String jndiName, Class<T> requiredType) {
			assertThat(called).as("delegating to lookup(..) not cache").isFalse();
			assertThat(jndiName).isEqualTo(DESTINATION_NAME);
			called = true;
			return requiredType.cast(DESTINATION);
		}
	}

	private static class CountingCannedJndiDestinationResolver extends JndiDestinationResolver {

		private int callCount;

		public int getCallCount() {
			return this.callCount;
		}

		@Override
		protected <T> T lookup(String jndiName, Class<T> requiredType) {
			++this.callCount;
			return requiredType.cast(DESTINATION);
		}
	}
}
