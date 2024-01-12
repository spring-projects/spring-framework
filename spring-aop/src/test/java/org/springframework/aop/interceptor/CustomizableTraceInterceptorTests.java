/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.ALLOWED_PLACEHOLDERS;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENTS;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENT_TYPES;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_EXCEPTION;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_INVOCATION_TIME;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_METHOD_NAME;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_NAME;
import static org.springframework.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_SHORT_NAME;

/**
 * Tests for {@link CustomizableTraceInterceptor}.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class CustomizableTraceInterceptorTests {

	private final CustomizableTraceInterceptor interceptor = new CustomizableTraceInterceptor();


	@Test
	void setEmptyEnterMessage() {
		// Must not be able to set empty enter message
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(""));
	}

	@Test
	void setEnterMessageWithReturnValuePlaceholder() {
		// Must not be able to set enter message with return value placeholder
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(PLACEHOLDER_RETURN_VALUE));
	}

	@Test
	void setEnterMessageWithExceptionPlaceholder() {
		// Must not be able to set enter message with exception placeholder
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(PLACEHOLDER_EXCEPTION));
	}

	@Test
	void setEnterMessageWithInvocationTimePlaceholder() {
		// Must not be able to set enter message with invocation time placeholder
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(PLACEHOLDER_INVOCATION_TIME));
	}

	@Test
	void setEmptyExitMessage() {
		// Must not be able to set empty exit message
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExitMessage(""));
	}

	@Test
	void setExitMessageWithExceptionPlaceholder() {
		// Must not be able to set exit message with exception placeholder
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExitMessage(PLACEHOLDER_EXCEPTION));
	}

	@Test
	void setEmptyExceptionMessage() {
		// Must not be able to set empty exception message
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExceptionMessage(""));
	}

	@Test
	void setExceptionMethodWithReturnValuePlaceholder() {
		// Must not be able to set exception message with return value placeholder
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExceptionMessage(PLACEHOLDER_RETURN_VALUE));
	}

	@Test
	void sunnyDayPathLogsCorrectly() throws Throwable {
		MethodInvocation methodInvocation = mock();
		given(methodInvocation.getMethod()).willReturn(String.class.getMethod("toString"));
		given(methodInvocation.getThis()).willReturn(this);

		Log log = mock();
		given(log.isTraceEnabled()).willReturn(true);

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		interceptor.invoke(methodInvocation);

		verify(log, times(2)).trace(anyString());
	}

	@Test
	void exceptionPathLogsCorrectly() throws Throwable {
		MethodInvocation methodInvocation = mock();

		IllegalArgumentException exception = new IllegalArgumentException();
		given(methodInvocation.getMethod()).willReturn(String.class.getMethod("toString"));
		given(methodInvocation.getThis()).willReturn(this);
		given(methodInvocation.proceed()).willThrow(exception);

		Log log = mock();
		given(log.isTraceEnabled()).willReturn(true);

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		assertThatIllegalArgumentException().isThrownBy(() -> interceptor.invoke(methodInvocation));

		verify(log).trace(anyString());
		verify(log).trace(anyString(), eq(exception));
	}

	@Test
	void sunnyDayPathLogsCorrectlyWithPrettyMuchAllPlaceholdersMatching() throws Throwable {
		MethodInvocation methodInvocation = mock();

		given(methodInvocation.getMethod()).willReturn(String.class.getMethod("toString"));
		given(methodInvocation.getThis()).willReturn(this);
		given(methodInvocation.getArguments()).willReturn(new Object[]{"$ One \\$", 2L});
		given(methodInvocation.proceed()).willReturn("Hello!");

		Log log = mock();
		given(log.isTraceEnabled()).willReturn(true);

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		interceptor.setEnterMessage(new StringBuilder()
			.append("Entering the '").append(PLACEHOLDER_METHOD_NAME)
			.append("' method of the [").append(PLACEHOLDER_TARGET_CLASS_NAME)
			.append("] class with the following args (").append(PLACEHOLDER_ARGUMENTS)
			.append(") and arg types (").append(PLACEHOLDER_ARGUMENT_TYPES)
			.append(").").toString());
		interceptor.setExitMessage(new StringBuilder()
			.append("Exiting the '").append(PLACEHOLDER_METHOD_NAME)
			.append("' method of the [").append(PLACEHOLDER_TARGET_CLASS_SHORT_NAME)
			.append("] class with the following args (").append(PLACEHOLDER_ARGUMENTS)
			.append(") and arg types (").append(PLACEHOLDER_ARGUMENT_TYPES)
			.append("), returning '").append(PLACEHOLDER_RETURN_VALUE)
			.append("' and taking '").append(PLACEHOLDER_INVOCATION_TIME)
			.append("' this long.").toString());
		interceptor.invoke(methodInvocation);

		verify(log, times(2)).trace(anyString());
	}

	/**
	 * This test effectively verifies that the internal ALLOWED_PLACEHOLDERS set
	 * is properly configured in {@link CustomizableTraceInterceptor}.
	 */
	@Test
	void supportedPlaceholderValues() {
		assertThat(ALLOWED_PLACEHOLDERS).containsExactlyInAnyOrderElementsOf(getPlaceholderConstantValues());
	}

	private List<String> getPlaceholderConstantValues() {
		return Arrays.stream(CustomizableTraceInterceptor.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.filter(field -> field.getName().startsWith("PLACEHOLDER_"))
				.map(this::getFieldValue)
				.map(String.class::cast)
				.toList();
	}

	private Object getFieldValue(Field field) {
		try {
			return field.get(null);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}


	@SuppressWarnings("serial")
	private static class StubCustomizableTraceInterceptor extends CustomizableTraceInterceptor {

		private final Log log;

		StubCustomizableTraceInterceptor(Log log) {
			super.setUseDynamicLogger(false);
			this.log = log;
		}

		@Override
		protected Log getLoggerForInvocation(MethodInvocation invocation) {
			return this.log;
		}
	}

}
