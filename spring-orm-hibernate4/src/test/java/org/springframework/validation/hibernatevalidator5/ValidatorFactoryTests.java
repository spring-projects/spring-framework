/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.validation.hibernatevalidator5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorFactory;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Copy of {@link org.springframework.validation.beanvalidation.ValidatorFactoryTests},
 * here to be tested against Hibernate Validator 5.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public class ValidatorFactoryTests {

	@Test
	public void testSimpleValidation() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		Set<ConstraintViolation<ValidPerson>> result = validator.validate(person);
		assertEquals(2, result.size());
		for (ConstraintViolation<ValidPerson> cv : result) {
			String path = cv.getPropertyPath().toString();
			if ("name".equals(path) || "address.street".equals(path)) {
				assertTrue(cv.getConstraintDescriptor().getAnnotation() instanceof NotNull);
			}
			else {
				fail("Invalid constraint violation with path '" + path + "'");
			}
		}

		Validator nativeValidator = validator.unwrap(Validator.class);
		assertTrue(nativeValidator.getClass().getName().startsWith("org.hibernate"));
		assertTrue(validator.unwrap(ValidatorFactory.class) instanceof HibernateValidatorFactory);
		assertTrue(validator.unwrap(HibernateValidatorFactory.class) instanceof HibernateValidatorFactory);

		validator.destroy();
	}

	@Test
	public void testSimpleValidationWithCustomProvider() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.setProviderClass(HibernateValidator.class);
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		Set<ConstraintViolation<ValidPerson>> result = validator.validate(person);
		assertEquals(2, result.size());
		for (ConstraintViolation<ValidPerson> cv : result) {
			String path = cv.getPropertyPath().toString();
			if ("name".equals(path) || "address.street".equals(path)) {
				assertTrue(cv.getConstraintDescriptor().getAnnotation() instanceof NotNull);
			}
			else {
				fail("Invalid constraint violation with path '" + path + "'");
			}
		}

		Validator nativeValidator = validator.unwrap(Validator.class);
		assertTrue(nativeValidator.getClass().getName().startsWith("org.hibernate"));
		assertTrue(validator.unwrap(ValidatorFactory.class) instanceof HibernateValidatorFactory);
		assertTrue(validator.unwrap(HibernateValidatorFactory.class) instanceof HibernateValidatorFactory);

		validator.destroy();
	}

	@Test
	public void testSimpleValidationWithClassLevel() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		ValidPerson person = new ValidPerson();
		person.setName("Juergen");
		person.getAddress().setStreet("Juergen's Street");
		Set<ConstraintViolation<ValidPerson>> result = validator.validate(person);
		assertEquals(1, result.size());
		Iterator<ConstraintViolation<ValidPerson>> iterator = result.iterator();
		ConstraintViolation<?> cv = iterator.next();
		assertEquals("", cv.getPropertyPath().toString());
		assertTrue(cv.getConstraintDescriptor().getAnnotation() instanceof NameAddressValid);
	}

	@Test
	public void testSpringValidationFieldType() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.setName("Phil");
		person.getAddress().setStreet("Phil's Street");
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, errors);
		assertEquals(1, errors.getErrorCount());
		assertThat("Field/Value type mismatch", errors.getFieldError("address").getRejectedValue(),
				instanceOf(ValidAddress.class));
	}

	@Test
	public void testSpringValidation() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertEquals(2, result.getErrorCount());
		FieldError fieldError = result.getFieldError("name");
		assertEquals("name", fieldError.getField());
		List<String> errorCodes = Arrays.asList(fieldError.getCodes());
		assertEquals(4, errorCodes.size());
		assertTrue(errorCodes.contains("NotNull.person.name"));
		assertTrue(errorCodes.contains("NotNull.name"));
		assertTrue(errorCodes.contains("NotNull.java.lang.String"));
		assertTrue(errorCodes.contains("NotNull"));
		fieldError = result.getFieldError("address.street");
		assertEquals("address.street", fieldError.getField());
		errorCodes = Arrays.asList(fieldError.getCodes());
		assertEquals(5, errorCodes.size());
		assertTrue(errorCodes.contains("NotNull.person.address.street"));
		assertTrue(errorCodes.contains("NotNull.address.street"));
		assertTrue(errorCodes.contains("NotNull.street"));
		assertTrue(errorCodes.contains("NotNull.java.lang.String"));
		assertTrue(errorCodes.contains("NotNull"));
	}

	@Test
	public void testSpringValidationWithClassLevel() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.setName("Juergen");
		person.getAddress().setStreet("Juergen's Street");
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertEquals(1, result.getErrorCount());
		ObjectError globalError = result.getGlobalError();
		List<String> errorCodes = Arrays.asList(globalError.getCodes());
		assertEquals(2, errorCodes.size());
		assertTrue(errorCodes.contains("NameAddressValid.person"));
		assertTrue(errorCodes.contains("NameAddressValid"));
	}

	@Test
	public void testSpringValidationWithAutowiredValidator() throws Exception {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				LocalValidatorFactoryBean.class);
		LocalValidatorFactoryBean validator = ctx.getBean(LocalValidatorFactoryBean.class);

		ValidPerson person = new ValidPerson();
		person.expectsAutowiredValidator = true;
		person.setName("Juergen");
		person.getAddress().setStreet("Juergen's Street");
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertEquals(1, result.getErrorCount());
		ObjectError globalError = result.getGlobalError();
		List<String> errorCodes = Arrays.asList(globalError.getCodes());
		assertEquals(2, errorCodes.size());
		assertTrue(errorCodes.contains("NameAddressValid.person"));
		assertTrue(errorCodes.contains("NameAddressValid"));
		ctx.close();
	}

	@Test
	public void testSpringValidationWithErrorInListElement() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.getAddressList().add(new ValidAddress());
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertEquals(3, result.getErrorCount());
		FieldError fieldError = result.getFieldError("name");
		assertEquals("name", fieldError.getField());
		fieldError = result.getFieldError("address.street");
		assertEquals("address.street", fieldError.getField());
		fieldError = result.getFieldError("addressList[0].street");
		assertEquals("addressList[0].street", fieldError.getField());
	}

	@Test
	public void testSpringValidationWithErrorInSetElement() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.getAddressSet().add(new ValidAddress());
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertEquals(3, result.getErrorCount());
		FieldError fieldError = result.getFieldError("name");
		assertEquals("name", fieldError.getField());
		fieldError = result.getFieldError("address.street");
		assertEquals("address.street", fieldError.getField());
		fieldError = result.getFieldError("addressSet[].street");
		assertEquals("addressSet[].street", fieldError.getField());
	}

	@Test
	public void testInnerBeanValidation() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		MainBean mainBean = new MainBean();
		Errors errors = new BeanPropertyBindingResult(mainBean, "mainBean");
		validator.validate(mainBean, errors);
		Object rejected = errors.getFieldValue("inner.value");
		assertNull(rejected);
	}

	@Test
	public void testValidationWithOptionalField() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		MainBeanWithOptional mainBean = new MainBeanWithOptional();
		Errors errors = new BeanPropertyBindingResult(mainBean, "mainBean");
		validator.validate(mainBean, errors);
		Object rejected = errors.getFieldValue("inner.value");
		assertNull(rejected);
	}


	@NameAddressValid
	public static class ValidPerson {

		@NotNull
		private String name;

		@Valid
		private ValidAddress address = new ValidAddress();

		@Valid
		private List<ValidAddress> addressList = new LinkedList<>();

		@Valid
		private Set<ValidAddress> addressSet = new LinkedHashSet<>();

		public boolean expectsAutowiredValidator = false;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ValidAddress getAddress() {
			return address;
		}

		public void setAddress(ValidAddress address) {
			this.address = address;
		}

		public List<ValidAddress> getAddressList() {
			return addressList;
		}

		public void setAddressList(List<ValidAddress> addressList) {
			this.addressList = addressList;
		}

		public Set<ValidAddress> getAddressSet() {
			return addressSet;
		}

		public void setAddressSet(Set<ValidAddress> addressSet) {
			this.addressSet = addressSet;
		}
	}


	public static class ValidAddress {

		@NotNull
		private String street;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}
	}


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Constraint(validatedBy = NameAddressValidator.class)
	public @interface NameAddressValid {

		String message() default "Street must not contain name";

		Class<?>[] groups() default {};

		Class<?>[] payload() default {};
	}


	public static class NameAddressValidator implements ConstraintValidator<NameAddressValid, ValidPerson> {

		@Autowired
		private Environment environment;

		@Override
		public void initialize(NameAddressValid constraintAnnotation) {
		}

		@Override
		public boolean isValid(ValidPerson value, ConstraintValidatorContext context) {
			if (value.expectsAutowiredValidator) {
				assertNotNull(this.environment);
			}
			boolean valid = (value.name == null || !value.address.street.contains(value.name));
			if (!valid && "Phil".equals(value.name)) {
				context.buildConstraintViolationWithTemplate(
						context.getDefaultConstraintMessageTemplate()).addPropertyNode("address").addConstraintViolation().disableDefaultConstraintViolation();
			}
			return valid;
		}
	}


	public static class MainBean {

		@InnerValid
		private InnerBean inner = new InnerBean();

		public InnerBean getInner() {
			return inner;
		}
	}


	public static class MainBeanWithOptional {

		@InnerValid
		private InnerBean inner = new InnerBean();

		public Optional<InnerBean> getInner() {
			return Optional.ofNullable(inner);
		}
	}


	public static class InnerBean {

		private String value;

		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Constraint(validatedBy=InnerValidator.class)
	public static @interface InnerValid {

		String message() default "NOT VALID";

		Class<?>[] groups() default { };

		Class<? extends Payload>[] payload() default {};
	}


	public static class InnerValidator implements ConstraintValidator<InnerValid, InnerBean> {

		@Override
		public void initialize(InnerValid constraintAnnotation) {
		}

		@Override
		public boolean isValid(InnerBean bean, ConstraintValidatorContext context) {
			context.disableDefaultConstraintViolation();
			if (bean.getValue() == null) {
				context.buildConstraintViolationWithTemplate("NULL").addPropertyNode("value").addConstraintViolation();
				return false;
			}
			return true;
		}
	}

}
