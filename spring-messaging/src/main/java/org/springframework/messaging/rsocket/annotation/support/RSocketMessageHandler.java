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

package org.springframework.messaging.rsocket.annotation.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.reactive.MessageMappingMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.StringUtils;

/**
 * Extension of {@link MessageMappingMessageHandler} to use as an RSocket
 * responder by handling incoming streams via {@code @MessageMapping} annotated
 * methods.
 * <p>Use {@link #clientAcceptor()} and {@link #serverAcceptor()} to obtain
 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(Function) client} or
 * {@link io.rsocket.RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor) server}
 * side adapters.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketMessageHandler extends MessageMappingMessageHandler {

	private final List<Encoder<?>> encoders = new ArrayList<>();

	@Nullable
	private RSocketStrategies rsocketStrategies;

	@Nullable
	private MimeType defaultDataMimeType;

	private MimeType defaultMetadataMimeType = MessagingRSocket.COMPOSITE_METADATA;


	/**
	 * Configure the encoders to use for encoding handler method return values.
	 */
	public void setEncoders(List<? extends Encoder<?>> encoders) {
		this.encoders.addAll(encoders);
	}

	/**
	 * Return the configured {@link #setEncoders(List) encoders}.
	 */
	public List<? extends Encoder<?>> getEncoders() {
		return this.encoders;
	}

	/**
	 * Provide configuration in the form of {@link RSocketStrategies}. This is
	 * an alternative to using {@link #setEncoders(List)},
	 * {@link #setDecoders(List)}, and others directly. It is convenient when
	 * you also configuring an {@link RSocketRequester} in which case the
	 * {@link RSocketStrategies} encapsulates required configuration for re-use.
	 * @param rsocketStrategies the strategies to use
	 */
	public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
		Assert.notNull(rsocketStrategies, "RSocketStrategies must not be null");
		this.rsocketStrategies = rsocketStrategies;
		setDecoders(rsocketStrategies.decoders());
		setEncoders(rsocketStrategies.encoders());
		setReactiveAdapterRegistry(rsocketStrategies.reactiveAdapterRegistry());
	}

	/**
	 * Return the {@code RSocketStrategies} instance provided via
	 * {@link #setRSocketStrategies rsocketStrategies}, or
	 * otherwise initialize it with the configured {@link #setEncoders(List)
	 * encoders}, {@link #setDecoders(List) decoders}, and others.
	 */
	public RSocketStrategies getRSocketStrategies() {
		if (this.rsocketStrategies == null) {
			this.rsocketStrategies = RSocketStrategies.builder()
					.decoder(getDecoders().toArray(new Decoder<?>[0]))
					.encoder(getEncoders().toArray(new Encoder<?>[0]))
					.reactiveAdapterStrategy(getReactiveAdapterRegistry())
					.build();
		}
		return this.rsocketStrategies;
	}

	/**
	 * Configure the default content type to use for data payloads if the
	 * {@code SETUP} frame did not specify one.
	 * <p>By default this is not set.
	 * @param mimeType the MimeType to use
	 */
	public void setDefaultDataMimeType(@Nullable MimeType mimeType) {
		this.defaultDataMimeType = mimeType;
	}

	/**
	 * Configure the default {@code MimeType} for payload data if the
	 * {@code SETUP} frame did not specify one.
	 * <p>By default this is set to {@code "message/x.rsocket.composite-metadata.v0"}
	 * @param mimeType the MimeType to use
	 */
	public void setDefaultMetadataMimeType(MimeType mimeType) {
		Assert.notNull(mimeType, "'metadataMimeType' is required");
		this.defaultMetadataMimeType = mimeType;
	}


	@Override
	public void afterPropertiesSet() {
		getArgumentResolverConfigurer().addCustomResolver(new RSocketRequesterMethodArgumentResolver());
		super.afterPropertiesSet();
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
		handlers.add(new RSocketPayloadReturnValueHandler(this.encoders, getReactiveAdapterRegistry()));
		handlers.addAll(getReturnValueHandlerConfigurer().getCustomHandlers());
		return handlers;
	}

	@Override
	protected void handleNoMatch(@Nullable RouteMatcher.Route destination, Message<?> message) {

		// MessagingRSocket will raise an error anyway if reply Mono is expected
		// Here we raise a more helpful message if a destination is present

		// It is OK if some messages (ConnectionSetupPayload, metadataPush) are not handled
		// This works but would be better to have a more explicit way to differentiate

		if (destination != null && StringUtils.hasText(destination.value())) {
			throw new MessageDeliveryException("No handler for destination '" + destination.value() + "'");
		}
	}

	/**
	 * Return an adapter for a
	 * {@link io.rsocket.RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor)
	 * server acceptor}. The adapter implements a responding {@link RSocket} by
	 * wrapping {@code Payload} data and metadata as {@link Message} and
	 * delegating to this {@link RSocketMessageHandler} to handle and reply.
	 */
	public SocketAcceptor serverAcceptor() {
		return (setupPayload, sendingRSocket) -> {
			MessagingRSocket rsocket = createRSocket(setupPayload, sendingRSocket);

			// Allow handling of the ConnectionSetupPayload via @MessageMapping methods.
			// However, if the handling is to make requests to the client, it's expected
			// it will do so decoupled from the handling, e.g. via .subscribe().
			return rsocket.handleConnectionSetupPayload(setupPayload).then(Mono.just(rsocket));
		};
	}

	/**
	 * Return an adapter for a
	 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(BiFunction)
	 * client acceptor}. The adapter implements a responding {@link RSocket} by
	 * wrapping {@code Payload} data and metadata as {@link Message} and
	 * delegating to this {@link RSocketMessageHandler} to handle and reply.
	 */
	public BiFunction<ConnectionSetupPayload, RSocket, RSocket> clientAcceptor() {
		return this::createRSocket;
	}

	private MessagingRSocket createRSocket(ConnectionSetupPayload setupPayload, RSocket rsocket) {
		String s = setupPayload.dataMimeType();
		MimeType dataMimeType = StringUtils.hasText(s) ? MimeTypeUtils.parseMimeType(s) : this.defaultDataMimeType;
		Assert.notNull(dataMimeType, "No `dataMimeType` in ConnectionSetupPayload and no default value");

		s = setupPayload.metadataMimeType();
		MimeType metaMimeType = StringUtils.hasText(s) ? MimeTypeUtils.parseMimeType(s) : this.defaultMetadataMimeType;
		Assert.notNull(dataMimeType, "No `metadataMimeType` in ConnectionSetupPayload and no default value");

		RSocketRequester requester = RSocketRequester.wrap(
				rsocket, dataMimeType, metaMimeType, getRSocketStrategies());

		return new MessagingRSocket(this, getRouteMatcher(), requester,
				dataMimeType, metaMimeType, getRSocketStrategies().dataBufferFactory());
	}

}
