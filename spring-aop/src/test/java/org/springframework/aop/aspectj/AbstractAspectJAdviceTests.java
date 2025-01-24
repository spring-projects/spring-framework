/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractAspectJAdvice}.
 *
 * @author Joshua Chen
 */
class AbstractAspectJAdviceTests {

	@Test
	void setArgumentNamesFromStringArray_withJoinPointAsFirstParameter() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsFirstParameter");
		assertArgumentNamesFromStringArray(advice);
	}

	@Test
	void setArgumentNamesFromStringArray_withJoinPointAsLastParameter() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsLastParameter");
		assertArgumentNamesFromStringArray(advice);
	}

	@Test
	void setArgumentNamesFromStringArray_withJoinPointAsMiddleParameter() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsMiddleParameter");
		assertArgumentNamesFromStringArray(advice);
	}

	@Test
	void setArgumentNamesFromStringArray_withProceedingJoinPoint() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithProceedingJoinPoint");
		assertArgumentNamesFromStringArray(advice);
	}

	@Test
	void setArgumentNamesFromStringArray_withStaticPart() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithStaticPart");
		assertArgumentNamesFromStringArray(advice);
	}

	private void assertArgumentNamesFromStringArray(AbstractAspectJAdvice advice) {
		assertThat(getArgumentNames(advice)[0]).isEqualTo("THIS_JOIN_POINT");
		assertThat(getArgumentNames(advice)[1]).isEqualTo("arg1");
		assertThat(getArgumentNames(advice)[2]).isEqualTo("arg2");
	}

	private @NotNull AbstractAspectJAdvice getAspectJAdvice(final String methodName) {
		AbstractAspectJAdvice advice = new TestAspectJAdvice(getMethod(methodName), mock(AspectJExpressionPointcut.class), mock(AspectInstanceFactory.class));
		advice.setArgumentNamesFromStringArray("arg1", "arg2");
		return advice;
	}

	private Method getMethod(final String name) {
		return Arrays.stream(this.getClass().getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
	}

	private String[] getArgumentNames(final AbstractAspectJAdvice advice) {
		try {
			Field field = AbstractAspectJAdvice.class.getDeclaredField("argumentNames");
			field.setAccessible(true);
			return (String[]) field.get(advice);
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static class TestAspectJAdvice extends AbstractAspectJAdvice {
		@Serial private static final long serialVersionUID = 1L;

		public TestAspectJAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
			super(aspectJAdviceMethod, pointcut, aspectInstanceFactory);
		}

		@Override
		public boolean isBeforeAdvice() {
			return false;
		}

		@Override
		public boolean isAfterAdvice() {
			return false;
		}
	}

	void methodWithJoinPointAsFirstParameter(JoinPoint joinPoint, String arg1, String arg2) {
	}

	void methodWithJoinPointAsLastParameter(String arg1, String arg2, JoinPoint joinPoint) {
	}

	void methodWithJoinPointAsMiddleParameter(String arg1, JoinPoint joinPoint, String arg2) {
	}

	void methodWithProceedingJoinPoint(ProceedingJoinPoint joinPoint, String arg1, String arg2) {
	}

	void methodWithStaticPart(JoinPoint.StaticPart staticPart, String arg1, String arg2) {
	}
}
