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

package org.springframework.web.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SseUtils}.
 * @author Brian Clozel
 */
class SseUtilsTests {

	@Test
	void appendFieldValueWithoutLineSeparatorAppendsAsIs() {
		StringBuilder sb = new StringBuilder();
		sb.append("data:");
		SseUtils.appendFieldValue("data", "no newlines here", sb);
		assertThat(sb).hasToString("data:no newlines here");
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("newLineCharacters")
	void appendFieldValueReplacesLineSeparatorWithFieldPrefix(String newLine, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append("data:");
		SseUtils.appendFieldValue("data", "first" + newLine + "second", sb);
		assertThat(sb).hasToString("data:first\ndata:second");
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("newLineCharacters")
	void appendFieldValueUsesEmptyFieldForComments(String newLine, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append(":");
		SseUtils.appendFieldValue("", "first" + newLine + "second", sb);
		assertThat(sb).hasToString(":first\n:second");
	}

	@Test
	void assertNoLineSeparatorAcceptsPlainContent() {
		SseUtils.assertNoLineSeparator("no newlines here");
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("newLineCharacters")
	void assertNoLineSeparatorRejectsLineSeparator(String newLine, String description) {
		assertThatIllegalArgumentException().isThrownBy(() ->
				SseUtils.assertNoLineSeparator("first" + newLine + "second"));
	}

	static Stream<Arguments> newLineCharacters() {
		return Stream.of(
				Arguments.of("\n", "LF"),
				Arguments.of("\r", "CR"),
				Arguments.of("\r\n", "CRLF")
		);
	}

}
