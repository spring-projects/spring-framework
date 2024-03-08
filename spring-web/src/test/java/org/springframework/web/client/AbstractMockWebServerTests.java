/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.client;

import java.io.EOFException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.MULTIPART_MIXED;

/**
 * @author Brian Clozel
 * @author Sam Brannen
 */
abstract class AbstractMockWebServerTests {

	protected static final MediaType MULTIPART_RELATED = new MediaType("multipart", "related");

	protected static final MediaType textContentType =
			new MediaType("text", "plain", Collections.singletonMap("charset", "UTF-8"));

	protected static final String helloWorld = "H\u00e9llo W\u00f6rld";

	private final MockWebServer server = new MockWebServer();

	protected int port;

	protected String baseUrl;


	@BeforeEach
	void setUp() throws Exception {
		this.server.setDispatcher(new TestDispatcher());
		this.server.start();
		this.port = this.server.getPort();
		this.baseUrl = "http://localhost:" + this.port;
	}

	@AfterEach
	void tearDown() throws Exception {
		this.server.shutdown();
	}


	private MockResponse getRequest(RecordedRequest request, byte[] body, String contentType) {
		if (request.getMethod().equals("OPTIONS")) {
			return new MockResponse().setResponseCode(200).setHeader("Allow", "GET, OPTIONS, HEAD, TRACE");
		}
		Buffer buf = new Buffer();
		buf.write(body);
		MockResponse response = new MockResponse()
				.setHeader(CONTENT_LENGTH, body.length)
				.setBody(buf)
				.setResponseCode(200);
		if (contentType != null) {
			response = response.setHeader(CONTENT_TYPE, contentType);
		}
		return response;
	}

	private MockResponse postRequest(RecordedRequest request, String expectedRequestContent,
			String location, String contentType, byte[] responseBody) {

		assertThat(request.getHeaders().values(CONTENT_LENGTH)).hasSize(1);
		assertThat(Integer.parseInt(request.getHeader(CONTENT_LENGTH))).as("Invalid request content-length").isGreaterThan(0);
		String requestContentType = request.getHeader(CONTENT_TYPE);
		assertThat(requestContentType).as("No content-type").isNotNull();
		Charset charset = StandardCharsets.ISO_8859_1;
		if (requestContentType.contains("charset=")) {
			String charsetName = requestContentType.split("charset=")[1];
			charset = Charset.forName(charsetName);
		}
		assertThat(request.getBody().readString(charset)).as("Invalid request body").isEqualTo(expectedRequestContent);
		Buffer buf = new Buffer();
		buf.write(responseBody);
		return new MockResponse()
				.setHeader(LOCATION, baseUrl + location)
				.setHeader(CONTENT_TYPE, contentType)
				.setHeader(CONTENT_LENGTH, responseBody.length)
				.setBody(buf)
				.setResponseCode(201);
	}

	private MockResponse jsonPostRequest(RecordedRequest request, String location, String contentType) {
		if (request.getBodySize() > 0) {
			String contentLength = request.getHeader(CONTENT_LENGTH);
			if (contentLength != null) {
				assertThat(Integer.parseInt(contentLength)).as("Invalid request content-length").isGreaterThan(0);
			}
			assertThat(request.getHeader(CONTENT_TYPE)).as("No content-type").isNotNull();
		}
		return new MockResponse()
				.setHeader(LOCATION, baseUrl + location)
				.setHeader(CONTENT_TYPE, contentType)
				.setHeader(CONTENT_LENGTH, request.getBody().size())
				.setBody(request.getBody())
				.setResponseCode(201);
	}

	private MockResponse multipartFormDataRequest(RecordedRequest request) {
		MediaType mediaType = MediaType.parseMediaType(request.getHeader(CONTENT_TYPE));
		assertThat(mediaType.isCompatibleWith(MULTIPART_FORM_DATA)).as(MULTIPART_FORM_DATA.toString()).isTrue();
		assertMultipart(request, mediaType);
		return new MockResponse().setResponseCode(200);
	}

