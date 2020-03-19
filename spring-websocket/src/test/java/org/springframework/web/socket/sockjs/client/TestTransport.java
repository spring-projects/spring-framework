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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
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

	private ListenableFuture future;


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
	public ListenableFutureCallback<WebSocketSession> getConnectCallback() {
		ArgumentCaptor<ListenableFutureCallback> captor = ArgumentCaptor.forClass(ListenableFutureCallback.class);
		verify(this.future).addCallback(captor.capture());
		return captor.getValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler handler) {
		this.request = request;
		this.future = mock(ListenableFuture.class);
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
