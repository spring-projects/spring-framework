/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author Stephane Nicoll
 */
public class HttpComponentsClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

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

	@Test
	public void assertCustomConfig() throws Exception {
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(httpClient);
		hrf.setConnectTimeout(1234);
		hrf.setConnectionRequestTimeout(4321);

		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest) hrf.createRequest(uri, HttpMethod.GET);

		Object config = request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
		assertThat(config).as("Request config should be set").isNotNull();
		assertThat(config).as("Wrong request config type " + config.getClass().getName()).isInstanceOf(RequestConfig.class);
		RequestConfig requestConfig = (RequestConfig) config;
		assertThat(requestConfig.getConnectTimeout()).as("Wrong custom connection timeout").isEqualTo(Timeout.of(1234, MILLISECONDS));
		assertThat(requestConfig.getConnectionRequestTimeout()).as("Wrong custom connection request timeout").isEqualTo(Timeout.of(4321, MILLISECONDS));
	}

	@Test
	public void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234, MILLISECONDS).build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
		assertThat(retrieveRequestConfig(hrf)).as("Default client configuration is expected").isSameAs(defaultConfig);

		hrf.setConnectionRequestTimeout(4567);
		RequestConfig requestConfig = retrieveRequestConfig(hrf);
		assertThat(requestConfig).isNotNull();
		assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(4567, MILLISECONDS));
		// Default connection timeout merged
		assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.of(1234, MILLISECONDS));
	}

	@Test
	public void localSettingsOverrideClientDefaultSettings() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234, MILLISECONDS)
				.setConnectionRequestTimeout(6789, MILLISECONDS)
				.build();
		CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
		hrf.setConnectTimeout(5000);

		RequestConfig requestConfig = retrieveRequestConfig(hrf);
		assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.of(5000, MILLISECONDS));
		assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(6789, MILLISECONDS));
	}

	@Test
	public void mergeBasedOnCurrentHttpClient() throws Exception {
		RequestConfig defaultConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(1234, MILLISECONDS)
				.build();
		final CloseableHttpClient client = mock(CloseableHttpClient.class,
				withSettings().extraInterfaces(Configurable.class));
		Configurable configurable = (Configurable) client;
		given(configurable.getConfig()).willReturn(defaultConfig);

		HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory() {
			@Override
			public HttpClient getHttpClient() {
				return client;
			}
		};
		hrf.setConnectionRequestTimeout(5000);

		RequestConfig requestConfig = retrieveRequestConfig(hrf);
		assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(5000, MILLISECONDS));
		assertThat(requestConfig.getConnectTimeout()).isEqualTo(RequestConfig.DEFAULT.getConnectTimeout());

		// Update the Http client so that it returns an updated config
		RequestConfig updatedDefaultConfig = RequestConfig.custom()
				.setConnectTimeout(1234, MILLISECONDS).build();
		given(configurable.getConfig()).willReturn(updatedDefaultConfig);
		hrf.setConnectionRequestTimeout(7000);
		RequestConfig requestConfig2 = retrieveRequestConfig(hrf);
		assertThat(requestConfig2.getConnectTimeout()).isEqualTo(Timeout.of(1234, MILLISECONDS));
		assertThat(requestConfig2.getConnectionRequestTimeout()).isEqualTo(Timeout.of(7000, MILLISECONDS));
	}

	private RequestConfig retrieveRequestConfig(HttpComponentsClientHttpRequestFactory factory) throws Exception {
		URI uri = new URI(baseUrl + "/status/ok");
		HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
				factory.createRequest(uri, HttpMethod.GET);
		return (RequestConfig) request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
	}

}
