/*
 * Copyright 2002-2025 the original author or authors.
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

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * {@link HttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link RestClient} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@link HttpServiceProxyFactory} configured with the given {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
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
	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
		return newRequest(values).retrieve().toBodilessEntity().getHeaders();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		if (bodyType.getType().equals(InputStream.class)) {
			return (T) newRequest(values).exchange((request, response) -> response.getBody(), false);
		}
		return newRequest(values).retrieve().body(bodyType);
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		return newRequest(values).retrieve().toBodilessEntity();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		if (bodyType.getType().equals(InputStream.class)) {
			return (ResponseEntity<T>) newRequest(values).exchangeForRequiredValue((request, response) ->
					ResponseEntity.status(response.getStatusCode())
							.headers(response.getHeaders())
							.body(response.getBody()), false);
		}
		return newRequest(values).retrieve().toEntity(bodyType);
	}

	@SuppressWarnings("unchecked")
	private <B> RestClient.RequestBodySpec newRequest(HttpRequestValues values) {

		HttpMethod httpMethod = values.getHttpMethod();
		Assert.notNull(httpMethod, "HttpMethod is required");

		RestClient.RequestBodyUriSpec uriSpec = this.restClient.method(httpMethod);

		RestClient.RequestBodySpec bodySpec;
		if (values.getUri() != null) {
			bodySpec = uriSpec.uri(values.getUri());
		}
		else if (values.getUriTemplate() != null) {
			UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
			if (uriBuilderFactory != null) {
				URI uri = uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables());
				bodySpec = uriSpec.uri(uri);
			}
			else {
				bodySpec = uriSpec.uri(values.getUriTemplate(), values.getUriVariables());
			}
		}
		else {
			throw new IllegalStateException("Neither full URL nor URI template");
		}

		bodySpec.headers(headers -> headers.putAll(values.getHeaders()));

		if (!values.getCookies().isEmpty()) {
			List<String> cookies = new ArrayList<>();
			values.getCookies().forEach((name, cookieValues) -> cookieValues.forEach(value -> {
				HttpCookie cookie = new HttpCookie(name, value);
				cookies.add(cookie.toString());
			}));
			bodySpec.header(HttpHeaders.COOKIE, String.join("; ", cookies));
		}

		if (values.getApiVersion() != null) {
			bodySpec.apiVersion(values.getApiVersion());
		}

		bodySpec.attributes(attributes -> attributes.putAll(values.getAttributes()));

		B body = (B) values.getBodyValue();
		if (body != null) {
			if (body instanceof StreamingHttpOutputMessage.Body streamingBody) {
				bodySpec.body(streamingBody);
			}
			else if (values.getBodyValueType() != null) {
				bodySpec.body(body, (ParameterizedTypeReference<? super B>) values.getBodyValueType());
			}
			else {
				bodySpec.body(body);
			}
		}

		return bodySpec;
	}


	/**
	 * Create a {@link RestClientAdapter} for the given {@link RestClient}.
	 */
	public static RestClientAdapter create(RestClient restClient) {
		return new RestClientAdapter(restClient);
	}

}
