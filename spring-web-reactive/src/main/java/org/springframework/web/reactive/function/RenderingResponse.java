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
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 */
class RenderingResponse extends AbstractResponse<Rendering> {

	private final String name;

	private final Map<String, Object> model;

	private final Rendering rendering = new DefaultRendering();

	public RenderingResponse(int statusCode, HttpHeaders headers, String name,
			Map<String, Object> model) {
		super(statusCode, headers);
		this.name = name;
		this.model = Collections.unmodifiableMap(model);
	}

	@Override
	public Rendering body() {
		return this.rendering;
	}

	@Override
	public Mono<Void> writeTo(ServerWebExchange exchange) {
		writeStatusAndHeaders(exchange);
		MediaType contentType = exchange.getResponse().getHeaders().getContentType();
		Locale locale = Locale.ENGLISH; // TODO
		return Flux.fromStream(viewResolverStream(exchange))
				.concatMap(viewResolver -> viewResolver.resolveViewName(this.name, locale))
				.next()
				.otherwiseIfEmpty(Mono.error(new IllegalArgumentException("Could not resolve view with name '" + this.name +"'")))
				.then(view -> view.render(this.model, contentType, exchange));
	}

	private Stream<ViewResolver> viewResolverStream(ServerWebExchange exchange) {
		return exchange.<Supplier<Stream<ViewResolver>>>getAttribute(
				Router.VIEW_RESOLVERS_ATTRIBUTE)
				.orElseThrow(() -> new IllegalStateException(
						"Could not find ViewResolvers in ServerWebExchange"))
				.get();
	}

	private class DefaultRendering implements Rendering {

		@Override
		public String name() {
			return name;
		}

		@Override
		public Map<String, Object> model() {
			return model;
		}
	}
}
