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

package org.springframework.test.validation;

import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to
 * {@link BindingResult}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @param <SELF> the type of assertions
 */
public abstract class AbstractBindingResultAssert<SELF extends AbstractBindingResultAssert<SELF>> extends AbstractAssert<SELF, BindingResult> {

	private final Failures failures = Failures.instance();

	private final String name;


	protected AbstractBindingResultAssert(String name, BindingResult bindingResult, Class<?> selfType) {
		super(bindingResult, selfType);
		this.name = name;
		as("Binding result for attribute '%s", this.name);
	}

	/**
	 * Verify that the total number of errors is equal to the expected value.
	 * @param expected the expected number of errors
	 */
	public SELF hasErrorsCount(int expected) {
		assertThat(this.actual.getErrorCount())
				.as("check errors for attribute '%s'", this.name).isEqualTo(expected);
		return this.myself;
	}

	/**
	 * Verify that the actual binding result contains fields in error with the
	 * given {@code fieldNames}.
	 * @param fieldNames the names of fields that should be in error
	 */
	public SELF hasFieldErrors(String... fieldNames) {
		assertThat(fieldErrorNames()).contains(fieldNames);
		return this.myself;
	}

	/**
	 * Verify that the actual binding result contains <em>only</em> fields in
	 * error with the given {@code fieldNames}, and nothing else.
	 * @param fieldNames the exhaustive list of field names that should be in error
	 */
	public SELF hasOnlyFieldErrors(String... fieldNames) {
		assertThat(fieldErrorNames()).containsOnly(fieldNames);
		return this.myself;
	}

	/**
	 * Verify that the field with the given {@code fieldName} has an error
	 * matching the given {@code errorCode}.
	 * @param fieldName the name of a field in error
	 * @param errorCode the error code for that field
	 */
	public SELF hasFieldErrorCode(String fieldName, String errorCode) {
		Assertions.assertThat(getFieldError(fieldName).getCode())
				.as("check error code for field '%s'", fieldName).isEqualTo(errorCode);
		return this.myself;
	}

	protected AssertionError unexpectedBindingResult(String reason, Object... arguments) {
		return this.failures.failure(this.info, new UnexpectedBindingResult(reason, arguments));
	}

	private AssertProvider<ListAssert<String>> fieldErrorNames() {
		return () -> {
			List<String> actual = this.actual.getFieldErrors().stream().map(FieldError::getField).toList();
			return new ListAssert<>(actual).as("check field errors");
		};
	}

	private FieldError getFieldError(String fieldName) {
		FieldError fieldError = this.actual.getFieldError(fieldName);
		if (fieldError == null) {
			throw unexpectedBindingResult("to have at least an error for field '%s'", fieldName);
		}
		return fieldError;
	}


	private final class UnexpectedBindingResult extends BasicErrorMessageFactory {

		private UnexpectedBindingResult(String reason, Object... arguments) {
			super("%nExpecting binding result:%n  %s%n%s", AbstractBindingResultAssert.this.actual,
					reason.formatted(arguments));
		}
	}

}
