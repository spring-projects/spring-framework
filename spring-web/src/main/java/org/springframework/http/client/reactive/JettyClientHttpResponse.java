/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.reactive.client.ReactiveResponse;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">
 *     Jetty ReactiveStreams HttpClient</a>
 */
class JettyClientHttpResponse extends AbstractClientHttpResponse {

	private static final Pattern SAMESITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");


	public JettyClientHttpResponse(ReactiveResponse reactiveResponse, Flux<DataBuffer> content) {

		super(reactiveResponse.getStatus(),
				adaptHeaders(reactiveResponse),
				adaptCookies(reactiveResponse),
				content);
	}

	private static HttpHeaders adaptHeaders(ReactiveResponse response) {
		MultiValueMap<String, String> headers = (Jetty10HttpFieldsHelper.jetty10Present() ?
				Jetty10HttpFieldsHelper.getHttpHeaders(response.getResponse()) :
				new JettyHeadersAdapter(response.getHeaders()));
		return HttpHeaders.readOnlyHttpHeaders(headers);
	}

	private static MultiValueMap<String, ResponseCookie> adaptCookies(ReactiveResponse response) {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		MultiValueMap<String, String> headers = adaptHeaders(response);
		List<String> cookieHeader = headers.get(HttpHeaders.SET_COOKIE);
		if (!CollectionUtils.isEmpty(cookieHeader)) {
			cookieHeader.forEach(header ->
					HttpCookie.parse(header).forEach(cookie -> result.add(cookie.getName(),
							ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
									.domain(cookie.getDomain())
									.path(cookie.getPath())
									.maxAge(cookie.getMaxAge())
									.secure(cookie.getSecure())
									.httpOnly(cookie.isHttpOnly())
									.sameSite(parseSameSite(header))
									.build()))
			);
		}
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Nullable
	private static String parseSameSite(String headerValue) {
		Matcher matcher = SAMESITE_PATTERN.matcher(headerValue);
		return (matcher.matches() ? matcher.group(1) : null);
	}

}
