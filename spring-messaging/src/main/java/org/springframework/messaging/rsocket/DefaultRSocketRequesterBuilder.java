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
import java.util.List;
import java.util.function.Consumer;

import io.rsocket.RSocketFactory;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Default implementation of {@link RSocketRequester.Builder}.
 *
 * @author Brian Clozel
 * @since 5.2
 */
final class DefaultRSocketRequesterBuilder implements RSocketRequester.Builder {

	private List<Consumer<RSocketFactory.ClientRSocketFactory>> factoryConfigurers = new ArrayList<>();

	@Nullable
	private RSocketStrategies strategies;

	private List<Consumer<RSocketStrategies.Builder>> strategiesConfigurers = new ArrayList<>();


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
		return Mono.defer(() -> {
			RSocketStrategies strategies = getRSocketStrategies();
			MimeType dataMimeType = getDefaultDataMimeType(strategies);

			RSocketFactory.ClientRSocketFactory factory = RSocketFactory.connect();
			if (dataMimeType != null) {
				factory.dataMimeType(dataMimeType.toString());
			}
			this.factoryConfigurers.forEach(configurer -> configurer.accept(factory));

			return factory.transport(transport).start()
					.map(rsocket -> new DefaultRSocketRequester(rsocket, dataMimeType, strategies));
		});
	}

	private RSocketStrategies getRSocketStrategies() {
		if (this.strategiesConfigurers.isEmpty()) {
			return this.strategies != null ? this.strategies : RSocketStrategies.builder().build();
		}
		RSocketStrategies.Builder strategiesBuilder = this.strategies != null ?
				this.strategies.mutate() : RSocketStrategies.builder();
		this.strategiesConfigurers.forEach(configurer -> configurer.accept(strategiesBuilder));
		return strategiesBuilder.build();
	}

	@Nullable
	private MimeType getDefaultDataMimeType(RSocketStrategies strategies) {
		return strategies.encoders().stream()
				.flatMap(encoder -> encoder.getEncodableMimeTypes().stream())
				.filter(MimeType::isConcrete)
				.findFirst()
				.orElseGet(() ->
						strategies.decoders().stream()
								.flatMap(encoder -> encoder.getDecodableMimeTypes().stream())
								.filter(MimeType::isConcrete)
								.findFirst()
								.orElse(null));
	}

}
