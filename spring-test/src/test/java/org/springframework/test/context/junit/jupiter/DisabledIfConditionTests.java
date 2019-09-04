/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.reflect.Method;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit.SpringJUnitJupiterTestSuite;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DisabledIfCondition} that verify actual condition evaluation
 * results and exception handling; whereas, {@link DisabledIfTests} only tests
 * the <em>happy paths</em>.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see DisabledIfTests
 */
class DisabledIfConditionTests {

	private final DisabledIfCondition condition = new DisabledIfCondition();


	@Test
	void missingDisabledIf() {
		assertResult(condition.evaluateExecutionCondition(buildExtensionContext("missingDisabledIf")), false,
			endsWith("missingDisabledIf() is enabled since @DisabledIf is not present"));
	}

	@Test
	void disabledByEmptyExpression() {
		// @formatter:off
		assertAll(
			() -> assertExpressionIsBlank("emptyExpression"),
			() -> assertExpressionIsBlank("blankExpression")
		);
		// @formatter:on
	}

	@Test
	void invalidExpressionEvaluationType() {
		String methodName = "nonBooleanOrStringExpression";
		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)));

		Method method = ReflectionUtils.findMethod(getClass(), methodName);

		assertThat(exception.getMessage(),
			is(equalTo("@DisabledIf(\"#{6 * 7}\") on " + method + " must evaluate to a String or a Boolean, not java.lang.Integer")));
	}

	@Test
	void unsupportedStringEvaluationValue() {
		String methodName = "stringExpressionThatIsNeitherTrueNorFalse";
		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)));

		Method method = ReflectionUtils.findMethod(getClass(), methodName);

		assertThat(exception.getMessage(),
			is(equalTo("@DisabledIf(\"#{'enigma'}\") on " + method + " must evaluate to \"true\" or \"false\", not \"enigma\"")));
	}

	@Test
	void disabledWithCustomReason() {
		assertResult(condition.evaluateExecutionCondition(buildExtensionContext("customReason")), true, is(equalTo("Because... 42!")));
	}

	@Test
	void disabledWithDefaultReason() {
		assertResult(condition.evaluateExecutionCondition(buildExtensionContext("defaultReason")), true,
			endsWith("defaultReason() is disabled because @DisabledIf(\"#{1 + 1 eq 2}\") evaluated to true"));
	}

	@Test
	void notDisabledWithDefaultReason() {
		assertResult(condition.evaluateExecutionCondition(buildExtensionContext("neverDisabledWithDefaultReason")), false, endsWith(
			"neverDisabledWithDefaultReason() is enabled because @DisabledIf(\"false\") did not evaluate to true"));
	}

	// -------------------------------------------------------------------------

	private ExtensionContext buildExtensionContext(String methodName) {
		Class<?> testClass = SpringTestCase.class;
		Method method = ReflectionUtils.findMethod(getClass(), methodName);
		Store store = mock(Store.class);
		when(store.getOrComputeIfAbsent(any(), any(), any())).thenReturn(new TestContextManager(testClass));

		ExtensionContext extensionContext = mock(ExtensionContext.class);
		when(extensionContext.getTestClass()).thenReturn(Optional.of(testClass));
		when(extensionContext.getElement()).thenReturn(Optional.of(method));
		when(extensionContext.getStore(any())).thenReturn(store);
		return extensionContext;
	}

	private void assertExpressionIsBlank(String methodName) {
		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)));

		assertThat(exception.getMessage(), containsString("must not be blank"));
	}

	private void assertResult(ConditionEvaluationResult result, boolean disabled, Matcher<String> matcher) {
		assertNotNull(result);

		if (disabled) {
			assertTrue(result.isDisabled());
		}
		else {
			assertFalse(result.isDisabled());
		}

		Optional<String> reason = result.getReason();
		assertTrue(reason.isPresent());
		assertThat(reason.get(), matcher);
	}

	// -------------------------------------------------------------------------

	@DisabledIf("")
	private void emptyExpression() {
	}

	@DisabledIf("\t")
	private void blankExpression() {
	}

	@DisabledIf("#{6 * 7}")
	private void nonBooleanOrStringExpression() {
	}

	@DisabledIf("#{'enigma'}")
	private void stringExpressionThatIsNeitherTrueNorFalse() {
	}

	@DisabledIf(expression = "#{6 * 7 == 42}", reason = "Because... 42!")
	private void customReason() {
	}

	@DisabledIf("#{1 + 1 eq 2}")
	private void defaultReason() {
	}

	@DisabledIf("false")
	private void neverDisabledWithDefaultReason() {
	}


	private static class SpringTestCase {

		@Configuration
		static class Config {
		}
	}

}
