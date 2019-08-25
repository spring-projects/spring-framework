/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.remoting.httpinvoker;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author Stephane Nicoll
 */
public class HttpComponentsHttpInvokerRequestExecutorTests {

	@Test
	public void customizeConnectionTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setConnectTimeout(5000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat(httpPost.getConfig().getConnectTimeout()).isEqualTo(5000);
	}

	@Test
	public void customizeConnectionRequestTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setConnectionRequestTimeout(7000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat(httpPost.getConfig().getConnectionRequestTimeout()).isEqualTo(7000);
	}

	@Test
	public void customizeReadTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setReadTimeout(10000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat(httpPost.getConfig().getSocketTimeout()).isEqualTo(10000);
	}

	@Test
	public void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws IOException {
		RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor(client);
		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat(httpPost.getConfig()).as("Default client configuration is expected").isSameAs(defaultConfig);

		executor.setConnectionRequestTimeout(4567);
		HttpPost httpPost2 = executor.createHttpPost(config);
		assertThat(httpPost2.getConfig()).isNotNull();
		assertThat(httpPost2.getConfig().getConnectionRequestTimeout()).isEqualTo(4567);
		// Default connection timeout merged
		assertThat(httpPost2.getConfig().getConnectTimeout()).isEqualTo(1234);
	}

	@Test
	public void localSettingsOverrideClientDefaultSettings() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).setConnectionRequestTimeout(6789).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor(client);
		executor.setConnectTimeout(5000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		RequestConfig requestConfig = httpPost.getConfig();
		assertThat(requestConfig.getConnectTimeout()).isEqualTo(5000);
		assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(6789);
		assertThat(requestConfig.getSocketTimeout()).isEqualTo(-1);
	}

	@Test
	public void mergeBasedOnCurrentHttpClient() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setSocketTimeout(1234).build();
		final CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsHttpInvokerRequestExecutor executor =
				new HttpComponentsHttpInvokerRequestExecutor() {
					@Override
					public HttpClient getHttpClient() {
						return client;
					}
				};
		executor.setReadTimeout(5000);
		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		RequestConfig requestConfig = httpPost.getConfig();
		assertThat(requestConfig.getConnectTimeout()).isEqualTo(-1);
		assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(-1);
		assertThat(requestConfig.getSocketTimeout()).isEqualTo(5000);

		// Update the Http client so that it returns an updated  config
		RequestConfig updatedDefaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234).build();
		given(configurable.getConfig()).willReturn(updatedDefaultConfig);
		executor.setReadTimeout(7000);
		HttpPost httpPost2 = executor.createHttpPost(config);
		RequestConfig requestConfig2 = httpPost2.getConfig();
		assertThat(requestConfig2.getConnectTimeout()).isEqualTo(1234);
		assertThat(requestConfig2.getConnectionRequestTimeout()).isEqualTo(-1);
		assertThat(requestConfig2.getSocketTimeout()).isEqualTo(7000);
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

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("https://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertThat(httpPost.getConfig()).as("custom request config should not be set").isNull();
	}

	private HttpInvokerClientConfiguration mockHttpInvokerClientConfiguration(String serviceUrl) {
		HttpInvokerClientConfiguration config = mock(HttpInvokerClientConfiguration.class);
		given(config.getServiceUrl()).willReturn(serviceUrl);
		return config;
	}

}
