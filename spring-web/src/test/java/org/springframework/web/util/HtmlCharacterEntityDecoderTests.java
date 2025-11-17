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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link HtmlCharacterEntityDecoder}.
 */
class HtmlCharacterEntityDecoderTests {

	@Test
	void unescapeHandlesSupplementaryCharactersAsDecimal() {
		String expectedCharacter = "😀";
		String decimalEntity = "&#128512;";
		String actualResultFromDecimal = HtmlUtils.htmlUnescape(decimalEntity);
		assertThat(actualResultFromDecimal).as("Decimal entity was not converted correctly.").isEqualTo(expectedCharacter);
	}

	@Test
	void unescapeHandlesSupplementaryCharactersAsHexadecimal() {
		String expectedCharacter = "😀";
		String hexEntity = "&#x1F600;";
		String actualResultFromHex = HtmlUtils.htmlUnescape(hexEntity);
		assertThat(actualResultFromHex).as("Hexadecimal entity was not converted correctly.").isEqualTo(expectedCharacter);
	}

	@Test
	void unescapeHandlesBasicEntities() {
		String input = "&lt;p&gt;Tom &amp; Jerry&#39;s &quot;Show&quot;&lt;/p&gt;";
		String expectedOutput = "<p>Tom & Jerry's \"Show\"</p>";
		String actualOutput = HtmlUtils.htmlUnescape(input);
		assertThat(actualOutput).as("Basic HTML entities were not unescaped correctly.").isEqualTo(expectedOutput);
	}

}
