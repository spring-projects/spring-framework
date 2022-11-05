/*
 * Copyright 2002-2022 the original author or authors.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.rsocket.Payload;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceStrategy;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Default implementation of {@link RSocketRequester.Builder}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.2
 */
final class DefaultRSocketRequesterBuilder implements RSocketRequester.Builder {

	private static final Map<String, Object> HINTS = Collections.emptyMap();

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private static final Payload EMPTY_SETUP_PAYLOAD = DefaultPayload.create(EMPTY_BYTE_ARRAY);


	@Nullable
	private MimeType dataMimeType;

	@Nullable
	private MimeType metadataMimeType;

	@Nullable
	private Object setupData;

	@Nullable
	private String setupRoute;

	@Nullable
	private Object[] setupRouteVars;

	@Nullable
	private Map<Object, MimeType> setupMetadata;

	@Nullable
	private RSocketStrategies strategies;

	private final List<Consumer<RSocketStrategies.Builder>> strategiesConfigurers = new ArrayList<>();

	private final List<RSocketConnectorConfigurer> rsocketConnectorConfigurers = new ArrayList<>();


	@Override
	public RSocketRequester.Builder dataMimeType(@Nullable MimeType mimeType) {
		this.dataMimeType = mimeType;
		return this;
	}

	@Override
	public RSocketRequester.Builder metadataMimeType(MimeType mimeType) {
		Assert.notNull(mimeType, "'metadataMimeType' is required");
		this.metadataMimeType = mimeType;
		return this;
	}

	@Override
	public RSocketRequester.Builder setupData(Object data) {
		this.setupData = data;
		return this;
	}

	@Override
	public RSocketRequester.Builder setupRoute(String route, Object... routeVars) {
		this.setupRoute = route;
		this.setupRouteVars = routeVars;
		return this;
	}

	@Override
	public RSocketRequester.Builder setupMetadata(Object metadata, @Nullable MimeType mimeType) {
		this.setupMetadata = (this.setupMetadata == null ? new LinkedHashMap<>(4) : this.setupMetadata);
		this.setupMetadata.put(metadata, mimeType);
		return this;
	}

	@Override
	public RSocketRequester.Builder rsocketStrategies(@Nullable RSocketStrategies strategies) {
		this.strategies = strategies;
		return this;
	}

	@Override
	public RSocketRequester.Builder rsocketStrategies(Consumer<RSocketStrategies.Builder> configurer) {
		this.strategiesConfigurers.add(configurer);
		return this;
	}

	@Override
	public RSocketRequester.Builder rsocketConnector(RSocketConnectorConfigurer configurer) {
		this.rsocketConnectorConfigurers.add(configurer);
		return this;
	}

	@Override
	public RSocketRequester.Builder apply(Consumer<RSocketRequester.Builder> configurer) {
		configurer.accept(this);
		return this;
	}

	@Override
	public RSocketRequester tcp(String host, int port) {
		return transport(TcpClientTransport.create(host, port));
	}

	@Override
	public RSocketRequester websocket(URI uri) {
		return transport(WebsocketClientTransport.create(uri));
	}

	@Override
	public RSocketRequester transport(ClientTransport transport) {
		RSocketStrategies strategies = getRSocketStrategies();
		MimeType metaMimeType = getMetadataMimeType();
		MimeType dataMimeType = getDataMimeType(strategies);

		RSocketConnector connector = initConnector(
				this.rsocketConnectorConfigurers, metaMimeType, dataMimeType, strategies);

		RSocketClient client = RSocketClient.from(connector.connect(transport));
		return new DefaultRSocketRequester(client, null, dataMimeType, metaMimeType, strategies);
	}

	@Override
	public RSocketRequester transports(
			Publisher<List<LoadbalanceTarget>> targetPublisher, LoadbalanceStrategy loadbalanceStrategy) {

		RSocketStrategies strategies = getRSocketStrategies();
		MimeType metaMimeType = getMetadataMimeType();
		MimeType dataMimeType = getDataMimeType(strategies);

		RSocketConnector connector = initConnector(
				this.rsocketConnectorConfigurers, metaMimeType, dataMimeType, strategies);

		LoadbalanceRSocketClient client = LoadbalanceRSocketClient.builder(targetPublisher)
				.connector(connector)
				.loadbalanceStrategy(loadbalanceStrategy)
				.build();

		return new DefaultRSocketRequester(client, null, dataMimeType, metaMimeType, strategies);
	}

	@Override
	@SuppressWarnings("deprecation")
	public Mono<RSocketRequester> connectTcp(String host, int port) {
		return connect(TcpClientTransport.create(host, port));
	}

	@Override
	@SuppressWarnings("deprecation")
	public Mono<RSocketRequester> connectWebSocket(URI uri) {
		return connect(WebsocketClientTransport.create(uri));
	}

	@Override
	@SuppressWarnings("deprecation")
	public Mono<RSocketRequester> connect(ClientTransport transport) {
		RSocketStrategies rsocketStrategies = getRSocketStrategies();
		MimeType metaMimeType = getMetadataMimeType();
		MimeType dataMimeType = getDataMimeType(rsocketStrategies);

		RSocketConnector connector = initConnector(
				this.rsocketConnectorConfigurers, metaMimeType, dataMimeType, rsocketStrategies);

		return connector.connect(transport).map(rsocket ->
				new DefaultRSocketRequester(null, rsocket, dataMimeType, metaMimeType, rsocketStrategies));
	}

