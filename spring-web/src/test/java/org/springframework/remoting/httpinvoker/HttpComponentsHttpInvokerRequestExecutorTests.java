package org.springframework.remoting.httpinvoker;

import java.io.IOException;

import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
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
	public void customizeReadTimeout() throws IOException {
		HttpComponentsHttpInvokerRequestExecutor executor = new HttpComponentsHttpInvokerRequestExecutor();
		executor.setReadTimeout(10000);

		HttpInvokerClientConfiguration config = mockHttpInvokerClientConfiguration("http://fake-service");
		HttpPost httpPost = executor.createHttpPost(config);
		assertEquals(10000, httpPost.getConfig().getSocketTimeout());
	}

	private HttpInvokerClientConfiguration mockHttpInvokerClientConfiguration(String serviceUrl) {
		HttpInvokerClientConfiguration config = mock(HttpInvokerClientConfiguration.class);
		when(config.getServiceUrl()).thenReturn(serviceUrl);
		return config;
	}

}
