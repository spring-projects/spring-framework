/*
 * Copyright 2002-2014 the original author or authors.
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

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * Unit tests for {@code SockJsUrlInfo}.
 * @author Rossen Stoyanchev
 */
public class SockJsUrlInfoTests {


	@Test
	public void serverId() throws Exception {
		SockJsUrlInfo info = new SockJsUrlInfo(new URI("http://example.com"));
		int serverId = Integer.valueOf(info.getServerId());
		assertTrue("Invalid serverId: " + serverId, serverId >= 0 && serverId < 1000);
	}

	@Test
	public void sessionId() throws Exception {
		SockJsUrlInfo info = new SockJsUrlInfo(new URI("http://example.com"));
		assertEquals("Invalid sessionId: " + info.getSessionId(), 32, info.getSessionId().length());
	}

	@Test
	public void infoUrl() throws Exception {
		testInfoUrl("http", "http");
		testInfoUrl("http", "http");
		testInfoUrl("https", "https");
		testInfoUrl("https", "https");
		testInfoUrl("ws", "http");
		testInfoUrl("ws", "http");
		testInfoUrl("wss", "https");
		testInfoUrl("wss", "https");
	}

	private void testInfoUrl(String scheme, String expectedScheme) throws Exception {
		SockJsUrlInfo info = new SockJsUrlInfo(new URI(scheme + "://example.com"));
		Assert.assertThat(info.getInfoUrl(), is(equalTo(new URI(expectedScheme + "://example.com/info"))));
	}

	@Test
	public void transportUrl() throws Exception {
		testTransportUrl("http", "http", TransportType.XHR_STREAMING);
		testTransportUrl("http", "ws", TransportType.WEBSOCKET);
		testTransportUrl("https", "https", TransportType.XHR_STREAMING);
		testTransportUrl("https", "wss", TransportType.WEBSOCKET);
		testTransportUrl("ws", "http", TransportType.XHR_STREAMING);
		testTransportUrl("ws", "ws", TransportType.WEBSOCKET);
		testTransportUrl("wss", "https", TransportType.XHR_STREAMING);
		testTransportUrl("wss", "wss", TransportType.WEBSOCKET);
	}

	private void testTransportUrl(String scheme, String expectedScheme, TransportType transportType) throws Exception {
		SockJsUrlInfo info = new SockJsUrlInfo(new URI(scheme + "://example.com"));
		String serverId = info.getServerId();
		String sessionId = info.getSessionId();
		String transport = transportType.toString().toLowerCase();
		URI expected = new URI(expectedScheme + "://example.com/" + serverId + "/" + sessionId + "/" + transport);
		assertThat(info.getTransportUrl(transportType), equalTo(expected));
	}

}
