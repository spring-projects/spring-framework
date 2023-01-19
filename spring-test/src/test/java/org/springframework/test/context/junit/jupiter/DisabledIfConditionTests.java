/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestContextManager;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DisabledIfCondition} that verify actual condition evaluation
 * results and exception handling; whereas, {@link DisabledIfTests} only tests
 * the <em>happy paths</em>.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see DisabledIfTests
 */
class DisabledIfConditionTests {

	private final DisabledIfCondition condition = new DisabledIfCondition();


	@Test
	void missingDisabledIf() {
		ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("missingDisabledIf"));
		assertThat(result.isDisabled()).isFalse();
		assertThat(result.getReason().get()).endsWith("missingDisabledIf() is enabled since @DisabledIf is not present");
	}

	@Test
	void disabledByEmptyExpression() {
		assertExpressionIsBlank("emptyExpression");
		assertExpressionIsBlank("blankExpression");
	}

	@Test
	void invalidExpressionEvaluationType() {
		String methodName = "nonBooleanOrStringExpression";
		Method method = ReflectionUtils.findMethod(getClass(), methodName);

		assertThatIllegalStateException()
			.isThrownBy(() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)))
			.withMessageContaining(
				"@DisabledIf(\"#{6 * 7}\") on " + method + " must evaluate to a String or a Boolean, not java.lang.Integer");
	}

	@Test
	void unsupportedStringEvaluationValue() {
		String methodName = "stringExpressionThatIsNeitherTrueNorFalse";
		Method method = ReflectionUtils.findMethod(getClass(), methodName);

		assertThatIllegalStateException()
			.isThrownBy(() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)))
			.withMessageContaining(
				"@DisabledIf(\"#{'enigma'}\") on " + method + " must evaluate to \"true\" or \"false\", not \"enigma\"");
	}

	@Test
	void disabledWithCustomReason() {
		ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("customReason"));
		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason()).contains("Because... 42!");
	}

	@Test
	void disabledWithDefaultReason() {
		ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("defaultReason"));
		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason().get())
			.endsWith("defaultReason() is disabled because @DisabledIf(\"#{1 + 1 eq 2}\") evaluated to true");
	}

	@Test
	void notDisabledWithDefaultReason() {
		ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("neverDisabledWithDefaultReason"));
		assertThat(result.isDisabled()).isFalse();
		assertThat(result.getReason().get())
			.endsWith("neverDisabledWithDefaultReason() is enabled because @DisabledIf(\"false\") did not evaluate to true");
	}

	// -------------------------------------------------------------------------

	private ExtensionContext buildExtensionContext(String methodName) {
		Class<?> testClass = SpringTestCase.class;
		Method method = ReflectionUtils.findMethod(getClass(), methodName);
		Store store = mock();
		given(store.getOrComputeIfAbsent(any(), any(), any())).willReturn(new TestContextManager(testClass));

		ExtensionContext extensionContext = mock();
		given(extensionContext.getTestClass()).willReturn(Optional.of(testClass));
		given(extensionContext.getElement()).willReturn(Optional.of(method));
		given(extensionContext.getStore(any())).willReturn(store);
		return extensionContext;
	}

	private void assertExpressionIsBlank(String methodName) {
		assertThatIllegalStateException()
			.isThrownBy(() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)))
			.withMessageContaining("must not be blank");
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
