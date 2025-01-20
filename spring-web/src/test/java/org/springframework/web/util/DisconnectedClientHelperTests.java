/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.util;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import org.apache.catalina.connector.ClientAbortException;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.netty.channel.AbortedException;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.testfixture.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DisconnectedClientHelper}.
 * @author Rossen Stoyanchev
 */
public class DisconnectedClientHelperTests {

	@ParameterizedTest
	@ValueSource(strings = {"broKen pipe", "connection reset By peer"})
	void exceptionPhrases(String phrase) {
		Exception ex = new IOException(phrase);
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();

		ex = new IOException(ex);
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();
	}

	@Test
	void connectionResetExcluded() {
		Exception ex = new IOException("connection reset");
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isFalse();
	}

	@ParameterizedTest
	@MethodSource("disconnectedExceptions")
	void name(Exception ex) {
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();
	}

	static List<Exception> disconnectedExceptions() {
		return List.of(
				new AbortedException(""), new ClientAbortException(""),
				new EOFException(), new EofException(), new AsyncRequestNotUsableException(""));
	}

	@Test // gh-33064
	void nestedDisconnectedException() {
		Exception ex = new HttpMessageNotReadableException(
				"I/O error while reading input message", new ClientAbortException(),
				new MockHttpInputMessage(new byte[0]));

		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();
	}

	@Test // gh-34264
	void onwardClientDisconnectedExceptionPhrase() {
		Exception ex = new ResourceAccessException("I/O error", new EOFException("Connection reset by peer"));
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isFalse();
	}

	@Test
	void onwardClientDisconnectedExceptionType() {
		Exception ex = new ResourceAccessException("I/O error", new EOFException());
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isFalse();
	}

}
