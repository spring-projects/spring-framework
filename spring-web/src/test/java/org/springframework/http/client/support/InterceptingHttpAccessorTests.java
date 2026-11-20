/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.support;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InterceptingHttpAccessor}.
 *
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
class InterceptingHttpAccessorTests {

	@Test
	void getInterceptors() {
		TestInterceptingHttpAccessor accessor = new TestInterceptingHttpAccessor();
		List<ClientHttpRequestInterceptor> interceptors = Arrays.asList(
				new SecondClientHttpRequestInterceptor(),
				new ThirdClientHttpRequestInterceptor(),
				new FirstClientHttpRequestInterceptor()

		);
		accessor.setInterceptors(interceptors);
		assertThat(accessor.getInterceptors()).hasExactlyElementsOfTypes(
				FirstClientHttpRequestInterceptor.class,
				SecondClientHttpRequestInterceptor.class,
				ThirdClientHttpRequestInterceptor.class);
	}


	private static class TestInterceptingHttpAccessor extends InterceptingHttpAccessor {
	}


	@Order(1)
	private static class FirstClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
			return null;
		}
	}


	private static class SecondClientHttpRequestInterceptor implements ClientHttpRequestInterceptor, Ordered {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
			return null;
		}

		@Override
		public int getOrder() {
			return 2;
		}
	}


	private static class ThirdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
			return null;
		}
	}

}
