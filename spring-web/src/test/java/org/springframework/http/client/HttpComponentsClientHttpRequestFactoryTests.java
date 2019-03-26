/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.client;

import java.net.URI;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Stephane Nicoll
 */
public class HttpComponentsClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTestCase {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory();
	}

	@Override
	@Test
	public void httpMethods() throws Exception {
		super.httpMethods();
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
	public void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		when(configurable.getConfig()).thenReturn(defaultConfig);

		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
		assertSame("Default client configuration is expected", defaultConfig, retrieveRequestConfig(hrf));

		hrf.setConnectionRequestTimeout(4567);
		RequestConfig requestConfig = retrieveRequestConfig(hrf);
		assertNotNull(requestConfig);
		assertEquals(4567, requestConfig.getConnectionRequestTimeout());
		// Default connection timeout merged
		assertEquals(1234, requestConfig.getConnectTimeout());
	}

	@Test
	public void localSettingsOverrideClientDefaultSettings() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).setConnectionRequestTimeout(6789).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		when(configurable.getConfig()).thenReturn(defaultConfig);

		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
		hrf.setConnectTimeout(5000);

		RequestConfig requestConfig = retrieveRequestConfig(hrf);
		assertEquals(5000, requestConfig.getConnectTimeout());
		assertEquals(6789, requestConfig.getConnectionRequestTimeout());
		assertEquals(-1, requestConfig.getSocketTimeout());
	}

	@Test
	public void mergeBasedOnCurrentHttpClient() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setSocketTimeout(1234).build();
		final CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		when(configurable.getConfig()).thenReturn(defaultConfig);

		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory() {
			@Override
			public HttpClient getHttpClient() {
				return client;
			}
		};
		hrf.setReadTimeout(5000);

		RequestConfig requestConfig = retrieveRequestConfig(hrf);
		assertEquals(-1, requestConfig.getConnectTimeout());
		assertEquals(-1, requestConfig.getConnectionRequestTimeout());
		assertEquals(5000, requestConfig.getSocketTimeout());

		// Update the Http client so that it returns an updated  config
		RequestConfig updatedDefaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).build();
		when(configurable.getConfig()).thenReturn(updatedDefaultConfig);
		hrf.setReadTimeout(7000);
		RequestConfig requestConfig2 = retrieveRequestConfig(hrf);
		assertEquals(1234, requestConfig2.getConnectTimeout());
		assertEquals(-1, requestConfig2.getConnectionRequestTimeout());
		assertEquals(7000, requestConfig2.getSocketTimeout());
	}

	private RequestConfig retrieveRequestConfig(HttpComponentsClientHttpRequestFactory factory) throws Exception {
		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
				factory.createRequest(uri, HttpMethod.GET);
		return (RequestConfig) request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
	}

	@Test
	public void createHttpUriRequest() throws Exception {
		URI uri = new URI("https://example.com");
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
