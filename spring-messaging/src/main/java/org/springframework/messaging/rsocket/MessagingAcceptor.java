/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.rsocket;

import java.util.function.Function;
import java.util.function.Predicate;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * RSocket acceptor for
 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(Function) client} or
 * {@link io.rsocket.RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor) server}
 * side use. It wraps requests with a {@link Message} envelope and sends them
 * to a {@link ReactiveMessageChannel} for handling, e.g. via
 * {@code @MessageMapping} method.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public final class MessagingAcceptor implements SocketAcceptor, Function<RSocket, RSocket> {

	private final ReactiveMessageChannel messageChannel;

	private final RSocketStrategies rsocketStrategies;

	@Nullable
	private MimeType defaultDataMimeType;


	/**
	 * Constructor with a message channel to send messages to.
	 * @param messageChannel the message channel to use
	 * <p>This assumes a Spring configuration setup with a
	 * {@code ReactiveMessageChannel} and an {@link RSocketMessageHandler} which
	 * by default auto-detects {@code @MessageMapping} methods in
	 * {@code @Controller} classes, but can also be configured with a
	 * {@link RSocketMessageHandler#setHandlerPredicate(Predicate) handlerPredicate}
	 * or with handler instances.
	 */
	public MessagingAcceptor(ReactiveMessageChannel messageChannel) {
		this(messageChannel, RSocketStrategies.builder().build());
	}

	/**
	 * Variant of {@link #MessagingAcceptor(ReactiveMessageChannel)} with an
	 * {@link RSocketStrategies} for wrapping the sending {@link RSocket} as
	 * {@link RSocketRequester}.
	 */
	public MessagingAcceptor(ReactiveMessageChannel messageChannel, RSocketStrategies rsocketStrategies) {
		Assert.notNull(messageChannel, "ReactiveMessageChannel is required");
		Assert.notNull(rsocketStrategies, "RSocketStrategies is required");
		this.messageChannel = messageChannel;
		this.rsocketStrategies = rsocketStrategies;
	}


	/**
	 * Configure the default content type to use for data payloads.
	 * <p>By default this is not set. However a server acceptor will use the
	 * content type from the {@link ConnectionSetupPayload}.
	 * @param defaultDataMimeType the MimeType to use
	 */
	public void setDefaultDataMimeType(@Nullable MimeType defaultDataMimeType) {
		this.defaultDataMimeType = defaultDataMimeType;
	}


	@Override
	public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket sendingRSocket) {
		MessagingRSocket rsocket = createRSocket(sendingRSocket);
		rsocket.handleConnectionSetupPayload(setupPayload).subscribe();
		return Mono.just(rsocket);
	}

	@Override
	public RSocket apply(RSocket sendingRSocket) {
		return createRSocket(sendingRSocket);
	}

	private MessagingRSocket createRSocket(RSocket rsocket) {
		return new MessagingRSocket(this.messageChannel, rsocket, this.defaultDataMimeType, this.rsocketStrategies);
	}

}
