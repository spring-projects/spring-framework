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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;
import reactor.io.net.http.model.Cookie;
import reactor.io.net.http.model.Status;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Reactor Net {@link HttpChannel}.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 */
public class ReactorServerHttpResponse extends AbstractServerHttpResponse {

	private final HttpChannel<?, Buffer> channel;


	public ReactorServerHttpResponse(HttpChannel<?, Buffer> response) {
		Assert.notNull("'response' must not be null.");
		this.channel = response;
	}


	public HttpChannel<?, Buffer> getReactorChannel() {
		return this.channel;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		getReactorChannel().responseStatus(Status.valueOf(status.value()));
	}

	@Override
	protected Mono<Void> setBodyInternal(Publisher<DataBuffer> publisher) {
		return Mono.from(this.channel.writeWith(
				Flux.from(publisher).map(buffer -> new Buffer(buffer.asByteBuffer()))));
	}

	@Override
	protected void writeHeaders() {
		for (String name : getHeaders().keySet()) {
			for (String value : getHeaders().get(name)) {
				this.channel.responseHeaders().add(name, value);
			}
		}
	}

	@Override
	protected void writeCookies() {
		for (String name : getHeaders().getCookies().keySet()) {
			for (HttpCookie httpCookie : getHeaders().getCookies().get(name)) {
				Cookie cookie = new ReactorCookie(name, httpCookie);
				this.channel.addResponseCookie(name, cookie);
			}
		}
	}

	private final static class ReactorCookie extends Cookie {

		final HttpCookie httpCookie;
		final String name;

		public ReactorCookie(String name, HttpCookie httpCookie) {
			this.name = name;
			this.httpCookie = httpCookie;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String value() {
			return httpCookie.getValue();
		}

		@Override
		public boolean httpOnly() {
			return httpCookie.isHttpOnly();
		}

		@Override
		public long maxAge() {
			return httpCookie.getMaxAge() > -1 ? httpCookie.getMaxAge() : -1;
		}

		@Override
		public String domain() {
			return httpCookie.getDomain();
		}

		@Override
		public String path() {
			return httpCookie.getPath();
		}

		@Override
		public boolean secure() {
			return httpCookie.isSecure();
		}
	}
}
