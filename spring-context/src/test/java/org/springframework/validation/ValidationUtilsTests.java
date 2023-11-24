/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.validation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link ValidationUtils}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @author Arjen Poutsma
 * @since 08.10.2004
 */
public class ValidationUtilsTests {

	private final Validator emptyValidator = Validator.forInstanceOf(TestBean.class, (testBean, errors) ->
			ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY", "You must enter a name!"));

	private final Validator emptyOrWhitespaceValidator = Validator.forInstanceOf(TestBean.class, (testBean, errors) ->
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", "You must enter a name!"));


	@Test
	public void testInvokeValidatorWithNullValidator() {
		TestBean tb = new TestBean();
		Errors errors = new SimpleErrors(tb);
		assertThatIllegalArgumentException().isThrownBy(() ->
				ValidationUtils.invokeValidator(null, tb, errors));
	}

	@Test
	public void testInvokeValidatorWithNullErrors() {
		TestBean tb = new TestBean();
		assertThatIllegalArgumentException().isThrownBy(() ->
				ValidationUtils.invokeValidator(emptyValidator, tb, null));
	}

	@Test
	public void testInvokeValidatorSunnyDay() {
		TestBean tb = new TestBean();
		Errors errors = new SimpleErrors(tb);
		ValidationUtils.invokeValidator(emptyValidator, tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
	}

	@Test
	public void testValidationUtilsSunnyDay() {
		TestBean tb = new TestBean("");

		tb.setName(" ");
		Errors errors = emptyValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isFalse();

		tb.setName("Roddy");
		errors = emptyValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isFalse();

		// Should not raise exception
		errors.failOnError(IllegalStateException::new);
	}

	@Test
	public void testValidationUtilsNull() {
		TestBean tb = new TestBean();
		Errors errors = emptyValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");

		assertThatIllegalStateException()
				.isThrownBy(() -> errors.failOnError(IllegalStateException::new))
				.withMessageContaining("'name'").withMessageContaining("EMPTY");
	}

	@Test
	public void testValidationUtilsEmpty() {
		TestBean tb = new TestBean("");
		Errors errors = emptyValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");

		assertThatIllegalStateException()
				.isThrownBy(() -> errors.failOnError(IllegalStateException::new))
				.withMessageContaining("'name'").withMessageContaining("EMPTY");
	}

	@Test
	public void testValidationUtilsEmptyVariants() {
		TestBean tb = new TestBean();

		Errors errors = new SimpleErrors(tb);
		ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"});
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");

		errors = new SimpleErrors(tb);
		ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"}, "msg");
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");
		assertThat(errors.getFieldError("name").getDefaultMessage()).isEqualTo("msg");
	}

	@Test
	public void testValidationUtilsEmptyOrWhitespace() {
		TestBean tb = new TestBean();

		// Test null
		Errors errors = emptyOrWhitespaceValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

		// Test empty String
		tb.setName("");
		errors = emptyOrWhitespaceValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

		// Test whitespace String
		tb.setName(" ");
		errors = emptyOrWhitespaceValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

		// Test OK
		tb.setName("Roddy");
		errors = emptyOrWhitespaceValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isFalse();
	}

	@Test
	public void testValidationUtilsEmptyOrWhitespaceVariants() {
		TestBean tb = new TestBean();
		tb.setName(" ");

		Errors errors = new SimpleErrors(tb);
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"});
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");

		errors = new SimpleErrors(tb);
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"}, "msg");
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");
		assertThat(errors.getFieldError("name").getDefaultMessage()).isEqualTo("msg");
	}

}
