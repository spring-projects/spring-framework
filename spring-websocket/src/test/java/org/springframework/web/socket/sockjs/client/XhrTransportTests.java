/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link AbstractXhrTransport}.
 *
 * @author Rossen Stoyanchev
 */
class XhrTransportTests {

	@Test
	void infoResponse() {
		TestXhrTransport transport = new TestXhrTransport();
		transport.infoResponseToReturn = new ResponseEntity<>("body", HttpStatus.OK);
		assertThat(transport.executeInfoRequest(URI.create("https://example.com/info"), null)).isEqualTo("body");
	}

	@Test
	void infoResponseError() {
		TestXhrTransport transport = new TestXhrTransport();
		transport.infoResponseToReturn = new ResponseEntity<>("body", HttpStatus.BAD_REQUEST);
		assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() ->
				transport.executeInfoRequest(URI.create("https://example.com/info"), null));
	}

	@Test
	void sendMessage() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("foo", "bar");
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);
		TestXhrTransport transport = new TestXhrTransport();
		transport.sendMessageResponseToReturn = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		URI url = URI.create("https://example.com");
		transport.executeSendRequest(url, requestHeaders, new TextMessage("payload"));
		assertThat(transport.actualSendRequestHeaders).hasSize(2);
		assertThat(transport.actualSendRequestHeaders.getFirst("foo")).isEqualTo("bar");
		assertThat(transport.actualSendRequestHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void sendMessageError() {
		TestXhrTransport transport = new TestXhrTransport();
		transport.sendMessageResponseToReturn = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		URI url = URI.create("https://example.com");
		assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() ->
				transport.executeSendRequest(url, new HttpHeaders(), new TextMessage("payload")));
	}

	@Test
	@SuppressWarnings("deprecation")
	void connect() {
		HttpHeaders handshakeHeaders = new HttpHeaders();
		handshakeHeaders.setOrigin("foo");

		TransportRequest request = mock();
		given(request.getSockJsUrlInfo()).willReturn(new SockJsUrlInfo(URI.create("https://example.com")));
		given(request.getHandshakeHeaders()).willReturn(handshakeHeaders);
		given(request.getHttpRequestHeaders()).willReturn(new HttpHeaders());

		TestXhrTransport transport = new TestXhrTransport();
		WebSocketHandler handler = mock();
		transport.connect(request, handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(request).getSockJsUrlInfo();
		verify(request).addTimeoutTask(captor.capture());
		verify(request).getTransportUrl();
		verify(request).getHandshakeHeaders();
		verify(request).getHttpRequestHeaders();
		verifyNoMoreInteractions(request);

		assertThat(transport.actualHandshakeHeaders).hasSize(1);
		assertThat(transport.actualHandshakeHeaders.getOrigin()).isEqualTo("foo");

		assertThat(transport.actualSession.isDisconnected()).isFalse();
		captor.getValue().run();
		assertThat(transport.actualSession.isDisconnected()).isTrue();
	}


	private static class TestXhrTransport extends AbstractXhrTransport {

		private ResponseEntity<String> infoResponseToReturn;

		private ResponseEntity<String> sendMessageResponseToReturn;

		private HttpHeaders actualSendRequestHeaders;

		private HttpHeaders actualHandshakeHeaders;

		private XhrClientSockJsSession actualSession;


		@Override
		protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
			return this.infoResponseToReturn;
		}

		@Override
		protected ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
			this.actualSendRequestHeaders = headers;
			return this.sendMessageResponseToReturn;
		}

		@Override
		protected void connectInternal(TransportRequest request, WebSocketHandler handler, URI receiveUrl,
				HttpHeaders handshakeHeaders, XhrClientSockJsSession session,
				CompletableFuture<WebSocketSession> connectFuture) {

			this.actualHandshakeHeaders = handshakeHeaders;
			this.actualSession = session;
		}
	}

}
