/*
 * Copyright 2002-2016 the original author or authors.
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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * An extension of {@link AbstractClientSockJsSession} for use with EventSource
 * transports simulating a WebSocket session.

 * @author Sebastian Lövdahl
 * @since 5.0
 */
public class EventSourceClientSockJsSession extends AbstractClientSockJsSession {

	private final EventSourceTransport transport;

	private final URI sendUrl;

	private final HttpHeaders headers;

	private final HttpHeaders sendHeaders;

	private int textMessageSizeLimit = -1;

	private int binaryMessageSizeLimit = -1;

	public EventSourceClientSockJsSession(TransportRequest request, WebSocketHandler handler,
			EventSourceTransport transport,
			SettableListenableFuture<WebSocketSession> connectFuture) {

		super(request, handler, connectFuture);
		Assert.notNull(transport, "'restTemplate' is required");
		this.transport = transport;
		this.headers = request.getHttpRequestHeaders();
		this.sendHeaders = new HttpHeaders();
		if (this.headers != null) {
			this.sendHeaders.putAll(this.headers);
		}
		this.sendHeaders.setContentType(MediaType.APPLICATION_JSON);
		this.sendUrl = request.getSockJsUrlInfo().getTransportUrl(TransportType.XHR_SEND);
	}


	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(getUri().getHost(), getUri().getPort());
	}

	@Override
	public String getAcceptedProtocol() {
		return null;
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		this.textMessageSizeLimit = messageSizeLimit;
	}

	@Override
	public int getTextMessageSizeLimit() {
		return this.textMessageSizeLimit;
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		this.binaryMessageSizeLimit = -1;
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		return this.binaryMessageSizeLimit;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		return null;
	}

	@Override
	protected void sendInternal(TextMessage message) {
		this.transport.executeSendRequest(this.sendUrl, this.sendHeaders, message);
	}

	@Override
	protected void disconnect(CloseStatus status) {
		// Nothing to do, EventSource transport checks if session is disconnected
	}

}
