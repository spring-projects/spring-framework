/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.Collections;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.ClassUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 */
class ServerSentEventResponse<T extends Publisher<?>> extends AbstractResponse<T> {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					DefaultConfiguration.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							DefaultConfiguration.class.getClassLoader());

	private static final ResolvableType SERVER_SIDE_EVENT_TYPE = ResolvableType.forClass(ServerSentEvent.class);

	private final ServerSentEventHttpMessageWriter messageWriter;

	private final T eventsPublisher;

	private final ResolvableType eventType;


	private ServerSentEventResponse(int statusCode, HttpHeaders headers, T eventsPublisher, ResolvableType eventType) {
		super(statusCode, headers);
		this.eventsPublisher = eventsPublisher;
		this.eventType = eventType;
		this.messageWriter =
				jackson2Present ? new ServerSentEventHttpMessageWriter(Collections.singletonList(new Jackson2JsonEncoder())) :
						new ServerSentEventHttpMessageWriter();
	}

	public static <T, S extends Publisher<T>> ServerSentEventResponse<S> fromPublisher(int statusCode, HttpHeaders headers, S eventsPublisher, Class<? extends T> eventType) {
		return new ServerSentEventResponse<S>(statusCode, headers, eventsPublisher, ResolvableType.forClass(eventType));
	}

	public static <T, S extends Publisher<ServerSentEvent<T>>> ServerSentEventResponse<S> fromSseEvents(int statusCode, HttpHeaders headers, S eventsPublisher) {
		return new ServerSentEventResponse<S>(statusCode, headers, eventsPublisher, SERVER_SIDE_EVENT_TYPE);
	}

	@Override
	public T body() {
		return this.eventsPublisher;
	}

	@Override
	public Mono<Void> writeTo(ServerWebExchange exchange) {
		writeStatusAndHeaders(exchange);
		return this.messageWriter.write(this.eventsPublisher, this.eventType, null, exchange.getResponse());
	}

}
