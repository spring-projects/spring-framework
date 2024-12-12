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

import java.util.List;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.support.HttpCookieParser;
import org.springframework.http.support.JettyHeadersAdapter;
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

	public JettyClientHttpResponse(ReactiveResponse reactiveResponse, Flux<DataBuffer> content, HttpCookieParser httpCookieParser) {

		super(HttpStatusCode.valueOf(reactiveResponse.getStatus()),
				adaptHeaders(reactiveResponse),
				adaptCookies(reactiveResponse, httpCookieParser),
				content);
	}

	private static HttpHeaders adaptHeaders(ReactiveResponse response) {
		MultiValueMap<String, String> headers = new JettyHeadersAdapter(response.getHeaders());
		return HttpHeaders.readOnlyHttpHeaders(headers);
	}
	private static MultiValueMap<String, ResponseCookie> adaptCookies(ReactiveResponse response, HttpCookieParser httpCookieParser) {
		List<HttpField> cookieHeaders = response.getHeaders().getFields(HttpHeaders.SET_COOKIE);
		MultiValueMap<String, ResponseCookie> result = cookieHeaders.stream()
				.flatMap(header -> httpCookieParser.parse(header.getValue()))
				.collect(LinkedMultiValueMap::new,
						(cookies, cookie) -> cookies.add(cookie.getName(), cookie),
						LinkedMultiValueMap::addAll);
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

}
