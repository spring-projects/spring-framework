/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.r2dbc.connection.lookup;

import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AbstractRoutingConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
class AbstractRoutingConnectionFactoryTests {

	private static final String ROUTING_KEY = "routingKey";

	final DummyRoutingConnectionFactory connectionFactory = new DummyRoutingConnectionFactory();

	@Mock
	ConnectionFactory defaultConnectionFactory;

	@Mock
	ConnectionFactory routedConnectionFactory;


	@BeforeEach
	void before() {
		connectionFactory.setDefaultTargetConnectionFactory(defaultConnectionFactory);
	}


	@Test
	void shouldDetermineRoutedFactory() {
		connectionFactory.setTargetConnectionFactories(Map.of("key", routedConnectionFactory));
		connectionFactory.setConnectionFactoryLookup(new MapConnectionFactoryLookup());
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.contextWrite(Context.of(ROUTING_KEY, "key"))
				.as(StepVerifier::create)
				.expectNext(routedConnectionFactory)
				.verifyComplete();
	}

	@Test
	void shouldFallbackToDefaultConnectionFactory() {
		connectionFactory.setTargetConnectionFactories(Map.of("key", routedConnectionFactory));
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.as(StepVerifier::create)
				.expectNext(defaultConnectionFactory)
				.verifyComplete();
	}

	@Test
	void initializationShouldFailUnsupportedLookupKey() {
		connectionFactory.setTargetConnectionFactories(Map.of("key", new Object()));

		assertThatIllegalArgumentException().isThrownBy(connectionFactory::initialize);
	}

	@Test
	void initializationShouldFailUnresolvableKey() {
		connectionFactory.setTargetConnectionFactories(Map.of("key", "value"));
		connectionFactory.setConnectionFactoryLookup(new MapConnectionFactoryLookup());

		assertThatThrownBy(connectionFactory::initialize)
				.isInstanceOf(ConnectionFactoryLookupFailureException.class)
				.hasMessageContaining("No ConnectionFactory with name 'value' registered");
	}

	@Test
	void unresolvableConnectionFactoryRetrievalShouldFail() {
		connectionFactory.setLenientFallback(false);
		connectionFactory.setConnectionFactoryLookup(new MapConnectionFactoryLookup());
		connectionFactory.setTargetConnectionFactories(Map.of("key", routedConnectionFactory));
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.contextWrite(Context.of(ROUTING_KEY, "unknown"))
				.as(StepVerifier::create)
				.verifyError(IllegalStateException.class);
	}

	@Test
	void connectionFactoryRetrievalWithUnknownLookupKeyShouldReturnDefaultConnectionFactory() {
		connectionFactory.setTargetConnectionFactories(Map.of("key", routedConnectionFactory));
		connectionFactory.setDefaultTargetConnectionFactory(defaultConnectionFactory);
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.contextWrite(Context.of(ROUTING_KEY, "unknown"))
				.as(StepVerifier::create)
				.expectNext(defaultConnectionFactory)
				.verifyComplete();
	}

	@Test
	void connectionFactoryRetrievalWithoutLookupKeyShouldReturnDefaultConnectionFactory() {
		connectionFactory.setTargetConnectionFactories(Map.of("key", routedConnectionFactory));
		connectionFactory.setDefaultTargetConnectionFactory(defaultConnectionFactory);
		connectionFactory.setLenientFallback(false);
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.as(StepVerifier::create)
				.expectNext(defaultConnectionFactory)
				.verifyComplete();
	}

	@Test
	void shouldLookupFromMap() {
		MapConnectionFactoryLookup lookup =
				new MapConnectionFactoryLookup("lookup-key", routedConnectionFactory);

		connectionFactory.setConnectionFactoryLookup(lookup);
		connectionFactory.setTargetConnectionFactories(Map.of("my-key", "lookup-key"));
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.contextWrite(Context.of(ROUTING_KEY, "my-key"))
				.as(StepVerifier::create)
				.expectNext(routedConnectionFactory)
				.verifyComplete();
	}

	@Test
	void shouldAllowModificationsAfterInitialization() {
		MapConnectionFactoryLookup lookup = new MapConnectionFactoryLookup();

		connectionFactory.setConnectionFactoryLookup(lookup);
		connectionFactory.setTargetConnectionFactories(lookup.getConnectionFactories());
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.contextWrite(Context.of(ROUTING_KEY, "lookup-key"))
				.as(StepVerifier::create)
				.expectNext(defaultConnectionFactory)
				.verifyComplete();

		lookup.addConnectionFactory("lookup-key", routedConnectionFactory);
		connectionFactory.afterPropertiesSet();

		connectionFactory.determineTargetConnectionFactory()
				.contextWrite(Context.of(ROUTING_KEY, "lookup-key"))
				.as(StepVerifier::create)
				.expectNext(routedConnectionFactory)
				.verifyComplete();
	}

	@Test
	void initializeShouldDetermineRoutedFactory() {
		connectionFactory.setTargetConnectionFactories(Map.of("key", routedConnectionFactory));
		connectionFactory.setConnectionFactoryLookup(new MapConnectionFactoryLookup());
		connectionFactory.initialize();

		connectionFactory.determineTargetConnectionFactory()
				.contextWrite(Context.of(ROUTING_KEY, "key"))
				.as(StepVerifier::create)
				.expectNext(routedConnectionFactory)
				.verifyComplete();
	}

	static class DummyRoutingConnectionFactory extends AbstractRoutingConnectionFactory {

		@Override
		protected Mono<Object> determineCurrentLookupKey() {
			return Mono.deferContextual(context -> {
				if (context.hasKey(ROUTING_KEY)) {
					return Mono.just(context.get(ROUTING_KEY));
				}
				return Mono.empty();
			});
		}
	}

}
