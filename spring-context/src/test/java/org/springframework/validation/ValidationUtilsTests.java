/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ValidationUtils}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @author Arjen Poutsma
 * @since 08.10.2004
 */
class ValidationUtilsTests {

	private final Validator emptyValidator = Validator.forInstanceOf(TestBean.class, (testBean, errors) ->
			ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY", "You must enter a name!"));

	private final Validator emptyOrWhitespaceValidator = Validator.forInstanceOf(TestBean.class, (testBean, errors) ->
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", "You must enter a name!"));


	@Test
	void invokeValidatorWithNullValidator() {
		TestBean tb = new TestBean();
		Errors errors = new SimpleErrors(tb);
		assertThatIllegalArgumentException().isThrownBy(() ->
				ValidationUtils.invokeValidator(null, tb, errors));
	}

	@Test
	void invokeValidatorWithNullErrors() {
		TestBean tb = new TestBean();
		assertThatIllegalArgumentException().isThrownBy(() ->
				ValidationUtils.invokeValidator(emptyValidator, tb, null));
	}

	@Test
	void invokeValidatorSunnyDay() {
		TestBean tb = new TestBean();
		Errors errors = new SimpleErrors(tb);
		ValidationUtils.invokeValidator(emptyValidator, tb, errors);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");
	}

	@Test
	void validationUtilsSunnyDay() {
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
	void validationUtilsNull() {
		TestBean tb = new TestBean();
		Errors errors = emptyValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");

		assertThatIllegalStateException()
				.isThrownBy(() -> errors.failOnError(IllegalStateException::new))
				.withMessageContaining("'name'").withMessageContaining("EMPTY");
	}

	@Test
	void validationUtilsEmpty() {
		TestBean tb = new TestBean("");
		Errors errors = emptyValidator.validateObject(tb);
		assertThat(errors.hasFieldErrors("name")).isTrue();
		assertThat(errors.getFieldError("name").getCode()).isEqualTo("EMPTY");

		assertThatIllegalStateException()
				.isThrownBy(() -> errors.failOnError(IllegalStateException::new))
				.withMessageContaining("'name'").withMessageContaining("EMPTY");
	}

	@Test
	void validationUtilsEmptyVariants() {
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
	void validationUtilsEmptyOrWhitespace() {
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
	void validationUtilsEmptyOrWhitespaceVariants() {
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
