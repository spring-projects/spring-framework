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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderValues;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpRequest} to the Underow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServerExchange exchange;

	private final Flux<ByteBuffer> body;


	public UndertowServerHttpRequest(HttpServerExchange exchange, Publisher<ByteBuffer> body) {
		Assert.notNull(exchange, "'exchange' is required.");
		Assert.notNull(exchange, "'body' is required.");
		this.exchange = exchange;
		this.body = Flux.from(body);
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.getUndertowExchange().getRequestMethod().toString());
	}

	@Override
	protected URI initUri() throws URISyntaxException {
		return new URI(this.exchange.getRequestScheme(), null,
				this.exchange.getHostName(), this.exchange.getHostPort(),
				this.exchange.getRequestURI(), this.exchange.getQueryString(), null);
	}

	@Override
	protected void initHeaders(HttpHeaders headers) {
		for (HeaderValues values : this.getUndertowExchange().getRequestHeaders()) {
			headers.put(values.getHeaderName().toString(), values);
		}
	}

	@Override
	protected void initCookies(Map<String, List<HttpCookie>> map) {
		for (String name : this.exchange.getRequestCookies().keySet()) {
			List<HttpCookie> list = map.get(name);
			if (list == null) {
				list = new ArrayList<>();
				map.put(name, list);
			}
			Cookie cookie = this.exchange.getRequestCookies().get(name);
			list.add(HttpCookie.clientCookie(name, cookie.getValue()));
		}
	}

	@Override
	public Flux<ByteBuffer> getBody() {
		return this.body;
	}

}
