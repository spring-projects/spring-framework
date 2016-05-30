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

package org.springframework.http.server.reactive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.HttpString;
import org.reactivestreams.Publisher;
import org.xnio.channels.StreamSinkChannel;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowServerHttpResponse extends AbstractServerHttpResponse
		implements ZeroCopyHttpOutputMessage {

	private final HttpServerExchange exchange;

	private final StreamSinkChannel responseChannel;

	private final Function<Publisher<DataBuffer>, Mono<Void>> responseBodyWriter;

	public UndertowServerHttpResponse(HttpServerExchange exchange,
			StreamSinkChannel responseChannel,
			Function<Publisher<DataBuffer>, Mono<Void>> responseBodyWriter,
			DataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
		Assert.notNull(exchange, "'exchange' is required.");
		Assert.notNull(responseChannel, "'responseChannel' must not be null");
		Assert.notNull(responseBodyWriter, "'responseBodyWriter' must not be null");
		this.exchange = exchange;
		this.responseChannel = responseChannel;
		this.responseBodyWriter = responseBodyWriter;
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		Assert.notNull(status);
		getUndertowExchange().setStatusCode(status.value());
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<DataBuffer> publisher) {
		return this.responseBodyWriter.apply(publisher);
	}

	@Override
	public Mono<Void> writeWith(File file, long position, long count) {
		writeHeaders();
		writeCookies();
		try {
			FileChannel in = new FileInputStream(file).getChannel();
			long result = this.responseChannel.transferFrom(in, position, count);
			if (result < count) {
				return Mono.error(new IOException("Could only write " + result +
						" out of " + count + " bytes"));
			}
			else {
				return Mono.empty();
			}
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
	}

	@Override
	protected void writeHeaders() {
		for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
			HttpString headerName = HttpString.tryFromString(entry.getKey());
			this.exchange.getResponseHeaders().addAll(headerName, entry.getValue());
		}
	}

	@Override
	protected void writeCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie cookie = new CookieImpl(name, httpCookie.getValue());
				if (!httpCookie.getMaxAge().isNegative()) {
					cookie.setMaxAge((int) httpCookie.getMaxAge().getSeconds());
				}
				httpCookie.getDomain().ifPresent(cookie::setDomain);
				httpCookie.getPath().ifPresent(cookie::setPath);
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.exchange.getResponseCookies().putIfAbsent(name, cookie);
			}
		}
	}

}
