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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.concurrent.ListenableFutureCallback;
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
 * Unit tests for {@link org.springframework.web.socket.sockjs.client.SockJsClient}.
 *
 * @author Rossen Stoyanchev
 */
public class SockJsClientTests {

	private static final String URL = "https://example.com";

	private static final WebSocketHandler handler = mock(WebSocketHandler.class);


	private SockJsClient sockJsClient;

	private InfoReceiver infoReceiver;

	private TestTransport webSocketTransport;

	private XhrTestTransport xhrTransport;

	private ListenableFutureCallback<WebSocketSession> connectCallback;


	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setup() {
		this.infoReceiver = mock(InfoReceiver.class);
		this.webSocketTransport = new TestTransport("WebSocketTestTransport");
		this.xhrTransport = new XhrTestTransport("XhrTestTransport");

		List<Transport> transports = new ArrayList<>();
		transports.add(this.webSocketTransport);
		transports.add(this.xhrTransport);
		this.sockJsClient = new SockJsClient(transports);
		this.sockJsClient.setInfoReceiver(this.infoReceiver);

		this.connectCallback = mock(ListenableFutureCallback.class);
	}

	@Test
	public void connectWebSocket() throws Exception {
		setupInfoRequest(true);
		this.sockJsClient.doHandshake(handler, URL).addCallback(this.connectCallback);
		assertThat(this.webSocketTransport.invoked()).isTrue();
		WebSocketSession session = mock(WebSocketSession.class);
		this.webSocketTransport.getConnectCallback().onSuccess(session);
		verify(this.connectCallback).onSuccess(session);
		verifyNoMoreInteractions(this.connectCallback);
	}

	@Test
	public void connectWebSocketDisabled() throws URISyntaxException {
		setupInfoRequest(false);
		this.sockJsClient.doHandshake(handler, URL);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.getRequest().getTransportUrl().toString().endsWith("xhr_streaming")).isTrue();
	}

	@Test
	public void connectXhrStreamingDisabled() throws Exception {
		setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);
		this.sockJsClient.doHandshake(handler, URL).addCallback(this.connectCallback);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.getRequest().getTransportUrl().toString().endsWith("xhr")).isTrue();
	}

	// SPR-13254

	@Test
	public void connectWithHandshakeHeaders() throws Exception {
		ArgumentCaptor<HttpHeaders> headersCaptor = setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.set("foo", "bar");
		headers.set("auth", "123");
		this.sockJsClient.doHandshake(handler, headers, new URI(URL)).addCallback(this.connectCallback);

		HttpHeaders httpHeaders = headersCaptor.getValue();
		assertThat(httpHeaders.size()).isEqualTo(2);
		assertThat(httpHeaders.getFirst("foo")).isEqualTo("bar");
		assertThat(httpHeaders.getFirst("auth")).isEqualTo("123");

		httpHeaders = this.xhrTransport.getRequest().getHttpRequestHeaders();
		assertThat(httpHeaders.size()).isEqualTo(2);
		assertThat(httpHeaders.getFirst("foo")).isEqualTo("bar");
		assertThat(httpHeaders.getFirst("auth")).isEqualTo("123");
	}

	@Test
	public void connectAndUseSubsetOfHandshakeHeadersForHttpRequests() throws Exception {
		ArgumentCaptor<HttpHeaders> headersCaptor = setupInfoRequest(false);
		this.xhrTransport.setStreamingDisabled(true);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.set("foo", "bar");
		headers.set("auth", "123");
		this.sockJsClient.setHttpHeaderNames("auth");
		this.sockJsClient.doHandshake(handler, headers, new URI(URL)).addCallback(this.connectCallback);

		assertThat(headersCaptor.getValue().size()).isEqualTo(1);
		assertThat(headersCaptor.getValue().getFirst("auth")).isEqualTo("123");
		assertThat(this.xhrTransport.getRequest().getHttpRequestHeaders().size()).isEqualTo(1);
		assertThat(this.xhrTransport.getRequest().getHttpRequestHeaders().getFirst("auth")).isEqualTo("123");
	}

	@Test
	public void connectSockJsInfo() throws Exception {
		setupInfoRequest(true);
		this.sockJsClient.doHandshake(handler, URL);
		verify(this.infoReceiver, times(1)).executeInfoRequest(any(), any());
	}

	@Test
	public void connectSockJsInfoCached() throws Exception {
		setupInfoRequest(true);
		this.sockJsClient.doHandshake(handler, URL);
		this.sockJsClient.doHandshake(handler, URL);
		this.sockJsClient.doHandshake(handler, URL);
		verify(this.infoReceiver, times(1)).executeInfoRequest(any(), any());
	}

	@Test
	public void connectInfoRequestFailure() throws URISyntaxException {
		HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
		given(this.infoReceiver.executeInfoRequest(any(), any())).willThrow(exception);
		this.sockJsClient.doHandshake(handler, URL).addCallback(this.connectCallback);
		verify(this.connectCallback).onFailure(exception);
		assertThat(this.webSocketTransport.invoked()).isFalse();
		assertThat(this.xhrTransport.invoked()).isFalse();
	}

	private ArgumentCaptor<HttpHeaders> setupInfoRequest(boolean webSocketEnabled) {
		ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);
		given(this.infoReceiver.executeInfoRequest(any(), headersCaptor.capture())).willReturn(
				"{\"entropy\":123," +
						"\"origins\":[\"*:*\"]," +
						"\"cookie_needed\":true," +
						"\"websocket\":" + webSocketEnabled + "}");
		return headersCaptor;
	}

}
