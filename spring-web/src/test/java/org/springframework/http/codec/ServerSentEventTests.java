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

package org.springframework.http.codec;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

	private static Stream<Arguments> newLineCharacters() {
		return Stream.of(
				Arguments.of("\n", "LF"),
				Arguments.of("\r", "CR"),
				Arguments.of("\r\n", "CRLF")
		);
	}

}
