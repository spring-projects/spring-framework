/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link DefaultTransportRequest}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultTransportRequestTests {

	private final Jackson2SockJsMessageCodec CODEC = new Jackson2SockJsMessageCodec();

	private CompletableFuture<WebSocketSession> connectFuture = new CompletableFuture<>();

	@SuppressWarnings("unchecked")
	private BiConsumer<WebSocketSession, Throwable> connectCallback = mock();

	private TestTransport webSocketTransport = new TestTransport("WebSocketTestTransport");

	private TestTransport xhrTransport = new TestTransport("XhrTestTransport");


	@BeforeEach
	void setup() {
		this.connectFuture.whenComplete(this.connectCallback);
	}


	@Test
	void connect() throws Exception {
		DefaultTransportRequest request = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		request.connect(null, this.connectFuture);
		WebSocketSession session = mock();
		this.webSocketTransport.getConnectCallback().accept(session, null);
		assertThat(this.connectFuture.get()).isSameAs(session);
	}

	@Test
	void fallbackAfterTransportError() {
		DefaultTransportRequest request1 = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		DefaultTransportRequest request2 = createTransportRequest(this.xhrTransport, TransportType.XHR_STREAMING);
		request1.setFallbackRequest(request2);
		request1.connect(null, this.connectFuture);

		// Transport error => fallback
		this.webSocketTransport.getConnectCallback().accept(null, new IOException("Fake exception 1"));
		assertThat(this.connectFuture.isDone()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();

		// Transport error => no more fallback
		this.xhrTransport.getConnectCallback().accept(null, new IOException("Fake exception 2"));
		assertThat(this.connectFuture.isDone()).isTrue();
		assertThatExceptionOfType(ExecutionException.class)
			.isThrownBy(this.connectFuture::get)
			.withMessageContaining("Fake exception 2");
	}

	@Test
	void fallbackAfterTimeout() {
		TaskScheduler scheduler = mock();
		Runnable sessionCleanupTask = mock();
		DefaultTransportRequest request1 = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		DefaultTransportRequest request2 = createTransportRequest(this.xhrTransport, TransportType.XHR_STREAMING);
		request1.setFallbackRequest(request2);
		request1.setTimeoutScheduler(scheduler);
		request1.addTimeoutTask(sessionCleanupTask);
		request1.connect(null, this.connectFuture);

		assertThat(this.webSocketTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.invoked()).isFalse();

		// Get and invoke the scheduled timeout task
		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler).schedule(taskCaptor.capture(), any(Instant.class));
		verifyNoMoreInteractions(scheduler);
		taskCaptor.getValue().run();

		assertThat(this.xhrTransport.invoked()).isTrue();
		verify(sessionCleanupTask).run();
	}

	protected DefaultTransportRequest createTransportRequest(Transport transport, TransportType type) {
		SockJsUrlInfo urlInfo = new SockJsUrlInfo(URI.create("https://example.com"));
		return new DefaultTransportRequest(urlInfo, new HttpHeaders(), new HttpHeaders(), transport, type, CODEC);
	}

}
