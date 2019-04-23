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

	@Nullable
	private List<Consumer<RSocketFactory.ClientRSocketFactory>> factoryConfigurers = new ArrayList<>();

	@Nullable
	private List<Consumer<RSocketStrategies.Builder>> strategiesConfigurers = new ArrayList<>();

	@Override
	public RSocketRequester.Builder rsocketFactory(Consumer<RSocketFactory.ClientRSocketFactory> configurer) {
		this.factoryConfigurers.add(configurer);
		return this;
	}

	@Override
	public RSocketRequester.Builder rsocketStrategies(Consumer<RSocketStrategies.Builder> configurer) {
		this.strategiesConfigurers.add(configurer);
		return this;
	}

	@Override
	public Mono<RSocketRequester> connect(ClientTransport transport, MimeType dataMimeType) {
		return Mono.defer(() -> {
			RSocketStrategies.Builder strategiesBuilder = RSocketStrategies.builder();
			this.strategiesConfigurers.forEach(configurer -> configurer.accept(strategiesBuilder));
			RSocketFactory.ClientRSocketFactory clientFactory = RSocketFactory.connect()
					.dataMimeType(dataMimeType.toString());
			this.factoryConfigurers.forEach(configurer -> configurer.accept(clientFactory));
			return clientFactory.transport(transport).start()
					.map(rsocket -> RSocketRequester.create(rsocket, dataMimeType, strategiesBuilder.build()));
		});
	}

	@Override
	public Mono<RSocketRequester> connectTcp(String host, int port, MimeType dataMimeType) {
		return connect(TcpClientTransport.create(host, port), dataMimeType);
	}

	@Override
	public Mono<RSocketRequester> connectWebSocket(URI uri, MimeType dataMimeType) {
		return connect(WebsocketClientTransport.create(uri), dataMimeType);
	}

}
