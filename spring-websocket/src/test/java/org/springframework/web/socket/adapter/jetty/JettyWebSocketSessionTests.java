/*
 * Copyright 2002-present the original author or authors.
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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.web.socket.BinaryMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
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

	@Test
	void binaryMessageWithSharedBufferSendsToMultipleSessions() throws Exception {
		byte[] data = {1, 2, 3, 4, 5};
		ByteBuffer sharedBuffer = ByteBuffer.wrap(data);
		BinaryMessage message = new BinaryMessage(sharedBuffer);

		ByteBuffer[] captured = new ByteBuffer[2];
		JettyWebSocketSession session1 = createMockSession((buffer, idx) -> captured[0] = buffer);
		JettyWebSocketSession session2 = createMockSession((buffer, idx) -> captured[1] = buffer);

		session1.sendMessage(message);
		session2.sendMessage(message);

		assertThat(captured[0].array()).isEqualTo(data);
		assertThat(captured[1].array()).isEqualTo(data);

		assertThat(sharedBuffer.position()).isEqualTo(0);
	}

	private JettyWebSocketSession createMockSession(BiConsumer<ByteBuffer, Integer> captureFunction) {
		Session mockSession = mock(Session.class);

		given(mockSession.getUpgradeRequest()).willReturn(request);
		given(mockSession.getUpgradeResponse()).willReturn(response);
		given(mockSession.isOpen()).willReturn(true);
		given(request.getUserPrincipal()).willReturn(null);
		given(response.getAcceptedSubProtocol()).willReturn(null);

		doAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			Callback callback = invocation.getArgument(1);
			ByteBuffer copy = ByteBuffer.allocate(buffer.remaining());
			copy.put(buffer);
			copy.flip();
			captureFunction.accept(copy, 0);
			callback.succeed();
			return null;
		}).when(mockSession).sendBinary(any(ByteBuffer.class), any(Callback.class));

		JettyWebSocketSession session = new JettyWebSocketSession(attributes);
		session.initializeNativeSession(mockSession);
		return session;
	}

}
