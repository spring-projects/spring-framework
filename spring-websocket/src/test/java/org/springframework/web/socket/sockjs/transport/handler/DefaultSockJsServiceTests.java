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

package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.StubSockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.TestSockJsSession;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link DefaultSockJsService}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultSockJsServiceTests extends AbstractHttpRequestTests {

	private static final String sockJsPrefix = "mysockjs";

	private static final String sessionId = "session1";

	private static final String sessionUrlPrefix = "/mysockjs/server1/" + sessionId + "/";


	@Mock private SessionCreatingTransportHandler xhrHandler;

	@Mock private TransportHandler xhrSendHandler;

	@Mock private WebSocketHandler wsHandler;

	@Mock private TaskScheduler taskScheduler;

	private TestSockJsSession session;

	private DefaultSockJsService service;


	@Before
	public void setup() {

		super.setUp();

		MockitoAnnotations.initMocks(this);

		this.session = new TestSockJsSession(sessionId, new StubSockJsServiceConfig(), this.wsHandler);

		when(this.xhrHandler.getTransportType()).thenReturn(TransportType.XHR);
		when(this.xhrHandler.createSession(sessionId, this.wsHandler)).thenReturn(this.session);
		when(this.xhrSendHandler.getTransportType()).thenReturn(TransportType.XHR_SEND);

		this.service = new DefaultSockJsService(this.taskScheduler,
				Arrays.<TransportHandler>asList(this.xhrHandler, this.xhrSendHandler));
		this.service.setValidSockJsPrefixes(sockJsPrefix);
	}

	@Test
	public void defaultTransportHandlers() {

		DefaultSockJsService service = new DefaultSockJsService(mock(TaskScheduler.class));
		Map<TransportType, TransportHandler> handlers = service.getTransportHandlers();

		assertEquals(8, handlers.size());
		assertNotNull(handlers.get(TransportType.WEBSOCKET));
		assertNotNull(handlers.get(TransportType.XHR));
		assertNotNull(handlers.get(TransportType.XHR_SEND));
		assertNotNull(handlers.get(TransportType.XHR_STREAMING));
		assertNotNull(handlers.get(TransportType.JSONP));
		assertNotNull(handlers.get(TransportType.JSONP_SEND));
		assertNotNull(handlers.get(TransportType.HTML_FILE));
		assertNotNull(handlers.get(TransportType.EVENT_SOURCE));
	}

	@Test
	public void handleTransportRequestXhr() throws Exception {

		setRequest("POST", sessionUrlPrefix + "xhr");
		this.service.handleRequest(this.request, this.response, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus());
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);
		verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(service.getDisconnectDelay()));

		assertEquals("no-store, no-cache, must-revalidate, max-age=0", this.response.getHeaders().getCacheControl());
		assertEquals("*", this.response.getHeaders().getFirst("Access-Control-Allow-Origin"));
		assertEquals("true", this.response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
	}

	@Test
	public void handleTransportRequestXhrOptions() throws Exception {

		setRequest("OPTIONS", sessionUrlPrefix + "xhr");
		this.service.handleRequest(this.request, this.response, this.wsHandler);

		assertEquals(204, this.servletResponse.getStatus());
		assertEquals("*", this.response.getHeaders().getFirst("Access-Control-Allow-Origin"));
		assertEquals("true", this.response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
		assertEquals("OPTIONS, POST", this.response.getHeaders().getFirst("Access-Control-Allow-Methods"));
	}

	@Test
	public void dummySessionCookieEnabled() throws Exception {

		setRequest("POST", sessionUrlPrefix + "xhr");
		this.service.setDummySessionCookieEnabled(true);
		this.service.handleRequest(this.request, this.response, this.wsHandler);
		this.response.flush();

		assertEquals(200, this.servletResponse.getStatus());
		assertEquals("JSESSIONID=dummy;path=/", this.servletResponse.getHeader("Set-Cookie"));
	}

	@Test
	public void dummySessionCookieDisabled() throws Exception {

		setRequest("POST", sessionUrlPrefix + "xhr");
		this.service.setDummySessionCookieEnabled(false);
		this.service.handleTransportRequest(this.request, this.response, this.wsHandler, sessionId, "xhr");

		assertEquals(200, this.servletResponse.getStatus());
		assertNull(this.servletResponse.getHeader("Set-Cookie"));
	}

	@Test
	public void dummySessionCookieReuseRequestCookieValue() throws Exception {

		setRequest("POST", sessionUrlPrefix + "xhr");
		this.servletRequest.addHeader("Cookie", "JSESSIONID=123456789");
		this.service.handleTransportRequest(this.request, this.response, this.wsHandler, sessionId, "xhr");

		assertEquals(200, this.servletResponse.getStatus());
		assertNull(this.servletResponse.getHeader("Set-Cookie"));
	}

	@Test
	public void handleTransportRequestNoSuitableHandler() throws Exception {

		setRequest("POST", sessionUrlPrefix + "eventsource");
		this.service.handleRequest(this.request, this.response, this.wsHandler);

		assertEquals(404, this.servletResponse.getStatus());
	}

	@Test
	public void handleTransportRequestXhrSend() throws Exception {

		setRequest("POST", sessionUrlPrefix + "xhr_send");
		this.service.handleRequest(this.request, this.response, this.wsHandler);

		assertEquals(404, this.servletResponse.getStatus()); // no session yet

		resetResponse();
		setRequest("POST", sessionUrlPrefix + "xhr");
		this.service.handleRequest(this.request, this.response, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus()); // session created
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);

		resetResponse();
		setRequest("POST", sessionUrlPrefix + "xhr_send");
		this.service.handleRequest(this.request, this.response, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus()); // session exists
		verify(this.xhrSendHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);
	}


	interface SessionCreatingTransportHandler extends TransportHandler, SockJsSessionFactory {
	}

}
