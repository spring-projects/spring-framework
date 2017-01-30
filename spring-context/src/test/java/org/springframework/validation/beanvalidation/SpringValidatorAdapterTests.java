/*
 * Copyright 2002-2017 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.Validation;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * @author Kazuki Shimizu
 * @author Juergen Hoeller
 * @since 4.3
 */
public class SpringValidatorAdapterTests {

	private final SpringValidatorAdapter validatorAdapter = new SpringValidatorAdapter(
			Validation.buildDefaultValidatorFactory().getValidator());

	private final StaticMessageSource messageSource = new StaticMessageSource();


	@Before
	public void setupSpringValidatorAdapter() {
		messageSource.addMessage("Size", Locale.ENGLISH, "Size of {0} is must be between {2} and {1}");
		messageSource.addMessage("Same", Locale.ENGLISH, "{2} must be same value with {1}");
		messageSource.addMessage("password", Locale.ENGLISH, "Password");
		messageSource.addMessage("confirmPassword", Locale.ENGLISH, "Password(Confirm)");
	}


	@Test  // SPR-13406
	public void testNoStringArgumentValue() {
		TestBean testBean = new TestBean();
		testBean.setPassword("pass");
		testBean.setConfirmPassword("pass");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(testBean, "testBean");
		validatorAdapter.validate(testBean, errors);

		assertThat(errors.getFieldErrorCount("password"), is(1));
		assertThat(messageSource.getMessage(errors.getFieldError("password"), Locale.ENGLISH),
				is("Size of Password is must be between 8 and 128"));
	}

	@Test  // SPR-13406
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

	@Test  // SPR-13406
	public void testApplyMessageSourceResolvableToStringArgumentValueWithUnresolvedLogicalFieldName() {
		TestBean testBean = new TestBean();
		testBean.setEmail("test@example.com");
		testBean.setConfirmEmail("TEST@EXAMPLE.IO");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(testBean, "testBean");
		validatorAdapter.validate(testBean, errors);

		assertThat(errors.getFieldErrorCount("email"), is(1));
		assertThat(errors.getFieldErrorCount("confirmEmail"), is(1));
		assertThat(messageSource.getMessage(errors.getFieldError("email"), Locale.ENGLISH),
				is("email must be same value with confirmEmail"));
		assertThat(messageSource.getMessage(errors.getFieldError("confirmEmail"), Locale.ENGLISH),
				is("Email required"));
	}

	@Test  // SPR-15123
	public void testApplyMessageSourceResolvableToStringArgumentValueWithAlwaysUseMessageFormat() {
		messageSource.setAlwaysUseMessageFormat(true);

		TestBean testBean = new TestBean();
		testBean.setEmail("test@example.com");
		testBean.setConfirmEmail("TEST@EXAMPLE.IO");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(testBean, "testBean");
		validatorAdapter.validate(testBean, errors);

		assertThat(errors.getFieldErrorCount("email"), is(1));
		assertThat(errors.getFieldErrorCount("confirmEmail"), is(1));
		assertThat(messageSource.getMessage(errors.getFieldError("email"), Locale.ENGLISH),
				is("email must be same value with confirmEmail"));
		assertThat(messageSource.getMessage(errors.getFieldError("confirmEmail"), Locale.ENGLISH),
				is("Email required"));
	}


	@Same(field = "password", comparingField = "confirmPassword")
	@Same(field = "email", comparingField = "confirmEmail")
	static class TestBean {

		@Size(min = 8, max = 128)
		private String password;

		private String confirmPassword;

		private String email;

		@Pattern(regexp = "[\\p{L} -]*", message = "Email required")
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
	@interface Same {

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
	@interface SameGroup {

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
			boolean matched = ObjectUtils.nullSafeEquals(fieldValue, comparingFieldValue);
			if (matched) {
				return true;
			}
			else {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate(message)
						.addPropertyNode(field)
						.addConstraintViolation();
				return false;
			}
		}
	}

}
