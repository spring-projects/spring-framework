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

package org.springframework.http.client.support;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Tests for {@link InterceptingHttpAccessor}.
 *
 * @author Brian Clozel
 */
public class InterceptingHttpAccessorTests {

	@Test
	public void getInterceptors() throws Exception {
		TestInterceptingHttpAccessor accessor = new TestInterceptingHttpAccessor();
		List<ClientHttpRequestInterceptor> interceptors = Arrays.asList(
				new SecondClientHttpRequestInterceptor(),
				new ThirdClientHttpRequestInterceptor(),
				new FirstClientHttpRequestInterceptor()

		);
		accessor.setInterceptors(interceptors);

		assertThat(accessor.getInterceptors().get(0), Matchers.instanceOf(FirstClientHttpRequestInterceptor.class));
		assertThat(accessor.getInterceptors().get(1), Matchers.instanceOf(SecondClientHttpRequestInterceptor.class));
		assertThat(accessor.getInterceptors().get(2), Matchers.instanceOf(ThirdClientHttpRequestInterceptor.class));
	}

	private class TestInterceptingHttpAccessor extends InterceptingHttpAccessor {
	}

	@Order(1)
	private class FirstClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			return null;
		}
	}

	private class SecondClientHttpRequestInterceptor implements ClientHttpRequestInterceptor, Ordered {
		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			return null;
		}

		@Override
		public int getOrder() {
			return 2;
		}
	}

	private class ThirdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			return null;
		}
	}

}