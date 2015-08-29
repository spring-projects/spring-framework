/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;

import javax.validation.*;
import javax.validation.constraints.Size;
import java.lang.annotation.*;
import java.util.Locale;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tested against SpringValidatorAdapter.
 *
 * @author Kazuki Shimizu
 * @since 4.2.1
 */
public class SpringValidatorAdapterTests {

	private static SpringValidatorAdapter validatorAdapter;
	private static StaticMessageSource messageSource;

	@BeforeClass
	public static void setUpSpringValidatorAdapter() {
		validatorAdapter = new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());

		messageSource = new StaticMessageSource();
		messageSource.addMessage("Size", Locale.ENGLISH, "Size of {0} is must be between {2} and {1}");
		messageSource.addMessage("Same", Locale.ENGLISH, "{2} must be same value with {1}");
		messageSource.addMessage("password", Locale.ENGLISH, "Password");
		messageSource.addMessage("confirmPassword", Locale.ENGLISH, "Password(Confirm)");
	}

	/**
	 * for SPR-13406
	 */
	@Test
	public void testApplyMessageSourceResolvableToStringArgumentValueWithResolvedLogicalFieldName() {

		TestBean testBean = new TestBean();
		testBean.setPassword("password");
		testBean.setConfirmPassword("PASSWORD");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(testBean, "testBean");
		validatorAdapter.validate(testBean, errors);

		assertThat(errors.getFieldErrorCount("password"), is(1));
		assertThat(messageSource.getMessage(errors.getFieldError("password"), Locale.ENGLISH),
				is("Password must be same value with Password(Confirm)"));

	}

	/**
	 * for SPR-13406
	 */
	@Test
	public void testApplyMessageSourceResolvableToStringArgumentValueWithUnresolvedLogicalFieldName() {

		TestBean testBean = new TestBean();
		testBean.setEmail("test@example.com");
		testBean.setConfirmEmail("TEST@EXAMPLE.IO");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(testBean, "testBean");
		validatorAdapter.validate(testBean, errors);

		assertThat(errors.getFieldErrorCount("email"), is(1));
		assertThat(messageSource.getMessage(errors.getFieldError("email"), Locale.ENGLISH),
				is("email must be same value with confirmEmail"));

	}

	/**
	 * for SPR-13406
	 */
	@Test
	public void testNoneStringArgumentValue() {

		TestBean testBean = new TestBean();
		testBean.setPassword("pass");
		testBean.setConfirmPassword("pass");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(testBean, "testBean");
		validatorAdapter.validate(testBean, errors);

		assertThat(errors.getFieldErrorCount("password"), is(1));
		assertThat(messageSource.getMessage(errors.getFieldError("password"), Locale.ENGLISH),
				is("Size of Password is must be between 8 and 128"));

	}

@Same(field = "password", comparingField = "confirmPassword")
@Same(field = "email", comparingField = "confirmEmail")
static class TestBean {

	@Size(min = 8, max = 128)
	private String password;
	private String confirmPassword;

	private String email;
	private String confirmEmail;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getConfirmEmail() {
		return confirmEmail;
	}

	public void setConfirmEmail(String confirmEmail) {
		this.confirmEmail = confirmEmail;
	}
}

	@Documented
	@Constraint(validatedBy = {SameValidator.class})
	@Target({TYPE, ANNOTATION_TYPE})
	@Retention(RUNTIME)
	@Repeatable(SameGroup.class)
	public @interface Same {

		String message() default "{org.springframework.validation.beanvalidation.Same.message}";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};

		String field();

		String comparingField();

		@Target({TYPE, ANNOTATION_TYPE})
		@Retention(RUNTIME)
		@Documented
		@interface List {
			Same[] value();
		}

	}

	@Documented
	@Inherited
	@Retention(RUNTIME)
	@Target({TYPE, ANNOTATION_TYPE})
	public @interface SameGroup {

		Same[] value();

	}

	public static class SameValidator implements ConstraintValidator<Same, Object> {
		private String field;
		private String comparingField;
		private String message;

		public void initialize(Same constraintAnnotation) {
			field = constraintAnnotation.field();
			comparingField = constraintAnnotation.comparingField();
			message = constraintAnnotation.message();
		}

		public boolean isValid(Object value, ConstraintValidatorContext context) {
			BeanWrapper beanWrapper = new BeanWrapperImpl(value);
			Object fieldValue = beanWrapper.getPropertyValue(field);
			Object comparingFieldValue = beanWrapper.getPropertyValue(comparingField);
			boolean matched = ObjectUtils.nullSafeEquals(fieldValue,
					comparingFieldValue);
			if (matched) {
				return true;
			} else {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate(message)
						.addNode(field)
						.addConstraintViolation();
				return false;
			}
		}

	}


}
