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

package org.springframework.http.client;

import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class StreamingSimpleClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setBufferRequestBody(false);
		return factory;
	}

	@Test  // SPR-8809
	public void interceptor() throws Exception {
		final String headerName = "MyHeader";
		final String headerValue = "MyValue";
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add(headerName, headerValue);
			return execution.execute(request, body);
		};
		InterceptingClientHttpRequestFactory factory = new InterceptingClientHttpRequestFactory(
				createRequestFactory(), Collections.singletonList(interceptor));

		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.GET);
			response = request.execute();
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			HttpHeaders responseHeaders = response.getHeaders();
			assertThat(responseHeaders.getFirst(headerName)).as("Custom header invalid").isEqualTo(headerValue);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

	@Test
	@Disabled
	public void largeFileUpload() throws Exception {
		Random rnd = new Random();
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/methods/post"), HttpMethod.POST);
			final int BUF_SIZE = 4096;
			final int ITERATIONS = Integer.MAX_VALUE / BUF_SIZE;
			// final int contentLength = ITERATIONS * BUF_SIZE;
			// request.getHeaders().setContentLength(contentLength);
			OutputStream body = request.getBody();
			for (int i = 0; i < ITERATIONS; i++) {
				byte[] buffer = new byte[BUF_SIZE];
				rnd.nextBytes(buffer);
				body.write(buffer);
			}
			response = request.execute();
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

}
