/*
 * Copyright 2002-2021 the original author or authors.
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
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.client.UndertowClient;
import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
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
 * An XHR transport based on Undertow's {@link io.undertow.client.UndertowClient}.
 * Requires Undertow 1.3 or 1.4, including XNIO, as of Spring Framework 5.0.
 *
 * <p>When used for testing purposes (e.g. load testing) or for specific use cases
 * (like HTTPS configuration), a custom OptionMap should be provided:
 *
 * <pre class="code">
 * OptionMap optionMap = OptionMap.builder()
 *   .set(Options.WORKER_IO_THREADS, 8)
 *   .set(Options.TCP_NODELAY, true)
 *   .set(Options.KEEP_ALIVE, true)
 *   .set(Options.WORKER_NAME, "SockJSClient")
 *   .getMap();
 *
 * UndertowXhrTransport transport = new UndertowXhrTransport(optionMap);
 * </pre>
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1.2
 * @see org.xnio.Options
 */
public class UndertowXhrTransport extends AbstractXhrTransport {

	private static final AttachmentKey<String> RESPONSE_BODY = AttachmentKey.create(String.class);


	private final OptionMap optionMap;

	private final UndertowClient httpClient;

	private final XnioWorker worker;

	private final ByteBufferPool bufferPool;


	public UndertowXhrTransport() throws IOException {
		this(OptionMap.builder().parse(Options.WORKER_NAME, "SockJSClient").getMap());
	}

	public UndertowXhrTransport(OptionMap optionMap) throws IOException {
		Assert.notNull(optionMap, "OptionMap is required");
		this.optionMap = optionMap;
		this.httpClient = UndertowClient.getInstance();
		this.worker = Xnio.getInstance().createWorker(optionMap);
		this.bufferPool = new DefaultByteBufferPool(false, 1024, -1, 2);
	}


	/**
	 * Return Undertow's native HTTP client.
	 */
	public UndertowClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Return the {@link org.xnio.XnioWorker} backing the I/O operations
	 * for Undertow's HTTP client.
	 * @see org.xnio.Xnio
	 */
	public XnioWorker getWorker() {
		return this.worker;
	}


	@Override
	protected void connectInternal(TransportRequest request, WebSocketHandler handler, URI receiveUrl,
			HttpHeaders handshakeHeaders, XhrClientSockJsSession session,
			SettableListenableFuture<WebSocketSession> connectFuture) {

		executeReceiveRequest(request, receiveUrl, handshakeHeaders, session, connectFuture);
	}

	private void executeReceiveRequest(final TransportRequest transportRequest,
			final URI url, final HttpHeaders headers, final XhrClientSockJsSession session,
			final SettableListenableFuture<WebSocketSession> connectFuture) {

		if (logger.isTraceEnabled()) {
			logger.trace("Starting XHR receive request for " + url);
		}

		ClientCallback<ClientConnection> clientCallback = new ClientCallback<>() {
			@Override
			public void completed(ClientConnection connection) {
				ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(url.getPath());
				HttpString headerName = HttpString.tryFromString(HttpHeaders.HOST);
				request.getRequestHeaders().add(headerName, url.getHost());
				addHttpHeaders(request, headers);
				HttpHeaders httpHeaders = transportRequest.getHttpRequestHeaders();
				connection.sendRequest(request, createReceiveCallback(transportRequest,
						url, httpHeaders, session, connectFuture));
			}

			@Override
			public void failed(IOException ex) {
				throw new SockJsTransportFailureException("Failed to execute request to " + url, ex);
			}
		};

		this.httpClient.connect(clientCallback, url, this.worker, this.bufferPool, this.optionMap);
	}

	private static void addHttpHeaders(ClientRequest request, HttpHeaders headers) {
		HeaderMap headerMap = request.getRequestHeaders();
		headers.forEach((key, values) -> {
			for (String value : values) {
				headerMap.add(HttpString.tryFromString(key), value);
			}
		});
	}

