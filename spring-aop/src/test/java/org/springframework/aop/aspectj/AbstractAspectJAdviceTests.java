/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractAspectJAdvice}.
 *
 * @author Joshua Chen
 * @author Stephane Nicoll
 */
class AbstractAspectJAdviceTests {

	@Test
	void setArgumentNamesFromStringArray_withoutJoinPointParameter() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithNoJoinPoint");
		assertThat(advice).satisfies(hasArgumentNames("arg1", "arg2"));
	}

	@Test
	void setArgumentNamesFromStringArray_withJoinPointAsFirstParameter() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsFirstParameter");
		assertThat(advice).satisfies(hasArgumentNames("THIS_JOIN_POINT", "arg1", "arg2"));
	}

	@Test
	void setArgumentNamesFromStringArray_withJoinPointAsLastParameter() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsLastParameter");
		assertThat(advice).satisfies(hasArgumentNames("arg1", "arg2", "THIS_JOIN_POINT"));
	}

	@Test
	void setArgumentNamesFromStringArray_withJoinPointAsMiddleParameter() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsMiddleParameter");
		assertThat(advice).satisfies(hasArgumentNames("arg1", "THIS_JOIN_POINT", "arg2"));
	}

	@Test
	void setArgumentNamesFromStringArray_withProceedingJoinPoint() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithProceedingJoinPoint");
		assertThat(advice).satisfies(hasArgumentNames("THIS_JOIN_POINT", "arg1", "arg2"));
	}

	@Test
	void setArgumentNamesFromStringArray_withStaticPart() {
		AbstractAspectJAdvice advice = getAspectJAdvice("methodWithStaticPart");
		assertThat(advice).satisfies(hasArgumentNames("THIS_JOIN_POINT", "arg1", "arg2"));
	}

	private Consumer<AbstractAspectJAdvice> hasArgumentNames(String... argumentNames) {
		return advice -> assertThat(advice).extracting("argumentNames")
				.asInstanceOf(InstanceOfAssertFactories.array(String[].class))
				.containsExactly(argumentNames);
	}

	private AbstractAspectJAdvice getAspectJAdvice(final String methodName) {
		AbstractAspectJAdvice advice = new TestAspectJAdvice(getMethod(methodName),
				mock(AspectJExpressionPointcut.class), mock(AspectInstanceFactory.class));
		advice.setArgumentNamesFromStringArray("arg1", "arg2");
		return advice;
	}

	private Method getMethod(final String methodName) {
		return Arrays.stream(Sample.class.getDeclaredMethods())
				.filter(method -> method.getName().equals(methodName)).findFirst()
				.orElseThrow();
	}

	@SuppressWarnings("serial")
	public static class TestAspectJAdvice extends AbstractAspectJAdvice {

		public TestAspectJAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut,
				AspectInstanceFactory aspectInstanceFactory) {
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

	@SuppressWarnings("unused")
	static class Sample {

		void methodWithNoJoinPoint(String arg1, String arg2) {
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

}
