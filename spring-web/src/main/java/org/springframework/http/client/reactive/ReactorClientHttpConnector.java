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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.function.Function;

import org.springframework.http.HttpMethod;

import reactor.core.publisher.Mono;

/**
 * Reactor-Netty implementation of {@link ClientHttpConnector}
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class ReactorClientHttpConnector implements ClientHttpConnector {

	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		return reactor.io.netty.http.HttpClient.create(uri.getHost(), uri.getPort())
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()),
						uri.toString(),
						httpOutbound -> requestCallback.apply(new ReactorClientHttpRequest(method, uri, httpOutbound)))
				.map(httpInbound -> new ReactorClientHttpResponse(httpInbound));
	}
}