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

package org.springframework.web.socket.sockjs.transport;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.StubSockJsConfig;
import org.springframework.web.socket.sockjs.TestSockJsSession;
import org.springframework.web.socket.sockjs.TransportErrorException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link AbstractHttpReceivingTransportHandler} and sub-classes
 * {@link XhrTransportHandler} and {@link JsonpTransportHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpReceivingTransportHandlerTests  extends AbstractHttpRequestTests {


	@Override
	@Before
	public void setUp() {
		super.setUp();
	}

	@Test
	public void readMessagesXhr() throws Exception {
		this.servletRequest.setContent("[\"x\"]".getBytes("UTF-8"));
		handleRequest(new XhrTransportHandler());

		assertEquals(204, this.servletResponse.getStatus());
	}

	@Test
	public void readMessagesJsonp() throws Exception {
		this.servletRequest.setContent("[\"x\"]".getBytes("UTF-8"));
		handleRequest(new JsonpTransportHandler());

		assertEquals(200, this.servletResponse.getStatus());
		assertEquals("ok", this.servletResponse.getContentAsString());
	}

	@Test
	public void readMessagesJsonpFormEncoded() throws Exception {
		this.servletRequest.setContent("d=[\"x\"]".getBytes("UTF-8"));
		this.servletRequest.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		handleRequest(new JsonpTransportHandler());

		assertEquals(200, this.servletResponse.getStatus());
		assertEquals("ok", this.servletResponse.getContentAsString());
	}

	// SPR-10621

	@Test
	public void readMessagesJsonpFormEncodedWithEncoding() throws Exception {
		this.servletRequest.setContent("d=[\"x\"]".getBytes("UTF-8"));
		this.servletRequest.setContentType("application/x-www-form-urlencoded;charset=UTF-8");
		handleRequest(new JsonpTransportHandler());

		assertEquals(200, this.servletResponse.getStatus());
		assertEquals("ok", this.servletResponse.getContentAsString());
	}

	@Test
	public void readMessagesBadContent() throws Exception {
		this.servletRequest.setContent("".getBytes("UTF-8"));
		handleRequestAndExpectFailure();

		this.servletRequest.setContent("[\"x]".getBytes("UTF-8"));
		handleRequestAndExpectFailure();
	}

	@Test
	public void readMessagesNoSession() throws Exception {
		WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);
		new XhrTransportHandler().handleRequest(this.request, this.response, webSocketHandler, null);

		assertEquals(404, this.servletResponse.getStatus());
	}

	@Test
	public void delegateMessageException() throws Exception {

		StubSockJsConfig sockJsConfig = new StubSockJsConfig();

		this.servletRequest.setContent("[\"x\"]".getBytes("UTF-8"));

		WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);
		TestSockJsSession session = new TestSockJsSession("1", sockJsConfig, webSocketHandler);
		session.delegateConnectionEstablished();

		doThrow(new Exception()).when(webSocketHandler).handleMessage(session, new TextMessage("x"));

		try {
			XhrTransportHandler transportHandler = new XhrTransportHandler();
			transportHandler.setSockJsConfiguration(sockJsConfig);
			transportHandler.handleRequest(this.request, this.response, webSocketHandler, session);
			fail("Expected exception");
		}
		catch (TransportErrorException ex) {
			assertEquals(CloseStatus.SERVER_ERROR, session.getStatus());
		}
	}


	private void handleRequest(AbstractHttpReceivingTransportHandler transportHandler)
			throws Exception {

		WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);
		AbstractSockJsSession session = new TestSockJsSession("1", new StubSockJsConfig(), webSocketHandler);

		transportHandler.setSockJsConfiguration(new StubSockJsConfig());
		transportHandler.handleRequest(this.request, this.response, webSocketHandler, session);

		assertEquals("text/plain;charset=UTF-8", this.response.getHeaders().getContentType().toString());
		verify(webSocketHandler).handleMessage(session, new TextMessage("x"));
	}

	private void handleRequestAndExpectFailure() throws Exception {

		resetResponse();

		WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);
		AbstractSockJsSession session = new TestSockJsSession("1", new StubSockJsConfig(), webSocketHandler);

		new XhrTransportHandler().handleRequest(this.request, this.response, webSocketHandler, session);

		assertEquals(500, this.servletResponse.getStatus());
		verifyNoMoreInteractions(webSocketHandler);
	}

}
