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

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.support.reactive.MessageMappingMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * RSocket-specific extension of {@link MessageMappingMessageHandler}.
 *
 * <p>The configured {@link #setEncoders(List) encoders} are used to encode the
 * return values from handler methods, with the help of
 * {@link RSocketPayloadReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketMessageHandler extends MessageMappingMessageHandler {

	private final List<Encoder<?>> encoders = new ArrayList<>();

	@Nullable
	private RSocketStrategies rsocketStrategies;


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
	protected void handleNoMatch(@Nullable String destination, Message<?> message) {

		// MessagingRSocket will raise an error anyway if reply Mono is expected
		// Here we raise a more helpful message a destination is present

		// It is OK if some messages (ConnectionSetupPayload, metadataPush) are not handled
		// We need a better way to avoid raising errors for those

		if (StringUtils.hasText(destination)) {
			throw new MessageDeliveryException("No handler for destination '" + destination + "'");
		}
	}

}
