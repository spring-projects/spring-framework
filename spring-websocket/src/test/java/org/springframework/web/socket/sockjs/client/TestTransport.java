/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test SockJS Transport.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("rawtypes")
class TestTransport implements Transport {

	private final String name;

	private TransportRequest request;

	private @Nullable CompletableFuture<WebSocketSession> future;


	public TestTransport(String name) {
		this.name = name;
	}

	@Override
	public List<TransportType> getTransportTypes() {
		return Collections.singletonList(TransportType.WEBSOCKET);
	}

	public TransportRequest getRequest() {
		return this.request;
	}

	public boolean invoked() {
		return this.future != null;
	}

	@SuppressWarnings("unchecked")
	public BiConsumer<WebSocketSession, Throwable> getConnectCallback() {
		ArgumentCaptor<BiConsumer> captor = ArgumentCaptor.forClass(BiConsumer.class);
		verify(this.future).whenComplete(captor.capture());
		return captor.getValue();
	}

	@Override
	public CompletableFuture<WebSocketSession> connectAsync(TransportRequest request, WebSocketHandler handler) {
		this.request = request;
		this.future = mock();
		return this.future;
	}

	@Override
	public String toString() {
		return "TestTransport[" + name + "]";
	}


	static class XhrTestTransport extends TestTransport implements XhrTransport {

		private boolean streamingDisabled;


		XhrTestTransport(String name) {
			super(name);
		}

		@Override
		public List<TransportType> getTransportTypes() {
			return (isXhrStreamingDisabled() ?
					Collections.singletonList(TransportType.XHR) :
					Arrays.asList(TransportType.XHR_STREAMING, TransportType.XHR));
		}

		public void setStreamingDisabled(boolean streamingDisabled) {
			this.streamingDisabled = streamingDisabled;
		}

		@Override
		public boolean isXhrStreamingDisabled() {
			return this.streamingDisabled;
		}

		@Override
		public void executeSendRequest(URI transportUrl, HttpHeaders headers, TextMessage message) {
		}

		@Override
		public String executeInfoRequest(URI infoUrl, HttpHeaders headers) {
			return null;
		}
	}

}
