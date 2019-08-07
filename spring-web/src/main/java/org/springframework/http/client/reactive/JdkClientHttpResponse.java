/*
 * Copyright 2002-2019 the original author or authors.
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
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Java 11 HTTP client.
 *
 * @author Julien Eyraud
 * @since 5.2
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html">Java HttpClient</a>
 */
class JdkClientHttpResponse implements ClientHttpResponse {
	private static final Pattern SAMESITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

	private final HttpResponse<Flow.Publisher<List<ByteBuffer>>> response;

	private final DataBufferFactory bufferFactory;


	public JdkClientHttpResponse(final HttpResponse<Flow.Publisher<List<ByteBuffer>>> response, final DataBufferFactory bufferFactory) {
		this.response = response;
		this.bufferFactory = bufferFactory;
	}

	@Nullable
	private static String parseSameSite(String headerValue) {
		Matcher matcher = SAMESITE_PATTERN.matcher(headerValue);
		return (matcher.matches() ? matcher.group(1) : null);
	}

	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.resolve(this.response.statusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.response.statusCode();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.response.headers().allValues(HttpHeaders.SET_COOKIE).stream()
				.flatMap(header ->
						HttpCookie.parse(header).stream().map(httpCookie ->
								ResponseCookie
										.from(httpCookie.getName(), httpCookie.getValue())
										.domain(httpCookie.getDomain())
										.httpOnly(httpCookie.isHttpOnly())
										.maxAge(httpCookie.getMaxAge())
										.path(httpCookie.getPath())
										.secure(httpCookie.getSecure())
										.sameSite(parseSameSite(header))
										.build()
						)
				).collect(LinkedMultiValueMap::new, (m, v) -> m.add(v.getName(), v), LinkedMultiValueMap::addAll);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return JdkFlowAdapter
				.flowPublisherToFlux(this.response.body())
				.flatMap(Flux::fromIterable)
				.map(this.bufferFactory::wrap)
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.response.headers().map().entrySet().stream().collect(HttpHeaders::new, (headers, entry) -> headers.addAll(entry.getKey(), entry.getValue()), HttpHeaders::addAll);
	}
}
