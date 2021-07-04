/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.net.HttpCookie;
import java.util.List;

import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 */
class JettyClientHttpResponse implements ClientHttpResponse {

	private final ReactiveResponse reactiveResponse;

	private final Flux<DataBuffer> content;


	public JettyClientHttpResponse(ReactiveResponse reactiveResponse, Publisher<DataBuffer> content) {
		this.reactiveResponse = reactiveResponse;
		this.content = Flux.from(content);
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(getRawStatusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.reactiveResponse.getStatus();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		List<String> cookieHeader = getHeaders().get(HttpHeaders.SET_COOKIE);
		if (cookieHeader != null) {
			cookieHeader.forEach(header -> HttpCookie.parse(header)
					.forEach(c -> result.add(c.getName(), ResponseCookie.fromClientResponse(c.getName(), c.getValue())
							.domain(c.getDomain())
							.path(c.getPath())
							.maxAge(c.getMaxAge())
							.secure(c.getSecure())
							.httpOnly(c.isHttpOnly())
							.build()))
			);
		}
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.content;
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		this.reactiveResponse.getHeaders().stream()
				.forEach(field -> headers.add(field.getName(), field.getValue()));
		return headers;
	}

}
