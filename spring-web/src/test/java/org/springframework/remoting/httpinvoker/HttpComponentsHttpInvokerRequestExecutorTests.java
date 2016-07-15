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

package org.springframework.remoting.httpinvoker;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Stephane Nicoll
 */
public class HttpComponentsHttpInvokerRequestExecutorTests {

	@Test
	public void customizeConnectionTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setConnectTimeout(5000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertEquals(5000, httpPost.getConfig().getConnectTimeout());
	}

	@Test
	public void customizeConnectionRequestTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setConnectionRequestTimeout(7000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertEquals(7000, httpPost.getConfig().getConnectionRequestTimeout());
	}

	@Test
	public void customizeReadTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setReadTimeout(10000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertEquals(10000, httpPost.getConfig().getSocketTimeout());
	}

	@Test
	public void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws IOException {
		RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		when(configurable.getConfig()).thenReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor(client);
		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertSame("Default client configuration is expected", defaultConfig, httpPost.getConfig());

		executor.setConnectionRequestTimeout(4567);
		HttpPost httpPost2 = executor.createHttpPost(config);
		assertNotNull(httpPost2.getConfig());
		assertEquals(4567, httpPost2.getConfig().getConnectionRequestTimeout());
		// Default connection timeout merged
		assertEquals(1234, httpPost2.getConfig().getConnectTimeout());
	}

	@Test
	public void localSettingsOverrideClientDefaultSettings() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).setConnectionRequestTimeout(6789).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		when(configurable.getConfig()).thenReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor(client);
		executor.setConnectTimeout(5000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		RequestConfig requestConfig = httpPost.getConfig();
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

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor() {
					@Override
					public HttpClient getHttpClient() {
						return client;
					}
				};
		executor.setReadTimeout(5000);
		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		RequestConfig requestConfig = httpPost.getConfig();
		assertEquals(-1, requestConfig.getConnectTimeout());
		assertEquals(-1, requestConfig.getConnectionRequestTimeout());
		assertEquals(5000, requestConfig.getSocketTimeout());

		// Update the Http client so that it returns an updated  config
		RequestConfig updatedDefaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).build();
		when(configurable.getConfig()).thenReturn(updatedDefaultConfig);
		executor.setReadTimeout(7000);
		HttpPost httpPost2 = executor.createHttpPost(config);
		RequestConfig requestConfig2 = httpPost2.getConfig();
		assertEquals(1234, requestConfig2.getConnectTimeout());
		assertEquals(-1, requestConfig2.getConnectionRequestTimeout());
		assertEquals(7000, requestConfig2.getSocketTimeout());
	}

	@Test
	public void ignoreFactorySettings() throws IOException {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor(httpClient) {
			@Override
			protected RequestConfig createRequestConfig(HttpInvokerClientConfiguration config) {
				return null;
			}
		};

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertNull("custom request config should not be set", httpPost.getConfig());
	}

	private HttpInvokerClientConfiguration mockHttpInvokerClientConfiguration(String serviceUrl) {
		HttpInvokerClientConfiguration config = mock(HttpInvokerClientConfiguration.class);
		when(config.getServiceUrl()).thenReturn(serviceUrl);
		return config;
	}

}
