/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.nativex;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonUtils}.
 *
 * @author Sebastien Deleuze
 */
public class JsonUtilsTests {

	@Test
	void unescaped() {
		assertThat(JsonUtils.escape("azerty")).isEqualTo("azerty");
	}

	@Test
	void escapeDoubleQuote() {
		assertThat(JsonUtils.escape("foo\"bar")).isEqualTo("foo\\\"bar");
	}

	@Test
	void escapeBackslash() {
		assertThat(JsonUtils.escape("foo\"bar")).isEqualTo("foo\\\"bar");
	}

	@Test
	void escapeBackspace() {
		assertThat(JsonUtils.escape("foo\bbar")).isEqualTo("foo\\bbar");
	}

	@Test
	void escapeFormfeed() {
		assertThat(JsonUtils.escape("foo\fbar")).isEqualTo("foo\\fbar");
	}

	@Test
	void escapeNewline() {
		assertThat(JsonUtils.escape("foo\nbar")).isEqualTo("foo\\nbar");
	}

	@Test
	void escapeCarriageReturn() {
		assertThat(JsonUtils.escape("foo\rbar")).isEqualTo("foo\\rbar");
	}

	@Test
	void escapeTab() {
		assertThat(JsonUtils.escape("foo\tbar")).isEqualTo("foo\\tbar");
	}

	@Test
	void escapeUnicode() {
		assertThat(JsonUtils.escape("foo\u001Fbar")).isEqualTo("foo\\u001fbar");
	}
}
