/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.TestPrincipal;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.sockjs.transport.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.StubSockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.TestSockJsSession;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link DefaultSockJsService}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Ben Kiefer
 */
public class DefaultSockJsServiceTests extends AbstractHttpRequestTests {

	private static final String sockJsPrefix = "/mysockjs";

	private static final String sessionId = "session1";

	private static final String sessionUrlPrefix = "/server1/" + sessionId + "/";


	@Mock private SessionCreatingTransportHandler xhrHandler;

	@Mock private TransportHandler xhrSendHandler;

	@Mock private HandshakeTransportHandler wsTransportHandler;

	@Mock private WebSocketHandler wsHandler;

	@Mock private TaskScheduler taskScheduler;

	private TestSockJsSession session;

	private TransportHandlingSockJsService service;


	@Before
	public void setup() {
		super.setup();
		MockitoAnnotations.initMocks(this);

		Map<String, Object> attributes = Collections.emptyMap();
		this.session = new TestSockJsSession(sessionId, new StubSockJsServiceConfig(), this.wsHandler, attributes);

		given(this.xhrHandler.getTransportType()).willReturn(TransportType.XHR);
		given(this.xhrHandler.createSession(sessionId, this.wsHandler, attributes)).willReturn(this.session);
		given(this.xhrSendHandler.getTransportType()).willReturn(TransportType.XHR_SEND);
		given(this.wsTransportHandler.getTransportType()).willReturn(TransportType.WEBSOCKET);

		this.service = new TransportHandlingSockJsService(this.taskScheduler, this.xhrHandler, this.xhrSendHandler);
	}


	@Test
	public void defaultTransportHandlers() {
		DefaultSockJsService service = new DefaultSockJsService(mock(TaskScheduler.class));
		Map<TransportType, TransportHandler> handlers = service.getTransportHandlers();

		assertEquals(6, handlers.size());
		assertNotNull(handlers.get(TransportType.WEBSOCKET));
		assertNotNull(handlers.get(TransportType.XHR));
		assertNotNull(handlers.get(TransportType.XHR_SEND));
		assertNotNull(handlers.get(TransportType.XHR_STREAMING));
		assertNotNull(handlers.get(TransportType.HTML_FILE));
		assertNotNull(handlers.get(TransportType.EVENT_SOURCE));
	}

	@Test
	public void defaultTransportHandlersWithOverride() {
		XhrReceivingTransportHandler xhrHandler = new XhrReceivingTransportHandler();

		DefaultSockJsService service = new DefaultSockJsService(mock(TaskScheduler.class), xhrHandler);
		Map<TransportType, TransportHandler> handlers = service.getTransportHandlers();

		assertEquals(6, handlers.size());
		assertSame(xhrHandler, handlers.get(xhrHandler.getTransportType()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidAllowedOrigins() {
		this.service.setAllowedOrigins(null);
	}

	@Test
	public void customizedTransportHandlerList() {
		TransportHandlingSockJsService service = new TransportHandlingSockJsService(
				mock(TaskScheduler.class), new XhrPollingTransportHandler(), new XhrReceivingTransportHandler());
		Map<TransportType, TransportHandler> actualHandlers = service.getTransportHandlers();

		assertEquals(2, actualHandlers.size());
	}

	@Test
	public void handleTransportRequestXhr() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus());
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);
		verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(service.getDisconnectDelay()));

		assertEquals("no-store, no-cache, must-revalidate, max-age=0", this.response.getHeaders().getCacheControl());
		assertNull(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertNull(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test  // SPR-12226
	public void handleTransportRequestXhrAllowedOriginsMatch() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com", "http://mydomain2.com"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain1.com");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus());
	}

	@Test  // SPR-12226
	public void handleTransportRequestXhrAllowedOriginsNoMatch() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com", "http://mydomain2.com"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain3.com");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(403, this.servletResponse.getStatus());
	}

	@Test  // SPR-13464
	public void handleTransportRequestXhrSameOrigin() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.com");
		this.servletRequest.setServerName("mydomain2.com");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus());
	}

	@Test  // SPR-13545
	public void handleInvalidTransportType() throws Exception {
		String sockJsPath = sessionUrlPrefix + "invalid";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("http://mydomain1.com"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.com");
		this.servletRequest.setServerName("mydomain2.com");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(404, this.servletResponse.getStatus());
	}

	@Test
	public void handleTransportRequestXhrOptions() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("OPTIONS", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(204, this.servletResponse.getStatus());
		assertNull(this.servletResponse.getHeader("Access-Control-Allow-Origin"));
		assertNull(this.servletResponse.getHeader("Access-Control-Allow-Credentials"));
		assertNull(this.servletResponse.getHeader("Access-Control-Allow-Methods"));
	}

	@Test
	public void handleTransportRequestNoSuitableHandler() throws Exception {
		String sockJsPath = sessionUrlPrefix + "eventsource";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(404, this.servletResponse.getStatus());
	}

	@Test
	public void handleTransportRequestXhrSend() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr_send";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(404, this.servletResponse.getStatus()); // no session yet

		resetResponse();
		sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus()); // session created
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);

		resetResponse();
		sockJsPath = sessionUrlPrefix + "xhr_send";
		setRequest("POST", sockJsPrefix + sockJsPath);
		given(this.xhrSendHandler.checkSessionType(this.session)).willReturn(true);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus()); // session exists
		verify(this.xhrSendHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);
	}

	@Test
	public void handleTransportRequestXhrSendWithDifferentUser() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(200, this.servletResponse.getStatus()); // session created
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);

		this.session.setPrincipal(new TestPrincipal("little red riding hood"));
		this.servletRequest.setUserPrincipal(new TestPrincipal("wolf"));

		resetResponse();
		reset(this.xhrSendHandler);
		sockJsPath = sessionUrlPrefix + "xhr_send";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertEquals(404, this.servletResponse.getStatus());
		verifyNoMoreInteractions(this.xhrSendHandler);
	}

	@Test
	public void handleTransportRequestWebsocket() throws Exception {
		TransportHandlingSockJsService wsService = new TransportHandlingSockJsService(
				this.taskScheduler, this.wsTransportHandler);
		String sockJsPath = "/websocket";
		setRequest("GET", sockJsPrefix + sockJsPath);
		wsService.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertNotEquals(403, this.servletResponse.getStatus());

		resetRequestAndResponse();
		List<String> allowed = Collections.singletonList("http://mydomain1.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		wsService.setHandshakeInterceptors(Collections.singletonList(interceptor));
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain1.com");
		wsService.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertNotEquals(403, this.servletResponse.getStatus());

		resetRequestAndResponse();
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.com");
		wsService.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertEquals(403, this.servletResponse.getStatus());
	}

	@Test
	public void handleTransportRequestIframe() throws Exception {
		String sockJsPath = "/iframe.html";
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertNotEquals(404, this.servletResponse.getStatus());
		assertEquals("SAMEORIGIN", this.servletResponse.getHeader("X-Frame-Options"));

		resetRequestAndResponse();
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Collections.singletonList("http://mydomain1.com"));
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertEquals(404, this.servletResponse.getStatus());
		assertNull(this.servletResponse.getHeader("X-Frame-Options"));

		resetRequestAndResponse();
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Collections.singletonList("*"));
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertNotEquals(404, this.servletResponse.getStatus());
		assertNull(this.servletResponse.getHeader("X-Frame-Options"));
	}


	interface SessionCreatingTransportHandler extends TransportHandler, SockJsSessionFactory {
	}

	interface HandshakeTransportHandler extends TransportHandler, HandshakeHandler {
	}

}
