/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive.handler;

import org.reactivestreams.Publisher;
import reactor.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.WebServerExchange;

/**
 * Supports {@link HandlerResult} with a {@code void} or {@code Publisher<Void>} value.
 * An optional {link ConversionService} can be used to support types that can be converted to
 * {@code Publisher<Void>}, like {@code Observable<Void>} or {@code CompletableFuture<Void>}.
 *
 * @author Sebastien Deleuze
 */
public class SimpleHandlerResultHandler implements Ordered, HandlerResultHandler {

	private int order = Ordered.LOWEST_PRECEDENCE;

	private ConversionService conversionService;


	public SimpleHandlerResultHandler() {
	}

	public SimpleHandlerResultHandler(ConversionService conversionService) {
		Assert.notNull(conversionService, "'conversionService' is required.");
		this.conversionService = conversionService;
	}


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public boolean supports(HandlerResult result) {
		ResolvableType type = result.getResultType();
		return type != null && Void.TYPE.equals(type.getRawClass()) ||
				(Void.class.isAssignableFrom(type.getGeneric(0).getRawClass()) && isConvertibleToPublisher(type));
	}

	private boolean isConvertibleToPublisher(ResolvableType type) {
		return Publisher.class.isAssignableFrom(type.getRawClass()) ||
				((this.conversionService != null) &&
						this.conversionService.canConvert(type.getRawClass(), Publisher.class));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> handleResult(WebServerExchange exchange, HandlerResult result) {
		Object value = result.getResult();
		if (Void.TYPE.equals(result.getResultType().getRawClass())) {
			return Mono.empty();
		}
		return (value instanceof Mono ? (Mono<Void>)value :
				Mono.from(this.conversionService.convert(value, Publisher.class)));
	}

}
