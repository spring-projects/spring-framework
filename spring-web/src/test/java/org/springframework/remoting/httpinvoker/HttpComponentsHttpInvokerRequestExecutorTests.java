package org.springframework.remoting.httpinvoker;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Stephane Nicoll
 */
public class HttpComponentsHttpInvokerRequestExecutorTests {

	@SuppressWarnings("deprecation")
	@Test
	public void assertLegacyCustomConfig() {
		HttpClient httpClient = new org.apache.http.impl.client.DefaultHttpClient(); // Does not support RequestConfig
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor(httpClient);

		executor.setConnectTimeout(1234);
		assertEquals(1234, httpClient.getParams().getIntParameter(
				org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT, 0));

		executor.setReadTimeout(4567);
		assertEquals(4567, httpClient.getParams().getIntParameter(
				org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT, 0));
	}

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