	private ClientCallback<ClientExchange> createReceiveCallback(final TransportRequest transportRequest,
			final URI url, final HttpHeaders headers, final XhrClientSockJsSession sockJsSession,
			final SettableListenableFuture<WebSocketSession> connectFuture) {

		return new ClientCallback<>() {
			@Override
			public void completed(final ClientExchange exchange) {
				exchange.setResponseListener(new ClientCallback<ClientExchange>() {
					@Override
					public void completed(ClientExchange result) {
						ClientResponse response = result.getResponse();
						if (response.getResponseCode() != 200) {
							HttpStatus status = HttpStatus.valueOf(response.getResponseCode());
							IoUtils.safeClose(result.getConnection());
							onFailure(new HttpServerErrorException(status, "Unexpected XHR receive status"));
						}
						else {
							SockJsResponseListener listener = new SockJsResponseListener(
									transportRequest, result.getConnection(), url, headers,
									sockJsSession, connectFuture);
							listener.setup(result.getResponseChannel());
						}
						if (logger.isTraceEnabled()) {
							logger.trace("XHR receive headers: " + toHttpHeaders(response.getResponseHeaders()));
						}
						try {
							StreamSinkChannel channel = result.getRequestChannel();
							channel.shutdownWrites();
							if (!channel.flush()) {
								channel.getWriteSetter().set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
								channel.resumeWrites();
							}
						}
						catch (IOException exc) {
							IoUtils.safeClose(result.getConnection());
							onFailure(exc);
						}
					}

					@Override
					public void failed(IOException exc) {
						IoUtils.safeClose(exchange.getConnection());
						onFailure(exc);
					}
				});
			}

			@Override
			public void failed(IOException exc) {
				onFailure(exc);
			}

			private void onFailure(Throwable failure) {
				if (connectFuture.setException(failure)) {
					return;
				}
				if (sockJsSession.isDisconnected()) {
					sockJsSession.afterTransportClosed(null);
				}
				else {
					sockJsSession.handleTransportError(failure);
					sockJsSession.afterTransportClosed(new CloseStatus(1006, failure.getMessage()));
				}
			}
		};
	}

	private static HttpHeaders toHttpHeaders(HeaderMap headerMap) {
		HttpHeaders httpHeaders = new HttpHeaders();
		for (HttpString name : headerMap.getHeaderNames()) {
			for (String value : headerMap.get(name)) {
				httpHeaders.add(name.toString(), value);
			}
		}
		return httpHeaders;
	}

