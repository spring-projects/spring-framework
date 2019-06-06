/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.rsocket;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.ClientTransport;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Unit tests for {@link DefaultRSocketRequesterBuilder}.
 *
 * @author Brian Clozel
 */
public class DefaultRSocketRequesterBuilderTests {

	private ClientTransport transport;


	@Before
	public void setup() {
		this.transport = mock(ClientTransport.class);
		given(this.transport.connect(anyInt())).willReturn(Mono.just(new MockConnection()));
	}


	@Test
	@SuppressWarnings("unchecked")
	public void shouldApplyCustomizationsAtSubscription() {
		Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer = mock(Consumer.class);
		Consumer<RSocketStrategies.Builder> strategiesConfigurer = mock(Consumer.class);
		RSocketRequester.builder()
				.rsocketFactory(factoryConfigurer)
				.rsocketStrategies(strategiesConfigurer)
				.connect(this.transport);
		verifyZeroInteractions(this.transport, factoryConfigurer, strategiesConfigurer);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldApplyCustomizations() {
		RSocketStrategies strategies = RSocketStrategies.builder()
				.encoder(CharSequenceEncoder.allMimeTypes())
				.decoder(StringDecoder.allMimeTypes())
				.build();
		Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer = mock(Consumer.class);
		Consumer<RSocketStrategies.Builder> strategiesConfigurer = mock(Consumer.class);
		RSocketRequester.builder()
				.rsocketStrategies(strategies)
				.rsocketFactory(factoryConfigurer)
				.rsocketStrategies(strategiesConfigurer)
				.connect(this.transport)
				.block();
		verify(this.transport).connect(anyInt());
		verify(factoryConfigurer).accept(any(RSocketFactory.ClientRSocketFactory.class));
		verify(strategiesConfigurer).accept(any(RSocketStrategies.Builder.class));
	}

	@Test
	public void dataMimeType() throws NoSuchFieldException {
		RSocketStrategies strategies = RSocketStrategies.builder()
				.encoder(CharSequenceEncoder.allMimeTypes())
				.decoder(StringDecoder.allMimeTypes())
				.build();

		RSocketRequester requester = RSocketRequester.builder()
				.rsocketStrategies(strategies)
				.dataMimeType(MimeTypeUtils.APPLICATION_JSON)
				.connect(this.transport)
				.block();

		Field field = DefaultRSocketRequester.class.getDeclaredField("dataMimeType");
		ReflectionUtils.makeAccessible(field);
		MimeType dataMimeType = (MimeType) ReflectionUtils.getField(field, requester);
		assertThat(dataMimeType).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}


	static class MockConnection implements DuplexConnection {

		@Override
		public Mono<Void> send(Publisher<ByteBuf> frames) {
			return Mono.empty();
		}

		@Override
		public Flux<ByteBuf> receive() {
			return Flux.empty();
		}

		@Override
		public Mono<Void> onClose() {
			return Mono.empty();
		}

		@Override
		public void dispose() {
		}
	}

}
