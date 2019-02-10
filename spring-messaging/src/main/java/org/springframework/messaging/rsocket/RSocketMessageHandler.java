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

import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.ReactiveSubscribableChannel;
import org.springframework.messaging.handler.annotation.support.reactive.MessageMappingMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
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


	public RSocketMessageHandler(ReactiveSubscribableChannel inboundChannel) {
		super(inboundChannel);
	}

	public RSocketMessageHandler(ReactiveSubscribableChannel inboundChannel, List<Object> handlers) {
		super(inboundChannel);
		setHandlerPredicate(null);  // disable auto-detection..
		for (Object handler : handlers) {
			detectHandlerMethods(handler);
		}
	}


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


	@Override
	public void afterPropertiesSet() {
		getArgumentResolverConfigurer().addCustomResolver(new SendingRSocketMethodArgumentResolver());
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
		// Ignore empty destination, probably the ConnectionSetupPayload
		if (!StringUtils.isEmpty(destination)) {
			super.handleNoMatch(destination, message);
			throw new MessageDeliveryException("No handler for '" + destination + "'");
		}
	}

}
