package org.springframework.validation.annotation;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.stereotype.Validator;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class AnnotationMethodValidatorTest {
	private StaticApplicationContext context;

	private AnnotationMethodValidator validator;

	@Before
	public void setUp() throws Exception {
		context = new StaticApplicationContext();

		validator = new AnnotationMethodValidator();
	}

	@Test
	public void testSupportsWithNoRegisteredValidators() throws Exception {
		initializeValidator();

		assertFalse(validator.supports(String.class));
		assertFalse(validator.supports(Object.class));
		assertFalse(validator.supports(Integer.class));
	}

	@Test
	public void testSupportsWithRegisteredValidator() throws Exception {
		initializeValidatorWithRegistration();

		assertTrue(validator.supports(String.class));
		assertTrue(validator.supports(Object.class));
		assertFalse(validator.supports(Integer.class));
	}

	@Test
	public void testValidateWithUnvalidatedTarget() throws Exception {
		initializeValidatorWithRegistration();

		Integer target = 1;
		BindException errors = invokeValidatorWithTarget(target);

		assertEquals(0, errors.getErrorCount());
	}

	@Test
	public void testValidateWithObjectTarget() throws Exception {
		initializeValidatorWithRegistration();

		Object target = new Object();
		BindException errors = invokeValidatorWithTarget(target);

		assertEquals(1, errors.getErrorCount());
		assertErrorsContainsCodes(errors, "objectError");
	}

	@Test
	public void testValidateWithStringTarget() throws Exception {
		initializeValidatorWithRegistration();

		String target = "test";
		BindException errors = invokeValidatorWithTarget(target);

		assertEquals(3, errors.getErrorCount());
		assertErrorsContainsCodes(errors, "stringError1", "stringError2", "stringError3");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateWithInvalidParameterType() throws Exception {
		context.registerSingleton("badValidator", DummyInvalidParameterTypeValidator.class);
		initializeValidatorWithRegistration();

		String target = "test";
		invokeValidatorWithTarget(target);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateWithMissingErrorsParameter() throws Exception {
		context.registerSingleton("badValidator", DummyMissingErrorsParameterValidator.class);
		initializeValidatorWithRegistration();

		String target = "test";
		invokeValidatorWithTarget(target);
	}

	@Test(expected = UndeclaredThrowableException.class)
	public void testValidateWithRuntimeException() throws Exception {
		context.registerSingleton("badValidator", DummyExceptionThrowingValidator.class);
		initializeValidatorWithRegistration();

		String target = "test";
		invokeValidatorWithTarget(target);
	}

	private BindException invokeValidatorWithTarget(Object target) {
		BindException errors = new BindException(target, "target");
		validator.validate(target, errors);
		return errors;
	}

	private void initializeValidatorWithRegistration() throws Exception {
		context.registerSingleton("objectValidator", DummyObjectValidator.class);
		context.registerSingleton("stringValidator1", DummyStringValidator1.class);
		context.registerSingleton("stringValidator2", DummyStringValidator2.class);
		initializeValidator();
	}

	private void initializeValidator() throws Exception {
		validator.setApplicationContext(context);
		validator.afterPropertiesSet();
	}

	private void assertErrorsContainsCodes(Errors errors, String... codes) {
		List<ObjectError> allErrors = errors.getAllErrors();
		for (int i = 0; i < allErrors.size(); i++) {
			assertEquals(codes[i], allErrors.get(i).getCode());
		}
	}
}

@Validator(validates = Object.class)
class DummyObjectValidator {
	@Validate
	public void validateObject(Object obj, Errors errors) {
		errors.reject("objectError");
	}
}

@Validator(validates = String.class)
class DummyStringValidator1 {
	@Validate
	private void validateString1(Errors errors) {
		errors.reject("stringError1");
	}
}

@Validator(validates = String.class)
class DummyStringValidator2 {
	@Validate
	private void validateString2(String str, Errors errors) {
		errors.reject("stringError2");
	}

	@Validate
	private void validateString3(Errors errors, String str) {
		errors.reject("stringError3");
	}
}

@Validator(validates = String.class)
class DummyInvalidParameterTypeValidator {
	@Validate
	private void invalidParameterType(Errors errors, Integer number) {
	}
}

@Validator(validates = String.class)
class DummyMissingErrorsParameterValidator {
	@Validate
	private void missingErrorsParameter(String str) {
	}
}

@Validator(validates = String.class)
class DummyExceptionThrowingValidator {
	@Validate
	private void throwException(String str, Errors errors) throws IOException {
		throw new IOException("forced error");
	}
}

