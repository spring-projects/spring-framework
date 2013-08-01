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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsProcessingException;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.transport.session.AbstractSockJsSession;
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

	@Override
	@Before
	public void setUp() {
		super.setUp();
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

		setRequest("POST", "/a/server/session/xhr");

		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		StubXhrTransportHandler xhrHandler = new StubXhrTransportHandler();
		Set<TransportHandler> transportHandlers = Collections.<TransportHandler>singleton(xhrHandler);
		WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);

		DefaultSockJsService service = new DefaultSockJsService(taskScheduler, transportHandlers);
		service.handleTransportRequest(this.request, this.response, webSocketHandler, "123", TransportType.XHR.value());

		assertEquals(200, this.servletResponse.getStatus());
		assertNotNull(xhrHandler.session);
		assertSame(webSocketHandler, xhrHandler.webSocketHandler);

		verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(service.getDisconnectDelay()));

		assertEquals("no-store, no-cache, must-revalidate, max-age=0", this.response.getHeaders().getCacheControl());
		assertEquals("JSESSIONID=dummy;path=/", this.response.getHeaders().getFirst("Set-Cookie"));
		assertEquals("*", this.response.getHeaders().getFirst("Access-Control-Allow-Origin"));
		assertEquals("true", this.response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
	}

	@Test
	public void handleTransportRequestXhrOptions() throws Exception {

		setRequest("OPTIONS", "/a/server/session/xhr");

		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		StubXhrTransportHandler xhrHandler = new StubXhrTransportHandler();
		Set<TransportHandler> transportHandlers = Collections.<TransportHandler>singleton(xhrHandler);

		DefaultSockJsService service = new DefaultSockJsService(taskScheduler, transportHandlers);
		service.handleTransportRequest(this.request, this.response, null, "123", TransportType.XHR.value());

		assertEquals(204, this.servletResponse.getStatus());
		assertEquals("*", this.response.getHeaders().getFirst("Access-Control-Allow-Origin"));
		assertEquals("true", this.response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
		assertEquals("OPTIONS, POST", this.response.getHeaders().getFirst("Access-Control-Allow-Methods"));
	}

	@Test
	public void handleTransportRequestNoSuitableHandler() throws Exception {

		setRequest("POST", "/a/server/session/xhr");

		Set<TransportHandler> transportHandlers = new HashSet<>();
		DefaultSockJsService service = new DefaultSockJsService(mock(TaskScheduler.class), transportHandlers);
		service.handleTransportRequest(this.request, this.response, null, "123", TransportType.XHR.value());

		assertEquals(404, this.servletResponse.getStatus());
	}

	@Test
	public void handleTransportRequestXhrSend() throws Exception {

		this.servletRequest.setMethod("POST");

		Set<TransportHandler> transportHandlers = new HashSet<>();
		transportHandlers.add(new StubXhrTransportHandler());
		transportHandlers.add(new StubXhrSendTransportHandler());
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);
		DefaultSockJsService service = new DefaultSockJsService(mock(TaskScheduler.class), transportHandlers);

		service.handleTransportRequest(this.request, this.response, wsHandler, "123", TransportType.XHR_SEND.value());

		assertEquals(404, this.servletResponse.getStatus()); // dropped (no session)

		resetResponse();
		service.handleTransportRequest(this.request, this.response, wsHandler, "123", TransportType.XHR.value());

		assertEquals(200, this.servletResponse.getStatus());

		resetResponse();
		service.handleTransportRequest(this.request, this.response, wsHandler, "123", TransportType.XHR_SEND.value());

		assertEquals(200, this.servletResponse.getStatus());
	}


	private static class StubXhrTransportHandler implements TransportHandler, SockJsSessionFactory {

		WebSocketHandler webSocketHandler;

		WebSocketSession session;

		@Override
		public TransportType getTransportType() {
			return TransportType.XHR;
		}

		@Override
		public void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler handler, WebSocketSession session) throws SockJsProcessingException {

			this.webSocketHandler = handler;
			this.session = session;
		}

		@Override
		public AbstractSockJsSession createSession(String sessionId, WebSocketHandler webSocketHandler) {
			return new TestSockJsSession(sessionId, new StubSockJsServiceConfig(), webSocketHandler);
		}

	}

	private static class StubXhrSendTransportHandler implements TransportHandler {

		@Override
		public TransportType getTransportType() {
			return TransportType.XHR_SEND;
		}

		@Override
		public void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler handler, WebSocketSession session) throws SockJsProcessingException {

			if (session == null) {
				response.setStatusCode(HttpStatus.NOT_FOUND);
			}
		}
	}

}
