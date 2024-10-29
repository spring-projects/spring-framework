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
import java.util.List;
import java.util.function.BiConsumer;

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
 * Tests for {@link SockJsClient}.
 *
 * @author Rossen Stoyanchev
 */
class SockJsClientTests {

	private static final String URL = "https://example.com";

	private static final WebSocketHandler handler = mock();

	private final InfoReceiver infoReceiver = mock();

	private final TestTransport webSocketTransport = new TestTransport("WebSocketTestTransport");

	private final XhrTestTransport xhrTransport = new XhrTestTransport("XhrTestTransport");

	private final BiConsumer<WebSocketSession, Throwable> connectCallback = mock();

	private final SockJsClient sockJsClient = new SockJsClient(List.of(this.webSocketTransport, this.xhrTransport));


	@BeforeEach
	void setup() {
		this.sockJsClient.setInfoReceiver(this.infoReceiver);
	}

	@Test
	void connectWebSocket() {
		setupInfoRequest(true);
		this.sockJsClient.execute(handler, URL).whenComplete(this.connectCallback);
		assertThat(this.webSocketTransport.invoked()).isTrue();
		WebSocketSession session = mock();
		this.webSocketTransport.getConnectCallback().accept(session, null);
		verify(this.connectCallback).accept(session, null);
		verifyNoMoreInteractions(this.connectCallback);
	}

	@Test
	void connectWebSocketDisabled() {
		setupInfoRequest(false);
		this.sockJsClient.execute(handler, URL);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.getRequest().getTransportUrl().toString()).endsWith("xhr_streaming");
	}

	@Test
	void connectXhrStreamingDisabled() {
		setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);
		this.sockJsClient.execute(handler, URL).whenComplete(this.connectCallback);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.getRequest().getTransportUrl().toString()).endsWith("xhr");
	}

	@Test  // SPR-13254
	void connectWithHandshakeHeaders() {
		ArgumentCaptor<HttpHeaders> headersCaptor = setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.set("foo", "bar");
		headers.set("auth", "123");
		this.sockJsClient.execute(handler, headers, URI.create(URL)).whenComplete(this.connectCallback);

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
	void connectAndUseSubsetOfHandshakeHeadersForHttpRequests() {
		ArgumentCaptor<HttpHeaders> headersCaptor = setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.set("foo", "bar");
		headers.set("auth", "123");
		this.sockJsClient.setHttpHeaderNames("auth");
		this.sockJsClient.execute(handler, headers, URI.create(URL)).whenComplete(this.connectCallback);

		assertThat(headersCaptor.getValue()).hasSize(1);
		assertThat(headersCaptor.getValue().getFirst("auth")).isEqualTo("123");
		assertThat(this.xhrTransport.getRequest().getHttpRequestHeaders()).hasSize(1);
		assertThat(this.xhrTransport.getRequest().getHttpRequestHeaders().getFirst("auth")).isEqualTo("123");
	}

	@Test
	void connectSockJsInfo() {
		setupInfoRequest(true);
		this.sockJsClient.execute(handler, URL);
		verify(this.infoReceiver, times(1)).executeInfoRequest(any(), any());
	}

	@Test
	void connectSockJsInfoCached() {
		setupInfoRequest(true);
		this.sockJsClient.execute(handler, URL);
		this.sockJsClient.execute(handler, URL);
		this.sockJsClient.execute(handler, URL);
		verify(this.infoReceiver, times(1)).executeInfoRequest(any(), any());
	}

	@Test
	void connectInfoRequestFailure() {
		HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
		given(this.infoReceiver.executeInfoRequest(any(), any())).willThrow(exception);
		this.sockJsClient.execute(handler, URL).whenComplete(this.connectCallback);
		verify(this.connectCallback).accept(null, exception);
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
