/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.client.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link HttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link RestTemplate} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@link HttpServiceProxyFactory} configured with the given {@link RestTemplate}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 6.1
 */
public final class RestTemplateAdapter implements HttpExchangeAdapter {

	private final RestTemplate restTemplate;


	private RestTemplateAdapter(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}


	@Override
	public boolean supportsRequestAttributes() {
		return false;
	}

	@Override
	public void exchange(HttpRequestValues values) {
		this.restTemplate.exchange(newRequest(values), Void.class);
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
		return this.restTemplate.exchange(newRequest(values), Void.class).getHeaders();
	}

	@Override
	public <T> T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return this.restTemplate.exchange(newRequest(values), bodyType).getBody();
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		return this.restTemplate.exchange(newRequest(values), Void.class);
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return this.restTemplate.exchange(newRequest(values), bodyType);
	}

	private RequestEntity<?> newRequest(HttpRequestValues values) {
		URI uri;
		if (values.getUri() != null) {
			uri = values.getUri();
		}
		else if (values.getUriTemplate() != null) {
			String uriTemplate = values.getUriTemplate();
			Map<String, String> variables = values.getUriVariables();
			uri = this.restTemplate.getUriTemplateHandler().expand(uriTemplate, variables);
		}
		else {
			throw new IllegalStateException("Neither full URL nor URI template");
		}

		HttpMethod httpMethod = values.getHttpMethod();
		Assert.notNull(httpMethod, "HttpMethod is required");

		RequestEntity.BodyBuilder builder = RequestEntity.method(httpMethod, uri);
		builder.headers(values.getHeaders());

		if (!values.getCookies().isEmpty()) {
			List<String> cookies = new ArrayList<>();
			values.getCookies().forEach((name, cookieValues) -> cookieValues.forEach(value -> {
				HttpCookie cookie = new HttpCookie(name, value);
				cookies.add(cookie.toString());
			}));
			builder.header(HttpHeaders.COOKIE, String.join("; ", cookies));
		}

		if (values.getBodyValue() != null) {
			return builder.body(values.getBodyValue());
		}

		return builder.build();
	}


	/**
	 * Create a {@link RestTemplateAdapter} for the given {@link RestTemplate}.
	 */
	public static RestTemplateAdapter create(RestTemplate restTemplate) {
		return new RestTemplateAdapter(restTemplate);
	}

}
