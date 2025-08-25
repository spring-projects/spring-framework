/*
 * Copyright 2002-present the original author or authors.
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
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdkClientHttpRequestFactory}.
 *
 * @author Marten Deinum
 * @author Brian Clozel
 */
class JdkClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	private static @Nullable String originalPropertyValue;


	@BeforeAll
	static void setProperty() {
		originalPropertyValue = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
		System.setProperty("jdk.httpclient.allowRestrictedHeaders", "expect");
	}

	@AfterAll
	static void restoreProperty() {
		if (originalPropertyValue != null) {
			System.setProperty("jdk.httpclient.allowRestrictedHeaders", originalPropertyValue);
		}
		else {
			System.clearProperty("jdk.httpclient.allowRestrictedHeaders");
		}
	}


	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new JdkClientHttpRequestFactory();
	}

	@Override
	@Test
	void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

	@Test
	void customizeDisallowedHeaders() throws IOException {
		URI uri = URI.create(this.baseUrl + "/status/299");
		ClientHttpRequest request = this.factory.createRequest(uri, HttpMethod.PUT);
		request.getHeaders().set("Expect", "299");

		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatusCode.valueOf(299));
		}
	}

	@Test // gh-31451
	public void contentLength0() throws IOException {
		URI uri = URI.create(this.baseUrl + "/methods/get");
		ClientHttpRequest request =
				new BufferingClientHttpRequestFactory(this.factory).createRequest(uri, HttpMethod.GET);

		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
		}
	}

	@Test // gh-35068
	void deleteRequestWithBody() throws Exception {
		URI uri = URI.create(baseUrl + "/echo");
		ClientHttpRequest request = this.factory.createRequest(uri, HttpMethod.DELETE);
		StreamUtils.copy("body", StandardCharsets.UTF_8, request.getBody());
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).as("Invalid request body").hasContent("body");
		}
	}

	@Test
	void compressionDisabled() throws IOException {
		URI uri = URI.create(baseUrl + "/compress/");
		ClientHttpRequest request = this.factory.createRequest(uri, HttpMethod.POST);
		StreamUtils.copy("Payload to compress", StandardCharsets.UTF_8, request.getBody());
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			assertThat(response.getHeaders().containsHeader("Content-Encoding")).isFalse();
			assertThat(response.getBody()).as("Invalid request body").hasContent("Payload to compress");
		}
	}

	@Test
	void compressionGzip() throws IOException {
		URI uri = URI.create(baseUrl + "/compress/gzip");
		JdkClientHttpRequestFactory requestFactory = (JdkClientHttpRequestFactory) this.factory;
		requestFactory.enableCompression(true);
		ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
		StreamUtils.copy("Payload to compress", StandardCharsets.UTF_8, request.getBody());
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			assertThat(response.getHeaders().getFirst("Content-Encoding"))
					.as("Invalid content encoding").isEqualTo("gzip");
			assertThat(response.getBody()).as("Invalid request body").hasContent("Payload to compress");
		}
	}

	@Test
	void compressionDeflate() throws IOException {
		URI uri = URI.create(baseUrl + "/compress/deflate");
		JdkClientHttpRequestFactory requestFactory = (JdkClientHttpRequestFactory) this.factory;
		requestFactory.enableCompression(true);
		ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
		StreamUtils.copy("Payload to compress", StandardCharsets.UTF_8, request.getBody());
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			assertThat(response.getHeaders().getFirst("Content-Encoding"))
					.as("Invalid content encoding").isEqualTo("deflate");
			assertThat(response.getBody()).as("Invalid request body").hasContent("Payload to compress");
		}
	}

	@Test // gh-34971
	@EnabledForJreRange(min = JRE.JAVA_19) // behavior fixed in Java 19
	void requestContentLengthHeaderWhenNoBody() throws Exception {
		URI uri = URI.create(baseUrl + "/header/Content-Length");
		assertNoContentLength(uri, HttpMethod.GET);
		assertNoContentLength(uri, HttpMethod.DELETE);
	}

	protected void assertNoContentLength(URI uri, HttpMethod method) throws Exception {
		ClientHttpRequest request = factory.createRequest(uri, method);
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			assertThat(StreamUtils.copyToString(response.getBody(), StandardCharsets.ISO_8859_1))
					.as("Invalid Content-Length request header").isEqualTo("Content-Length:null");
		}
	}

}
