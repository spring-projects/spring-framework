/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.TestBean;

/**
 * Unit tests for {@link ValidationUtils}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 08.10.2004
 */
public class ValidationUtilsTests {

	@Test(expected=IllegalArgumentException.class)
	public void testInvokeValidatorWithNullValidator() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.invokeValidator(null, tb, errors);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvokeValidatorWithNullErrors() throws Exception {
		TestBean tb = new TestBean();
		ValidationUtils.invokeValidator(new EmptyValidator(), tb, null);
	}

	@Test
	public void testInvokeValidatorSunnyDay() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.invokeValidator(new EmptyValidator(), tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY", errors.getFieldError("name").getCode());
	}

	@Test
	public void testValidationUtilsSunnyDay() throws Exception {
		TestBean tb = new TestBean("");

		Validator testValidator = new EmptyValidator();
		tb.setName(" ");
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertFalse(errors.hasFieldErrors("name"));

		tb.setName("Roddy");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertFalse(errors.hasFieldErrors("name"));
	}

	@Test
	public void testValidationUtilsNull() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		Validator testValidator = new EmptyValidator();
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY", errors.getFieldError("name").getCode());
	}

	@Test
	public void testValidationUtilsEmpty() throws Exception {
		TestBean tb = new TestBean("");
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		Validator testValidator = new EmptyValidator();
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY", errors.getFieldError("name").getCode());
	}

	@Test
	public void testValidationUtilsEmptyVariants() {
		TestBean tb = new TestBean();

		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"});
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());
		assertEquals("arg", errors.getFieldError("name").getArguments()[0]);

		errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"}, "msg");
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());
		assertEquals("arg", errors.getFieldError("name").getArguments()[0]);
		assertEquals("msg", errors.getFieldError("name").getDefaultMessage());
	}

	@Test
	public void testValidationUtilsEmptyOrWhitespace() throws Exception {
		TestBean tb = new TestBean();
		Validator testValidator = new EmptyOrWhitespaceValidator();

		// Test null
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());

		// Test empty String
		tb.setName("");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());

		// Test whitespace String
		tb.setName(" ");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());

		// Test OK
		tb.setName("Roddy");
		errors = new BeanPropertyBindingResult(tb, "tb");
		testValidator.validate(tb, errors);
		assertFalse(errors.hasFieldErrors("name"));
	}

	@Test
	public void testValidationUtilsEmptyOrWhitespaceVariants() {
		TestBean tb = new TestBean();
		tb.setName(" ");

		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"});
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());
		assertEquals("arg", errors.getFieldError("name").getArguments()[0]);

		errors = new BeanPropertyBindingResult(tb, "tb");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", new Object[] {"arg"}, "msg");
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());
		assertEquals("arg", errors.getFieldError("name").getArguments()[0]);
		assertEquals("msg", errors.getFieldError("name").getDefaultMessage());
	}


	private static class EmptyValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object obj, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY", "You must enter a name!");
		}
	}


	private static class EmptyOrWhitespaceValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object obj, Errors errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", "You must enter a name!");
		}
	}

}
