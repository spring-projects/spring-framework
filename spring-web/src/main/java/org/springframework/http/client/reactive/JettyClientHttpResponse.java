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

import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ClientHttpResponse} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">
 *     Jetty ReactiveStreams HttpClient</a>
 */
class JettyClientHttpResponse implements ClientHttpResponse {

	private static final Pattern SAMESITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

	private static final ClassLoader loader = JettyClientHttpResponse.class.getClassLoader();

	private static final boolean jetty10Present = ClassUtils.isPresent(
			"org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer", loader);


	private final ReactiveResponse reactiveResponse;

	private final Flux<DataBuffer> content;

	private final HttpHeaders headers;


	public JettyClientHttpResponse(ReactiveResponse reactiveResponse, Publisher<DataBuffer> content) {
		this.reactiveResponse = reactiveResponse;
		this.content = Flux.from(content);

		MultiValueMap<String, String> headers = (jetty10Present ?
				Jetty10HttpFieldsHelper.getHttpHeaders(reactiveResponse) :
				new JettyHeadersAdapter(reactiveResponse.getHeaders()));

		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(getRawStatusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.reactiveResponse.getStatus();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		List<String> cookieHeader = getHeaders().get(HttpHeaders.SET_COOKIE);
		if (cookieHeader != null) {
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


	@Override
	public Flux<DataBuffer> getBody() {
		return this.content;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}


	private static class Jetty10HttpFieldsHelper {

		private static final Method getHeadersMethod;

		private static final Method getNameMethod;

		private static final Method getValueMethod;

		static {
			try {
				getHeadersMethod = Response.class.getMethod("getHeaders");
				Class<?> type = loader.loadClass("org.eclipse.jetty.http.HttpField");
				getNameMethod = type.getMethod("getName");
				getValueMethod = type.getMethod("getValue");
			}
			catch (ClassNotFoundException | NoSuchMethodException ex) {
				throw new IllegalStateException("No compatible Jetty version found", ex);
			}
		}

		public static HttpHeaders getHttpHeaders(ReactiveResponse response) {
			HttpHeaders headers = new HttpHeaders();
			Iterable<?> iterator = (Iterable<?>)
					ReflectionUtils.invokeMethod(getHeadersMethod, response.getResponse());
			Assert.notNull(iterator, "Iterator must not be null");
			for (Object field : iterator) {
				String name = (String) ReflectionUtils.invokeMethod(getNameMethod, field);
				Assert.notNull(name, "Header name must not be null");
				String value = (String) ReflectionUtils.invokeMethod(getValueMethod, field);
				headers.add(name, value);
			}
			return headers;
		}
	}

}