	@Override
	protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
		return executeRequest(infoUrl, Methods.GET, headers, null);
	}

	@Override
	protected ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		return executeRequest(url, Methods.POST, headers, message.getPayload());
	}

	protected ResponseEntity<String> executeRequest(
			URI url, HttpString method, HttpHeaders headers, @Nullable String body) {

		CountDownLatch latch = new CountDownLatch(1);
		List<ClientResponse> responses = new CopyOnWriteArrayList<>();

		try {
			ClientConnection connection =
					this.httpClient.connect(url, this.worker, this.bufferPool, this.optionMap).get();
			try {
				ClientRequest request = new ClientRequest().setMethod(method).setPath(url.getPath());
				request.getRequestHeaders().add(HttpString.tryFromString(HttpHeaders.HOST), url.getHost());
				if (StringUtils.hasLength(body)) {
					HttpString headerName = HttpString.tryFromString(HttpHeaders.CONTENT_LENGTH);
					request.getRequestHeaders().add(headerName, body.length());
				}
				addHttpHeaders(request, headers);
				connection.sendRequest(request, createRequestCallback(body, responses, latch));

				latch.await();
				ClientResponse response = responses.iterator().next();
				HttpStatus status = HttpStatus.valueOf(response.getResponseCode());
				HttpHeaders responseHeaders = toHttpHeaders(response.getResponseHeaders());
				String responseBody = response.getAttachment(RESPONSE_BODY);
				return (responseBody != null ?
						new ResponseEntity<>(responseBody, responseHeaders, status) :
						new ResponseEntity<>(responseHeaders, status));
			}
			finally {
				IoUtils.safeClose(connection);
			}
		}
		catch (IOException ex) {
			throw new SockJsTransportFailureException("Failed to execute request to " + url, ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new SockJsTransportFailureException("Interrupted while processing request to " + url, ex);
		}
	}

	private ClientCallback<ClientExchange> createRequestCallback(final @Nullable String body,
			final List<ClientResponse> responses, final CountDownLatch latch) {

		return new ClientCallback<>() {
			@Override
			public void completed(ClientExchange result) {
				result.setResponseListener(new ClientCallback<ClientExchange>() {
					@Override
					public void completed(final ClientExchange result) {
						responses.add(result.getResponse());
						new StringReadChannelListener(result.getConnection().getBufferPool()) {
							@Override
							protected void stringDone(String string) {
								result.getResponse().putAttachment(RESPONSE_BODY, string);
								latch.countDown();
							}
							@Override
							protected void error(IOException ex) {
								onFailure(latch, ex);
							}
						}.setup(result.getResponseChannel());
					}
					@Override
					public void failed(IOException ex) {
						onFailure(latch, ex);
					}
				});
				try {
					if (body != null) {
						result.getRequestChannel().write(ByteBuffer.wrap(body.getBytes()));
					}
					result.getRequestChannel().shutdownWrites();
					if (!result.getRequestChannel().flush()) {
						result.getRequestChannel().getWriteSetter()
								.set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
						result.getRequestChannel().resumeWrites();
					}
				}
				catch (IOException ex) {
					onFailure(latch, ex);
				}
			}

			@Override
			public void failed(IOException ex) {
				onFailure(latch, ex);
			}

			private void onFailure(CountDownLatch latch, IOException ex) {
				latch.countDown();
				throw new SockJsTransportFailureException("Failed to execute request", ex);
			}
		};
	}


	private class SockJsResponseListener implements ChannelListener<StreamSourceChannel> {

		private final TransportRequest request;

		private final ClientConnection connection;

		private final URI url;

		private final HttpHeaders headers;

		private final XhrClientSockJsSession session;

		private final SettableListenableFuture<WebSocketSession> connectFuture;

		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		public SockJsResponseListener(TransportRequest request, ClientConnection connection, URI url,
				HttpHeaders headers, XhrClientSockJsSession sockJsSession,
				SettableListenableFuture<WebSocketSession> connectFuture) {

			this.request = request;
			this.connection = connection;
			this.url = url;
			this.headers = headers;
			this.session = sockJsSession;
			this.connectFuture = connectFuture;
		}

		public void setup(StreamSourceChannel channel) {
			channel.suspendReads();
			channel.getReadSetter().set(this);
			channel.resumeReads();
		}

		@Override
		public void handleEvent(StreamSourceChannel channel) {
			if (this.session.isDisconnected()) {
				if (logger.isDebugEnabled()) {
					logger.debug("SockJS sockJsSession closed, closing response.");
				}
				IoUtils.safeClose(this.connection);
				throw new SockJsException("Session closed.", this.session.getId(), null);
			}

			try (PooledByteBuffer pooled = bufferPool.allocate()) {
				int r;
				do {
					ByteBuffer buffer = pooled.getBuffer();
					buffer.clear();
					r = channel.read(buffer);
					buffer.flip();
					if (r == 0) {
						return;
					}
					else if (r == -1) {
						onSuccess();
					}
					else {
						while (buffer.hasRemaining()) {
							int b = buffer.get();
							if (b == '\n') {
								handleFrame();
							}
							else {
								this.outputStream.write(b);
							}
						}
					}
				}
				while (r > 0);
			}
			catch (IOException exc) {
				onFailure(exc);
			}
		}

		private void handleFrame() {
			String content = StreamUtils.copyToString(this.outputStream, SockJsFrame.CHARSET);
			this.outputStream.reset();
			if (logger.isTraceEnabled()) {
				logger.trace("XHR content received: " + content);
			}
			if (!PRELUDE.equals(content)) {
				this.session.handleFrame(content);
			}
		}

		public void onSuccess() {
			if (this.outputStream.size() > 0) {
				handleFrame();
			}
			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive request completed.");
			}
			IoUtils.safeClose(this.connection);
			executeReceiveRequest(this.request, this.url, this.headers, this.session, this.connectFuture);
		}

		public void onFailure(Throwable failure) {
			IoUtils.safeClose(this.connection);
			if (this.connectFuture.setException(failure)) {
				return;
			}
			if (this.session.isDisconnected()) {
				this.session.afterTransportClosed(null);
			}
			else {
				this.session.handleTransportError(failure);
				this.session.afterTransportClosed(new CloseStatus(1006, failure.getMessage()));
			}
		}
	}

}
