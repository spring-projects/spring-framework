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

package org.springframework.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PatternMatchUtils}.
 *
 * @author Juergen Hoeller
 * @author Johan Gorter
 * @author Sam Brannen
 */
class PatternMatchUtilsTests {

	@Test
	void nullAndEmptyValues() {
		assertDoesNotMatch((String) null, null);
		assertDoesNotMatch((String) null, "");
		assertDoesNotMatch("123", null);

		assertDoesNotMatch((String[]) null, null);
		assertDoesNotMatch((String[]) null, "");
		assertDoesNotMatch(new String[] {}, null);
	}

	@Test
	void trivial() {
		assertMatches("", "");
		assertMatches("123", "123");
		assertMatches("*", "123");

		assertMatches(new String[] { "" }, "");
		assertMatches(new String[] { "123" }, "123");
		assertMatches(new String[] { "*" }, "123");

		assertMatches(new String[] { null, "" }, "");
		assertMatches(new String[] { null, "123" }, "123");
		assertMatches(new String[] { null, "*" }, "123");
	}

	@Test
	void startsWith() {
		assertMatches("get*", "getMe");
		assertDoesNotMatch("get*", "setMe");
	}

	@Test
	void endsWith() {
		assertMatches("*Test", "getMeTest");
		assertDoesNotMatch("*Test", "setMe");
	}

	@Test
	void between() {
		assertDoesNotMatch("*stuff*", "getMeTest");
		assertMatches("*stuff*", "getstuffTest");
		assertMatches("*stuff*", "stuffTest");
		assertMatches("*stuff*", "getstuff");
		assertMatches("*stuff*", "stuff");
	}

	@Test
	void startsEnds() {
		assertMatches("on*Event", "onMyEvent");
		assertMatches("on*Event", "onEvent");
		assertDoesNotMatch("3*3", "3");
		assertMatches("3*3", "33");
	}

	@Test
	void startsEndsBetween() {
		assertMatches("12*45*78", "12345678");
		assertDoesNotMatch("12*45*78", "123456789");
		assertDoesNotMatch("12*45*78", "012345678");
		assertMatches("12*45*78", "124578");
		assertMatches("12*45*78", "1245457878");
		assertDoesNotMatch("3*3*3", "33");
		assertMatches("3*3*3", "333");
	}

	@Test
	void ridiculous() {
		assertMatches("*1*2*3*", "0011002001010030020201030");
		assertDoesNotMatch("1*2*3*4", "10300204");
		assertDoesNotMatch("1*2*3*3", "10300203");
		assertMatches("*1*2*3*", "123");
		assertDoesNotMatch("*1*2*3*", "132");
	}

	@Test
	void patternVariants() {
		assertDoesNotMatch("*a", "*");
		assertMatches("*a", "a");
		assertDoesNotMatch("*a", "b");
		assertMatches("*a", "aa");
		assertMatches("*a", "ba");
		assertDoesNotMatch("*a", "ab");
		assertDoesNotMatch("**a", "*");
		assertMatches("**a", "a");
		assertDoesNotMatch("**a", "b");
		assertMatches("**a", "aa");
		assertMatches("**a", "ba");
		assertDoesNotMatch("**a", "ab");
	}

	private void assertMatches(String pattern, String str) {
		assertThat(PatternMatchUtils.simpleMatch(pattern, str)).isTrue();
	}

	private void assertDoesNotMatch(String pattern, String str) {
		assertThat(PatternMatchUtils.simpleMatch(pattern, str)).isFalse();
	}

	private void assertMatches(String[] patterns, String str) {
		assertThat(PatternMatchUtils.simpleMatch(patterns, str)).isTrue();
	}

	private void assertDoesNotMatch(String[] patterns, String str) {
		assertThat(PatternMatchUtils.simpleMatch(patterns, str)).isFalse();
	}

}
