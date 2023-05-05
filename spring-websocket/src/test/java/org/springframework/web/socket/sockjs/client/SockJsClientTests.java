/*
 * Copyright 2002-2023 the original author or authors.
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
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.client.TestTransport.XhrTestTransport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link SockJsClient}.
 *
 * @author Rossen Stoyanchev
 */
class SockJsClientTests {

	private static final String URL = "https://example.com";

	private static final WebSocketHandler handler = mock();


	private final InfoReceiver infoReceiver = mock();

	private final TestTransport webSocketTransport = new TestTransport("WebSocketTestTransport");

	private final XhrTestTransport xhrTransport = new XhrTestTransport("XhrTestTransport");

	@SuppressWarnings({ "deprecation", "unchecked" })
	private org.springframework.util.concurrent.ListenableFutureCallback<WebSocketSession> connectCallback = mock();

	private SockJsClient sockJsClient = new SockJsClient(List.of(this.webSocketTransport, this.xhrTransport));


	@BeforeEach
	void setup() {
		this.sockJsClient.setInfoReceiver(this.infoReceiver);
	}

	@Test
	@SuppressWarnings("deprecation")
	void connectWebSocket() {
		setupInfoRequest(true);
		this.sockJsClient.doHandshake(handler, URL).addCallback(this.connectCallback);
		assertThat(this.webSocketTransport.invoked()).isTrue();
		WebSocketSession session = mock();
		this.webSocketTransport.getConnectCallback().accept(session, null);
		verify(this.connectCallback).onSuccess(session);
		verifyNoMoreInteractions(this.connectCallback);
	}

	@Test
	@SuppressWarnings("deprecation")
	void connectWebSocketDisabled() throws URISyntaxException {
		setupInfoRequest(false);
		this.sockJsClient.doHandshake(handler, URL);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.getRequest().getTransportUrl().toString()).endsWith("xhr_streaming");
	}

	@Test
	@SuppressWarnings("deprecation")
	void connectXhrStreamingDisabled() {
		setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);
		this.sockJsClient.doHandshake(handler, URL).addCallback(this.connectCallback);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.getRequest().getTransportUrl().toString()).endsWith("xhr");
	}

	@Test  // SPR-13254
	@SuppressWarnings("deprecation")
	void connectWithHandshakeHeaders() {
		ArgumentCaptor<HttpHeaders> headersCaptor = setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.set("foo", "bar");
		headers.set("auth", "123");
		this.sockJsClient.doHandshake(handler, headers, URI.create(URL)).addCallback(this.connectCallback);

		HttpHeaders httpHeaders = headersCaptor.getValue();
		assertThat(httpHeaders).hasSize(2);
		assertThat(httpHeaders.getFirst("foo")).isEqualTo("bar");
		assertThat(httpHeaders.getFirst("auth")).isEqualTo("123");

		httpHeaders = this.xhrTransport.getRequest().getHttpRequestHeaders();
		assertThat(httpHeaders).hasSize(2);
		assertThat(httpHeaders.getFirst("foo")).isEqualTo("bar");
		assertThat(httpHeaders.getFirst("auth")).isEqualTo("123");
	}

	@Test
	@SuppressWarnings("deprecation")
	void connectAndUseSubsetOfHandshakeHeadersForHttpRequests() {
		ArgumentCaptor<HttpHeaders> headersCaptor = setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.set("foo", "bar");
		headers.set("auth", "123");
		this.sockJsClient.setHttpHeaderNames("auth");
		this.sockJsClient.doHandshake(handler, headers, URI.create(URL)).addCallback(this.connectCallback);

		assertThat(headersCaptor.getValue()).hasSize(1);
		assertThat(headersCaptor.getValue().getFirst("auth")).isEqualTo("123");
		assertThat(this.xhrTransport.getRequest().getHttpRequestHeaders()).hasSize(1);
		assertThat(this.xhrTransport.getRequest().getHttpRequestHeaders().getFirst("auth")).isEqualTo("123");
	}

	@Test
	@SuppressWarnings("deprecation")
	void connectSockJsInfo() {
		setupInfoRequest(true);
		this.sockJsClient.doHandshake(handler, URL);
		verify(this.infoReceiver, times(1)).executeInfoRequest(any(), any());
	}

	@Test
	@SuppressWarnings("deprecation")
	void connectSockJsInfoCached() {
		setupInfoRequest(true);
		this.sockJsClient.doHandshake(handler, URL);
		this.sockJsClient.doHandshake(handler, URL);
		this.sockJsClient.doHandshake(handler, URL);
		verify(this.infoReceiver, times(1)).executeInfoRequest(any(), any());
	}

	@Test
	@SuppressWarnings("deprecation")
	void connectInfoRequestFailure() throws URISyntaxException {
		HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
		given(this.infoReceiver.executeInfoRequest(any(), any())).willThrow(exception);
		this.sockJsClient.doHandshake(handler, URL).addCallback(this.connectCallback);
		verify(this.connectCallback).onFailure(exception);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isFalse();
	}

	private ArgumentCaptor<HttpHeaders> setupInfoRequest(boolean webSocketEnabled) {
		String response = """
			{
			"entropy": 123,
			"origins": ["*:*"],
			"cookie_needed": true,
			"websocket": %s
			}""".formatted(webSocketEnabled).replace('\n', '\0');
		ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);
		given(this.infoReceiver.executeInfoRequest(any(), headersCaptor.capture())).willReturn(response);
		return headersCaptor;
	}

}
