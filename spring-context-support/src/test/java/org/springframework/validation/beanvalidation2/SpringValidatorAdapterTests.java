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

package org.springframework.validation.beanvalidation2;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * @author Kazuki Shimizu
 * @author Juergen Hoeller
 */
public class SpringValidatorAdapterTests {

	private final Validator nativeValidator = Validation.buildDefaultValidatorFactory().getValidator();

	private final SpringValidatorAdapter validatorAdapter = new SpringValidatorAdapter(nativeValidator);

	private final StaticMessageSource messageSource = new StaticMessageSource();


	@Before
	public void setupSpringValidatorAdapter() {
		messageSource.addMessage("Size", Locale.ENGLISH, "Size of {0} is must be between {2} and {1}");
		messageSource.addMessage("Same", Locale.ENGLISH, "{2} must be same value with {1}");
		messageSource.addMessage("password", Locale.ENGLISH, "Password");
		messageSource.addMessage("confirmPassword", Locale.ENGLISH, "Password(Confirm)");
	}


	@Test
	public void testUnwrap() {
		Validator nativeValidator = validatorAdapter.unwrap(Validator.class);
		assertSame(this.nativeValidator, nativeValidator);
	}

	@Test  // SPR-13406
	public void testNoStringArgumentValue() {
		TestBean testBean = new TestBean();
		testBean.setPassword("pass");
		testBean.setConfirmPassword("pass");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(testBean, "testBean");
		validatorAdapter.validate(testBean, errors);

		assertThat(errors.getFieldErrorCount("password"), is(1));
		assertThat(errors.getFieldValue("password"), is("pass"));
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
		assertThat(errors.getFieldValue("password"), is("password"));
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
		assertThat(errors.getFieldValue("email"), is("test@example.com"));
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
		assertThat(errors.getFieldValue("email"), is("test@example.com"));
		assertThat(errors.getFieldErrorCount("confirmEmail"), is(1));
		assertThat(messageSource.getMessage(errors.getFieldError("email"), Locale.ENGLISH),
				is("email must be same value with confirmEmail"));
		assertThat(messageSource.getMessage(errors.getFieldError("confirmEmail"), Locale.ENGLISH),
				is("Email required"));
	}

	@Test  // SPR-16177
	public void testWithList() {
		Parent parent = new Parent();
		parent.setName("Parent whit list");
		parent.getChildList().addAll(createChildren(parent));

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(parent, "parent");
		validatorAdapter.validate(parent, errors);

		assertTrue(errors.getErrorCount() > 0);
	}

	@Test  // SPR-16177
	public void testWithSet() {
		Parent parent = new Parent();
		parent.setName("Parent with set");
		parent.getChildSet().addAll(createChildren(parent));

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(parent, "parent");
		validatorAdapter.validate(parent, errors);

		assertTrue(errors.getErrorCount() > 0);
	}

	private List<Child> createChildren(Parent parent) {
		Child child1 = new Child();
		child1.setName("Child1");
		child1.setAge(null);
		child1.setParent(parent);

		Child child2 = new Child();
		child2.setName(null);
		child2.setAge(17);
		child2.setParent(parent);

		return Arrays.asList(child1, child2);
	}

	@Test  // SPR-15839
	public void testListElementConstraint() {
		BeanWithListElementConstraint bean = new BeanWithListElementConstraint();
		bean.setProperty(Arrays.asList("no", "element", "can", "be", null));

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(bean, "bean");
		validatorAdapter.validate(bean, errors);

		assertThat(errors.getFieldErrorCount("property[4]"), is(1));
		assertNull(errors.getFieldValue("property[4]"));
	}

	@Test  // SPR-15839
	public void testMapValueConstraint() {
		Map<String, String> property = new HashMap<>();
		property.put("no value can be", null);

		BeanWithMapEntryConstraint bean = new BeanWithMapEntryConstraint();
		bean.setProperty(property);

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(bean, "bean");
		validatorAdapter.validate(bean, errors);

		assertThat(errors.getFieldErrorCount("property[no value can be]"), is(1));
		assertNull(errors.getFieldValue("property[no value can be]"));
	}

	@Test  // SPR-15839
	public void testMapEntryConstraint() {
		Map<String, String> property = new HashMap<>();
		property.put(null, null);

		BeanWithMapEntryConstraint bean = new BeanWithMapEntryConstraint();
		bean.setProperty(property);

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(bean, "bean");
		validatorAdapter.validate(bean, errors);

		assertTrue(errors.hasFieldErrors("property[]"));
		assertNull(errors.getFieldValue("property[]"));
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


	public static class Parent {

		private Integer id;

		@NotNull
		private String name;

		@Valid
		private Set<Child> childSet = new LinkedHashSet<>();

		@Valid
		private List<Child> childList = new LinkedList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Child> getChildSet() {
			return childSet;
		}

		public void setChildSet(Set<Child> childSet) {
			this.childSet = childSet;
		}

		public List<Child> getChildList() {
			return childList;
		}

		public void setChildList(List<Child> childList) {
			this.childList = childList;
		}
	}


	@AnythingValid
	public static class Child {

		private Integer id;

		@javax.validation.constraints.NotNull
		private String name;

		@javax.validation.constraints.NotNull
		private Integer age;

		@javax.validation.constraints.NotNull
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}


	@Constraint(validatedBy = AnythingValidator.class)
	@Retention(RUNTIME)
	public @interface AnythingValid {

		String message() default "{AnythingValid.message}";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}


	public static class AnythingValidator implements ConstraintValidator<AnythingValid, Object> {

		private static final String ID = "id";

		@Override
		public void initialize(AnythingValid constraintAnnotation) {
		}

		@Override
		public boolean isValid(Object value, ConstraintValidatorContext context) {
			List<Field> fieldsErros = new ArrayList<>();
			Arrays.asList(value.getClass().getDeclaredFields()).forEach(f -> {
				f.setAccessible(true);
				try {
					if (!f.getName().equals(ID) && f.get(value) == null) {
						fieldsErros.add(f);
						context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
								.addPropertyNode(f.getName())
								.addConstraintViolation();
					}
				} catch (IllegalAccessException ex) {
					throw new IllegalStateException(ex);
				}

			});
			return fieldsErros.isEmpty();
		}
	}


	public class BeanWithListElementConstraint {

		@Valid
		private List<@NotNull String> property;

		public List<String> getProperty() {
			return property;
		}

		public void setProperty(List<String> property) {
			this.property = property;
		}
	}


	public class BeanWithMapEntryConstraint {

		@Valid
		private Map<@NotNull String, @NotNull String> property;

		public Map<String, String> getProperty() {
			return property;
		}

		public void setProperty(Map<String, String> property) {
			this.property = property;
		}
	}

}
