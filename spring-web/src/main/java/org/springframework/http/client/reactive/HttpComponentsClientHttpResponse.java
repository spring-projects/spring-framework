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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Apache HttpComponents HttpClient 5.x.
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @since 5.3
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 */
class HttpComponentsClientHttpResponse implements ClientHttpResponse {

	private final DataBufferFactory dataBufferFactory;

	private final Message<HttpResponse, Publisher<ByteBuffer>> message;

	private final HttpHeaders headers;

	private final HttpClientContext context;

	private final AtomicBoolean rejectSubscribers = new AtomicBoolean();


	public HttpComponentsClientHttpResponse(DataBufferFactory dataBufferFactory,
			Message<HttpResponse, Publisher<ByteBuffer>> message, HttpClientContext context) {

		this.dataBufferFactory = dataBufferFactory;
		this.message = message;
		this.context = context;

		MultiValueMap<String, String> adapter = new HttpComponentsHeadersAdapter(message.getHead());
		this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.message.getHead().getCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.message.getHead().getCode();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		LinkedMultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		this.context.getCookieStore().getCookies().forEach(cookie ->
				result.add(cookie.getName(),
						ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
								.domain(cookie.getDomain())
								.path(cookie.getPath())
								.maxAge(getMaxAgeSeconds(cookie))
								.secure(cookie.isSecure())
								.httpOnly(cookie.containsAttribute("httponly"))
								.sameSite(cookie.getAttribute("samesite"))
								.build()));
		return result;
	}

	private long getMaxAgeSeconds(Cookie cookie) {
		String maxAgeAttribute = cookie.getAttribute(Cookie.MAX_AGE_ATTR);
		return (maxAgeAttribute != null ? Long.parseLong(maxAgeAttribute) : -1);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.message.getBody())
				.doOnSubscribe(s -> {
					if (!this.rejectSubscribers.compareAndSet(false, true)) {
						throw new IllegalStateException("The client response body can only be consumed once.");
					}
				})
				.map(this.dataBufferFactory::wrap);
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
