/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.sockjs.transport.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.StubSockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.TestSockJsSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test fixture for {@link DefaultSockJsService}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Ben Kiefer
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultSockJsServiceTests extends AbstractHttpRequestTests {

	private static final String sockJsPrefix = "/mysockjs";

	private static final String sessionId = "session1";

	private static final String sessionUrlPrefix = "/server1/" + sessionId + "/";


	@Mock
	private SessionCreatingTransportHandler xhrHandler;

	@Mock
	private TransportHandler xhrSendHandler;

	@Mock
	private HandshakeTransportHandler wsTransportHandler;

	@Mock
	private WebSocketHandler wsHandler;

	@Mock
	private TaskScheduler taskScheduler;

	private TestSockJsSession session;

	private TransportHandlingSockJsService service;


	@Override
	@BeforeEach
	public void setup() {
		super.setup();

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

		assertThat(handlers.size()).isEqualTo(6);
		assertThat(handlers.get(TransportType.WEBSOCKET)).isNotNull();
		assertThat(handlers.get(TransportType.XHR)).isNotNull();
		assertThat(handlers.get(TransportType.XHR_SEND)).isNotNull();
		assertThat(handlers.get(TransportType.XHR_STREAMING)).isNotNull();
		assertThat(handlers.get(TransportType.HTML_FILE)).isNotNull();
		assertThat(handlers.get(TransportType.EVENT_SOURCE)).isNotNull();
	}

	@Test
	public void defaultTransportHandlersWithOverride() {
		XhrReceivingTransportHandler xhrHandler = new XhrReceivingTransportHandler();

		DefaultSockJsService service = new DefaultSockJsService(mock(TaskScheduler.class), xhrHandler);
		Map<TransportType, TransportHandler> handlers = service.getTransportHandlers();

		assertThat(handlers.size()).isEqualTo(6);
		assertThat(handlers.get(xhrHandler.getTransportType())).isSameAs(xhrHandler);
	}

	@Test
	public void invalidAllowedOrigins() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.service.setAllowedOrigins(null));
	}

	@Test
	public void customizedTransportHandlerList() {
		TransportHandlingSockJsService service = new TransportHandlingSockJsService(
				mock(TaskScheduler.class), new XhrPollingTransportHandler(), new XhrReceivingTransportHandler());
		Map<TransportType, TransportHandler> actualHandlers = service.getTransportHandlers();

		assertThat(actualHandlers.size()).isEqualTo(2);
	}

	@Test
	public void handleTransportRequestXhr() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);
		verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(service.getDisconnectDelay()));

		assertThat(this.response.getHeaders().getCacheControl()).isEqualTo("no-store, no-cache, must-revalidate, max-age=0");
		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
	}

	@Test  // SPR-12226
	public void handleTransportRequestXhrAllowedOriginsMatch() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("https://mydomain1.example", "https://mydomain2.example"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
	}

	@Test  // SPR-12226
	public void handleTransportRequestXhrAllowedOriginsNoMatch() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("https://mydomain1.example", "https://mydomain2.example"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain3.example");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(403);
	}

	@Test  // SPR-13464
	public void handleTransportRequestXhrSameOrigin() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("https://mydomain1.example"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		this.servletRequest.setServerName("mydomain2.example");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
	}

	@Test  // SPR-13545
	public void handleInvalidTransportType() throws Exception {
		String sockJsPath = sessionUrlPrefix + "invalid";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Arrays.asList("https://mydomain1.example"));
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
		this.servletRequest.setServerName("mydomain2.example");
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(404);
	}

	@Test
	public void handleTransportRequestXhrOptions() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("OPTIONS", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(204);
		assertThat(this.servletResponse.getHeader("Access-Control-Allow-Origin")).isNull();
		assertThat(this.servletResponse.getHeader("Access-Control-Allow-Credentials")).isNull();
		assertThat(this.servletResponse.getHeader("Access-Control-Allow-Methods")).isNull();
	}

	@Test
	public void handleTransportRequestNoSuitableHandler() throws Exception {
		String sockJsPath = sessionUrlPrefix + "eventsource";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(404);
	}

	@Test
	public void handleTransportRequestXhrSend() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr_send";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		// no session yet
		assertThat(this.servletResponse.getStatus()).isEqualTo(404);

		resetResponse();
		sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		// session created
		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);

		resetResponse();
		sockJsPath = sessionUrlPrefix + "xhr_send";
		setRequest("POST", sockJsPrefix + sockJsPath);
		given(this.xhrSendHandler.checkSessionType(this.session)).willReturn(true);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		// session exists
		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
		verify(this.xhrSendHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);
	}

	@Test
	public void handleTransportRequestXhrSendWithDifferentUser() throws Exception {
		String sockJsPath = sessionUrlPrefix + "xhr";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		// session created
		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
		verify(this.xhrHandler).handleRequest(this.request, this.response, this.wsHandler, this.session);

		this.session.setPrincipal(new TestPrincipal("little red riding hood"));
		this.servletRequest.setUserPrincipal(new TestPrincipal("wolf"));

		resetResponse();
		reset(this.xhrSendHandler);
		sockJsPath = sessionUrlPrefix + "xhr_send";
		setRequest("POST", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);

		assertThat(this.servletResponse.getStatus()).isEqualTo(404);
		verifyNoMoreInteractions(this.xhrSendHandler);
	}

	@Test
	public void handleTransportRequestWebsocket() throws Exception {
		TransportHandlingSockJsService wsService = new TransportHandlingSockJsService(
				this.taskScheduler, this.wsTransportHandler);
		String sockJsPath = "/websocket";
		setRequest("GET", sockJsPrefix + sockJsPath);
		wsService.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertThat(this.servletResponse.getStatus()).isNotEqualTo((long) 403);

		resetRequestAndResponse();
		List<String> allowed = Collections.singletonList("https://mydomain1.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		wsService.setHandshakeInterceptors(Collections.singletonList(interceptor));
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		wsService.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertThat(this.servletResponse.getStatus()).isNotEqualTo((long) 403);

		resetRequestAndResponse();
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
		wsService.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertThat(this.servletResponse.getStatus()).isEqualTo(403);
	}

	@Test
	public void handleTransportRequestIframe() throws Exception {
		String sockJsPath = "/iframe.html";
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertThat(this.servletResponse.getStatus()).isNotEqualTo((long) 404);
		assertThat(this.servletResponse.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");

		resetRequestAndResponse();
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Collections.singletonList("https://mydomain1.example"));
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertThat(this.servletResponse.getStatus()).isEqualTo(404);
		assertThat(this.servletResponse.getHeader("X-Frame-Options")).isNull();

		resetRequestAndResponse();
		setRequest("GET", sockJsPrefix + sockJsPath);
		this.service.setAllowedOrigins(Collections.singletonList("*"));
		this.service.handleRequest(this.request, this.response, sockJsPath, this.wsHandler);
		assertThat(this.servletResponse.getStatus()).isNotEqualTo((long) 404);
		assertThat(this.servletResponse.getHeader("X-Frame-Options")).isNull();
	}


	interface SessionCreatingTransportHandler extends TransportHandler, SockJsSessionFactory {
	}

	interface HandshakeTransportHandler extends TransportHandler, HandshakeHandler {
	}

}
