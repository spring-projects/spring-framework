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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

/**
 * An {@code EventSourceTransport} implementation that uses a
 * {@link RestTemplate RestTemplate}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastian Lövdahl
 * @since 5.0
 */
public class RestTemplateEventSourceTransport extends AbstractEventSourceTransport {

	private final RestOperations restTemplate;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	public RestTemplateEventSourceTransport() {
		this(new RestTemplate());
	}

	public RestTemplateEventSourceTransport(RestOperations restTemplate) {
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
	 * <p>By default {@link SimpleAsyncTaskExecutor
	 * SimpleAsyncTaskExecutor} is configured which creates a new thread every
	 * time the transports connects.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(this.taskExecutor, "TaskExecutor must not be null");
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
			final URI receiveUrl, final HttpHeaders handshakeHeaders, final EventSourceClientSockJsSession session,
			final SettableListenableFuture<WebSocketSession> connectFuture) {

		getTaskExecutor().execute(new Runnable() {
			@Override
			public void run() {
				HttpHeaders httpHeaders = transportRequest.getHttpRequestHeaders();
				EventSourceRequestCallback requestCallback = new EventSourceRequestCallback(handshakeHeaders);
				EventSourceRequestCallback requestCallbackAfterHandshake = new EventSourceRequestCallback(httpHeaders);
				EventSourceReceiveExtractor responseExtractor = new EventSourceReceiveExtractor(session);
				while (true) {
					if (session.isDisconnected()) {
						session.afterTransportClosed(null);
						break;
					}
					try {
						if (logger.isTraceEnabled()) {
							logger.trace("Starting EventSource receive request, url=" + receiveUrl);
						}
						getRestTemplate().execute(receiveUrl, HttpMethod.GET, requestCallback, responseExtractor);
						requestCallback = requestCallbackAfterHandshake;
					}
					catch (Throwable ex) {
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
			}
		});
	}

	@Override
	protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
		RequestCallback requestCallback = new EventSourceRequestCallback(headers);
		return this.restTemplate.execute(infoUrl, HttpMethod.GET, requestCallback, textResponseExtractor);
	}

	@Override
	public ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		RequestCallback requestCallback = new EventSourceRequestCallback(headers, message.getPayload());
		return this.restTemplate.execute(url, HttpMethod.POST, requestCallback, textResponseExtractor);
	}


	/**
	 * A simple ResponseExtractor that reads the body into a String.
	 */
	private final static ResponseExtractor<ResponseEntity<String>> textResponseExtractor =
			new ResponseExtractor<ResponseEntity<String>>() {
				@Override
				public ResponseEntity<String> extractData(ClientHttpResponse response) throws IOException {
					if (response.getBody() == null) {
						return new ResponseEntity<>(response.getHeaders(), response.getStatusCode());
					}
					else {
						String body = StreamUtils.copyToString(response.getBody(), SockJsFrame.CHARSET);
						return new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode());
					}
				}
			};

	/**
	 * A RequestCallback to add the headers and (optionally) String content.
	 */
	private static class EventSourceRequestCallback implements RequestCallback {

		private final HttpHeaders headers;

		private final String body;

		public EventSourceRequestCallback(HttpHeaders headers) {
			this(headers, null);
		}

		public EventSourceRequestCallback(HttpHeaders headers, String body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public void doWithRequest(ClientHttpRequest request) throws IOException {
			if (this.headers != null) {
				request.getHeaders().putAll(this.headers);
			}
			if (this.body != null) {
				StreamUtils.copy(this.body, SockJsFrame.CHARSET, request.getBody());
			}
		}
	}

	/**
	 * Splits the body of an HTTP response into SockJS frames and delegates those
	 * to an {@link EventSourceClientSockJsSession}.
	 */
	private class EventSourceReceiveExtractor implements ResponseExtractor<Object> {

		private final EventSourceClientSockJsSession sockJsSession;

		public EventSourceReceiveExtractor(EventSourceClientSockJsSession sockJsSession) {
			this.sockJsSession = sockJsSession;
		}

		@Override
		public Object extractData(ClientHttpResponse response) throws IOException {
			if (!HttpStatus.OK.equals(response.getStatusCode())) {
				throw new HttpServerErrorException(response.getStatusCode());
			}
			MediaType contentType = response.getHeaders().getContentType();
			if (contentType == null || !contentType.equals(MediaType.TEXT_EVENT_STREAM)) {
				throw new SockJsException("Unexpected EventSource response, wrong Content-Type header " +
						"received", null);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("EventSource receive headers: " + response.getHeaders());
			}
			InputStream is = response.getBody();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int previousB = -1;
			while (true) {
				if (this.sockJsSession.isDisconnected()) {
					if (os.size() > 0) {
						handleFrame(os);
					}
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
						logger.trace("EventSource receive completed");
					}
					break;
				}

				if (b == '\n') {
					handleFrame(os);
				}
				else if (b == '\r') {
					if (previousB == '\r') {
						os.write(b);
					}
					else {
						// ignore the \r for now, the next byte will probably be a \n
					}
				}
				else {
					if (previousB == '\r') {
						handleFrame(os);
					}
					os.write(b);
				}

				previousB = b;
			}
			return null;
		}

		private void handleFrame(ByteArrayOutputStream os) {
			handleEventSourceFrame(os, sockJsSession);
		}
	}

}
