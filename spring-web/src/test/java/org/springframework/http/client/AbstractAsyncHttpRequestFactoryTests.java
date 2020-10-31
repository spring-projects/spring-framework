/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Future;

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
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@SuppressWarnings("deprecation")
public abstract class AbstractAsyncHttpRequestFactoryTests extends AbstractMockWebServerTests {

	protected AsyncClientHttpRequestFactory factory;


	@BeforeEach
	public final void createFactory() throws Exception {
		this.factory = createRequestFactory();
		if (this.factory instanceof InitializingBean) {
			((InitializingBean) this.factory).afterPropertiesSet();
		}
	}

	@AfterEach
	public final void destroyFactory() throws Exception {
		if (this.factory instanceof DisposableBean) {
			((DisposableBean) this.factory).destroy();
		}
	}

	protected abstract AsyncClientHttpRequestFactory createRequestFactory();


	@Test
	public void status() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(uri, HttpMethod.GET);
		assertThat(request.getMethod()).as("Invalid HTTP method").isEqualTo(HttpMethod.GET);
		assertThat(request.getURI()).as("Invalid HTTP URI").isEqualTo(uri);
		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		try (ClientHttpResponse response = futureResponse.get()) {
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	public void statusCallback() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(uri, HttpMethod.GET);
		assertThat(request.getMethod()).as("Invalid HTTP method").isEqualTo(HttpMethod.GET);
		assertThat(request.getURI()).as("Invalid HTTP URI").isEqualTo(uri);
		ListenableFuture<ClientHttpResponse> listenableFuture = request.executeAsync();
		listenableFuture.addCallback(new ListenableFutureCallback<ClientHttpResponse>() {
			@Override
			public void onSuccess(ClientHttpResponse result) {
				try {
					assertThat(result.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.NOT_FOUND);
				}
				catch (IOException ex) {
					throw new AssertionError(ex.getMessage(), ex);
				}
			}
			@Override
			public void onFailure(Throwable ex) {
				throw new AssertionError(ex.getMessage(), ex);
			}
		});
		try (ClientHttpResponse response = listenableFuture.get()) {
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	public void echo() throws Exception {
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
		assertThat(request.getMethod()).as("Invalid HTTP method").isEqualTo(HttpMethod.PUT);
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		request.getHeaders().add(headerName, headerValue1);
		String headerValue2 = "value2";
		request.getHeaders().add(headerName, headerValue2);
		final byte[] body = "Hello World".getBytes("UTF-8");
		request.getHeaders().setContentLength(body.length);

		if (request instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingRequest = (StreamingHttpOutputMessage) request;
			streamingRequest.setBody(outputStream -> StreamUtils.copy(body, outputStream));
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		try ( ClientHttpResponse response = futureResponse.get()) {
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
			assertThat(response.getHeaders().containsKey(headerName)).as("Header not found").isTrue();
			assertThat(response.getHeaders().get(headerName)).as("Header value not found").isEqualTo(Arrays.asList(headerValue1, headerValue2));
			byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
			assertThat(Arrays.equals(body, result)).as("Invalid body").isTrue();
		}
	}

	@Test
	public void multipleWrites() throws Exception {
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
		final byte[] body = "Hello World".getBytes("UTF-8");

		if (request instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingRequest = (StreamingHttpOutputMessage) request;
			streamingRequest.setBody(outputStream -> StreamUtils.copy(body, outputStream));
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		try (ClientHttpResponse response = futureResponse.get()) {
			assertThat(response).isNotNull();
			assertThatIllegalStateException().isThrownBy(() -> FileCopyUtils.copy(body, request.getBody()));
		}
	}

	@Test
	public void headersAfterExecute() throws Exception {
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
		request.getHeaders().add("MyHeader", "value");
		byte[] body = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(body, request.getBody());

		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		try (ClientHttpResponse response = futureResponse.get()) {
			assertThat(response).isNotNull();
			assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
					request.getHeaders().add("MyHeader", "value"));
		}
	}

	@Test
	public void httpMethods() throws Exception {
		assertHttpMethod("get", HttpMethod.GET);
		assertHttpMethod("head", HttpMethod.HEAD);
		assertHttpMethod("post", HttpMethod.POST);
		assertHttpMethod("put", HttpMethod.PUT);
		assertHttpMethod("options", HttpMethod.OPTIONS);
		assertHttpMethod("delete", HttpMethod.DELETE);
	}

	protected void assertHttpMethod(String path, HttpMethod method) throws Exception {
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/methods/" + path), method);
		if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
			// requires a body
			request.getBody().write(32);
		}
		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		try (ClientHttpResponse response = futureResponse.get()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			assertThat(request.getMethod().name()).as("Invalid method").isEqualTo(path.toUpperCase(Locale.ENGLISH));
		}
	}

	@Test
	public void cancel() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(uri, HttpMethod.GET);
		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		futureResponse.cancel(true);
		assertThat(futureResponse.isCancelled()).isTrue();
	}

}
