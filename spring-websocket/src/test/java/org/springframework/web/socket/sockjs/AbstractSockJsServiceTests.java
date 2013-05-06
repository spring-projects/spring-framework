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

package org.springframework.web.socket.sockjs;

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

import static org.junit.Assert.*;

/**
 * @author Rossen Stoyanchev
 */
public class AbstractSockJsServiceTests extends AbstractHttpRequestTests {

	private TestSockJsService service;

	private WebSocketHandler handler;


	@Before
	public void setUp() {
		super.setUp();
		this.service = new TestSockJsService(new ThreadPoolTaskScheduler());
	}

	@Test
	public void getSockJsPath() throws Exception {

		handleRequest("/echo", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());

		handleRequest("/echo/info", HttpStatus.OK);
		assertTrue(this.servletResponse.getContentAsString().startsWith("{\"entropy\":"));

		handleRequest("/echo/", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());

		handleRequest("/echo/iframe.html", HttpStatus.OK);
		assertTrue(this.servletResponse.getContentAsString().startsWith("<!DOCTYPE html>\n"));

		handleRequest("/echo/websocket", HttpStatus.OK);
		assertNull(this.service.sessionId);
		assertSame(this.handler, this.service.handler);

		handleRequest("/echo/server1/session2/xhr", HttpStatus.OK);
		assertEquals("session2", this.service.sessionId);
		assertEquals(TransportType.XHR, this.service.transportType);
		assertSame(this.handler, this.service.handler);

		handleRequest("/echo/other", HttpStatus.NOT_FOUND);
		handleRequest("/echo//", HttpStatus.NOT_FOUND);
		handleRequest("/echo///", HttpStatus.NOT_FOUND);
	}


	@Test
	public void getSockJsPathGreetingRequest() throws Exception {
		handleRequest("/echo", HttpStatus.OK);
		assertEquals("Welcome to SockJS!\n", this.servletResponse.getContentAsString());
	}

	@Test
	public void getSockJsPathInfoRequest() throws Exception {
		handleRequest("/echo/info", HttpStatus.OK);
		assertTrue(this.servletResponse.getContentAsString().startsWith("{\"entropy\":"));
	}

	@Test
	public void getSockJsPathWithConfiguredPrefix() throws Exception {
		this.service.setValidSockJsPrefixes("/echo");
		handleRequest("/echo/s1/s2/xhr", HttpStatus.OK);
	}

	@Test
	public void getInfoOptions() throws Exception {
		setRequest("OPTIONS", "/echo/info");
		this.service.handleRequest(this.request, this.response, this.handler);

		assertEquals(204, servletResponse.getStatus());
	}


	private void handleRequest(String uri, HttpStatus httpStatus) throws IOException {
		resetResponse();
		setRequest("GET", uri);
		this.service.handleRequest(this.request, this.response, this.handler);

		assertEquals(httpStatus.value(), this.servletResponse.getStatus());
	}

	private static class TestSockJsService extends AbstractSockJsService {

		private String sessionId;

		private TransportType transportType;

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
		protected void handleTransportRequest(ServerHttpRequest request,
				ServerHttpResponse response, String sessionId,
				TransportType transportType, WebSocketHandler handler)
				throws IOException, TransportErrorException {

			this.sessionId = sessionId;
			this.transportType = transportType;
			this.handler = handler;
		}
	}

}
