/*
 * Copyright 2002-2023 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Arjen Poutsma
 */
abstract class AbstractHttpRequestFactoryTests extends AbstractMockWebServerTests {

	protected ClientHttpRequestFactory factory;


	@BeforeEach
	final void createFactory() throws Exception {
		factory = createRequestFactory();
		if (factory instanceof InitializingBean initializingBean) {
			initializingBean.afterPropertiesSet();
		}
	}

	@AfterEach
	final void destroyFactory() throws Exception {
		if (factory instanceof DisposableBean disposableBean) {
			disposableBean.destroy();
		}
	}


	protected abstract ClientHttpRequestFactory createRequestFactory();


	@Test
	void status() throws Exception {
		URI uri = URI.create(baseUrl + "/status/notfound");
		ClientHttpRequest request = factory.createRequest(uri, HttpMethod.GET);
		assertThat(request.getMethod()).as("Invalid HTTP method").isEqualTo(HttpMethod.GET);
		assertThat(request.getURI()).as("Invalid HTTP URI").isEqualTo(uri);

		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	void echo() throws Exception {
		ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/echo"), HttpMethod.PUT);
		assertThat(request.getMethod()).as("Invalid HTTP method").isEqualTo(HttpMethod.PUT);

		String headerName = "MyHeader";
		String headerValue1 = "value1";
		request.getHeaders().add(headerName, headerValue1);
		String headerValue2 = "value2";
		request.getHeaders().add(headerName, headerValue2);
		final byte[] body = "Hello World".getBytes(StandardCharsets.UTF_8);
		request.getHeaders().setContentLength(body.length);

		if (request instanceof StreamingHttpOutputMessage streamingRequest) {
			streamingRequest.setBody(outputStream -> StreamUtils.copy(body, outputStream));
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
			assertThat(response.getHeaders()).as("Header not found").containsKey(headerName);
			assertThat(response.getHeaders()).as("Header value not found")
					.containsEntry(headerName, Arrays.asList(headerValue1, headerValue2));
			byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
			assertThat(Arrays.equals(body, result)).as("Invalid body").isTrue();
		}
	}

	@Test
	void multipleWrites() throws Exception {
		ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/echo"), HttpMethod.POST);

		final byte[] body = "Hello World".getBytes(StandardCharsets.UTF_8);
		request.getHeaders().setContentLength(body.length);
		if (request instanceof StreamingHttpOutputMessage streamingRequest) {
			streamingRequest.setBody(outputStream -> StreamUtils.copy(body, outputStream));
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		try (ClientHttpResponse response = request.execute()) {
			assertThatIllegalStateException().isThrownBy(() ->
					FileCopyUtils.copy(body, request.getBody()));
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
		}
	}

	@Test
	void headersAfterExecute() throws Exception {
		ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/status/ok"), HttpMethod.POST);

		request.getHeaders().add("MyHeader", "value");
		byte[] body = "Hello World".getBytes(StandardCharsets.UTF_8);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
				if (request instanceof StreamingHttpOutputMessage streamingRequest) {
					streamingRequest.setBody(outputStream -> FileCopyUtils.copy(body, outputStream));
				}
				else {
					FileCopyUtils.copy(body, request.getBody());
				}
				try (ClientHttpResponse response = request.execute()) {
					assertThat(response).isNotNull();
					request.getHeaders().add("MyHeader", "value");
				}
		});
	}

	@Test
	void httpMethods() throws Exception {
		assertHttpMethod("get", HttpMethod.GET);
		assertHttpMethod("head", HttpMethod.HEAD);
		assertHttpMethod("post", HttpMethod.POST);
		assertHttpMethod("put", HttpMethod.PUT);
		assertHttpMethod("options", HttpMethod.OPTIONS);
		assertHttpMethod("delete", HttpMethod.DELETE);
	}

	protected void assertHttpMethod(String path, HttpMethod method) throws Exception {
		ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/methods/" + path), method);
		if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
			if (request instanceof StreamingHttpOutputMessage streamingRequest) {
				streamingRequest.setBody(outputStream -> outputStream.write(32));
			}
			else {
				request.getBody().write(32);
			}
		}

		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			assertThat(request.getMethod().name()).as("Invalid method").isEqualTo(path.toUpperCase(Locale.ENGLISH));
		}
	}

	@Test
	void queryParameters() throws Exception {
		URI uri = URI.create(baseUrl + "/params?param1=value&param2=value1&param2=value2");
		ClientHttpRequest request = factory.createRequest(uri, HttpMethod.GET);

		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
		}
	}

}
