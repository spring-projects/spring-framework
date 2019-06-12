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

import java.util.function.BiFunction;
import java.util.function.Function;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * Extension of {@link RSocketMessageHandler} that can be plugged directly into
 * RSocket to receive connections either on the
 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(Function) client} or on the
 * {@link io.rsocket.RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor) server}
 * side. Requests are handled by delegating to the "super" {@link #handleMessage(Message)}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public final class MessageHandlerAcceptor extends RSocketMessageHandler
		implements SocketAcceptor, BiFunction<ConnectionSetupPayload, RSocket, RSocket> {

	@Nullable
	private MimeType defaultDataMimeType;

	private MimeType defaultMetadataMimeType = DefaultRSocketRequester.COMPOSITE_METADATA;


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
	public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket sendingRSocket) {
		MessagingRSocket rsocket = createRSocket(setupPayload, sendingRSocket);

		// Allow handling of the ConnectionSetupPayload via @MessageMapping methods.
		// However, if the handling is to make requests to the client, it's expected
		// it will do so decoupled from the handling, e.g. via .subscribe().
		return rsocket.handleConnectionSetupPayload(setupPayload).then(Mono.just(rsocket));
	}

	@Override
	public RSocket apply(ConnectionSetupPayload setupPayload, RSocket sendingRSocket) {
		return createRSocket(setupPayload, sendingRSocket);
	}

	private MessagingRSocket createRSocket(ConnectionSetupPayload setupPayload, RSocket rsocket) {

		MimeType dataMimeType = StringUtils.hasText(setupPayload.dataMimeType()) ?
				MimeTypeUtils.parseMimeType(setupPayload.dataMimeType()) :
				this.defaultDataMimeType;
		Assert.notNull(dataMimeType,
				"No `dataMimeType` in the ConnectionSetupPayload and no default value");

		MimeType metadataMimeType = StringUtils.hasText(setupPayload.dataMimeType()) ?
				MimeTypeUtils.parseMimeType(setupPayload.metadataMimeType()) :
				this.defaultMetadataMimeType;
		Assert.notNull(dataMimeType,
				"No `metadataMimeType` in the ConnectionSetupPayload and no default value");

		RSocketRequester requester = RSocketRequester.wrap(
				rsocket, dataMimeType, metadataMimeType, getRSocketStrategies());

		return new MessagingRSocket(this, getRouteMatcher(), requester,
				dataMimeType, metadataMimeType, getRSocketStrategies().dataBufferFactory());
	}

}
