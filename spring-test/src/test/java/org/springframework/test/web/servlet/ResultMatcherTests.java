/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.web.servlet;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ResultMatcherTests {

	@Test
	void whenProvidedMatcherPassesThenSoftAssertionsAlsoPasses() {
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(this::doNothing);
		StubMvcResult stubMvcResult = new StubMvcResult(null, null, null, null, null, null, null);

		assertThatNoException().isThrownBy(() -> resultMatcher.match(stubMvcResult));
	}

	@Test
	void whenOneOfMatcherFailsThenSoftAssertionFailsWithTheVerySameMessage() {
		String failMessage = "fail message";
		StubMvcResult stubMvcResult = new StubMvcResult(null, null, null, null, null, null, null);
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(failMatcher(failMessage));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage("[0] " + failMessage);
	}

	@Test
	void whenMultipleMatchersFailsThenSoftAssertionFailsWithOneErrorWithMessageContainingAllErrorMessagesWithTheSameOrder() {
		String firstFail = "firstFail";
		String secondFail = "secondFail";
		StubMvcResult stubMvcResult = new StubMvcResult(null, null, null, null, null, null, null);
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(failMatcher(firstFail), failMatcher(secondFail));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage("[0] " + firstFail + "\n[1] " + secondFail);
	}

	@NotNull
	private ResultMatcher failMatcher(String failMessage) {
		return result -> {
			throw new AssertionError(failMessage);
		};
	}

	void doNothing(MvcResult mvcResult) {}
}
