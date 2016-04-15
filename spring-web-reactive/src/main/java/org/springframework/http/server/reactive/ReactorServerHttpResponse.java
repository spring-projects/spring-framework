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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.netty.http.HttpChannel;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
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

	private final HttpChannel channel;

	public ReactorServerHttpResponse(HttpChannel response,
			DataBufferAllocator allocator) {
		super(allocator);
		Assert.notNull("'response' must not be null.");
		this.channel = response;
	}


	public HttpChannel getReactorChannel() {
		return this.channel;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		getReactorChannel().responseStatus(HttpResponseStatus.valueOf(status.value()));
	}

	@Override
	protected Mono<Void> setBodyInternal(Publisher<DataBuffer> publisher) {
		return Mono.from(this.channel.send(
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
				Cookie cookie = new NettyCookie(httpCookie);
				this.channel.addResponseCookie(name, cookie);
			}
		}
	}

	private final static class NettyCookie implements Cookie {

		private final ResponseCookie httpCookie;


		public NettyCookie(ResponseCookie httpCookie) {
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
		public boolean isHttpOnly() {
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
		public void setValue(String value) {

		}

		@Override
		public boolean wrap() {
			return false;
		}

		@Override
		public void setWrap(boolean wrap) {
			throw new UnsupportedOperationException("Read-Only Cookie");
		}

		@Override
		public void setDomain(String domain) {
			throw new UnsupportedOperationException("Read-Only Cookie");
		}

		@Override
		public void setPath(String path) {
			throw new UnsupportedOperationException("Read-Only Cookie");
		}

		@Override
		public void setMaxAge(long maxAge) {
			throw new UnsupportedOperationException("Read-Only Cookie");
		}

		@Override
		public void setSecure(boolean secure) {
			throw new UnsupportedOperationException("Read-Only Cookie");
		}

		@Override
		public void setHttpOnly(boolean httpOnly) {
			throw new UnsupportedOperationException("Read-Only Cookie");
		}

		@Override
		public int compareTo(Cookie o) {
			return httpCookie.getName().compareTo(o.name());
		}

		@Override
		public boolean isSecure() {
			return this.httpCookie.isSecure();
		}
	}
}
