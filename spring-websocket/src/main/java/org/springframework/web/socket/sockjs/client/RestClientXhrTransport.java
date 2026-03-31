/*
 * Copyright 2002-present the original author or authors.
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
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

/**
 * An {@code XhrTransport} implementation that uses a
 * {@link RestClient}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 7.0.7
 */
public class RestClientXhrTransport extends AbstractXhrTransport {

	private final RestClient restClient;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	public RestClientXhrTransport() {
		this(RestClient.create());
	}

	public RestClientXhrTransport(RestClient restClient) {
		Assert.notNull(restClient, "'restClient' is required");
		this.restClient = restClient;
	}

	/**
	 * Return the configured {@code RestClient}.
	 */
	public RestClient getRestClient() {
		return this.restClient;
	}

	/**
	 * Configure the {@code TaskExecutor} to use to execute XHR receive requests.
	 * <p>By default {@link SimpleAsyncTaskExecutor
	 * SimpleAsyncTaskExecutor} is configured which creates a new thread every
	 * time the transports connects.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the configured {@code TaskExecutor}.
	 */
	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected void connectInternal(final TransportRequest transportRequest, final WebSocketHandler handler,
			final URI receiveUrl, final HttpHeaders handshakeHeaders, final XhrClientSockJsSession session,
			final CompletableFuture<WebSocketSession> connectFuture) {

		getTaskExecutor().execute(() -> {
			final AtomicBoolean handshakePerformed = new AtomicBoolean();
			XhrResponseReader responseReader = new XhrResponseReader(session);
			while (true) {
				if (session.isDisconnected()) {
					session.afterTransportClosed(null);
					break;
				}
				try {
					if (logger.isTraceEnabled()) {
						logger.trace("Starting XHR receive request, url=" + receiveUrl);
					}
					getRestClient().post().uri(receiveUrl)
							.headers(headers -> {
								if (handshakePerformed.compareAndSet(false, true)) {
									headers.putAll(handshakeHeaders);
								}
								else {
									headers.putAll(transportRequest.getHttpRequestHeaders());
								}
							})
							.exchange(responseReader, false);
				}
				catch (Exception ex) {
					if (!connectFuture.isDone()) {
						connectFuture.completeExceptionally(ex);
					}
					else {
						session.handleTransportError(ex);
						session.afterTransportClosed(new CloseStatus(1006, ex.getMessage()));
					}
					break;
				}
			}
		});
	}

	@Override
	protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
		return nonNull(this.restClient.get()
				.uri(infoUrl)
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.exchange(textResponseExchangeFunction));
	}

	@Override
	public ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		return nonNull(this.restClient.post()
				.uri(url)
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.body(message.getPayload())
				.exchange(textResponseExchangeFunction));
	}

	private static <T> T nonNull(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}

	/**
	 * A simple ExchangeFunction that reads the body into a String.
	 */
	private static final RestClient.RequestHeadersSpec.ExchangeFunction<ResponseEntity<String>> textResponseExchangeFunction =
			(clientRequest, clientResponse) -> {
				String body = StreamUtils.copyToString(clientResponse.getBody(), SockJsFrame.CHARSET);
				return ResponseEntity.status(clientResponse.getStatusCode()).headers(clientResponse.getHeaders()).body(body);
			};


	/**
	 * Splits the body of an HTTP response into SockJS frames and delegates those
	 * to an {@link XhrClientSockJsSession}.
	 */
	private class XhrResponseReader implements RestClient.RequestHeadersSpec.ExchangeFunction<Void> {

		private final XhrClientSockJsSession sockJsSession;

		public XhrResponseReader(XhrClientSockJsSession sockJsSession) {
			this.sockJsSession = sockJsSession;
		}

		@Override
		public Void exchange(HttpRequest clientRequest, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse clientResponse) throws IOException {
			HttpStatusCode httpStatus = clientResponse.getStatusCode();
			if (httpStatus != HttpStatus.OK) {
				throw new HttpServerErrorException(
						httpStatus, "response.getStatusCode().getStatusText()", clientResponse.getHeaders(), null, null);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive headers: " + clientResponse.getHeaders());
			}
			InputStream is = clientResponse.getBody();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			while (true) {
				if (this.sockJsSession.isDisconnected()) {
					if (logger.isDebugEnabled()) {
						logger.debug("SockJS sockJsSession closed, closing response.");
					}
					clientResponse.close();
					break;
				}
				int b = is.read();
				if (b == -1) {
					if (os.size() > 0) {
						handleFrame(os);
					}
					if (logger.isTraceEnabled()) {
						logger.trace("XHR receive completed");
					}
					break;
				}
				if (b == '\n') {
					handleFrame(os);
				}
				else {
					os.write(b);
				}
			}
			return null;
		}

		private void handleFrame(ByteArrayOutputStream os) {
			String content = os.toString(SockJsFrame.CHARSET);
			os.reset();
			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive content: " + content);
			}
			if (!PRELUDE.equals(content)) {
				this.sockJsSession.handleFrame(content);
			}
		}
	}

}
