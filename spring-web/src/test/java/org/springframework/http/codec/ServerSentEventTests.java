/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ServerSentEvent}.
 * @author Brian Clozel
 */
class ServerSentEventTests {

	@ParameterizedTest(name = "{1}")
	@MethodSource("newLineCharacters")
	void rejectsInvalidId(String newLine, String description) {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ServerSentEvent.<String>builder().id("first" + newLine + "second").build());
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("newLineCharacters")
	void rejectsInvalidEvent(String newLine, String description) {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ServerSentEvent.<String>builder().event("first" + newLine + "second").build());
	}


	@ParameterizedTest(name = "{1}")
	@MethodSource("newLineCharacters")
	void supportMultiLineComments(String newLine, String description) {
		ServerSentEvent<String> event = ServerSentEvent.<String>builder()
				.comment("foo" + newLine + "bar" + newLine + "baz").data("payload").build();
		assertThat(event.format()).isEqualTo(":foo\n:bar\n:baz\ndata: ");
	}

	private static Stream<Arguments> newLineCharacters() {
		return Stream.of(
				Arguments.of("\n", "LF"),
				Arguments.of("\r", "CR"),
				Arguments.of("\r\n", "CRLF")
		);
	}

}
