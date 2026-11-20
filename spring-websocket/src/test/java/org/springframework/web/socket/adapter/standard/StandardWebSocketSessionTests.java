/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.adapter.standard;

import java.util.HashMap;
import java.util.Map;

import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link StandardWebSocketSession}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("resource")
class StandardWebSocketSessionTests {

	private final HttpHeaders headers = new HttpHeaders();

	private final Map<String, Object> attributes = new HashMap<>();


	@Test
	@SuppressWarnings("resource")
	void getPrincipalWithConstructorArg() {
		TestPrincipal user = new TestPrincipal("joe");
		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null, user);

		assertThat(session.getPrincipal()).isSameAs(user);
	}

	@Test
	void getPrincipalWithNativeSession() {
		TestPrincipal user = new TestPrincipal("joe");

		Session nativeSession = mock();
		given(nativeSession.getUserPrincipal()).willReturn(user);

		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null);
		session.initializeNativeSession(nativeSession);

		assertThat(session.getPrincipal()).isSameAs(user);
	}

	@Test
	void getPrincipalNone() {
		Session nativeSession = mock();
		given(nativeSession.getUserPrincipal()).willReturn(null);

		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertThat(session.getPrincipal()).isNull();
		verifyNoMoreInteractions(nativeSession);
	}

	@Test
	void getAcceptedProtocol() {
		String protocol = "foo";

		Session nativeSession = mock();
		given(nativeSession.getNegotiatedSubprotocol()).willReturn(protocol);

		StandardWebSocketSession session = new StandardWebSocketSession(this.headers, this.attributes, null, null);
		session.initializeNativeSession(nativeSession);

		reset(nativeSession);

		assertThat(session.getAcceptedProtocol()).isEqualTo(protocol);
		verifyNoMoreInteractions(nativeSession);
	}

	@Test // gh-29315
	void addAttributesWithNullKeyOrValue() {
		this.attributes.put(null, "value");
		this.attributes.put("key", null);
		this.attributes.put("foo", "bar");

		assertThat(new StandardWebSocketSession(this.headers, this.attributes, null, null).getAttributes())
				.hasSize(1).containsEntry("foo", "bar");
	}

}
