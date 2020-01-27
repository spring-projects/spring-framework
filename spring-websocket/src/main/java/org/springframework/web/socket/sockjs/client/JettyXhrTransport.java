/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Enumeration;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

/**
 * An XHR transport based on Jetty's {@link org.eclipse.jetty.client.HttpClient}.
 *
 * <p>When used for testing purposes (e.g. load testing) the {@code HttpClient}
 * properties must be set to allow a larger than usual number of connections and
 * threads. For example:
 *
 * <pre class="code">
 * HttpClient httpClient = new HttpClient();
 * httpClient.setMaxConnectionsPerDestination(1000);
 * httpClient.setExecutor(new QueuedThreadPool(500));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class JettyXhrTransport extends AbstractXhrTransport implements Lifecycle {

	private final HttpClient httpClient;


	public JettyXhrTransport(HttpClient httpClient) {
		Assert.notNull(httpClient, "'httpClient' is required");
		this.httpClient = httpClient;
	}


	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Override
	public void start() {
		try {
			if (!this.httpClient.isRunning()) {
				this.httpClient.start();
			}
		}
		catch (Exception ex) {
			throw new SockJsException("Failed to start JettyXhrTransport", ex);
		}
	}

	@Override
	public void stop() {
		try {
			if (this.httpClient.isRunning()) {
				this.httpClient.stop();
			}
		}
		catch (Exception ex) {
			throw new SockJsException("Failed to stop JettyXhrTransport", ex);
		}
	}

	@Override
	public boolean isRunning() {
		return this.httpClient.isRunning();
	}


	@Override
	protected void connectInternal(TransportRequest transportRequest, WebSocketHandler handler,
			URI url, HttpHeaders handshakeHeaders, XhrClientSockJsSession session,
			SettableListenableFuture<WebSocketSession> connectFuture) {

		HttpHeaders httpHeaders = transportRequest.getHttpRequestHeaders();
		SockJsResponseListener listener = new SockJsResponseListener(url, httpHeaders, session, connectFuture);
		executeReceiveRequest(url, handshakeHeaders, listener);
	}

	private void executeReceiveRequest(URI url, HttpHeaders headers, SockJsResponseListener listener) {
		if (logger.isTraceEnabled()) {
			logger.trace("Starting XHR receive request, url=" + url);
		}
		Request httpRequest = this.httpClient.newRequest(url).method(HttpMethod.POST);
		addHttpHeaders(httpRequest, headers);
		httpRequest.send(listener);
	}

	@Override
	protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
		return executeRequest(infoUrl, HttpMethod.GET, headers, null);
	}

	@Override
	public ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		return executeRequest(url, HttpMethod.POST, headers, message.getPayload());
	}

	protected ResponseEntity<String> executeRequest(URI url, HttpMethod method,
			HttpHeaders headers, @Nullable String body) {

		Request httpRequest = this.httpClient.newRequest(url).method(method);
		addHttpHeaders(httpRequest, headers);
		if (body != null) {
			httpRequest.content(new StringContentProvider(body));
		}
		ContentResponse response;
		try {
			response = httpRequest.send();
		}
		catch (Exception ex) {
			throw new SockJsTransportFailureException("Failed to execute request to " + url, ex);
		}
		HttpStatus status = HttpStatus.valueOf(response.getStatus());
		HttpHeaders responseHeaders = toHttpHeaders(response.getHeaders());
		return (response.getContent() != null ?
				new ResponseEntity<>(response.getContentAsString(), responseHeaders, status) :
				new ResponseEntity<>(responseHeaders, status));
	}


	private static void addHttpHeaders(Request request, HttpHeaders headers) {
		headers.forEach((key, values) -> {
			for (String value : values) {
				request.header(key, value);
			}
		});
	}

	private static HttpHeaders toHttpHeaders(HttpFields httpFields) {
		HttpHeaders responseHeaders = new HttpHeaders();
		Enumeration<String> names = httpFields.getFieldNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Enumeration<String> values = httpFields.getValues(name);
			while (values.hasMoreElements()) {
				String value = values.nextElement();
				responseHeaders.add(name, value);
			}
		}
		return responseHeaders;
	}


	/**
	 * Jetty client {@link org.eclipse.jetty.client.api.Response.Listener Response
	 * Listener} that splits the body of the response into SockJS frames and
	 * delegates them to the {@link XhrClientSockJsSession}.
	 */
	private class SockJsResponseListener extends Response.Listener.Adapter {

		private final URI transportUrl;

		private final HttpHeaders receiveHeaders;

		private final XhrClientSockJsSession sockJsSession;

		private final SettableListenableFuture<WebSocketSession> connectFuture;

		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		public SockJsResponseListener(URI url, HttpHeaders headers,	XhrClientSockJsSession sockJsSession,
				SettableListenableFuture<WebSocketSession> connectFuture) {

			this.transportUrl = url;
			this.receiveHeaders = headers;
			this.connectFuture = connectFuture;
			this.sockJsSession = sockJsSession;
		}

		@Override
		public void onBegin(Response response) {
			if (response.getStatus() != 200) {
				HttpStatus status = HttpStatus.valueOf(response.getStatus());
				response.abort(new HttpServerErrorException(status, "Unexpected XHR receive status"));
			}
		}

		@Override
		public void onHeaders(Response response) {
			if (logger.isTraceEnabled()) {
				// Convert to HttpHeaders to avoid "\n"
				logger.trace("XHR receive headers: " + toHttpHeaders(response.getHeaders()));
			}
		}

		@Override
		public void onContent(Response response, ByteBuffer buffer) {
			while (true) {
				if (this.sockJsSession.isDisconnected()) {
					if (logger.isDebugEnabled()) {
						logger.debug("SockJS sockJsSession closed, closing response.");
					}
					response.abort(new SockJsException("Session closed.", this.sockJsSession.getId(), null));
					return;
				}
				if (buffer.remaining() == 0) {
					break;
				}
				int b = buffer.get();
				if (b == '\n') {
					handleFrame();
				}
				else {
					this.outputStream.write(b);
				}
			}
		}

		private void handleFrame() {
			byte[] bytes = this.outputStream.toByteArray();
			this.outputStream.reset();
			String content = new String(bytes, SockJsFrame.CHARSET);
			if (logger.isTraceEnabled()) {
				logger.trace("XHR content received: " + content);
			}
			if (!PRELUDE.equals(content)) {
				this.sockJsSession.handleFrame(new String(bytes, SockJsFrame.CHARSET));
			}
		}

		@Override
		public void onSuccess(Response response) {
			if (this.outputStream.size() > 0) {
				handleFrame();
			}
			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive request completed.");
			}
			executeReceiveRequest(this.transportUrl, this.receiveHeaders, this);
		}

		@Override
		public void onFailure(Response response, Throwable failure) {
			if (this.connectFuture.setException(failure)) {
				return;
			}
			if (this.sockJsSession.isDisconnected()) {
				this.sockJsSession.afterTransportClosed(null);
			}
			else {
				this.sockJsSession.handleTransportError(failure);
				this.sockJsSession.afterTransportClosed(new CloseStatus(1006, failure.getMessage()));
			}
		}
	}

}
