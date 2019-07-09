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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Default implementation of {@link RSocketRequester.Builder}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.2
 */
final class DefaultRSocketRequesterBuilder implements RSocketRequester.Builder {

	@Nullable
	private MimeType dataMimeType;

	private MimeType metadataMimeType = DefaultRSocketRequester.COMPOSITE_METADATA;

	private List<Consumer<RSocketFactory.ClientRSocketFactory>> factoryConfigurers = new ArrayList<>();

	@Nullable
	private RSocketStrategies strategies;

	private List<Consumer<RSocketStrategies.Builder>> strategiesConfigurers = new ArrayList<>();

	private List<Object> handlers = new ArrayList<>();

	@Override
	public RSocketRequester.Builder dataMimeType(@Nullable MimeType mimeType) {
		this.dataMimeType = mimeType;
		return this;
	}

	@Override
	public RSocketRequester.Builder metadataMimeType(MimeType mimeType) {
		Assert.notNull(mimeType, "`metadataMimeType` is required");
		this.metadataMimeType = mimeType;
		return this;
	}

	@Override
	public RSocketRequester.Builder rsocketFactory(Consumer<RSocketFactory.ClientRSocketFactory> configurer) {
		this.factoryConfigurers.add(configurer);
		return this;
	}

	@Override
	public RSocketRequester.Builder rsocketStrategies(@Nullable RSocketStrategies strategies) {
		this.strategies = strategies;
		return this;
	}

	@Override
	public RSocketRequester.Builder annotatedHandlers(Object... handlers) {
		this.handlers.addAll(Arrays.asList(handlers));
		return this;
	}

	@Override
	public RSocketRequester.Builder rsocketStrategies(Consumer<RSocketStrategies.Builder> configurer) {
		this.strategiesConfigurers.add(configurer);
		return this;
	}

	@Override
	public Mono<RSocketRequester> connectTcp(String host, int port) {
		return connect(TcpClientTransport.create(host, port));
	}

	@Override
	public Mono<RSocketRequester> connectWebSocket(URI uri) {
		return connect(WebsocketClientTransport.create(uri));
	}

	@Override
	public Mono<RSocketRequester> connect(ClientTransport transport) {
		return Mono.defer(() -> doConnect(transport));
	}

	private Mono<RSocketRequester> doConnect(ClientTransport transport) {
		RSocketStrategies rsocketStrategies = getRSocketStrategies();
		Assert.isTrue(!rsocketStrategies.encoders().isEmpty(), "No encoders");
		Assert.isTrue(!rsocketStrategies.decoders().isEmpty(), "No decoders");

		RSocketFactory.ClientRSocketFactory rsocketFactory = RSocketFactory.connect();
		MimeType dataMimeType = getDataMimeType(rsocketStrategies);
		rsocketFactory.dataMimeType(dataMimeType.toString());
		rsocketFactory.metadataMimeType(this.metadataMimeType.toString());

		if (!this.handlers.isEmpty()) {
			RSocketMessageHandler messageHandler = new RSocketMessageHandler();
			messageHandler.setHandlers(this.handlers);
			messageHandler.setRSocketStrategies(rsocketStrategies);
			messageHandler.afterPropertiesSet();
			rsocketFactory.acceptor(messageHandler.clientAcceptor());
		}
		rsocketFactory.frameDecoder(PayloadDecoder.ZERO_COPY);
		this.factoryConfigurers.forEach(consumer -> consumer.accept(rsocketFactory));

		return rsocketFactory.transport(transport)
				.start()
				.map(rsocket -> new DefaultRSocketRequester(
						rsocket, dataMimeType, this.metadataMimeType, rsocketStrategies));
	}

	private RSocketStrategies getRSocketStrategies() {
		if (!this.strategiesConfigurers.isEmpty()) {
			RSocketStrategies.Builder builder =
					this.strategies != null ? this.strategies.mutate() : RSocketStrategies.builder();
			this.strategiesConfigurers.forEach(c -> c.accept(builder));
			return builder.build();
		}
		else {
			return this.strategies != null ? this.strategies : RSocketStrategies.builder().build();
		}
	}

	private MimeType getDataMimeType(RSocketStrategies strategies) {
		if (this.dataMimeType != null) {
			return this.dataMimeType;
		}
		return Stream
				.concat(
						strategies.encoders().stream()
								.flatMap(encoder -> encoder.getEncodableMimeTypes().stream()),
						strategies.decoders().stream()
								.flatMap(encoder -> encoder.getDecodableMimeTypes().stream())
				)
				.filter(MimeType::isConcrete)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Failed to select data MimeType to use."));
	}

}
