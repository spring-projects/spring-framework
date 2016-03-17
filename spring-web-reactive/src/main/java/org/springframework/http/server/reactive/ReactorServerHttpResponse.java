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

import java.time.Duration;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.netty.http.HttpChannel;
import reactor.io.netty.http.model.Cookie;
import reactor.io.netty.http.model.Status;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie cookie = new ReactorCookie(httpCookie);
				this.channel.addResponseCookie(name, cookie);
			}
		}
	}


	/**
	 * At present Reactor does not provide a {@link Cookie} implementation.
	 */
	private final static class ReactorCookie extends Cookie {

		private final ResponseCookie httpCookie;


		public ReactorCookie(ResponseCookie httpCookie) {
			this.httpCookie = httpCookie;
		}

		@Override
		public String name() {
			return this.httpCookie.getName();
		}

		@Override
		public String value() {
			return this.httpCookie.getValue();
		}

		@Override
		public boolean httpOnly() {
			return this.httpCookie.isHttpOnly();
		}

		@Override
		public long maxAge() {
			Duration maxAge = this.httpCookie.getMaxAge();
			return (!maxAge.isNegative() ?  maxAge.getSeconds() : -1);
		}

		@Override
		public String domain() {
			Optional<String> domain = this.httpCookie.getDomain();
			return (domain.isPresent() ? domain.get() : null);
		}

		@Override
		public String path() {
			Optional<String> path = this.httpCookie.getPath();
			return (path.isPresent() ? path.get() : null);
		}

		@Override
		public boolean secure() {
			return this.httpCookie.isSecure();
		}
	}
}