	public MimeType getMetadataMimeType() {
		return this.metadataMimeType != null ? this.metadataMimeType :
				MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());
	}

	private RSocketStrategies getRSocketStrategies() {
		RSocketStrategies result;
		if (!this.strategiesConfigurers.isEmpty()) {
			RSocketStrategies.Builder builder =
					this.strategies != null ? this.strategies.mutate() : RSocketStrategies.builder();
			this.strategiesConfigurers.forEach(c -> c.accept(builder));
			result = builder.build();
		}
		else {
			result = this.strategies != null ? this.strategies : RSocketStrategies.builder().build();
		}
		Assert.isTrue(!result.encoders().isEmpty(), "No encoders");
		Assert.isTrue(!result.decoders().isEmpty(), "No decoders");
		return result;
	}

	private MimeType getDataMimeType(RSocketStrategies strategies) {
		if (this.dataMimeType != null) {
			return this.dataMimeType;
		}
		// First non-basic Decoder (e.g. CBOR, Protobuf)
		for (Decoder<?> candidate : strategies.decoders()) {
			if (!isCoreCodec(candidate) && !candidate.getDecodableMimeTypes().isEmpty()) {
				return getMimeType(candidate);
			}
		}
		// First core decoder (e.g. String)
		for (Decoder<?> decoder : strategies.decoders()) {
			if (!decoder.getDecodableMimeTypes().isEmpty()) {
				return getMimeType(decoder);
			}
		}
		throw new IllegalArgumentException("Failed to select data MimeType to use.");
	}

	private static boolean isCoreCodec(Object codec) {
		return codec.getClass().getPackage().equals(StringDecoder.class.getPackage());
	}

	private static MimeType getMimeType(Decoder<?> decoder) {
		MimeType mimeType = decoder.getDecodableMimeTypes().get(0);
		return mimeType.getParameters().isEmpty() ? mimeType : new MimeType(mimeType, Collections.emptyMap());
	}

	private Mono<Payload> getSetupPayload(
			MimeType dataMimeType, MimeType metaMimeType, RSocketStrategies strategies) {

		Object data = this.setupData;
		boolean hasMetadata = (this.setupRoute != null || !CollectionUtils.isEmpty(this.setupMetadata));
		if (!hasMetadata && data == null) {
			return Mono.just(EMPTY_SETUP_PAYLOAD);
		}

		Mono<DataBuffer> dataMono = Mono.empty();
		if (data != null) {
			ReactiveAdapter adapter = strategies.reactiveAdapterRegistry().getAdapter(data.getClass());
			Assert.isTrue(adapter == null || !adapter.isMultiValue(), () -> "Expected single value: " + data);
			Mono<?> mono = (adapter != null ? Mono.from(adapter.toPublisher(data)) : Mono.just(data));
			dataMono = mono.map(value -> {
				ResolvableType type = ResolvableType.forClass(value.getClass());
				Encoder<Object> encoder = strategies.encoder(type, dataMimeType);
				Assert.notNull(encoder, () -> "No encoder for " + dataMimeType + ", " + type);
				return encoder.encodeValue(value, strategies.dataBufferFactory(), type, dataMimeType, HINTS);
			});
		}

		Mono<DataBuffer> metaMono = Mono.empty();
		if (hasMetadata) {
			metaMono = new MetadataEncoder(metaMimeType, strategies)
					.metadataAndOrRoute(this.setupMetadata, this.setupRoute, this.setupRouteVars)
					.encode();
		}

		Mono<DataBuffer> emptyBuffer = Mono.fromCallable(() ->
				strategies.dataBufferFactory().wrap(EMPTY_BYTE_ARRAY));

		dataMono = dataMono.switchIfEmpty(emptyBuffer);
		metaMono = metaMono.switchIfEmpty(emptyBuffer);

		return Mono.zip(dataMono, metaMono)
				.map(tuple -> PayloadUtils.createPayload(tuple.getT1(), tuple.getT2()))
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release)
				.doOnDiscard(Payload.class, Payload::release);
	}

	@SuppressWarnings("deprecation")
	private RSocketConnector initConnector(List<RSocketConnectorConfigurer> connectorConfigurers,
			MimeType metaMimeType, MimeType dataMimeType, RSocketStrategies rsocketStrategies) {

		RSocketConnector connector = RSocketConnector.create();
		connectorConfigurers.forEach(c -> c.configure(connector));

		if (rsocketStrategies.dataBufferFactory() instanceof NettyDataBufferFactory) {
			connector.payloadDecoder(PayloadDecoder.ZERO_COPY);
		}

		connector.metadataMimeType(metaMimeType.toString());
		connector.dataMimeType(dataMimeType.toString());

		Mono<Payload> setupPayloadMono = getSetupPayload(dataMimeType, metaMimeType, rsocketStrategies);
		if (setupPayloadMono != EMPTY_SETUP_PAYLOAD) {
			connector.setupPayload(setupPayloadMono);
		}

		return connector;
	}

}
