/* Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.adapter.standard;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import org.junit.Test;

import org.mockito.Mockito;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.handler.TestPrincipal;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link org.springframework.web.socket.adapter.standard.StandardWebSocketSession}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardWebSocketSessionTests {

	private final HttpHeaders headers = new HttpHeaders();

	private final Map<String, Object> attributes = new HashMap<>();


	@Test
	@SuppressWarnings("resource")
	public void getPrincipalWithConstructorArg() {
		TestPrincipal user = new TestPrincipal("joe");
		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null, user);

		assertSame(user, session.getPrincipal());
	}

	@Test
	@SuppressWarnings("resource")
	public void getPrincipalWithNativeSession() {
		TestPrincipal user = new TestPrincipal("joe");

		Session nativeSession = Mockito.mock(Session.class);
		given(nativeSession.getUserPrincipal()).willReturn(user);

		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null);
		session.initializeNativeSession(nativeSession);

		assertSame(user, session.getPrincipal());
	}

	@Test
	@SuppressWarnings("resource")
	public void getPrincipalNone() {
		Session nativeSession = Mockito.mock(Session.class);
		given(nativeSession.getUserPrincipal()).willReturn(null);

		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertNull(session.getPrincipal());
		verifyNoMoreInteractions(nativeSession);
	}

	@Test
	@SuppressWarnings("resource")
	public void getAcceptedProtocol() {
		String protocol = "foo";

		Session nativeSession = Mockito.mock(Session.class);
		given(nativeSession.getNegotiatedSubprotocol()).willReturn(protocol);

		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertEquals(protocol, session.getAcceptedProtocol());
		verifyNoMoreInteractions(nativeSession);
	}

}
