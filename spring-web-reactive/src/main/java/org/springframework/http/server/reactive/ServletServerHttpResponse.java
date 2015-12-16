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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;
import reactor.Flux;
import reactor.Mono;

import org.springframework.http.ExtendedHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpServletResponse}.
 *
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

	private final HttpServletResponse response;

	private final Function<Publisher<ByteBuffer>, Mono<Void>> responseBodyWriter;

	private final HttpHeaders headers;


	public ServletServerHttpResponse(HttpServletResponse response,
			Function<Publisher<ByteBuffer>, Mono<Void>> responseBodyWriter) {

		Assert.notNull(response, "'response' must not be null");
		Assert.notNull(responseBodyWriter, "'responseBodyWriter' must not be null");
		this.response = response;
		this.responseBodyWriter = responseBodyWriter;
		this.headers = new ExtendedHttpHeaders(new ServletHeaderChangeListener());
	}


	public HttpServletResponse getServletResponse() {
		return this.response;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		getServletResponse().setStatus(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public Mono<Void> setBody(final Publisher<ByteBuffer> publisher) {
		return Flux.from(publisher).lift(new WriteWithOperator<>(this::setBodyInternal)).after();
	}

	protected Mono<Void> setBodyInternal(Publisher<ByteBuffer> publisher) {
		return this.responseBodyWriter.apply(publisher);
	}


	private class ServletHeaderChangeListener implements ExtendedHttpHeaders.HeaderChangeListener {

		@Override
		public void headerAdded(String name, String value) {
			getServletResponse().addHeader(name, value);
		}

		@Override
		public void headerPut(String key, List<String> values) {
			// We can only add but not remove
			for (String value : values) {
				getServletResponse().addHeader(key, value);
			}
		}

		@Override
		public void headerRemoved(String key) {
			// No Servlet support for removing headers
		}
	}

}
