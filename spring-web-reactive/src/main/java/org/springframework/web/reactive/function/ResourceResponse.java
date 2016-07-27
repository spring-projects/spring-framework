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

import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 */
class ResourceResponse extends AbstractResponse<Resource> {

	private static final ResolvableType RESOURCE_TYPE = ResolvableType.forClass(Resource.class);

	private final ResourceHttpMessageWriter messageWriter = new ResourceHttpMessageWriter();

	private final Resource resource;

	public ResourceResponse(int statusCode, HttpHeaders httpHeaders, Resource resource) {
		super(statusCode, httpHeaders);
		this.resource = resource;
	}

	@Override
	public Resource body() {
		return this.resource;
	}

	@Override
	public Mono<Void> writeTo(ServerWebExchange exchange) {
		writeStatusAndHeaders(exchange);
		return this.messageWriter
				.write(Mono.just(this.resource), RESOURCE_TYPE, null, exchange.getResponse());
	}

}
