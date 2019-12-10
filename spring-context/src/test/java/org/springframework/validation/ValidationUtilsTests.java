/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ValidationUtils}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 08.10.2004
 */
public class ValidationUtilsTests {

	@Test
	public void testInvokeValidatorWithNullValidator() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		assertThatIllegalArgumentException().isThrownBy(() ->
				ValidationUtils.invokeValidator(null, tb, errors));
	}

	@Test
	public void testInvokeValidatorWithNullErrors() throws Exception {
		TestBean tb = new TestBean();
		assertThatIllegalArgumentException().isThrownBy(() ->
				ValidationUtils.invokeValidator(new EmptyValidator(), tb, null));
	}

	@Test
	public void testInvokeValidatorSunnyDay() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.invokeValidator(new EmptyValidator(), tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
	}

	@Test
	public void testValidationUtilsSunnyDay() throws Exception {
		TestBean tb = new TestBean("");

		Validator testValidator = new EmptyValidator();
		tb.setName(" ");
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isFalse();

		tb.setName("Roddy");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isFalse();
	}

	@Test
	public void testValidationUtilsNull() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		Validator testValidator = new EmptyValidator();
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
	}

	@Test
	public void testValidationUtilsEmpty() throws Exception {
		TestBean tb = new TestBean("");
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		Validator testValidator = new EmptyValidator();
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
	}

	@Test
	public void testValidationUtilsEmptyVariants() {
		TestBean tb = new TestBean();

		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"});
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");

		errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"}, "msg");
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");
		assertThat(errors.getFieldError("name").getDefaultMessage()).isEqualTo("msg");
	}

	@Test
	public void testValidationUtilsEmptyOrWhitespace() throws Exception {
		TestBean tb = new TestBean();
		Validator testValidator = new EmptyOrWhitespaceValidator();

		// Test null
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

		// Test empty String
		tb.setName("");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

		// Test whitespace String
		tb.setName(" ");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");

		// Test OK
		tb.setName("Roddy");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertThat(errors.hasFieldErrors("name")).isFalse();
	}

	@Test
	public void testValidationUtilsEmptyOrWhitespaceVariants() {
		TestBean tb = new TestBean();
		tb.setName(" ");

		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"});
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");

		errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"}, "msg");
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY_OR_WHITESPACE");
		assertThat(errors.getFieldError("name").getArguments()[0]).isEqualTo("arg");
		assertThat(errors.getFieldError("name").getDefaultMessage()).isEqualTo("msg");
	}


	private static class EmptyValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(@Nullable Object obj, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY", "You must enter a name!");
		}
	}


	private static class EmptyOrWhitespaceValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(@Nullable Object obj, Errors errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", "You must enter a name!");
		}
	}

}
