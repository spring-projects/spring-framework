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

package org.springframework.web.socket.adapter.jetty;

import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.security.TestPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link JettyWebSocketSession}.
 *
 * @author Rossen Stoyanchev
 */
class JettyWebSocketSessionTests {

	private final Map<String, Object> attributes = Map.of();

	private final UpgradeRequest request = mock();

	private final UpgradeResponse response = mock();

	private final Session nativeSession = mock();


	@Test
	@SuppressWarnings("resource")
	void getPrincipalWithConstructorArg() {
		TestPrincipal user = new TestPrincipal("joe");
		JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);

		assertThat(session.getPrincipal()).isSameAs(user);
	}

	@Test
	@SuppressWarnings("resource")
	void getPrincipalFromNativeSession() {
		TestPrincipal user = new TestPrincipal("joe");

		given(request.getUserPrincipal()).willReturn(user);

		given(response.getAcceptedSubProtocol()).willReturn(null);

		given(nativeSession.getUpgradeRequest()).willReturn(request);
		given(nativeSession.getUpgradeResponse()).willReturn(response);

		JettyWebSocketSession session = new JettyWebSocketSession(attributes);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertThat(session.getPrincipal()).isSameAs(user);
		verifyNoMoreInteractions(nativeSession);
	}

	@Test
	@SuppressWarnings("resource")
	void getPrincipalNotAvailable() {
		given(request.getUserPrincipal()).willReturn(null);

		given(response.getAcceptedSubProtocol()).willReturn(null);

		given(nativeSession.getUpgradeRequest()).willReturn(request);
		given(nativeSession.getUpgradeResponse()).willReturn(response);

		JettyWebSocketSession session = new JettyWebSocketSession(attributes);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertThat(session.getPrincipal()).isNull();
		verifyNoMoreInteractions(nativeSession);
	}

	@Test
	@SuppressWarnings("resource")
	void getAcceptedProtocol() {
		String protocol = "foo";

		given(request.getUserPrincipal()).willReturn(null);

		given(response.getAcceptedSubProtocol()).willReturn(protocol);

		given(nativeSession.getUpgradeRequest()).willReturn(request);
		given(nativeSession.getUpgradeResponse()).willReturn(response);

		JettyWebSocketSession session = new JettyWebSocketSession(attributes);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertThat(session.getAcceptedProtocol()).isSameAs(protocol);
		verifyNoMoreInteractions(nativeSession);
	}

}
