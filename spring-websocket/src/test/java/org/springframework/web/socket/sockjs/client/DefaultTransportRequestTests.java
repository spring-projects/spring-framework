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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultTransportRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultTransportRequestTests {

	private static final Jackson2SockJsMessageCodec CODEC = new Jackson2SockJsMessageCodec();


	private SettableListenableFuture<WebSocketSession> connectFuture;

	private ListenableFutureCallback<WebSocketSession> connectCallback;

	private TestTransport webSocketTransport;

	private TestTransport xhrTransport;


	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {
		this.connectCallback = mock(ListenableFutureCallback.class);
		this.connectFuture = new SettableListenableFuture<>();
		this.connectFuture.addCallback(this.connectCallback);
		this.webSocketTransport = new TestTransport("WebSocketTestTransport");
		this.xhrTransport = new TestTransport("XhrTestTransport");
	}


	@Test
	public void connect() throws Exception {
		DefaultTransportRequest request = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		request.connect(null, this.connectFuture);
		WebSocketSession session = mock(WebSocketSession.class);
		this.webSocketTransport.getConnectCallback().onSuccess(session);
		assertSame(session, this.connectFuture.get());
	}

	@Test
	public void fallbackAfterTransportError() throws Exception {
		DefaultTransportRequest request1 = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		DefaultTransportRequest request2 = createTransportRequest(this.xhrTransport, TransportType.XHR_STREAMING);
		request1.setFallbackRequest(request2);
		request1.connect(null, this.connectFuture);

		// Transport error => fallback
		this.webSocketTransport.getConnectCallback().onFailure(new IOException("Fake exception 1"));
		assertFalse(this.connectFuture.isDone());
		assertTrue(this.xhrTransport.invoked());

		// Transport error => no more fallback
		this.xhrTransport.getConnectCallback().onFailure(new IOException("Fake exception 2"));
		assertTrue(this.connectFuture.isDone());
		this.thrown.expect(ExecutionException.class);
		this.thrown.expectMessage("Fake exception 2");
		this.connectFuture.get();
	}

	@Test
	public void fallbackAfterTimeout() throws Exception {
		TaskScheduler scheduler = mock(TaskScheduler.class);
		Runnable sessionCleanupTask = mock(Runnable.class);
		DefaultTransportRequest request1 = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		DefaultTransportRequest request2 = createTransportRequest(this.xhrTransport, TransportType.XHR_STREAMING);
		request1.setFallbackRequest(request2);
		request1.setTimeoutScheduler(scheduler);
		request1.addTimeoutTask(sessionCleanupTask);
		request1.connect(null, this.connectFuture);

		assertTrue(this.webSocketTransport.invoked());
		assertFalse(this.xhrTransport.invoked());

		// Get and invoke the scheduled timeout task
		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler).schedule(taskCaptor.capture(), any(Date.class));
		verifyNoMoreInteractions(scheduler);
		taskCaptor.getValue().run();

		assertTrue(this.xhrTransport.invoked());
		verify(sessionCleanupTask).run();
	}

	protected DefaultTransportRequest createTransportRequest(Transport transport, TransportType type) throws Exception {
		SockJsUrlInfo urlInfo = new SockJsUrlInfo(new URI("http://example.com"));
		return new DefaultTransportRequest(urlInfo, new HttpHeaders(), new HttpHeaders(), transport, type, CODEC);
	}

}
