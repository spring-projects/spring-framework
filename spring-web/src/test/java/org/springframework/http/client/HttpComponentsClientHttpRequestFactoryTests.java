/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.client;

import java.net.URI;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import static org.junit.Assert.*;

public class HttpComponentsClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTestCase {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory();
	}

	@Override
	@Test
	public void httpMethods() throws Exception {
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void assertLegacyCustomConfig() {
		HttpClient httpClient = new org.apache.http.impl.client.DefaultHttpClient(); // Does not support RequestConfig
		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(httpClient);
		hrf.setConnectTimeout(1234);
		assertEquals(1234, httpClient.getParams().getIntParameter(
				org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT, 0));

		hrf.setReadTimeout(4567);
		assertEquals(4567, httpClient.getParams().getIntParameter(
				org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT, 0));
	}

	@Test
	public void assertCustomConfig() throws Exception {
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(httpClient);
		hrf.setConnectTimeout(1234);
		hrf.setConnectionRequestTimeout(4321);
		hrf.setReadTimeout(4567);

		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
				hrf.createRequest(uri, HttpMethod.GET);

		Object config = request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
		assertNotNull("Request config should be set", config);
		assertTrue("Wrong request config type" + config.getClass().getName(),
				RequestConfig.class.isInstance(config));
		RequestConfig requestConfig = (RequestConfig) config;
		assertEquals("Wrong custom connection timeout", 1234, requestConfig.getConnectTimeout());
		assertEquals("Wrong custom connection request timeout", 4321, requestConfig.getConnectionRequestTimeout());
		assertEquals("Wrong custom socket timeout", 4567, requestConfig.getSocketTimeout());
	}

	@Test
	public void customHttpClientUsesItsDefault() throws Exception {
		HttpComponentsClientHttpRequestFactory hrf =
				new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build());

		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
				hrf.createRequest(uri, HttpMethod.GET);

		assertNull("No custom config should be set with a custom HttpClient",
				request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG));
	}

	@Test
	public void defaultSettingsOfHttpClientLostOnExecutorCustomization() throws Exception {
		CloseableHttpClient client = HttpClientBuilder.create()
				.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(1234).build())
				.build();
		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);

		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
				hrf.createRequest(uri, HttpMethod.GET);

		assertNull("No custom config should be set with a custom HttpClient",
				request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG));

		hrf.setConnectionRequestTimeout(4567);
		HttpComponentsClientHttpRequest request2 = (HttpComponentsClientHttpRequest)
				hrf.createRequest(uri, HttpMethod.GET);
		Object requestConfigAttribute = request2.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
		assertNotNull(requestConfigAttribute);
		RequestConfig requestConfig = (RequestConfig) requestConfigAttribute;

		assertEquals(4567, requestConfig.getConnectionRequestTimeout());
		// No way to access the request config of the HTTP client so no way to "merge" our customizations
		assertEquals(-1, requestConfig.getConnectTimeout());
	}

	@Test
	public void createHttpUriRequest() throws Exception {
		URI uri = new URI("http://example.com");
		testRequestBodyAllowed(uri, HttpMethod.GET, false);
		testRequestBodyAllowed(uri, HttpMethod.HEAD, false);
		testRequestBodyAllowed(uri, HttpMethod.OPTIONS, false);
		testRequestBodyAllowed(uri, HttpMethod.TRACE, false);
		testRequestBodyAllowed(uri, HttpMethod.PUT, true);
		testRequestBodyAllowed(uri, HttpMethod.POST, true);
		testRequestBodyAllowed(uri, HttpMethod.PATCH, true);
		testRequestBodyAllowed(uri, HttpMethod.DELETE, true);

	}

	private void testRequestBodyAllowed(URI uri, HttpMethod method, boolean allowed) {
		HttpUriRequest request = ((HttpComponentsClientHttpRequestFactory) this.factory).createHttpUriRequest(method, uri);
		assertEquals(allowed, request instanceof HttpEntityEnclosingRequest);
	}

}
