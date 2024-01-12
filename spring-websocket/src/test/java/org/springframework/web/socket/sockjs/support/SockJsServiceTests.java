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

package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link AbstractSockJsService}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class SockJsServiceTests extends AbstractHttpRequestTests {

	private final TestSockJsService service = new TestSockJsService(new ThreadPoolTaskScheduler());


	@Test
	void validateRequest() {
		this.service.setWebSocketEnabled(false);
		resetResponseAndHandleRequest("GET", "/echo/server/session/websocket", HttpStatus.NOT_FOUND);

		this.service.setWebSocketEnabled(true);
		resetResponseAndHandleRequest("GET", "/echo/server/session/websocket", HttpStatus.OK);

		resetResponseAndHandleRequest("GET", "/echo//", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo///", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo/other", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo//service/websocket", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo/server//websocket", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo/server/session/", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo/s.erver/session/websocket", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo/server/s.ession/websocket", HttpStatus.NOT_FOUND);
		resetResponseAndHandleRequest("GET", "/echo/server/session/jsonp;Setup.pl", HttpStatus.NOT_FOUND);
	}

	@Test
	void handleInfoGet() throws IOException {
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		assertThat(this.servletResponse.getContentType()).isEqualTo("application/json;charset=UTF-8");
		String header = this.servletResponse.getHeader(HttpHeaders.CACHE_CONTROL);
		assertThat(header).isEqualTo("no-store, no-cache, must-revalidate, max-age=0");
		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
		assertThat(this.servletResponse.getHeader(HttpHeaders.VARY)).isNull();

		String body = this.servletResponse.getContentAsString();
		assertThat(body.substring(0, body.indexOf(':'))).isEqualTo("{\"entropy\"");
		assertThat(body.substring(body.indexOf(','))).isEqualTo(",\"origins\":[\"*:*\"],\"cookie_needed\":true,\"websocket\":true}");

		this.service.setSessionCookieNeeded(false);
		this.service.setWebSocketEnabled(false);
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		body = this.servletResponse.getContentAsString();
		assertThat(body.substring(body.indexOf(','))).isEqualTo(",\"origins\":[\"*:*\"],\"cookie_needed\":false,\"websocket\":false}");

		this.service.setAllowedOrigins(Collections.singletonList("https://mydomain1.example"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);
		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
		assertThat(this.servletResponse.getHeader(HttpHeaders.VARY)).isNull();
	}

	@Test  // SPR-12226 and SPR-12660
	void handleInfoGetWithOrigin() throws IOException {
		this.servletRequest.setServerName("mydomain2.example");
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		assertThat(this.servletResponse.getContentType()).isEqualTo("application/json;charset=UTF-8");
		String header = this.servletResponse.getHeader(HttpHeaders.CACHE_CONTROL);
		assertThat(header).isEqualTo("no-store, no-cache, must-revalidate, max-age=0");
		String body = this.servletResponse.getContentAsString();
		assertThat(body.substring(0, body.indexOf(':'))).isEqualTo("{\"entropy\"");
		assertThat(body.substring(body.indexOf(','))).isEqualTo(",\"origins\":[\"*:*\"],\"cookie_needed\":true,\"websocket\":true}");

		this.service.setAllowedOrigins(Collections.singletonList("http://mydomain1.example"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.example", "http://mydomain2.example", "http://mydomain3.example"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		this.service.setAllowedOrigins(Collections.singletonList("*"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		this.servletRequest.setServerName("mydomain3.com");
		this.service.setAllowedOrigins(Collections.singletonList("http://mydomain1.example"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.FORBIDDEN);
	}

	@Test  // SPR-11443
	void handleInfoGetCorsFilter() {
		// Simulate scenario where Filter would have already set CORS headers
		this.servletResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "foobar:123");

		handleRequest("GET", "/echo/info", HttpStatus.OK);

		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("foobar:123");
	}

	@Test  // SPR-11919
	void handleInfoGetWildflyNPE() throws IOException {
		HttpServletResponse mockResponse = mock();
		ServletOutputStream ous = mock();
		given(mockResponse.getHeaders(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).willThrow(NullPointerException.class);
		given(mockResponse.getOutputStream()).willReturn(ous);
		this.response = new ServletServerHttpResponse(mockResponse);

		handleRequest("GET", "/echo/info", HttpStatus.OK);

		verify(mockResponse, times(1)).getOutputStream();
	}

	@Test  // SPR-12660
	void handleInfoOptions() {
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Last-Modified");
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNull();

		this.service.setAllowedOrigins(Collections.singletonList("https://mydomain1.example"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNull();
	}

	@Test  // SPR-12226 and SPR-12660
	void handleInfoOptionsWithAllowedOrigin() {
		this.servletRequest.setServerName("mydomain2.example");
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Last-Modified");
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNotNull();

		this.service.setAllowedOrigins(Collections.singletonList("http://mydomain1.example"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNotNull();

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.example", "http://mydomain2.example", "http://mydomain3.example"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNotNull();

		this.service.setAllowedOrigins(Collections.singletonList("*"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNotNull();
	}

	@Test  // SPR-16304
	void handleInfoOptionsWithForbiddenOrigin() {
		this.servletRequest.setServerName("mydomain3.com");
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Last-Modified");
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.FORBIDDEN);
		CorsConfiguration corsConfiguration = this.service.getCorsConfiguration(this.servletRequest);
		assertThat(corsConfiguration.getAllowedOrigins()).isEmpty();

		this.service.setAllowedOrigins(Collections.singletonList("https://mydomain1.example"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.FORBIDDEN);
		corsConfiguration = this.service.getCorsConfiguration(this.servletRequest);
		assertThat(corsConfiguration.getAllowedOrigins()).isEqualTo(Collections.singletonList("https://mydomain1.example"));
	}

	@Test  // SPR-12283
	void handleInfoOptionsWithOriginAndCorsHeadersDisabled() {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
		this.service.setAllowedOriginPatterns(Collections.singletonList("*"));
		this.service.setSuppressCors(true);

		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Last-Modified");
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNull();

		this.service.setAllowedOrigins(Collections.singletonList("https://mydomain1.example"));
		this.service.setAllowedOriginPatterns(Collections.emptyList());
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.FORBIDDEN);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNull();

		this.service.setAllowedOrigins(Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example"));
		this.service.setAllowedOriginPatterns(Collections.emptyList());
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertThat(this.service.getCorsConfiguration(this.servletRequest)).isNull();
	}

	@Test
	void handleIframeRequest() throws IOException {
		resetResponseAndHandleRequest("GET", "/echo/iframe.html", HttpStatus.OK);

		assertThat(this.servletResponse.getContentType()).isEqualTo("text/html;charset=UTF-8");
		assertThat(this.servletResponse.getContentAsString()).startsWith("<!DOCTYPE html>\n");
		assertThat(this.servletResponse.getContentLength()).isEqualTo(521);
		assertThat(this.response.getHeaders().getCacheControl()).isEqualTo("no-store, no-cache, must-revalidate, max-age=0");
		assertThat(this.response.getHeaders().getETag()).isEqualTo("\"0d5374d44ec6545f2deae3a688b7ec9a8\"");
	}

	@Test
	void handleIframeRequestNotModified() {
		this.servletRequest.addHeader("If-None-Match", "\"0d5374d44ec6545f2deae3a688b7ec9a8\"");
		resetResponseAndHandleRequest("GET", "/echo/iframe.html", HttpStatus.NOT_MODIFIED);
	}

	@Test
	void handleRawWebSocketRequest() throws IOException {
		resetResponseAndHandleRequest("GET", "/echo", HttpStatus.OK);
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("Welcome to SockJS!\n");

		resetResponseAndHandleRequest("GET", "/echo/websocket", HttpStatus.OK);
		assertThat(this.service.sessionId).as("Raw WebSocket should not open a SockJS session").isNull();
	}

	@Test
	void handleEmptyContentType() {
		this.servletRequest.setContentType("");
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		assertThat(this.servletResponse.getStatus()).as("Invalid/empty content should have been ignored").isEqualTo(200);
	}


	private void resetResponseAndHandleRequest(String httpMethod, String uri, HttpStatus httpStatus) {
		resetResponse();
		handleRequest(httpMethod, uri, httpStatus);
	}

	private void handleRequest(String httpMethod, String uri, HttpStatus httpStatus) {
		setRequest(httpMethod, uri);
		String sockJsPath = uri.substring("/echo".length());
		this.service.handleRequest(this.request, this.response, sockJsPath, null);

		assertThat(this.servletResponse.getStatus()).isEqualTo(httpStatus.value());
	}


	private static class TestSockJsService extends AbstractSockJsService {

		private String sessionId;


		TestSockJsService(TaskScheduler scheduler) {
			super(scheduler);
		}

		@Override
		protected void handleRawWebSocketRequest(ServerHttpRequest req, ServerHttpResponse res,
				WebSocketHandler handler) {
		}

		@Override
		protected void handleTransportRequest(ServerHttpRequest req, ServerHttpResponse res, WebSocketHandler handler,
				String sessionId, String transport) throws SockJsException {
			this.sessionId = sessionId;
		}
	}

}
