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

import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brian Clozel
 */
public abstract class AbstractMockWebServerTests {

	private MockWebServer server;

	protected int port;

	protected String baseUrl;


	@BeforeEach
	void setUp() throws Exception {
		this.server = new MockWebServer();
		this.server.setDispatcher(new TestDispatcher());
		this.server.start();
		this.port = this.server.getPort();
		this.baseUrl = "http://localhost:" + this.port;
	}

	@AfterEach
	void tearDown() {
		this.server.close();
	}


	protected class TestDispatcher extends Dispatcher {

		@Override
		public MockResponse dispatch(RecordedRequest request) {
			try {
				if (request.getTarget().equals("/echo")) {
					assertThat(request.getHeaders().get("Host")).contains("localhost:" + port);
					MockResponse.Builder builder = new MockResponse.Builder().headers(request.getHeaders());
					if (request.getBody() != null) {
						builder = builder.body(request.getBody().utf8());
					}
					else {
						builder.setHeader("Content-Length", 0);
					}
					return builder.code(200).build();
				}
				else if(request.getTarget().equals("/status/ok")) {
					return new MockResponse.Builder().build();
				}
				else if(request.getTarget().equals("/status/notfound")) {
					return new MockResponse.Builder().code(404).build();
				}
				else if (request.getTarget().equals("/status/299")) {
					assertThat(request.getHeaders().get("Expect")).contains("299");
					return new MockResponse.Builder().code(299).build();
				}
				else if(request.getTarget().startsWith("/params")) {
					assertThat(request.getTarget()).contains("param1=value");
					assertThat(request.getTarget()).contains("param2=value1&param2=value2");
					return new MockResponse.Builder().build();
				}
				else if(request.getTarget().equals("/methods/post")) {
					assertThat(request.getMethod()).isEqualTo("POST");
					String transferEncoding = request.getHeaders().get("Transfer-Encoding");
					if(StringUtils.hasLength(transferEncoding)) {
						assertThat(transferEncoding).isEqualTo("chunked");
					}
					else {
						long contentLength = Long.parseLong(request.getHeaders().get("Content-Length"));
						assertThat(request.getBody().size()).isEqualTo(contentLength);
					}
					return new MockResponse.Builder().code(200).build();
				}
				else if(request.getTarget().startsWith("/methods/")) {
					String expectedMethod = request.getTarget().replace("/methods/","").toUpperCase();
					assertThat(request.getMethod()).isEqualTo(expectedMethod);
					return new MockResponse.Builder().build();
				}
				else if(request.getTarget().startsWith("/header/")) {
					String headerName = request.getTarget().replace("/header/","");
					return new MockResponse.Builder().body(headerName + ":" + request.getHeaders().get(headerName)).code(200).build();
				}
				else if(request.getTarget().startsWith("/compress/") && request.getBody() != null) {
					String encoding = request.getTarget().replace("/compress/","");
					String requestBody = request.getBody().utf8();
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					if (encoding.equals("gzip")) {
						try(GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
							gzipOutputStream.write(requestBody.getBytes());
							gzipOutputStream.flush();
						}
					}
					else if(encoding.equals("deflate")) {
							try(DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
							deflaterOutputStream.write(requestBody.getBytes());
							deflaterOutputStream.flush();
						}
					}
					else {
						outputStream.write(requestBody.getBytes());
					}
					Buffer buffer = new Buffer();
					buffer.write(outputStream.toByteArray());
					MockResponse.Builder builder = new MockResponse.Builder()
							.body(buffer)
							.code(200);
					if (!encoding.isEmpty()) {
						builder.setHeader(HttpHeaders.CONTENT_ENCODING, encoding);
					}
					return builder.build();
				}
				return new MockResponse.Builder().code(404).build();
			}
			catch (Throwable ex) {
				return new MockResponse.Builder().code(500).body(ex.toString()).build();
			}
		}
	}
}
