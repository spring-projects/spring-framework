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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

	private static final String EOL = "\n";

	private final StubMvcResult stubMvcResult = new StubMvcResult(null, null, null, null, null, null, null);


	@Test
	void softAssertionsWithNoFailures() {
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(this::doNothing);

		assertThatNoException().isThrownBy(() -> resultMatcher.match(stubMvcResult));
	}

	@Test
	void softAssertionsWithOneAssertionError() {
		String failureMessage = "error";
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(assertionErrorMatcher(failureMessage));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage(failureMessage)
				.withNoCause()
				.satisfies(error -> assertThat(error).hasNoSuppressedExceptions());
	}

	@Test
	void softAssertionsWithOneRuntimeException() {
		String failureMessage = "exception";
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(uncheckedExceptionMatcher(failureMessage));

		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage(failureMessage)
				.withNoCause()
				.satisfies(error -> assertThat(error).hasNoSuppressedExceptions());
	}

	@Test
	void softAssertionsWithOneCheckedException() {
		String failureMessage = "exception";
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(checkedExceptionMatcher(failureMessage));

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage(failureMessage)
				.withNoCause()
				.satisfies(exception -> assertThat(exception).hasNoSuppressedExceptions());
	}

	@Test
	void softAssertionsWithTwoFailures() {
		String firstFailure = "firstFailure";
		String secondFailure = "secondFailure";
		String thirdFailure = "thirdFailure";
		ResultMatcher resultMatcher = ResultMatcher.matchAllSoftly(assertionErrorMatcher(firstFailure),
			checkedExceptionMatcher(secondFailure), uncheckedExceptionMatcher(thirdFailure));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resultMatcher.match(stubMvcResult))
				.withMessage("Multiple Exceptions (3):" + EOL + firstFailure + EOL + secondFailure + EOL + thirdFailure)
				.satisfies(error -> assertThat(error.getSuppressed()).hasSize(3));
	}

	private ResultMatcher assertionErrorMatcher(String failureMessage) {
		return result -> {
			throw new AssertionError(failureMessage);
		};
	}

	private ResultMatcher uncheckedExceptionMatcher(String failureMessage) {
		return result -> {
			throw new RuntimeException(failureMessage);
		};
	}

	private ResultMatcher checkedExceptionMatcher(String failureMessage) {
		return result -> {
			throw new Exception(failureMessage);
		};
	}

	void doNothing(MvcResult mvcResult) {
	}

}
