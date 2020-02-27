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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

/**
 * An {@code XhrTransport} implementation that uses a
 * {@link org.springframework.web.client.RestTemplate RestTemplate}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class RestTemplateXhrTransport extends AbstractXhrTransport {

	private final RestOperations restTemplate;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	public RestTemplateXhrTransport() {
		this(new RestTemplate());
	}

	public RestTemplateXhrTransport(RestOperations restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' is required");
		this.restTemplate = restTemplate;
	}


	/**
	 * Return the configured {@code RestTemplate}.
	 */
	public RestOperations getRestTemplate() {
		return this.restTemplate;
	}

	/**
	 * Configure the {@code TaskExecutor} to use to execute XHR receive requests.
	 * <p>By default {@link org.springframework.core.task.SimpleAsyncTaskExecutor
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
			final SettableListenableFuture<WebSocketSession> connectFuture) {

		getTaskExecutor().execute(() -> {
			HttpHeaders httpHeaders = transportRequest.getHttpRequestHeaders();
			XhrRequestCallback requestCallback = new XhrRequestCallback(handshakeHeaders);
			XhrRequestCallback requestCallbackAfterHandshake = new XhrRequestCallback(httpHeaders);
			XhrReceiveExtractor responseExtractor = new XhrReceiveExtractor(session);
			while (true) {
				if (session.isDisconnected()) {
					session.afterTransportClosed(null);
					break;
				}
				try {
					if (logger.isTraceEnabled()) {
						logger.trace("Starting XHR receive request, url=" + receiveUrl);
					}
					getRestTemplate().execute(receiveUrl, HttpMethod.POST, requestCallback, responseExtractor);
					requestCallback = requestCallbackAfterHandshake;
				}
				catch (Exception ex) {
					if (!connectFuture.isDone()) {
						connectFuture.setException(ex);
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
		RequestCallback requestCallback = new XhrRequestCallback(headers);
		return nonNull(this.restTemplate.execute(infoUrl, HttpMethod.GET, requestCallback, textResponseExtractor));
	}

	@Override
	public ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		RequestCallback requestCallback = new XhrRequestCallback(headers, message.getPayload());
		return nonNull(this.restTemplate.execute(url, HttpMethod.POST, requestCallback, textResponseExtractor));
	}

	private static <T> T nonNull(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}


	/**
	 * A simple ResponseExtractor that reads the body into a String.
	 */
	private static final ResponseExtractor<ResponseEntity<String>> textResponseExtractor =
			response -> {
				String body = StreamUtils.copyToString(response.getBody(), SockJsFrame.CHARSET);
				return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).body(body);
			};


	/**
	 * A RequestCallback to add the headers and (optionally) String content.
	 */
	private static class XhrRequestCallback implements RequestCallback {

		private final HttpHeaders headers;

		@Nullable
		private final String body;

		public XhrRequestCallback(HttpHeaders headers) {
			this(headers, null);
		}

		public XhrRequestCallback(HttpHeaders headers, @Nullable String body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public void doWithRequest(ClientHttpRequest request) throws IOException {
			request.getHeaders().putAll(this.headers);
			if (this.body != null) {
				if (request instanceof StreamingHttpOutputMessage) {
					((StreamingHttpOutputMessage) request).setBody(outputStream ->
							StreamUtils.copy(this.body, SockJsFrame.CHARSET, outputStream));
				}
				else {
					StreamUtils.copy(this.body, SockJsFrame.CHARSET, request.getBody());
				}
			}
		}
	}

	/**
	 * Splits the body of an HTTP response into SockJS frames and delegates those
	 * to an {@link XhrClientSockJsSession}.
	 */
	private class XhrReceiveExtractor implements ResponseExtractor<Object> {

		private final XhrClientSockJsSession sockJsSession;

		public XhrReceiveExtractor(XhrClientSockJsSession sockJsSession) {
			this.sockJsSession = sockJsSession;
		}

		@Override
		public Object extractData(ClientHttpResponse response) throws IOException {
			HttpStatus httpStatus = HttpStatus.resolve(response.getRawStatusCode());
			if (httpStatus == null) {
				throw new UnknownHttpStatusCodeException(
						response.getRawStatusCode(), response.getStatusText(), response.getHeaders(), null, null);
			}
			if (httpStatus != HttpStatus.OK) {
				throw new HttpServerErrorException(
						httpStatus, response.getStatusText(), response.getHeaders(), null, null);
			}

			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive headers: " + response.getHeaders());
			}
			InputStream is = response.getBody();
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			while (true) {
				if (this.sockJsSession.isDisconnected()) {
					if (logger.isDebugEnabled()) {
						logger.debug("SockJS sockJsSession closed, closing response.");
					}
					response.close();
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
			byte[] bytes = os.toByteArray();
			os.reset();
			String content = new String(bytes, SockJsFrame.CHARSET);
			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive content: " + content);
			}
			if (!PRELUDE.equals(content)) {
				this.sockJsSession.handleFrame(new String(bytes, SockJsFrame.CHARSET));
			}
		}
	}

}