	private MockResponse multipartMixedRequest(RecordedRequest request) {
		MediaType mediaType = MediaType.parseMediaType(request.getHeader(CONTENT_TYPE));
		assertThat(mediaType.isCompatibleWith(MULTIPART_MIXED)).as(MULTIPART_MIXED.toString()).isTrue();
		assertMultipart(request, mediaType);
		return new MockResponse().setResponseCode(200);
	}

	private MockResponse multipartRelatedRequest(RecordedRequest request) {
		MediaType mediaType = MediaType.parseMediaType(request.getHeader(CONTENT_TYPE));
		assertThat(mediaType.isCompatibleWith(MULTIPART_RELATED)).as(MULTIPART_RELATED.toString()).isTrue();
		assertMultipart(request, mediaType);
		return new MockResponse().setResponseCode(200);
	}

	private void assertMultipart(RecordedRequest request, MediaType mediaType) {
		assertThat(mediaType.isCompatibleWith(new MediaType("multipart", "*"))).as("multipart/*").isTrue();
		String boundary = mediaType.getParameter("boundary");
		assertThat(boundary).as("boundary").isNotBlank();
		Buffer body = request.getBody();
		try {
			assertPart(body, "form-data", boundary, "name 1", "text/plain", "value 1");
			assertPart(body, "form-data", boundary, "name 2", "text/plain", "value 2+1");
			assertPart(body, "form-data", boundary, "name 2", "text/plain", "value 2+2");
			assertFilePart(body, "form-data", boundary, "logo", "logo.jpg", "image/jpeg");
		}
		catch (EOFException ex) {
			throw new AssertionError(ex);
		}
	}

	private void assertPart(Buffer buffer, String disposition, String boundary, String name,
			String contentType, String value) throws EOFException {

		assertThat(buffer.readUtf8Line()).contains("--" + boundary);
		String line = buffer.readUtf8Line();
		assertThat(line).contains("Content-Disposition: " + disposition);
		assertThat(line).contains("name=\"" + name + "\"");
		assertThat(buffer.readUtf8Line()).startsWith("Content-Type: " + contentType);
		assertThat(buffer.readUtf8Line()).isEqualTo("Content-Length: " + value.length());
		assertThat(buffer.readUtf8Line()).isEmpty();
		assertThat(buffer.readUtf8Line()).isEqualTo(value);
	}

	private void assertFilePart(Buffer buffer, String disposition, String boundary, String name,
			String filename, String contentType) throws EOFException {

		assertThat(buffer.readUtf8Line()).contains("--" + boundary);
		String line = buffer.readUtf8Line();
		assertThat(line).contains("Content-Disposition: " + disposition);
		assertThat(line).contains("name=\"" + name + "\"");
		assertThat(line).contains("filename=\"" + filename + "\"");
		assertThat(buffer.readUtf8Line()).startsWith("Content-Type: " + contentType);
		assertThat(buffer.readUtf8Line()).startsWith("Content-Length: ");
		assertThat(buffer.readUtf8Line()).isEmpty();
		assertThat(buffer.readUtf8Line()).isNotNull();
	}

	private MockResponse formRequest(RecordedRequest request) {
		assertThat(request.getHeader(CONTENT_TYPE)).isEqualTo("application/x-www-form-urlencoded");
		assertThat(request.getBody().readUtf8()).contains("name+1=value+1", "name+2=value+2%2B1", "name+2=value+2%2B2");
		return new MockResponse().setResponseCode(200);
	}

