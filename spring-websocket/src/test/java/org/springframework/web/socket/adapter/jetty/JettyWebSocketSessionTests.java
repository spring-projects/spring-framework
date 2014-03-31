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

package org.springframework.web.socket.adapter.jetty;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.handler.TestPrincipal;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.springframework.web.socket.adapter.jetty.JettyWebSocketSession}.
 *
 * @author Rossen Stoyanchev
 */
public class JettyWebSocketSessionTests {

	private Map<String,Object> attributes;


	@Before
	public void setup() {
		this.attributes = new HashMap<>();
	}


	@Test
	public void getPrincipalWithConstructorArg() {
		TestPrincipal user = new TestPrincipal("joe");
		JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);

		assertSame(user, session.getPrincipal());
	}

	@Test
	public void getPrincipalFromNativeSession() {

		TestPrincipal user = new TestPrincipal("joe");

		UpgradeRequest request = Mockito.mock(UpgradeRequest.class);
		when(request.getUserPrincipal()).thenReturn(user);

		UpgradeResponse response = Mockito.mock(UpgradeResponse.class);
		when(response.getAcceptedSubProtocol()).thenReturn(null);

		Session nativeSession = Mockito.mock(Session.class);
		when(nativeSession.getUpgradeRequest()).thenReturn(request);
		when(nativeSession.getUpgradeResponse()).thenReturn(response);

		JettyWebSocketSession session = new JettyWebSocketSession(attributes);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertSame(user, session.getPrincipal());
		verifyNoMoreInteractions(nativeSession);
	}

	@Test
	public void getPrincipalNotAvailable() {

		UpgradeRequest request = Mockito.mock(UpgradeRequest.class);
		when(request.getUserPrincipal()).thenReturn(null);

		UpgradeResponse response = Mockito.mock(UpgradeResponse.class);
		when(response.getAcceptedSubProtocol()).thenReturn(null);

		Session nativeSession = Mockito.mock(Session.class);
		when(nativeSession.getUpgradeRequest()).thenReturn(request);
		when(nativeSession.getUpgradeResponse()).thenReturn(response);

		JettyWebSocketSession session = new JettyWebSocketSession(attributes);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertNull(session.getPrincipal());
		verify(nativeSession).isOpen();
		verifyNoMoreInteractions(nativeSession);
	}

	@Test
	public void getAcceptedProtocol() {

		String protocol = "foo";

		UpgradeRequest request = Mockito.mock(UpgradeRequest.class);
		when(request.getUserPrincipal()).thenReturn(null);

		UpgradeResponse response = Mockito.mock(UpgradeResponse.class);
		when(response.getAcceptedSubProtocol()).thenReturn(protocol);

		Session nativeSession = Mockito.mock(Session.class);
		when(nativeSession.getUpgradeRequest()).thenReturn(request);
		when(nativeSession.getUpgradeResponse()).thenReturn(response);

		JettyWebSocketSession session = new JettyWebSocketSession(attributes);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertSame(protocol, session.getAcceptedProtocol());
		verifyNoMoreInteractions(nativeSession);
	}

}
