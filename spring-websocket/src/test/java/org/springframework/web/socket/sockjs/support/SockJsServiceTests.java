/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link AbstractSockJsService}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class SockJsServiceTests extends AbstractHttpRequestTests {

	private TestSockJsService service;

	private WebSocketHandler handler;


	@Override
	@Before
	public void setUp() {
		super.setUp();
		this.service = new TestSockJsService(new ThreadPoolTaskScheduler());
	}


	@Test
	public void validateRequest() throws Exception {

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
	public void handleInfoGet() throws Exception {
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		assertEquals("application/json;charset=UTF-8", this.servletResponse.getContentType());
		assertEquals("no-store, no-cache, must-revalidate, max-age=0", this.servletResponse.getHeader(HttpHeaders.CACHE_CONTROL));
		assertNull(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertNull(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertNull(this.servletResponse.getHeader(HttpHeaders.VARY));

		String body = this.servletResponse.getContentAsString();
		assertEquals("{\"entropy\"", body.substring(0, body.indexOf(':')));
		assertEquals(",\"origins\":[\"*:*\"],\"cookie_needed\":true,\"websocket\":true}",
				body.substring(body.indexOf(',')));

		this.service.setSessionCookieNeeded(false);
		this.service.setWebSocketEnabled(false);
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		body = this.servletResponse.getContentAsString();
		assertEquals(",\"origins\":[\"*:*\"],\"cookie_needed\":false,\"websocket\":false}",
				body.substring(body.indexOf(',')));

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);
		assertNull(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertNull(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertNull(this.servletResponse.getHeader(HttpHeaders.VARY));
	}

	@Test  // SPR-12226 and SPR-12660
	public void handleInfoGetWithOrigin() throws Exception {
		this.servletRequest.setServerName("mydomain2.com");
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.com");
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		assertEquals("application/json;charset=UTF-8", this.servletResponse.getContentType());
		assertEquals("no-store, no-cache, must-revalidate, max-age=0", this.servletResponse.getHeader(HttpHeaders.CACHE_CONTROL));
		String body = this.servletResponse.getContentAsString();
		assertEquals("{\"entropy\"", body.substring(0, body.indexOf(':')));
		assertEquals(",\"origins\":[\"*:*\"],\"cookie_needed\":true,\"websocket\":true}", body.substring(body.indexOf(',')));

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com", "http://mydomain2.com", "http://mydomain3.com"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		this.service.setAllowedOrigins(Arrays.asList("*"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		this.servletRequest.setServerName("mydomain3.com");
		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.FORBIDDEN);
	}

	@Test  // SPR-11443
	public void handleInfoGetCorsFilter() throws Exception {

		// Simulate scenario where Filter would have already set CORS headers
		this.servletResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "foobar:123");

		handleRequest("GET", "/echo/info", HttpStatus.OK);

		assertEquals("foobar:123", this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test  // SPR-11919
	@SuppressWarnings("unchecked")
	public void handleInfoGetWildflyNPE() throws Exception {
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		ServletOutputStream ous = mock(ServletOutputStream.class);
		given(mockResponse.getHeaders(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).willThrow(NullPointerException.class);
		given(mockResponse.getOutputStream()).willReturn(ous);
		this.response = new ServletServerHttpResponse(mockResponse);

		handleRequest("GET", "/echo/info", HttpStatus.OK);

		verify(mockResponse, times(1)).getOutputStream();
	}

	@Test  // SPR-12660
	public void handleInfoOptions() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Last-Modified");
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNull(this.service.getCorsConfiguration(this.servletRequest));

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNull(this.service.getCorsConfiguration(this.servletRequest));
	}

	@Test  // SPR-12226 and SPR-12660
	public void handleInfoOptionsWithOrigin() throws Exception {
		this.servletRequest.setServerName("mydomain2.com");
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.com");
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Last-Modified");
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNotNull(this.service.getCorsConfiguration(this.servletRequest));

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNotNull(this.service.getCorsConfiguration(this.servletRequest));

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com", "http://mydomain2.com", "http://mydomain3.com"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNotNull(this.service.getCorsConfiguration(this.servletRequest));

		this.service.setAllowedOrigins(Arrays.asList("*"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNotNull(this.service.getCorsConfiguration(this.servletRequest));

		this.servletRequest.setServerName("mydomain3.com");
		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.FORBIDDEN);
	}

	@Test  // SPR-12283
	public void handleInfoOptionsWithOriginAndCorsHeadersDisabled() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.com");
		this.service.setAllowedOrigins(Arrays.asList("*"));
		this.service.setSuppressCors(true);

		this.servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Last-Modified");
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNull(this.service.getCorsConfiguration(this.servletRequest));

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.FORBIDDEN);
		assertNull(this.service.getCorsConfiguration(this.servletRequest));

		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com", "http://mydomain2.com", "http://mydomain3.com"));
		resetResponseAndHandleRequest("OPTIONS", "/echo/info", HttpStatus.NO_CONTENT);
		assertNull(this.service.getCorsConfiguration(this.servletRequest));
	}

	@Test
	public void handleIframeRequest() throws Exception {
		resetResponseAndHandleRequest("GET", "/echo/iframe.html", HttpStatus.OK);

		assertEquals("text/html;charset=UTF-8", this.servletResponse.getContentType());
		assertTrue(this.servletResponse.getContentAsString().startsWith("<!DOCTYPE html>\n"));
		assertEquals(490, this.servletResponse.getContentLength());
		assertEquals("no-store, no-cache, must-revalidate, max-age=0", this.response.getHeaders().getCacheControl());
		assertEquals("\"0096cbd37f2a5218c33bb0826a7c74cbf\"", this.response.getHeaders().getETag());
	}

	@Test
	public void handleIframeRequestNotModified() throws Exception {
		this.servletRequest.addHeader("If-None-Match", "\"0096cbd37f2a5218c33bb0826a7c74cbf\"");
		resetResponseAndHandleRequest("GET", "/echo/iframe.html", HttpStatus.NOT_MODIFIED);
	}

	@Test
	public void handleRawWebSocketRequest() throws Exception {
		resetResponseAndHandleRequest("GET", "/echo", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());

		resetResponseAndHandleRequest("GET", "/echo/websocket", HttpStatus.OK);
		assertNull("Raw WebSocket should not open a SockJS session", this.service.sessionId);
		assertSame(this.handler, this.service.handler);
	}

	@Test
	public void handleEmptyContentType() throws Exception {
		this.servletRequest.setContentType("");
		resetResponseAndHandleRequest("GET", "/echo/info", HttpStatus.OK);

		assertEquals("Invalid/empty content should have been ignored", 200, this.servletResponse.getStatus());
	}


	private void resetResponseAndHandleRequest(String httpMethod, String uri, HttpStatus httpStatus) throws IOException {
		resetResponse();
		handleRequest(httpMethod, uri, httpStatus);
	}

	private void handleRequest(String httpMethod, String uri, HttpStatus httpStatus) throws IOException {
		setRequest(httpMethod, uri);
		String sockJsPath = uri.substring("/echo".length());
		this.service.handleRequest(this.request, this.response, sockJsPath, this.handler);

		assertEquals(httpStatus.value(), this.servletResponse.getStatus());
	}


	private static class TestSockJsService extends AbstractSockJsService {

		private String sessionId;

		@SuppressWarnings("unused")
		private String transport;

		private WebSocketHandler handler;

		public TestSockJsService(TaskScheduler scheduler) {
			super(scheduler);
		}

		@Override
		protected void handleRawWebSocketRequest(ServerHttpRequest req, ServerHttpResponse res,
				WebSocketHandler handler) throws IOException {
			this.handler = handler;
		}

		@Override
		protected void handleTransportRequest(ServerHttpRequest req, ServerHttpResponse res, WebSocketHandler handler,
				String sessionId, String transport) throws SockJsException {
			this.sessionId = sessionId;
			this.transport = transport;
			this.handler = handler;
		}
	}

}
