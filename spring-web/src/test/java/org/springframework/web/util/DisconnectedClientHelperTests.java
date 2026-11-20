/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.MessagingException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.testfixture.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DisconnectedClientHelper}.
 * @author Rossen Stoyanchev
 */
class DisconnectedClientHelperTests {

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

	@ParameterizedTest // gh-34264
	@MethodSource("excludedExceptionsTypes")
	void excludedExceptionTypes(Exception ex) {
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isFalse();
	}

	static List<Exception> excludedExceptionsTypes() {
		EOFException cause = new EOFException();
		return List.of(
				new ResourceAccessException("", cause),
				new DataAccessResourceFailureException("", new EOFException()),
				new MessagingException("", cause));
	}

	@Test // gh-34533
	void nullException() {
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(null)).isFalse();
	}

}
