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

import java.sql.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.SockJsFrame;
import org.springframework.web.socket.sockjs.SockJsFrame.FrameFormat;
import org.springframework.web.socket.sockjs.StubSockJsConfig;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link AbstractHttpSendingTransportHandler} and sub-classes.
 *
 * @author Rossen Stoyanchev
 */
public class HttpSendingTransportHandlerTests  extends AbstractHttpRequestTests {

	private WebSocketHandler webSocketHandler;

	private StubSockJsConfig sockJsConfig;

	private TaskScheduler taskScheduler;


	@Override
	@Before
	public void setUp() {
		super.setUp();

		this.webSocketHandler = mock(WebSocketHandler.class);
		this.taskScheduler = mock(TaskScheduler.class);

		this.sockJsConfig = new StubSockJsConfig();
		this.sockJsConfig.setTaskScheduler(this.taskScheduler);
	}

	@Test
	public void handleRequestXhr() throws Exception {

		XhrPollingTransportHandler transportHandler = new XhrPollingTransportHandler();
		transportHandler.setSockJsConfiguration(sockJsConfig);

		AbstractSockJsSession session = transportHandler.createSession("1", webSocketHandler);
		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertEquals("application/javascript;charset=UTF-8", this.response.getHeaders().getContentType().toString());
		assertEquals("o\n", this.servletResponse.getContentAsString());
		assertFalse("Polling request should complete after open frame", this.servletRequest.isAsyncStarted());
		verify(webSocketHandler).afterConnectionEstablished(session);

		resetResponse();
		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertTrue("Polling request should remain open", this.servletRequest.isAsyncStarted());
		verify(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));

		resetRequestAndResponse();
		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertFalse("Request should have been rejected", this.servletRequest.isAsyncStarted());
		assertEquals("c[2010,\"Another connection still open\"]\n", this.servletResponse.getContentAsString());
	}

	@Test
	public void jsonpTransport() throws Exception {

		JsonpPollingTransportHandler transportHandler = new JsonpPollingTransportHandler();
		transportHandler.setSockJsConfiguration(sockJsConfig);
		PollingSockJsSession session = transportHandler.createSession("1", webSocketHandler);

		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertEquals(500, this.servletResponse.getStatus());
		assertEquals("\"callback\" parameter required", this.servletResponse.getContentAsString());

		resetRequestAndResponse();
		this.servletRequest.addParameter("c", "callback");
		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertEquals("application/javascript;charset=UTF-8", this.response.getHeaders().getContentType().toString());
		assertFalse("Polling request should complete after open frame", this.servletRequest.isAsyncStarted());
		verify(webSocketHandler).afterConnectionEstablished(session);
	}

	@Test
	public void handleRequestXhrStreaming() throws Exception {

		XhrStreamingTransportHandler transportHandler = new XhrStreamingTransportHandler();
		transportHandler.setSockJsConfiguration(sockJsConfig);
		AbstractSockJsSession session = transportHandler.createSession("1", webSocketHandler);

		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertEquals("application/javascript;charset=UTF-8", this.response.getHeaders().getContentType().toString());
		assertTrue("Streaming request not started", this.servletRequest.isAsyncStarted());
		verify(webSocketHandler).afterConnectionEstablished(session);
	}

	@Test
	public void htmlFileTransport() throws Exception {

		HtmlFileTransportHandler transportHandler = new HtmlFileTransportHandler();
		transportHandler.setSockJsConfiguration(sockJsConfig);
		StreamingSockJsSession session = transportHandler.createSession("1", webSocketHandler);

		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertEquals(500, this.servletResponse.getStatus());
		assertEquals("\"callback\" parameter required", this.servletResponse.getContentAsString());

		resetRequestAndResponse();
		this.servletRequest.addParameter("c", "callback");
		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertEquals("text/html;charset=UTF-8", this.response.getHeaders().getContentType().toString());
		assertTrue("Streaming request not started", this.servletRequest.isAsyncStarted());
		verify(webSocketHandler).afterConnectionEstablished(session);
	}

	@Test
	public void eventSourceTransport() throws Exception {

		EventSourceTransportHandler transportHandler = new EventSourceTransportHandler();
		transportHandler.setSockJsConfiguration(sockJsConfig);
		StreamingSockJsSession session = transportHandler.createSession("1", webSocketHandler);

		transportHandler.handleRequest(request, response, webSocketHandler, session);

		assertEquals("text/event-stream;charset=UTF-8", this.response.getHeaders().getContentType().toString());
		assertTrue("Streaming request not started", this.servletRequest.isAsyncStarted());
		verify(webSocketHandler).afterConnectionEstablished(session);
	}

	@Test
	public void frameFormats() throws Exception {

		this.servletRequest.addParameter("c", "callback");

		SockJsFrame frame = SockJsFrame.openFrame();

		FrameFormat format = new XhrPollingTransportHandler().getFrameFormat(request);
		SockJsFrame formatted = format.format(frame);
		assertEquals(frame.getContent() + "\n", formatted.getContent());

		format = new XhrStreamingTransportHandler().getFrameFormat(request);
		formatted = format.format(frame);
		assertEquals(frame.getContent() + "\n", formatted.getContent());

		format = new HtmlFileTransportHandler().getFrameFormat(request);
		formatted = format.format(frame);
		assertEquals("<script>\np(\"" + frame.getContent() + "\");\n</script>\r\n", formatted.getContent());

		format = new EventSourceTransportHandler().getFrameFormat(request);
		formatted = format.format(frame);
		assertEquals("data: " + frame.getContent() + "\r\n\r\n", formatted.getContent());

		format = new JsonpPollingTransportHandler().getFrameFormat(request);
		formatted = format.format(frame);
		assertEquals("callback(\"" + frame.getContent() + "\");\r\n", formatted.getContent());
	}

}
