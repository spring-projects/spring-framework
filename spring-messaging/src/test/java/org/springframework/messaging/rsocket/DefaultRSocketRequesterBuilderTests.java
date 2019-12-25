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
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.ClientTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link DefaultRSocketRequesterBuilder}.
 *
 * @author Brian Clozel
 */
public class DefaultRSocketRequesterBuilderTests {

	private ClientTransport transport;

	private final MockConnection connection = new MockConnection();

	private final TestRSocketFactoryConfigurer rsocketFactoryConfigurer = new TestRSocketFactoryConfigurer();


	@BeforeEach
	public void setup() {
		this.transport = mock(ClientTransport.class);
		given(this.transport.connect(anyInt())).willReturn(Mono.just(this.connection));
	}


	@Test
	@SuppressWarnings("unchecked")
	public void rsocketFactoryConfigurerAppliesAtSubscription() {
		Consumer<RSocketStrategies.Builder> strategiesConfigurer = mock(Consumer.class);
		RSocketRequester.builder()
				.rsocketFactory(this.rsocketFactoryConfigurer)
				.rsocketStrategies(strategiesConfigurer)
				.connect(this.transport);

		verifyNoInteractions(this.transport);
		assertThat(this.rsocketFactoryConfigurer.rsocketFactory()).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void rsocketFactoryConfigurer() {
		Consumer<RSocketStrategies.Builder> rsocketStrategiesConfigurer = mock(Consumer.class);
		RSocketRequester.builder()
				.rsocketFactory(this.rsocketFactoryConfigurer)
				.rsocketStrategies(rsocketStrategiesConfigurer)
				.connect(this.transport)
				.block();

		// RSocketStrategies and RSocketFactory configurers should have been called

		verify(this.transport).connect(anyInt());
		verify(rsocketStrategiesConfigurer).accept(any(RSocketStrategies.Builder.class));
		assertThat(this.rsocketFactoryConfigurer.rsocketFactory()).isNotNull();
	}

	@Test
	public void defaultDataMimeType() {
		RSocketRequester requester = RSocketRequester.builder()
				.connect(this.transport)
				.block();

		assertThat(requester.dataMimeType())
				.as("Default data MimeType, based on the first Decoder")
				.isEqualTo(MimeTypeUtils.TEXT_PLAIN);
	}

	@Test
	public void defaultDataMimeTypeWithCustomDecoderRegistered() {
		RSocketStrategies strategies = RSocketStrategies.builder()
				.decoder(new TestJsonDecoder(MimeTypeUtils.APPLICATION_JSON))
				.build();

		RSocketRequester requester = RSocketRequester.builder()
				.rsocketStrategies(strategies)
				.connect(this.transport)
				.block();

		assertThat(requester.dataMimeType())
				.as("Default data MimeType, based on the first configured, non-default Decoder")
				.isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void dataMimeTypeExplicitlySet() {
		RSocketRequester requester = RSocketRequester.builder()
				.dataMimeType(MimeTypeUtils.APPLICATION_JSON)
				.connect(this.transport)
				.block();

		ConnectionSetupPayload setupPayload = Mono.from(this.connection.sentFrames())
				.map(ConnectionSetupPayload::create)
				.block();

		assertThat(setupPayload.dataMimeType()).isEqualTo("application/json");
		assertThat(requester.dataMimeType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void mimeTypesCannotBeChangedAtRSocketFactoryLevel() {
		MimeType dataMimeType = MimeTypeUtils.APPLICATION_JSON;
		MimeType metaMimeType = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());

		RSocketRequester requester = RSocketRequester.builder()
				.metadataMimeType(metaMimeType)
				.dataMimeType(dataMimeType)
				.rsocketFactory(factory -> {
					factory.metadataMimeType("text/plain");
					factory.dataMimeType("application/xml");
				})
				.connect(this.transport)
				.block();

		ConnectionSetupPayload setupPayload = Mono.from(this.connection.sentFrames())
				.map(ConnectionSetupPayload::create)
				.block();

		assertThat(setupPayload.dataMimeType()).isEqualTo(dataMimeType.toString());
		assertThat(setupPayload.metadataMimeType()).isEqualTo(metaMimeType.toString());
		assertThat(requester.dataMimeType()).isEqualTo(dataMimeType);
		assertThat(requester.metadataMimeType()).isEqualTo(metaMimeType);
	}

	@Test
	public void setupRoute() {
		RSocketRequester.builder()
				.dataMimeType(MimeTypeUtils.TEXT_PLAIN)
				.metadataMimeType(MimeTypeUtils.TEXT_PLAIN)
				.setupRoute("toA")
				.setupData("My data")
				.connect(this.transport)
				.block();

		ConnectionSetupPayload setupPayload = Mono.from(this.connection.sentFrames())
				.map(ConnectionSetupPayload::create)
				.block();

		assertThat(setupPayload.getMetadataUtf8()).isEqualTo("toA");
		assertThat(setupPayload.getDataUtf8()).isEqualTo("My data");
	}

	@Test
	public void setupWithAsyncValues() {

		Mono<String> asyncMeta1 = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async Metadata 1");
		Mono<String> asyncMeta2 = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async Metadata 2");
		Mono<String> data = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async data");

		RSocketRequester.builder()
				.dataMimeType(MimeTypeUtils.TEXT_PLAIN)
				.setupRoute("toA")
				.setupMetadata(asyncMeta1, new MimeType("text", "x.test.metadata1"))
				.setupMetadata(asyncMeta2, new MimeType("text", "x.test.metadata2"))
				.setupData(data)
				.connect(this.transport)
				.block();

		ConnectionSetupPayload payload = Mono.from(this.connection.sentFrames())
				.map(ConnectionSetupPayload::create)
				.block();

		MimeType compositeMimeType =
				MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());

		DefaultMetadataExtractor extractor = new DefaultMetadataExtractor(StringDecoder.allMimeTypes());
		extractor.metadataToExtract(new MimeType("text", "x.test.metadata1"), String.class, "asyncMeta1");
		extractor.metadataToExtract(new MimeType("text", "x.test.metadata2"), String.class, "asyncMeta2");
		Map<String, Object> metadataValues = extractor.extract(payload, compositeMimeType);

		assertThat(metadataValues.get("asyncMeta1")).isEqualTo("Async Metadata 1");
		assertThat(metadataValues.get("asyncMeta2")).isEqualTo("Async Metadata 2");
		assertThat(payload.getDataUtf8()).isEqualTo("Async data");
	}

	@Test
	public void frameDecoderMatchesDataBufferFactory() throws Exception {
		testFrameDecoder(new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), PayloadDecoder.ZERO_COPY);
		testFrameDecoder(new DefaultDataBufferFactory(), PayloadDecoder.DEFAULT);
	}

	private void testFrameDecoder(DataBufferFactory bufferFactory, PayloadDecoder frameDecoder)
			throws NoSuchFieldException {

		RSocketStrategies strategies = RSocketStrategies.builder()
				.dataBufferFactory(bufferFactory)
				.build();

		RSocketRequester.builder()
				.rsocketStrategies(strategies)
				.rsocketFactory(this.rsocketFactoryConfigurer)
				.connect(this.transport)
				.block();

		RSocketFactory.ClientRSocketFactory factory = this.rsocketFactoryConfigurer.rsocketFactory();
		assertThat(factory).isNotNull();

		Field field = RSocketFactory.ClientRSocketFactory.class.getDeclaredField("payloadDecoder");
		ReflectionUtils.makeAccessible(field);
		PayloadDecoder decoder = (PayloadDecoder) ReflectionUtils.getField(field, factory);
		assertThat(decoder).isSameAs(frameDecoder);
	}


	static class MockConnection implements DuplexConnection {

		private Publisher<ByteBuf> sentFrames;


		public Publisher<ByteBuf> sentFrames() {
			return this.sentFrames;
		}

		@Override
		public Mono<Void> send(Publisher<ByteBuf> frames) {
			this.sentFrames = frames;
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


	static class TestRSocketFactoryConfigurer implements ClientRSocketFactoryConfigurer {

		private RSocketFactory.ClientRSocketFactory rsocketFactory;


		RSocketFactory.ClientRSocketFactory rsocketFactory() {
			return this.rsocketFactory;
		}


		@Override
		public void configure(RSocketFactory.ClientRSocketFactory rsocketFactory) {
			this.rsocketFactory = rsocketFactory;
		}
	}


	static class TestJsonDecoder implements Decoder<Object> {

		private final MimeType mimeType;


		TestJsonDecoder(MimeType mimeType) {
			this.mimeType = mimeType;
		}

		@Override
		public List<MimeType> getDecodableMimeTypes() {
			return Collections.singletonList(this.mimeType);
		}

		@Override
		public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Mono<Object> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
				MimeType mimeType, Map<String, Object> hints) {

			throw new UnsupportedOperationException();
		}

		@Override
		public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
				MimeType mimeType, Map<String, Object> hints) {

			throw new UnsupportedOperationException();
		}


		@Override
		public Object decode(DataBuffer buffer, ResolvableType targetType, MimeType mimeType,
				Map<String, Object> hints) throws DecodingException {

			throw new UnsupportedOperationException();
		}
	}

}
