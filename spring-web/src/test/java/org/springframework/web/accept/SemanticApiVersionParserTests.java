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

package org.springframework.web.accept;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link SemanticApiVersionParser}.
 * @author Rossen Stoyanchev
 */
public class SemanticApiVersionParserTests {

	private final SemanticApiVersionParser parser = new SemanticApiVersionParser();


	@Test
	void parse() {
		testParse("0", 0, 0, 0);
		testParse("0.3", 0, 3, 0);
		testParse("4.5", 4, 5, 0);
		testParse("6.7.8", 6, 7, 8);
		testParse("v01", 1, 0, 0);
		testParse("version-1.2", 1, 2, 0);
	}

	private void testParse(String input, int major, int minor, int patch) {
		SemanticApiVersionParser.Version actual = this.parser.parseVersion(input);
		assertThat(actual.getMajor()).isEqualTo(major);
		assertThat(actual.getMinor()).isEqualTo(minor);
		assertThat(actual.getPatch()).isEqualTo(patch);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "v", "1a", "1.0a", "1.0.0a", "1.0.0.", "1.0.0-"})
	void parseInvalid(String input) {
		testParseInvalid(input);
	}

	private void testParseInvalid(String input) {
		assertThatIllegalStateException().isThrownBy(() -> this.parser.parseVersion(input))
				.withMessage("Invalid API version format");
	}

}
