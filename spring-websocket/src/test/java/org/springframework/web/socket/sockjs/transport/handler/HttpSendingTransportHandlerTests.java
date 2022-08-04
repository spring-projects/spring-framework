/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.session.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.StreamingSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.StubSockJsServiceConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link AbstractHttpSendingTransportHandler} and subclasses.
 *
 * @author Rossen Stoyanchev
 */
public class HttpSendingTransportHandlerTests  extends AbstractHttpRequestTests {

	private WebSocketHandler webSocketHandler;

	private StubSockJsServiceConfig sockJsConfig;

	private TaskScheduler taskScheduler;


	@Override
	@BeforeEach
	public void setup() {
		super.setup();

		this.webSocketHandler = mock(WebSocketHandler.class);
		this.taskScheduler = mock(TaskScheduler.class);

		this.sockJsConfig = new StubSockJsServiceConfig();
		this.sockJsConfig.setTaskScheduler(this.taskScheduler);

		setRequest("POST", "/");
	}


	@Test
	public void handleRequestXhr() throws Exception {
		XhrPollingTransportHandler transportHandler = new XhrPollingTransportHandler();
		transportHandler.initialize(this.sockJsConfig);

		AbstractSockJsSession session = transportHandler.createSession("1", this.webSocketHandler, null);
		transportHandler.handleRequest(this.request, this.response, this.webSocketHandler, session);

		assertThat(this.response.getHeaders().getContentType().toString()).isEqualTo("application/javascript;charset=UTF-8");
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("o\n");
		assertThat(this.servletRequest.isAsyncStarted()).as("Polling request should complete after open frame").isFalse();
		verify(this.webSocketHandler).afterConnectionEstablished(session);

		resetRequestAndResponse();
		transportHandler.handleRequest(this.request, this.response, this.webSocketHandler, session);

		assertThat(this.servletRequest.isAsyncStarted()).as("Polling request should remain open").isTrue();
		verify(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));

		resetRequestAndResponse();
		transportHandler.handleRequest(this.request, this.response, this.webSocketHandler, session);

		assertThat(this.servletRequest.isAsyncStarted()).as("Request should have been rejected").isFalse();
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("c[2010,\"Another connection still open\"]\n");
	}

	@Test
	public void handleRequestXhrStreaming() throws Exception {
		XhrStreamingTransportHandler transportHandler = new XhrStreamingTransportHandler();
		transportHandler.initialize(this.sockJsConfig);
		AbstractSockJsSession session = transportHandler.createSession("1", this.webSocketHandler, null);

		transportHandler.handleRequest(this.request, this.response, this.webSocketHandler, session);

		assertThat(this.response.getHeaders().getContentType().toString()).isEqualTo("application/javascript;charset=UTF-8");
		assertThat(this.servletRequest.isAsyncStarted()).as("Streaming request not started").isTrue();
		verify(this.webSocketHandler).afterConnectionEstablished(session);
	}

	@Test
	public void htmlFileTransport() throws Exception {
		HtmlFileTransportHandler transportHandler = new HtmlFileTransportHandler();
		transportHandler.initialize(this.sockJsConfig);
		StreamingSockJsSession session = transportHandler.createSession("1", this.webSocketHandler, null);

		transportHandler.handleRequest(this.request, this.response, this.webSocketHandler, session);

		assertThat(this.servletResponse.getStatus()).isEqualTo(500);
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("\"callback\" parameter required");

		resetRequestAndResponse();
		setRequest("POST", "/");
		this.servletRequest.setQueryString("c=callback");
		this.servletRequest.addParameter("c", "callback");
		transportHandler.handleRequest(this.request, this.response, this.webSocketHandler, session);

		assertThat(this.response.getHeaders().getContentType().toString()).isEqualTo("text/html;charset=UTF-8");
		assertThat(this.servletRequest.isAsyncStarted()).as("Streaming request not started").isTrue();
		verify(this.webSocketHandler).afterConnectionEstablished(session);
	}

	@Test
	public void eventSourceTransport() throws Exception {
		EventSourceTransportHandler transportHandler = new EventSourceTransportHandler();
		transportHandler.initialize(this.sockJsConfig);
		StreamingSockJsSession session = transportHandler.createSession("1", this.webSocketHandler, null);

		transportHandler.handleRequest(this.request, this.response, this.webSocketHandler, session);

		assertThat(this.response.getHeaders().getContentType().toString()).isEqualTo("text/event-stream;charset=UTF-8");
		assertThat(this.servletRequest.isAsyncStarted()).as("Streaming request not started").isTrue();
		verify(this.webSocketHandler).afterConnectionEstablished(session);
	}

	@Test
	public void frameFormats() throws Exception {
		this.servletRequest.setQueryString("c=callback");
		this.servletRequest.addParameter("c", "callback");

		SockJsFrame frame = SockJsFrame.openFrame();

		SockJsFrameFormat format = new XhrPollingTransportHandler().getFrameFormat(this.request);
		String formatted = format.format(frame);
		assertThat(formatted).isEqualTo((frame.getContent() + "\n"));

		format = new XhrStreamingTransportHandler().getFrameFormat(this.request);
		formatted = format.format(frame);
		assertThat(formatted).isEqualTo((frame.getContent() + "\n"));

		format = new HtmlFileTransportHandler().getFrameFormat(this.request);
		formatted = format.format(frame);
		assertThat(formatted).isEqualTo(("<script>\np(\"" + frame.getContent() + "\");\n</script>\r\n"));

		format = new EventSourceTransportHandler().getFrameFormat(this.request);
		formatted = format.format(frame);
		assertThat(formatted).isEqualTo(("data: " + frame.getContent() + "\r\n\r\n"));
	}

}
