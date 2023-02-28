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

package org.springframework.messaging.rsocket;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import io.rsocket.core.DefaultConnectionSetupPayload;
import io.rsocket.core.RSocketConnector;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DefaultRSocketRequesterBuilder}.
 *
 * @author Brian Clozel
 */
public class DefaultRSocketRequesterBuilderTests {

	private ClientTransport transport = mock();

	private final MockConnection connection = new MockConnection();

	private final TestRSocketConnectorConfigurer connectorConfigurer = new TestRSocketConnectorConfigurer();


	@BeforeEach
	public void setup() {
		given(this.transport.connect()).willReturn(Mono.just(this.connection));
		given(this.transport.maxFrameLength()).willReturn(16777215);
	}


	@Test
	@SuppressWarnings("unchecked")
	public void rsocketConnectorConfigurer() {
		Consumer<RSocketStrategies.Builder> strategiesConfigurer = mock();
		RSocketRequester.builder()
				.rsocketConnector(this.connectorConfigurer)
				.rsocketStrategies(strategiesConfigurer)
				.transport(this.transport);

		// RSocketStrategies and RSocketConnector configurers should have been called

		verify(strategiesConfigurer).accept(any(RSocketStrategies.Builder.class));
		assertThat(this.connectorConfigurer.connector()).isNotNull();
	}

	@Test
	public void defaultDataMimeType() {
		RSocketRequester requester = RSocketRequester.builder().transport(this.transport);

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
				.transport(this.transport);

		assertThat(requester.dataMimeType())
				.as("Default data MimeType, based on the first configured, non-default Decoder")
				.isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void dataMimeTypeExplicitlySet() {
		RSocketRequester requester = RSocketRequester.builder()
				.dataMimeType(MimeTypeUtils.APPLICATION_JSON)
				.transport(this.transport);

		ConnectionSetupPayload setupPayload = getConnectionSetupPayload(requester);

		assertThat(setupPayload.dataMimeType()).isEqualTo("application/json");
		assertThat(requester.dataMimeType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void mimeTypesCannotBeChangedAtRSocketConnectorLevel() {
		MimeType dataMimeType = MimeTypeUtils.APPLICATION_JSON;
		MimeType metaMimeType = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());

		RSocketRequester requester = RSocketRequester.builder()
				.metadataMimeType(metaMimeType)
				.dataMimeType(dataMimeType)
				.rsocketConnector(connector -> {
					connector.metadataMimeType("text/plain");
					connector.dataMimeType("application/xml");
				})
				.transport(this.transport);

		ConnectionSetupPayload setupPayload = getConnectionSetupPayload(requester);

		assertThat(setupPayload.dataMimeType()).isEqualTo(dataMimeType.toString());
		assertThat(setupPayload.metadataMimeType()).isEqualTo(metaMimeType.toString());
		assertThat(requester.dataMimeType()).isEqualTo(dataMimeType);
		assertThat(requester.metadataMimeType()).isEqualTo(metaMimeType);
	}

	@Test
	public void setupRoute() {
		RSocketRequester requester = RSocketRequester.builder()
				.dataMimeType(MimeTypeUtils.TEXT_PLAIN)
				.metadataMimeType(MimeTypeUtils.TEXT_PLAIN)
				.setupRoute("toA")
				.setupData("My data")
				.transport(this.transport);

		ConnectionSetupPayload setupPayload = getConnectionSetupPayload(requester);

		assertThat(setupPayload.getMetadataUtf8()).isEqualTo("toA");
		assertThat(setupPayload.getDataUtf8()).isEqualTo("My data");
	}

	@Test
	public void setupWithAsyncValues() {

		Mono<String> asyncMeta1 = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async Metadata 1");
		Mono<String> asyncMeta2 = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async Metadata 2");
		Mono<String> data = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async data");

		RSocketRequester requester = RSocketRequester.builder()
				.dataMimeType(MimeTypeUtils.TEXT_PLAIN)
				.setupRoute("toA")
				.setupMetadata(asyncMeta1, new MimeType("text", "x.test.metadata1"))
				.setupMetadata(asyncMeta2, new MimeType("text", "x.test.metadata2"))
				.setupData(data)
				.transport(this.transport);

		ConnectionSetupPayload setupPayload = getConnectionSetupPayload(requester);

		MimeType compositeMimeType =
				MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());

		DefaultMetadataExtractor extractor = new DefaultMetadataExtractor(StringDecoder.allMimeTypes());
		extractor.metadataToExtract(new MimeType("text", "x.test.metadata1"), String.class, "asyncMeta1");
		extractor.metadataToExtract(new MimeType("text", "x.test.metadata2"), String.class, "asyncMeta2");
		Map<String, Object> metadataValues = extractor.extract(setupPayload, compositeMimeType);

		assertThat(metadataValues.get("asyncMeta1")).isEqualTo("Async Metadata 1");
		assertThat(metadataValues.get("asyncMeta2")).isEqualTo("Async Metadata 2");
		assertThat(setupPayload.getDataUtf8()).isEqualTo("Async data");
	}

	@Test
	public void frameDecoderMatchesDataBufferFactory() throws Exception {
		testPayloadDecoder(new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), PayloadDecoder.ZERO_COPY);
		testPayloadDecoder(DefaultDataBufferFactory.sharedInstance, PayloadDecoder.DEFAULT);
	}

	private ConnectionSetupPayload getConnectionSetupPayload(RSocketRequester requester) {
		// Trigger connection establishment
		requester.rsocketClient().source().block();
		return new DefaultConnectionSetupPayload(this.connection.setupFrame());
	}

	private void testPayloadDecoder(DataBufferFactory bufferFactory, PayloadDecoder payloadDecoder)
			throws NoSuchFieldException {

		RSocketStrategies strategies = RSocketStrategies.builder()
				.dataBufferFactory(bufferFactory)
				.build();

		RSocketRequester.builder()
				.rsocketStrategies(strategies)
				.rsocketConnector(this.connectorConfigurer)
				.transport(this.transport);

		RSocketConnector connector = this.connectorConfigurer.connector();
		assertThat(connector).isNotNull();

		Field field = RSocketConnector.class.getDeclaredField("payloadDecoder");
		ReflectionUtils.makeAccessible(field);
		PayloadDecoder decoder = (PayloadDecoder) ReflectionUtils.getField(field, connector);
		assertThat(decoder).isSameAs(payloadDecoder);
	}


	static class MockConnection implements DuplexConnection {

		private ByteBuf setupFrame;


		public ByteBuf setupFrame() {
			return this.setupFrame;
		}

		@Override
		public void sendFrame(int i, ByteBuf byteBuf) {
			this.setupFrame = this.setupFrame == null ? byteBuf : this.setupFrame;
		}

		@Override
		public void sendErrorAndClose(RSocketErrorException e) {
		}

		@Override
		public Flux<ByteBuf> receive() {
			return Flux.empty();
		}

		@Override
		public ByteBufAllocator alloc() {
			return ByteBufAllocator.DEFAULT;
		}

		@Override
		public Mono<Void> onClose() {
			return Mono.never();
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isDisposed() {
			return false;
		}

		@Override
		public SocketAddress remoteAddress() {
			return InetSocketAddress.createUnresolved("localhost", 9090);
		}

	}


	static class TestRSocketConnectorConfigurer implements RSocketConnectorConfigurer {

		private RSocketConnector connector;

		RSocketConnector connector() {
			return this.connector;
		}

		@Override
		public void configure(RSocketConnector connector) {
			this.connector = connector;
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
