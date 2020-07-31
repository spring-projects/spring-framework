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

package org.springframework.validation.beanvalidation2;

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
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
@SuppressWarnings("resource")
public class ValidatorFactoryTests {

	@Test
	@SuppressWarnings("cast")
	public void testSimpleValidation() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		Set<ConstraintViolation<ValidPerson>> result = validator.validate(person);
		assertThat(result.size()).isEqualTo(2);
		for (ConstraintViolation<ValidPerson> cv : result) {
			String path = cv.getPropertyPath().toString();
			assertThat(path).matches(actual -> "name".equals(actual) || "address.street".equals(actual));
			assertThat(cv.getConstraintDescriptor().getAnnotation()).isInstanceOf(NotNull.class);
		}

		Validator nativeValidator = validator.unwrap(Validator.class);
		assertThat(nativeValidator.getClass().getName().startsWith("org.hibernate")).isTrue();
		assertThat(validator.unwrap(ValidatorFactory.class) instanceof HibernateValidatorFactory).isTrue();
		assertThat(validator.unwrap(HibernateValidatorFactory.class) instanceof HibernateValidatorFactory).isTrue();

		validator.destroy();
	}

	@Test
	@SuppressWarnings("cast")
	public void testSimpleValidationWithCustomProvider() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.setProviderClass(HibernateValidator.class);
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		Set<ConstraintViolation<ValidPerson>> result = validator.validate(person);
		assertThat(result.size()).isEqualTo(2);
		for (ConstraintViolation<ValidPerson> cv : result) {
			String path = cv.getPropertyPath().toString();
			assertThat(path).matches(actual -> "name".equals(actual) || "address.street".equals(actual));
			assertThat(cv.getConstraintDescriptor().getAnnotation()).isInstanceOf(NotNull.class);
		}

		Validator nativeValidator = validator.unwrap(Validator.class);
		assertThat(nativeValidator.getClass().getName().startsWith("org.hibernate")).isTrue();
		assertThat(validator.unwrap(ValidatorFactory.class) instanceof HibernateValidatorFactory).isTrue();
		assertThat(validator.unwrap(HibernateValidatorFactory.class) instanceof HibernateValidatorFactory).isTrue();

		validator.destroy();
	}

	@Test
	public void testSimpleValidationWithClassLevel() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.setName("Juergen");
		person.getAddress().setStreet("Juergen's Street");
		Set<ConstraintViolation<ValidPerson>> result = validator.validate(person);
		assertThat(result.size()).isEqualTo(1);
		Iterator<ConstraintViolation<ValidPerson>> iterator = result.iterator();
		ConstraintViolation<?> cv = iterator.next();
		assertThat(cv.getPropertyPath().toString()).isEqualTo("");
		assertThat(cv.getConstraintDescriptor().getAnnotation() instanceof NameAddressValid).isTrue();
	}

	@Test
	public void testSpringValidationFieldType() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.setName("Phil");
		person.getAddress().setStreet("Phil's Street");
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, errors);
		assertThat(errors.getErrorCount()).isEqualTo(1);
		assertThat(errors.getFieldError("address").getRejectedValue()).isInstanceOf(ValidAddress.class);
	}

	@Test
	public void testSpringValidation() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertThat(result.getErrorCount()).isEqualTo(2);
		FieldError fieldError = result.getFieldError("name");
		assertThat(fieldError.getField()).isEqualTo("name");
		List<String> errorCodes = Arrays.asList(fieldError.getCodes());
		assertThat(errorCodes.size()).isEqualTo(4);
		assertThat(errorCodes.contains("NotNull.person.name")).isTrue();
		assertThat(errorCodes.contains("NotNull.name")).isTrue();
		assertThat(errorCodes.contains("NotNull.java.lang.String")).isTrue();
		assertThat(errorCodes.contains("NotNull")).isTrue();
		fieldError = result.getFieldError("address.street");
		assertThat(fieldError.getField()).isEqualTo("address.street");
		errorCodes = Arrays.asList(fieldError.getCodes());
		assertThat(errorCodes.size()).isEqualTo(5);
		assertThat(errorCodes.contains("NotNull.person.address.street")).isTrue();
		assertThat(errorCodes.contains("NotNull.address.street")).isTrue();
		assertThat(errorCodes.contains("NotNull.street")).isTrue();
		assertThat(errorCodes.contains("NotNull.java.lang.String")).isTrue();
		assertThat(errorCodes.contains("NotNull")).isTrue();
	}

	@Test
	public void testSpringValidationWithClassLevel() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.setName("Juergen");
		person.getAddress().setStreet("Juergen's Street");
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertThat(result.getErrorCount()).isEqualTo(1);
		ObjectError globalError = result.getGlobalError();
		List<String> errorCodes = Arrays.asList(globalError.getCodes());
		assertThat(errorCodes.size()).isEqualTo(2);
		assertThat(errorCodes.contains("NameAddressValid.person")).isTrue();
		assertThat(errorCodes.contains("NameAddressValid")).isTrue();
	}

	@Test
	public void testSpringValidationWithAutowiredValidator() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				LocalValidatorFactoryBean.class);
		LocalValidatorFactoryBean validator = ctx.getBean(LocalValidatorFactoryBean.class);

		ValidPerson person = new ValidPerson();
		person.expectsAutowiredValidator = true;
		person.setName("Juergen");
		person.getAddress().setStreet("Juergen's Street");
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertThat(result.getErrorCount()).isEqualTo(1);
		ObjectError globalError = result.getGlobalError();
		List<String> errorCodes = Arrays.asList(globalError.getCodes());
		assertThat(errorCodes.size()).isEqualTo(2);
		assertThat(errorCodes.contains("NameAddressValid.person")).isTrue();
		assertThat(errorCodes.contains("NameAddressValid")).isTrue();
		ctx.close();
	}

	@Test
	public void testSpringValidationWithErrorInListElement() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.getAddressList().add(new ValidAddress());
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertThat(result.getErrorCount()).isEqualTo(3);
		FieldError fieldError = result.getFieldError("name");
		assertThat(fieldError.getField()).isEqualTo("name");
		fieldError = result.getFieldError("address.street");
		assertThat(fieldError.getField()).isEqualTo("address.street");
		fieldError = result.getFieldError("addressList[0].street");
		assertThat(fieldError.getField()).isEqualTo("addressList[0].street");
	}

	@Test
	public void testSpringValidationWithErrorInSetElement() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ValidPerson person = new ValidPerson();
		person.getAddressSet().add(new ValidAddress());
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, result);
		assertThat(result.getErrorCount()).isEqualTo(3);
		FieldError fieldError = result.getFieldError("name");
		assertThat(fieldError.getField()).isEqualTo("name");
		fieldError = result.getFieldError("address.street");
		assertThat(fieldError.getField()).isEqualTo("address.street");
		fieldError = result.getFieldError("addressSet[].street");
		assertThat(fieldError.getField()).isEqualTo("addressSet[].street");
	}

	@Test
	public void testInnerBeanValidation() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		MainBean mainBean = new MainBean();
		Errors errors = new BeanPropertyBindingResult(mainBean, "mainBean");
		validator.validate(mainBean, errors);
		Object rejected = errors.getFieldValue("inner.value");
		assertThat(rejected).isNull();
	}

	@Test
	public void testValidationWithOptionalField() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		MainBeanWithOptional mainBean = new MainBeanWithOptional();
		Errors errors = new BeanPropertyBindingResult(mainBean, "mainBean");
		validator.validate(mainBean, errors);
		Object rejected = errors.getFieldValue("inner.value");
		assertThat(rejected).isNull();
	}

	@Test
	public void testListValidation() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		ListContainer listContainer = new ListContainer();
		listContainer.addString("A");
		listContainer.addString("X");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(listContainer, "listContainer");
		errors.initConversion(new DefaultConversionService());
		validator.validate(listContainer, errors);

		FieldError fieldError = errors.getFieldError("list[1]");
		assertThat(fieldError).isNotNull();
		assertThat(fieldError.getRejectedValue()).isEqualTo("X");
		assertThat(errors.getFieldValue("list[1]")).isEqualTo("X");
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
				assertThat(this.environment).isNotNull();
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


	public static class ListContainer {

		@NotXList
		private List<String> list = new LinkedList<>();

		public void addString(String value) {
			list.add(value);
		}

		public List<String> getList() {
			return list;
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Constraint(validatedBy = NotXListValidator.class)
	public @interface NotXList {

		String message() default "Should not be X";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}


	public static class NotXListValidator implements ConstraintValidator<NotXList, List<String>> {

		@Override
		public void initialize(NotXList constraintAnnotation) {
		}

		@Override
		public boolean isValid(List<String> list, ConstraintValidatorContext context) {
			context.disableDefaultConstraintViolation();
			boolean valid = true;
			for (int i = 0; i < list.size(); i++) {
				if ("X".equals(list.get(i))) {
					context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()).addBeanNode().inIterable().atIndex(i).addConstraintViolation();
					valid = false;
				}
			}
			return valid;
		}
	}

}
