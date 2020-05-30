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

import io.rsocket.RSocket;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;

/**
 * Resolves arguments of type {@link RSocket} that can be used for making
 * requests to the remote peer.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketRequesterMethodArgumentResolver implements HandlerMethodArgumentResolver {

	/**
	 * Message header name that is expected to have the {@link RSocket} to
	 * initiate new interactions to the remote peer with.
	 */
	public static final String RSOCKET_REQUESTER_HEADER = "rsocketRequester";


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> type = parameter.getParameterType();
		return (RSocketRequester.class.equals(type) || RSocket.class.isAssignableFrom(type));
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
		Object headerValue = message.getHeaders().get(RSOCKET_REQUESTER_HEADER);
		Assert.notNull(headerValue, "Missing '" + RSOCKET_REQUESTER_HEADER + "'");

		Assert.isInstanceOf(RSocketRequester.class, headerValue, "Expected header value of type RSocketRequester");
		RSocketRequester requester = (RSocketRequester) headerValue;

		Class<?> type = parameter.getParameterType();
		if (RSocketRequester.class.equals(type)) {
			return Mono.just(requester);
		}
		else if (RSocket.class.isAssignableFrom(type)) {
			return Mono.just(requester.rsocket());
		}
		else {
			return Mono.error(new IllegalArgumentException("Unexpected parameter type: " + parameter));
		}
	}

}
