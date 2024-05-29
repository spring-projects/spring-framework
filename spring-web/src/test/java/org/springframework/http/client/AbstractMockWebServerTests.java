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

package org.springframework.http.client;

import java.util.Collections;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brian Clozel
 */
public abstract class AbstractMockWebServerTests {

	private MockWebServer server;

	protected int port;

	protected String baseUrl;

	protected static final MediaType textContentType =
			new MediaType("text", "plain", Collections.singletonMap("charset", "UTF-8"));

	@BeforeEach
	void setUp() throws Exception {
		this.server = new MockWebServer();
		this.server.setDispatcher(new TestDispatcher());
		this.server.start();
		this.port = this.server.getPort();
		this.baseUrl = "http://localhost:" + this.port;
	}

	@AfterEach
	void tearDown() throws Exception {
		this.server.shutdown();
	}

	protected class TestDispatcher extends Dispatcher {
		@Override
		public MockResponse dispatch(RecordedRequest request) {
			try {
				if (request.getPath().equals("/echo")) {
					assertThat(request.getHeader("Host"))
							.contains("localhost:" + port);
					MockResponse response = new MockResponse()
							.setHeaders(request.getHeaders())
							.setHeader("Content-Length", request.getBody().size())
							.setResponseCode(200)
							.setBody(request.getBody());
					request.getBody().flush();
					return response;
				}
				else if(request.getPath().equals("/status/ok")) {
					return new MockResponse();
				}
				else if(request.getPath().equals("/status/notfound")) {
					return new MockResponse().setResponseCode(404);
				}
				else if (request.getPath().equals("/status/299")) {
					assertThat(request.getHeader("Expect"))
							.contains("299");
					return new MockResponse().setResponseCode(299);
				}
				else if(request.getPath().startsWith("/params")) {
					assertThat(request.getPath()).contains("param1=value");
					assertThat(request.getPath()).contains("param2=value1&param2=value2");
					return new MockResponse();
				}
				else if(request.getPath().equals("/methods/post")) {
					assertThat(request.getMethod()).isEqualTo("POST");
					String transferEncoding = request.getHeader("Transfer-Encoding");
					if(StringUtils.hasLength(transferEncoding)) {
						assertThat(transferEncoding).isEqualTo("chunked");
					}
					else {
						long contentLength = Long.parseLong(request.getHeader("Content-Length"));
						assertThat(request.getBody().size()).isEqualTo(contentLength);
					}
					return new MockResponse().setResponseCode(200);
				}
				else if(request.getPath().startsWith("/methods/")) {
					String expectedMethod = request.getPath().replace("/methods/","").toUpperCase();
					assertThat(request.getMethod()).isEqualTo(expectedMethod);
					return new MockResponse();
				}
				else if(request.getPath().startsWith("/header/")) {
					String headerName = request.getPath().replace("/header/","");
					return new MockResponse().setBody(headerName + ":" + request.getHeader(headerName)).setResponseCode(200);
				}
				return new MockResponse().setResponseCode(404);
			}
			catch (Throwable exc) {
				return new MockResponse().setResponseCode(500).setBody(exc.toString());
			}
		}
	}
}
