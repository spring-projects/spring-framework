/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.client.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.Assert;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * {@link HttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link RestTestClient} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@link HttpServiceProxyFactory} configured with the given {@link RestTestClient}.
 *
 * @author Devendra Reddy Pennabadi
 * @since 7.0
 */
public final class RestTestClientAdapter implements HttpExchangeAdapter {

	private final RestTestClient restTestClient;


	private RestTestClientAdapter(RestTestClient restTestClient) {
		this.restTestClient = restTestClient;
	}


	@Override
	public boolean supportsRequestAttributes() {
		return true;
	}

	@Override
	public void exchange(HttpRequestValues values) {
		newRequest(values).exchange().returnResult();
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
		return newRequest(values).exchange().returnResult().getResponseHeaders();
	}

	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return newRequest(values).exchange().expectBody(bodyType).returnResult().getResponseBody();
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		ExchangeResult result = newRequest(values).exchange().returnResult();
		return ResponseEntity.status(result.getStatus())
				.headers(result.getResponseHeaders())
				.build();
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		EntityExchangeResult<T> result = newRequest(values).exchange().expectBody(bodyType).returnResult();
		return ResponseEntity.status(result.getStatus())
				.headers(result.getResponseHeaders())
				.body(result.getResponseBody());
	}

	private RestTestClient.RequestBodySpec newRequest(HttpRequestValues values) {
		HttpMethod method = values.getHttpMethod();
		Assert.notNull(method, "HttpMethod is required");

		RestTestClient.RequestBodyUriSpec uriSpec = this.restTestClient.method(method);
		RestTestClient.RequestBodySpec spec = setUri(uriSpec, values);

		spec.headers(headers -> headers.putAll(values.getHeaders()));
		setCookieHeader(spec, values);
		if (values.getApiVersion() != null) {
			spec.apiVersion(values.getApiVersion());
		}
		spec.attributes(attributes -> attributes.putAll(values.getAttributes()));
		if (values.getBodyValue() != null) {
			spec.body(values.getBodyValue());
		}
		return spec;
	}

	private static RestTestClient.RequestBodySpec setUri(
			RestTestClient.RequestBodyUriSpec spec, HttpRequestValues values) {

		if (values.getUri() != null) {
			return spec.uri(values.getUri());
		}
		if (values.getUriTemplate() != null) {
			UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
			if (uriBuilderFactory != null) {
				URI uri = uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables());
				return spec.uri(uri);
			}
			return spec.uri(values.getUriTemplate(), values.getUriVariables());
		}
		throw new IllegalStateException("Neither full URL nor URI template");
	}

	private static void setCookieHeader(RestTestClient.RequestBodySpec spec, HttpRequestValues values) {
		if (!values.getCookies().isEmpty()) {
			List<String> cookies = new ArrayList<>();
			values.getCookies().forEach((name, cookieValues) -> cookieValues.forEach(value -> {
				HttpCookie cookie = new HttpCookie(name, value);
				cookies.add(cookie.toString());
			}));
			spec.header(HttpHeaders.COOKIE, String.join("; ", cookies));
		}
	}


	/**
	 * Create a {@link RestTestClientAdapter} for the given {@link RestTestClient}.
	 * @param restTestClient the test client to use
	 * @return the created adapter instance
	 */
	public static RestTestClientAdapter create(RestTestClient restTestClient) {
		return new RestTestClientAdapter(restTestClient);
	}

}
