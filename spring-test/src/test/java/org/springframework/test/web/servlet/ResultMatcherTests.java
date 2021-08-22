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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for {@link ResultMatcher}.
 *
 * @author Michał Rowicki
 * @author Sam Brannen
 * @since 5.3.10
 */
class ResultMatcherTests {

	private final StubMvcResult stubMvcResult = new StubMvcResult(null, null, null, null, null, null, null);


	@Test
	void softAssertionsWithNoFailures() {
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(this::doNothing);

		assertThatNoException().isThrownBy(() -> resultMatcher.match(stubMvcResult));
	}

	@Test
	void softAssertionsWithOneFailure() {
		String failureMessage = "failure message";
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(failingMatcher(failureMessage));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage(failureMessage);
	}

	@Test
	void softAssertionsWithTwoFailures() {
		String firstFailure = "firstFailure";
		String secondFailure = "secondFailure";
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(failingMatcher(firstFailure), exceptionalMatcher(secondFailure));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage(firstFailure + System.lineSeparator() + secondFailure);
	}

	private ResultMatcher failingMatcher(String failureMessage) {
		return result -> Assertions.fail(failureMessage);
	}

	private ResultMatcher exceptionalMatcher(String failureMessage) {
		return result -> {
			throw new RuntimeException(failureMessage);
		};
	}

	void doNothing(MvcResult mvcResult) {}

}
