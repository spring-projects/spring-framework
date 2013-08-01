/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.support;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsProcessingException;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link AbstractSockJsService}.
 *
 * @author Rossen Stoyanchev
 */
public class AbstractSockJsServiceTests extends AbstractHttpRequestTests {

	private TestSockJsService service;

	private WebSocketHandler handler;


	@Override
	@Before
	public void setUp() {
		super.setUp();
		this.service = new TestSockJsService(new ThreadPoolTaskScheduler());
	}

	@Test
	public void getSockJsPathForGreetingRequest() throws Exception {

		handleRequest("GET", "/a", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());

		handleRequest("GET", "/a/", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());

		this.service.setValidSockJsPrefixes("/b");

		handleRequest("GET", "/a", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/a/", HttpStatus.NOT_FOUND);

		handleRequest("GET", "/b", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());
	}

	@Test
	public void getSockJsPathForInfoRequest() throws Exception {

		handleRequest("GET", "/a/info", HttpStatus.OK);

		assertTrue(this.servletResponse.getContentAsString().startsWith("{\"entropy\":"));

		handleRequest("GET", "/a/server/session/xhr", HttpStatus.OK);

		assertEquals("session", this.service.sessionId);
		assertEquals(TransportType.XHR.value(), this.service.transport);
		assertSame(this.handler, this.service.handler);

		this.service.setValidSockJsPrefixes("/b");

		handleRequest("GET", "/a/info", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/b/info", HttpStatus.OK);

		assertTrue(this.servletResponse.getContentAsString().startsWith("{\"entropy\":"));
	}

	@Test
	public void getSockJsPathForTransportRequest() throws Exception {

		// Info or greeting requests must be first so "/a" is cached as a known prefix
		handleRequest("GET", "/a/info", HttpStatus.OK);
		handleRequest("GET", "/a/server/session/xhr", HttpStatus.OK);

		assertEquals("session", this.service.sessionId);
		assertEquals(TransportType.XHR.value(), this.service.transport);
		assertSame(this.handler, this.service.handler);
	}

	@Test
	public void getSockJsPathForTransportRequestWithConfiguredPrefix() throws Exception {

		this.service.setValidSockJsPrefixes("/a");
		handleRequest("GET", "/a/server/session/xhr", HttpStatus.OK);

		assertEquals("session", this.service.sessionId);
		assertEquals(TransportType.XHR.value(), this.service.transport);
		assertSame(this.handler, this.service.handler);
	}

	@Test
	public void validateRequest() throws Exception {

		this.service.setValidSockJsPrefixes("/echo");

		this.service.setWebSocketsEnabled(false);
		handleRequest("GET", "/echo/server/session/websocket", HttpStatus.NOT_FOUND);

		this.service.setWebSocketsEnabled(true);
		handleRequest("GET", "/echo/server/session/websocket", HttpStatus.OK);

		handleRequest("GET", "/echo//", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/echo///", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/echo/other", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/echo//service/websocket", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/echo/server//websocket", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/echo/server/session/", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/echo/s.erver/session/websocket", HttpStatus.NOT_FOUND);
		handleRequest("GET", "/echo/server/s.ession/websocket", HttpStatus.NOT_FOUND);
	}

	@Test
	public void handleInfoGet() throws Exception {

		handleRequest("GET", "/a/info", HttpStatus.OK);

		assertEquals("application/json;charset=UTF-8", this.servletResponse.getContentType());
		assertEquals("*", this.servletResponse.getHeader("Access-Control-Allow-Origin"));
		assertEquals("true", this.servletResponse.getHeader("Access-Control-Allow-Credentials"));
		assertEquals("no-store, no-cache, must-revalidate, max-age=0", this.servletResponse.getHeader("Cache-Control"));

		String body = this.servletResponse.getContentAsString();
		assertEquals("{\"entropy\"", body.substring(0, body.indexOf(':')));
		assertEquals(",\"origins\":[\"*:*\"],\"cookie_needed\":true,\"websocket\":true}",
				body.substring(body.indexOf(',')));

		this.service.setJsessionIdCookieRequired(false);
		this.service.setWebSocketsEnabled(false);
		handleRequest("GET", "/a/info", HttpStatus.OK);

		body = this.servletResponse.getContentAsString();
		assertEquals(",\"origins\":[\"*:*\"],\"cookie_needed\":false,\"websocket\":false}",
				body.substring(body.indexOf(',')));
	}

	@Test
	public void handleInfoOptions() throws Exception {

		this.servletRequest.addHeader("Access-Control-Request-Headers", "Last-Modified");

		handleRequest("OPTIONS", "/a/info", HttpStatus.NO_CONTENT);

		assertEquals("*", this.servletResponse.getHeader("Access-Control-Allow-Origin"));
		assertEquals("true", this.servletResponse.getHeader("Access-Control-Allow-Credentials"));
		assertEquals("Last-Modified", this.servletResponse.getHeader("Access-Control-Allow-Headers"));
		assertEquals("OPTIONS, GET", this.servletResponse.getHeader("Access-Control-Allow-Methods"));
		assertEquals("31536000", this.servletResponse.getHeader("Access-Control-Max-Age"));
	}

	@Test
	public void handleIframeRequest() throws Exception {

		this.service.setValidSockJsPrefixes("/a");
		handleRequest("GET", "/a/iframe.html", HttpStatus.OK);

		assertEquals("text/html;charset=UTF-8", this.servletResponse.getContentType());
		assertTrue(this.servletResponse.getContentAsString().startsWith("<!DOCTYPE html>\n"));
		assertEquals(496, this.servletResponse.getContentLength());
		assertEquals("public, max-age=31536000", this.response.getHeaders().getCacheControl());
		assertEquals("\"0da1ed070012f304e47b83c81c48ad620\"", this.response.getHeaders().getETag());
	}

	@Test
	public void handleIframeRequestNotModified() throws Exception {

		this.servletRequest.addHeader("If-None-Match", "\"0da1ed070012f304e47b83c81c48ad620\"");

		this.service.setValidSockJsPrefixes("/a");
		handleRequest("GET", "/a/iframe.html", HttpStatus.NOT_MODIFIED);
	}

	@Test
	public void handleRawWebSocketRequest() throws Exception {

		handleRequest("GET", "/a", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());

		handleRequest("GET", "/a/websocket", HttpStatus.OK);
		assertNull("Raw WebSocket should not open a SockJS session", this.service.sessionId);
		assertSame(this.handler, this.service.handler);
	}


	private void handleRequest(String httpMethod, String uri, HttpStatus httpStatus) throws IOException {
		resetResponse();
		setRequest(httpMethod, uri);
		this.service.handleRequest(this.request, this.response, this.handler);

		assertEquals(httpStatus.value(), this.servletResponse.getStatus());
	}

	private static class TestSockJsService extends AbstractSockJsService {

		private String sessionId;

		private String transport;

		private WebSocketHandler handler;

		public TestSockJsService(TaskScheduler scheduler) {
			super(scheduler);
		}

		@Override
		protected void handleRawWebSocketRequest(ServerHttpRequest request,
				ServerHttpResponse response, WebSocketHandler handler) throws IOException {

			this.handler = handler;
		}

		@Override
		protected void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler handler, String sessionId, String transport)
						throws IOException, SockJsProcessingException {

			this.sessionId = sessionId;
			this.transport = transport;
			this.handler = handler;
		}

		@Override
		protected boolean isValidTransportType(String transportType) {
			return TransportType.fromValue(transportType) != null;
		}
	}

}
