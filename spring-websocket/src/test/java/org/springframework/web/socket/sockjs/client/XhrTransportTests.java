/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.client;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for
 * {@link org.springframework.web.socket.sockjs.client.AbstractXhrTransport}.
 *
 * @author Rossen Stoyanchev
 */
public class XhrTransportTests {

	@Test
	public void infoResponse() throws Exception {
		TestXhrTransport transport = new TestXhrTransport();
		transport.infoResponseToReturn = new ResponseEntity<>("body", HttpStatus.OK);
		assertEquals("body", transport.executeInfoRequest(new URI("http://example.com/info")));
	}

	@Test(expected = HttpServerErrorException.class)
	public void infoResponseError() throws Exception {
		TestXhrTransport transport = new TestXhrTransport();
		transport.infoResponseToReturn = new ResponseEntity<>("body", HttpStatus.BAD_REQUEST);
		assertEquals("body", transport.executeInfoRequest(new URI("http://example.com/info")));
	}

	@Test
	public void sendMessage() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("foo", "bar");
		TestXhrTransport transport = new TestXhrTransport();
		transport.setRequestHeaders(requestHeaders);
		transport.sendMessageResponseToReturn = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		URI url = new URI("http://example.com");
		transport.executeSendRequest(url, new TextMessage("payload"));
		assertEquals(2, transport.actualSendRequestHeaders.size());
		assertEquals("bar", transport.actualSendRequestHeaders.getFirst("foo"));
		assertEquals(MediaType.APPLICATION_JSON, transport.actualSendRequestHeaders.getContentType());
	}

	@Test(expected = HttpServerErrorException.class)
	public void sendMessageError() throws Exception {
		TestXhrTransport transport = new TestXhrTransport();
		transport.sendMessageResponseToReturn = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		URI url = new URI("http://example.com");
		transport.executeSendRequest(url, new TextMessage("payload"));
	}

	@Test
	public void connect() throws Exception {
		HttpHeaders handshakeHeaders = new HttpHeaders();
		handshakeHeaders.setOrigin("foo");

		TransportRequest request = mock(TransportRequest.class);
		when(request.getSockJsUrlInfo()).thenReturn(new SockJsUrlInfo(new URI("http://example.com")));
		when(request.getHandshakeHeaders()).thenReturn(handshakeHeaders);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("foo", "bar");

		TestXhrTransport transport = new TestXhrTransport();
		transport.setRequestHeaders(requestHeaders);

		WebSocketHandler handler = mock(WebSocketHandler.class);
		transport.connect(request, handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(request).getSockJsUrlInfo();
		verify(request).addTimeoutTask(captor.capture());
		verify(request).getTransportUrl();
		verify(request).getHandshakeHeaders();
		verifyNoMoreInteractions(request);

		assertEquals(2, transport.actualHandshakeHeaders.size());
		assertEquals("foo", transport.actualHandshakeHeaders.getOrigin());
		assertEquals("bar", transport.actualHandshakeHeaders.getFirst("foo"));

		assertFalse(transport.actualSession.isDisconnected());
		captor.getValue().run();
		assertTrue(transport.actualSession.isDisconnected());
	}


	private static class TestXhrTransport extends AbstractXhrTransport {

		private ResponseEntity<String> infoResponseToReturn;

		private ResponseEntity<String> sendMessageResponseToReturn;

		private HttpHeaders actualSendRequestHeaders;

		private HttpHeaders actualHandshakeHeaders;

		private XhrClientSockJsSession actualSession;


		@Override
		protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl) {
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
				SettableListenableFuture<WebSocketSession> connectFuture) {

			this.actualHandshakeHeaders = handshakeHeaders;
			this.actualSession = session;
		}
	}

}
