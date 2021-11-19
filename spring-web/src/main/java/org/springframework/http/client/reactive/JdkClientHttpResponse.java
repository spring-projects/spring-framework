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
import java.util.Locale;
import java.util.Map;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @since 6.0
 */
class JdkClientHttpResponse implements ClientHttpResponse {

	private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");


	private final HttpResponse<Flow.Publisher<List<ByteBuffer>>> response;

	private final DataBufferFactory bufferFactory;

	private final HttpHeaders headers;


	public JdkClientHttpResponse(
			HttpResponse<Flow.Publisher<List<ByteBuffer>>> response, DataBufferFactory bufferFactory) {

		this.response = response;
		this.bufferFactory = bufferFactory;
		this.headers = adaptHeaders(response);
	}

	private static HttpHeaders adaptHeaders(HttpResponse<Flow.Publisher<List<ByteBuffer>>> response) {
		Map<String, List<String>> rawHeaders = response.headers().map();
		Map<String, List<String>> map = new LinkedCaseInsensitiveMap<>(rawHeaders.size(), Locale.ENGLISH);
		MultiValueMap<String, String> multiValueMap = CollectionUtils.toMultiValueMap(map);
		multiValueMap.putAll(rawHeaders);
		return HttpHeaders.readOnlyHttpHeaders(multiValueMap);
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
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.response.headers().allValues(HttpHeaders.SET_COOKIE).stream()
				.flatMap(header -> {
					Matcher matcher = SAME_SITE_PATTERN.matcher(header);
					String sameSite = (matcher.matches() ? matcher.group(1) : null);
					return HttpCookie.parse(header).stream().map(cookie -> toResponseCookie(cookie, sameSite));
				})
				.collect(LinkedMultiValueMap::new,
						(cookies, cookie) -> cookies.add(cookie.getName(), cookie),
						LinkedMultiValueMap::addAll);
	}

	private ResponseCookie toResponseCookie(HttpCookie cookie, @Nullable String sameSite) {
		return ResponseCookie.from(cookie.getName(), cookie.getValue())
				.domain(cookie.getDomain())
				.httpOnly(cookie.isHttpOnly())
				.maxAge(cookie.getMaxAge())
				.path(cookie.getPath())
				.secure(cookie.getSecure())
				.sameSite(sameSite)
				.build();
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return JdkFlowAdapter.flowPublisherToFlux(this.response.body())
				.flatMapIterable(Function.identity())
				.map(this.bufferFactory::wrap)
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
	}

}
