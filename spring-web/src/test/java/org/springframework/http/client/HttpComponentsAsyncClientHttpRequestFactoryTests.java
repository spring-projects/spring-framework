/*
 * Copyright 2002-2016 the original author or authors.
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

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.Test;

import org.springframework.http.HttpMethod;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 */
public class HttpComponentsAsyncClientHttpRequestFactoryTests extends AbstractAsyncHttpRequestFactoryTestCase {

	@Override
	protected AsyncClientHttpRequestFactory createRequestFactory() {
		return new HttpComponentsAsyncClientHttpRequestFactory();
	}


	@Override
	@Test
	public void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

	@Test
	public void customHttpAsyncClientUsesItsDefault() throws Exception {
		HttpComponentsAsyncClientHttpRequestFactory factory =
				new HttpComponentsAsyncClientHttpRequestFactory();

		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsAsyncClientHttpRequestFactory.HttpComponentsAsyncClientHttpRequest
				request = (HttpComponentsAsyncClientHttpRequestFactory.HttpComponentsAsyncClientHttpRequest)
				factory.createAsyncRequest(uri, HttpMethod.GET);

		assertNull("No custom config should be set with a custom HttpAsyncClient",
				request.httpContext.getAttribute(HttpClientContext.REQUEST_CONFIG));
	}

	@Test
	public void defaultSettingsOfHttpAsyncClientLostOnExecutorCustomization() throws Exception {
		CloseableHttpAsyncClient client = HttpAsyncClientBuilder.create()
				.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(1234).build())
				.build();
		HttpComponentsAsyncClientHttpRequestFactory factory = new HttpComponentsAsyncClientHttpRequestFactory(client);

		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsAsyncClientHttpRequestFactory.HttpComponentsAsyncClientHttpRequest
				request = (HttpComponentsAsyncClientHttpRequestFactory.HttpComponentsAsyncClientHttpRequest)
				factory.createAsyncRequest(uri, HttpMethod.GET);

		assertNull("No custom config should be set with a custom HttpClient",
				request.httpContext.getAttribute(HttpClientContext.REQUEST_CONFIG));

		factory.setConnectionRequestTimeout(4567);
		HttpComponentsAsyncClientHttpRequestFactory.HttpComponentsAsyncClientHttpRequest
				request2 = (HttpComponentsAsyncClientHttpRequestFactory.HttpComponentsAsyncClientHttpRequest)
				factory.createAsyncRequest(uri, HttpMethod.GET);
		Object requestConfigAttribute = request2.httpContext.getAttribute(HttpClientContext.REQUEST_CONFIG);
		assertNotNull(requestConfigAttribute);
		RequestConfig requestConfig = (RequestConfig) requestConfigAttribute;

		assertEquals(4567, requestConfig.getConnectionRequestTimeout());
		// No way to access the request config of the HTTP client so no way to "merge" our customizations
		assertEquals(-1, requestConfig.getConnectTimeout());
	}

}