	private MockResponse patchRequest(RecordedRequest request, String expectedRequestContent,
			String contentType, byte[] responseBody) {

		assertThat(request.getMethod()).isEqualTo("PATCH");
		assertThat(Integer.parseInt(request.getHeader(CONTENT_LENGTH))).as("Invalid request content-length").isGreaterThan(0);
		String requestContentType = request.getHeader(CONTENT_TYPE);
		assertThat(requestContentType).as("No content-type").isNotNull();
		Charset charset = StandardCharsets.ISO_8859_1;
		if (requestContentType.contains("charset=")) {
			String charsetName = requestContentType.split("charset=")[1];
			charset = Charset.forName(charsetName);
		}
		assertThat(request.getBody().readString(charset)).as("Invalid request body").isEqualTo(expectedRequestContent);
		Buffer buf = new Buffer();
		buf.write(responseBody);
		return new MockResponse().setResponseCode(201)
				.setHeader(CONTENT_LENGTH, responseBody.length)
				.setHeader(CONTENT_TYPE, contentType)
				.setBody(buf);
	}

	private MockResponse putRequest(RecordedRequest request, String expectedRequestContent) {
		assertThat(Integer.parseInt(request.getHeader(CONTENT_LENGTH))).as("Invalid request content-length").isGreaterThan(0);
		String requestContentType = request.getHeader(CONTENT_TYPE);
		assertThat(requestContentType).as("No content-type").isNotNull();
		Charset charset = StandardCharsets.ISO_8859_1;
		if (requestContentType.contains("charset=")) {
			String charsetName = requestContentType.split("charset=")[1];
			charset = Charset.forName(charsetName);
		}
		assertThat(request.getBody().readString(charset)).as("Invalid request body").isEqualTo(expectedRequestContent);
		return new MockResponse().setResponseCode(202);
	}


	protected class TestDispatcher extends Dispatcher {

		@Override
		public MockResponse dispatch(RecordedRequest request) {
			try {
				byte[] helloWorldBytes = helloWorld.getBytes(StandardCharsets.UTF_8);

				if (request.getPath().equals("/get")) {
					return getRequest(request, helloWorldBytes, textContentType.toString());
				}
				else if (request.getPath().equals("/get/nothing")) {
					return getRequest(request, new byte[0], textContentType.toString());
				}
				else if (request.getPath().equals("/get/nocontenttype")) {
					return getRequest(request, helloWorldBytes, null);
				}
				else if (request.getPath().equals("/post")) {
					return postRequest(request, helloWorld, "/post/1", textContentType.toString(), helloWorldBytes);
				}
				else if (request.getPath().equals("/jsonpost")) {
					return jsonPostRequest(request, "/jsonpost/1", "application/json; charset=utf-8");
				}
				else if (request.getPath().equals("/status/nocontent")) {
					return new MockResponse().setResponseCode(204);
				}
				else if (request.getPath().equals("/status/notmodified")) {
					return new MockResponse().setResponseCode(304);
				}
				else if (request.getPath().equals("/status/notfound")) {
					return new MockResponse().setResponseCode(404);
				}
				else if (request.getPath().equals("/status/badrequest")) {
					return new MockResponse().setResponseCode(400);
				}
				else if (request.getPath().equals("/status/server")) {
					return new MockResponse().setResponseCode(500);
				}
				else if (request.getPath().contains("/uri/")) {
					return new MockResponse().setBody(request.getPath()).setHeader(CONTENT_TYPE, "text/plain");
				}
				else if (request.getPath().equals("/multipartFormData")) {
					return multipartFormDataRequest(request);
				}
				else if (request.getPath().equals("/multipartMixed")) {
					return multipartMixedRequest(request);
				}
				else if (request.getPath().equals("/multipartRelated")) {
					return multipartRelatedRequest(request);
				}
				else if (request.getPath().equals("/form")) {
					return formRequest(request);
				}
				else if (request.getPath().equals("/delete")) {
					return new MockResponse().setResponseCode(200);
				}
				else if (request.getPath().equals("/patch")) {
					return patchRequest(request, helloWorld, textContentType.toString(), helloWorldBytes);
				}
				else if (request.getPath().equals("/put")) {
					return putRequest(request, helloWorld);
				}
				return new MockResponse().setResponseCode(404);
			}
			catch (Throwable ex) {
				return new MockResponse().setResponseCode(500).setBody(ex.toString());
			}
		}
	}

}
