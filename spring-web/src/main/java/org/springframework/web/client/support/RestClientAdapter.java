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

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link HttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory} to use
 * {@link RestClient} for request execution.
 *
 * <p>
 * Use static factory methods in this class to create an {@link HttpServiceProxyFactory}
 * configured with a given {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 6.1
 */
public final class RestClientAdapter implements HttpExchangeAdapter {

	private final RestClient restClient;

	private RestClientAdapter(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public boolean supportsRequestAttributes() {
		return true;
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		newRequest(requestValues).retrieve().toBodilessEntity();
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
		return newRequest(requestValues).retrieve().toBodilessEntity().getHeaders();
	}

	@Override
	public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return newRequest(requestValues).retrieve().body(bodyType);
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
		return newRequest(requestValues).retrieve().toBodilessEntity();
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues,
			ParameterizedTypeReference<T> bodyType) {
		return newRequest(requestValues).retrieve().toEntity(bodyType);
	}

	private RestClient.RequestBodySpec newRequest(HttpRequestValues requestValues) {

		HttpMethod httpMethod = requestValues.getHttpMethod();
		Assert.notNull(httpMethod, "HttpMethod is required");

		RestClient.RequestBodyUriSpec uriSpec = this.restClient.method(httpMethod);

		RestClient.RequestBodySpec bodySpec;
		if (requestValues.getUri() != null) {
			bodySpec = uriSpec.uri(requestValues.getUri());
		}
		else if (requestValues.getUriTemplate() != null) {
			bodySpec = uriSpec.uri(requestValues.getUriTemplate(), requestValues.getUriVariables());
		}
		else {
			throw new IllegalStateException("Neither full URL nor URI template");
		}

		bodySpec.headers(headers -> headers.putAll(requestValues.getHeaders()));

		if (!requestValues.getCookies().isEmpty()) {
			List<String> cookies = new ArrayList<>();
			requestValues.getCookies().forEach((name, values) -> values.forEach(value -> {
				HttpCookie cookie = new HttpCookie(name, value);
				cookies.add(cookie.toString());
			}));
			bodySpec.header(HttpHeaders.COOKIE, String.join("; ", cookies));
		}

		bodySpec.attributes(attributes -> attributes.putAll(requestValues.getAttributes()));

		if (requestValues.getBodyValue() != null) {
			bodySpec.body(requestValues.getBodyValue());
		}

		return bodySpec;
	}

	/**
	 * Create a {@link RestClientAdapter} with the given {@link RestClient}.
	 */
	public static RestClientAdapter create(RestClient restClient) {
		return new RestClientAdapter(restClient);
	}

}
