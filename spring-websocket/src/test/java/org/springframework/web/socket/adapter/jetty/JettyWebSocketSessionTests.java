/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
