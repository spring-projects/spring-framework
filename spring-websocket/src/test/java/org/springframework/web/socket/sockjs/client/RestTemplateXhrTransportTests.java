/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.Test;

import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link RestTemplateXhrTransport}.
 *
 * @author Rossen Stoyanchev
 */
public class RestTemplateXhrTransportTests {

	private static final Jackson2SockJsMessageCodec CODEC = new Jackson2SockJsMessageCodec();

	private final WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);


	@Test
	public void connectReceiveAndClose() throws Exception {
		String body = "o\n" + "a[\"foo\"]\n" + "c[3000,\"Go away!\"]";
		ClientHttpResponse response = response(HttpStatus.OK, body);
		connect(response);

		verify(this.webSocketHandler).afterConnectionEstablished(any());
		verify(this.webSocketHandler).handleMessage(any(), eq(new TextMessage("foo")));
		verify(this.webSocketHandler).afterConnectionClosed(any(), eq(new CloseStatus(3000, "Go away!")));
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void connectReceiveAndCloseWithPrelude() throws Exception {
		StringBuilder sb = new StringBuilder(2048);
		for (int i = 0; i < 2048; i++) {
			sb.append('h');
		}
		String body = sb.toString() + "\n" + "o\n" + "a[\"foo\"]\n" + "c[3000,\"Go away!\"]";
		ClientHttpResponse response = response(HttpStatus.OK, body);
		connect(response);

		verify(this.webSocketHandler).afterConnectionEstablished(any());
		verify(this.webSocketHandler).handleMessage(any(), eq(new TextMessage("foo")));
		verify(this.webSocketHandler).afterConnectionClosed(any(), eq(new CloseStatus(3000, "Go away!")));
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void connectReceiveAndCloseWithStompFrame() throws Exception {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/destination");
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<byte[]> message = MessageBuilder.createMessage("body".getBytes(StandardCharsets.UTF_8), headers);
		byte[] bytes = new StompEncoder().encode(message);
		TextMessage textMessage = new TextMessage(bytes);
		SockJsFrame frame = SockJsFrame.messageFrame(new Jackson2SockJsMessageCodec(), textMessage.getPayload());

		String body = "o\n" + frame.getContent() + "\n" + "c[3000,\"Go away!\"]";
		ClientHttpResponse response = response(HttpStatus.OK, body);
		connect(response);

		verify(this.webSocketHandler).afterConnectionEstablished(any());
		verify(this.webSocketHandler).handleMessage(any(), eq(textMessage));
		verify(this.webSocketHandler).afterConnectionClosed(any(), eq(new CloseStatus(3000, "Go away!")));
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void connectFailure() throws Exception {
		final HttpServerErrorException expected = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		RestOperations restTemplate = mock(RestOperations.class);
		given(restTemplate.execute((URI) any(), eq(HttpMethod.POST), any(), any())).willThrow(expected);

		final CountDownLatch latch = new CountDownLatch(1);
		connect(restTemplate).addCallback(
				new ListenableFutureCallback<WebSocketSession>() {
					@Override
					public void onSuccess(WebSocketSession result) {
					}
					@Override
					public void onFailure(Throwable ex) {
						if (ex == expected) {
							latch.countDown();
						}
					}
				}
		);
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void errorResponseStatus() throws Exception {
		connect(response(HttpStatus.OK, "o\n"), response(HttpStatus.INTERNAL_SERVER_ERROR, "Oops"));

		verify(this.webSocketHandler).afterConnectionEstablished(any());
		verify(this.webSocketHandler).handleTransportError(any(), any());
		verify(this.webSocketHandler).afterConnectionClosed(any(), any());
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void responseClosedAfterDisconnected() throws Exception {
		String body = "o\n" + "c[3000,\"Go away!\"]\n" + "a[\"foo\"]\n";
		ClientHttpResponse response = response(HttpStatus.OK, body);
		connect(response);

		verify(this.webSocketHandler).afterConnectionEstablished(any());
		verify(this.webSocketHandler).afterConnectionClosed(any(), any());
		verifyNoMoreInteractions(this.webSocketHandler);
		verify(response).close();
	}

	private ListenableFuture<WebSocketSession> connect(ClientHttpResponse... responses) throws Exception {
		return connect(new TestRestTemplate(responses));
	}

	private ListenableFuture<WebSocketSession> connect(RestOperations restTemplate, ClientHttpResponse... responses)
			throws Exception {

		RestTemplateXhrTransport transport = new RestTemplateXhrTransport(restTemplate);
		transport.setTaskExecutor(new SyncTaskExecutor());

		SockJsUrlInfo urlInfo = new SockJsUrlInfo(new URI("https://example.com"));
		HttpHeaders headers = new HttpHeaders();
		headers.add("h-foo", "h-bar");
		TransportRequest request = new DefaultTransportRequest(urlInfo, headers, headers,
				transport, TransportType.XHR, CODEC);

		return transport.connect(request, this.webSocketHandler);
	}

	private ClientHttpResponse response(HttpStatus status, String body) throws IOException {
		ClientHttpResponse response = mock(ClientHttpResponse.class);
		InputStream inputStream = getInputStream(body);
		given(response.getRawStatusCode()).willReturn(status.value());
		given(response.getBody()).willReturn(inputStream);
		return response;
	}

	private InputStream getInputStream(String content) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(bytes);
	}



	private static class TestRestTemplate extends RestTemplate {

		private Queue<ClientHttpResponse> responses = new LinkedBlockingDeque<>();


		private TestRestTemplate(ClientHttpResponse... responses) {
			this.responses.addAll(Arrays.asList(responses));
		}

		@Override
		public <T> T execute(URI url, HttpMethod method, @Nullable RequestCallback callback,
				@Nullable ResponseExtractor<T> extractor) throws RestClientException {

			try {
				extractor.extractData(this.responses.remove());
			}
			catch (Throwable t) {
				throw new RestClientException("Failed to invoke extractor", t);
			}
			return null;
		}
	}


}
