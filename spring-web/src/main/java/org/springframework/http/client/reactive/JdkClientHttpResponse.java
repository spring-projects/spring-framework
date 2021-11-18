/*
 * Copyright 2002-2021 the original author or authors.
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
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Function;
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
 * {@link ClientHttpResponse} implementation for Java's {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @since 6.0
 */
class JdkClientHttpResponse implements ClientHttpResponse {

	private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");


	private final HttpResponse<Flow.Publisher<List<ByteBuffer>>> response;

	private final DataBufferFactory bufferFactory;


	public JdkClientHttpResponse(
			HttpResponse<Flow.Publisher<List<ByteBuffer>>> response, DataBufferFactory bufferFactory) {

		this.response = response;
		this.bufferFactory = bufferFactory;
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.response.statusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.response.statusCode();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.response.headers().map().entrySet().stream()
				.collect(HttpHeaders::new,
						(headers, entry) -> headers.addAll(entry.getKey(), entry.getValue()),
						HttpHeaders::addAll);
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.response.headers().allValues(HttpHeaders.SET_COOKIE).stream()
				.flatMap(header ->
						HttpCookie.parse(header).stream().map(cookie ->
								ResponseCookie.from(cookie.getName(), cookie.getValue())
										.domain(cookie.getDomain())
										.httpOnly(cookie.isHttpOnly())
										.maxAge(cookie.getMaxAge())
										.path(cookie.getPath())
										.secure(cookie.getSecure())
										.sameSite(parseSameSite(header))
										.build()))
				.collect(LinkedMultiValueMap::new,
						(valueMap, cookie) -> valueMap.add(cookie.getName(), cookie),
						LinkedMultiValueMap::addAll);
	}

	@Nullable
	private static String parseSameSite(String headerValue) {
		Matcher matcher = SAME_SITE_PATTERN.matcher(headerValue);
		return (matcher.matches() ? matcher.group(1) : null);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return JdkFlowAdapter.flowPublisherToFlux(this.response.body())
				.flatMapIterable(Function.identity())
				.map(this.bufferFactory::wrap)
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
	}

}
