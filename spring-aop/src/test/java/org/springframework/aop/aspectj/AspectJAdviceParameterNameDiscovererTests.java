/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.AspectJAdviceParameterNameDiscoverer.AmbiguousBindingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link AspectJAdviceParameterNameDiscoverer}.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 * @author Sam Brannen
 */
class AspectJAdviceParameterNameDiscovererTests {

	@Nested
	class StandardTests {

		@Test
		void noArgs() {
			assertParameterNames(getMethod("noArgs"), "execution(* *(..))", new String[0]);
		}

		@Test
		void joinPointOnly() {
			assertParameterNames(getMethod("tjp"), "execution(* *(..))", new String[] {"thisJoinPoint"});
		}

		@Test
		void joinPointStaticPartOnly() {
			assertParameterNames(getMethod("tjpsp"), "execution(* *(..))", new String[] {"thisJoinPointStaticPart"});
		}

		@Test
		void twoJoinPoints() {
			assertException(getMethod("twoJoinPoints"), "foo()", IllegalStateException.class,
					"Failed to bind all argument names: 1 argument(s) could not be bound");
		}

		@Test
		void oneThrowable() {
			assertParameterNames(getMethod("oneThrowable"), "foo()", null, "ex", new String[] {"ex"});
		}

		@Test
		void oneJPAndOneThrowable() {
			assertParameterNames(getMethod("jpAndOneThrowable"), "foo()", null, "ex", new String[] {"thisJoinPoint", "ex"});
		}

		@Test
		void oneJPAndTwoThrowables() {
			assertException(getMethod("jpAndTwoThrowables"), "foo()", null, "ex", AmbiguousBindingException.class,
					"Binding of throwing parameter 'ex' is ambiguous: could be bound to argument 1 or argument 2");
		}

		@Test
		void throwableNoCandidates() {
			assertException(getMethod("noArgs"), "foo()", null, "ex", IllegalStateException.class,
					"Not enough arguments in method to satisfy binding of returning and throwing variables");
		}

		@Test
		void returning() {
			assertParameterNames(getMethod("oneObject"), "foo()", "obj", null, new String[] {"obj"});
		}

		@Test
		void ambiguousReturning() {
			assertException(getMethod("twoObjects"), "foo()", "obj", null, AmbiguousBindingException.class,
					"Binding of returning parameter 'obj' is ambiguous, there are 2 candidates.");
		}

		@Test
		void returningNoCandidates() {
			assertException(getMethod("noArgs"), "foo()", "obj", null, IllegalStateException.class,
					"Not enough arguments in method to satisfy binding of returning and throwing variables");
		}

		@Test
		void thisBindingOneCandidate() {
			assertParameterNames(getMethod("oneObject"), "this(x)", new String[] {"x"});
		}

		@Test
		void thisBindingWithAlternateTokenizations() {
			assertParameterNames(getMethod("oneObject"), "this( x )", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "this( x)", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "this (x )", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "this(x )", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "foo() && this(x)", new String[] {"x"});
		}

		@Test
		void thisBindingTwoCandidates() {
			assertException(getMethod("oneObject"), "this(x) || this(y)", AmbiguousBindingException.class,
					"Found 2 candidate this(), target() or args() variables but only one unbound argument slot");
		}

		@Test
		void thisBindingWithBadPointcutExpressions() {
			assertException(getMethod("oneObject"), "this(", IllegalStateException.class,
					"Failed to bind all argument names: 1 argument(s) could not be bound");
			assertException(getMethod("oneObject"), "this(x && foo()", IllegalStateException.class,
					"Failed to bind all argument names: 1 argument(s) could not be bound");
		}

		@Test
		void targetBindingOneCandidate() {
			assertParameterNames(getMethod("oneObject"), "target(x)", new String[] {"x"});
		}

		@Test
		void targetBindingWithAlternateTokenizations() {
			assertParameterNames(getMethod("oneObject"), "target( x )", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "target( x)", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "target (x )", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "target(x )", new String[] {"x"});
			assertParameterNames(getMethod("oneObject"), "foo() && target(x)", new String[] {"x"});
		}

		@Test
		void targetBindingTwoCandidates() {
			assertException(getMethod("oneObject"), "target(x) || target(y)", AmbiguousBindingException.class,
					"Found 2 candidate this(), target() or args() variables but only one unbound argument slot");
		}

		@Test
		void targetBindingWithBadPointcutExpressions() {
			assertException(getMethod("oneObject"), "target(", IllegalStateException.class,
					"Failed to bind all argument names: 1 argument(s) could not be bound");
			assertException(getMethod("oneObject"), "target(x && foo()", IllegalStateException.class,
					"Failed to bind all argument names: 1 argument(s) could not be bound");
		}

		@Test
		void argsBindingOneObject() {
			assertParameterNames(getMethod("oneObject"), "args(x)", new String[] {"x"});
		}

		@Test
		void argsBindingOneObjectTwoCandidates() {
			assertException(getMethod("oneObject"), "args(x,y)", AmbiguousBindingException.class,
					"Found 2 candidate this(), target() or args() variables but only one unbound argument slot");
		}

		@Test
		void ambiguousArgsBinding() {
			assertException(getMethod("twoObjects"), "args(x,y)", AmbiguousBindingException.class,
					"Still 2 unbound args at this(),target(),args() binding stage, with no way to determine between them");
		}

		@Test
		void argsOnePrimitive() {
			assertParameterNames(getMethod("onePrimitive"), "args(count)", new String[] {"count"});
		}

		@Test
		void argsOnePrimitiveOneObject() {
			assertException(getMethod("oneObjectOnePrimitive"), "args(count,obj)", AmbiguousBindingException.class,
					"Found 2 candidate variable names but only one candidate binding slot when matching primitive args");
		}

		@Test
		void thisAndPrimitive() {
			assertParameterNames(getMethod("oneObjectOnePrimitive"), "args(count) && this(obj)",
					new String[] {"obj", "count"});
		}

		@Test
		void targetAndPrimitive() {
			assertParameterNames(getMethod("oneObjectOnePrimitive"), "args(count) && target(obj)",
					new String[] {"obj", "count"});
		}

		@Test
		void throwingAndPrimitive() {
			assertParameterNames(getMethod("oneThrowableOnePrimitive"), "args(count)", null, "ex",
					new String[] {"ex", "count"});
		}

		@Test
		void allTogetherNow() {
			assertParameterNames(getMethod("theBigOne"), "this(foo) && args(x)", null, "ex",
					new String[] {"thisJoinPoint", "ex", "x", "foo"});
		}

		@Test
		void referenceBinding() {
			assertParameterNames(getMethod("onePrimitive"),"somepc(foo)", new String[] {"foo"});
		}

		@Test
		void referenceBindingWithAlternateTokenizations() {
			assertParameterNames(getMethod("onePrimitive"),"call(bar *) && somepc(foo)", new String[] {"foo"});
			assertParameterNames(getMethod("onePrimitive"),"somepc ( foo )", new String[] {"foo"});
			assertParameterNames(getMethod("onePrimitive"),"somepc( foo)", new String[] {"foo"});
		}
	}

	/**
	 * Tests just the annotation binding part of {@link AspectJAdviceParameterNameDiscoverer}.
	 */
	@Nested
	class AnnotationTests {

		@Test
		void atThis() {
			assertParameterNames(getMethod("oneAnnotation"),"@this(a)", new String[] {"a"});
		}

		@Test
		void atTarget() {
			assertParameterNames(getMethod("oneAnnotation"),"@target(a)", new String[] {"a"});
		}

		@Test
		void atArgs() {
			assertParameterNames(getMethod("oneAnnotation"),"@args(a)", new String[] {"a"});
		}

		@Test
		void atWithin() {
			assertParameterNames(getMethod("oneAnnotation"),"@within(a)", new String[] {"a"});
		}

		@Test
		void atWithincode() {
			assertParameterNames(getMethod("oneAnnotation"),"@withincode(a)", new String[] {"a"});
		}

		@Test
		void atAnnotation() {
			assertParameterNames(getMethod("oneAnnotation"),"@annotation(a)", new String[] {"a"});
		}

		@Test
		void ambiguousAnnotationTwoVars() {
			assertException(getMethod("twoAnnotations"),"@annotation(a) && @this(x)", AmbiguousBindingException.class,
					"Found 2 potential annotation variable(s), and 2 potential argument slots");
		}

		@Test
		void ambiguousAnnotationOneVar() {
			assertException(getMethod("oneAnnotation"),"@annotation(a) && @this(x)",IllegalArgumentException.class,
					"Found 2 candidate annotation binding variables but only one potential argument binding slot");
		}

		@Test
		void annotationMedley() {
			assertParameterNames(getMethod("annotationMedley"),"@annotation(a) && args(count) && this(foo)",
					null, "ex", new String[] {"ex", "foo", "count", "a"});
		}

		@Test
		void annotationBinding() {
			assertParameterNames(getMethod("pjpAndAnAnnotation"),
					"execution(* *(..)) && @annotation(ann)",
					new String[] {"thisJoinPoint","ann"});
		}

	}


	private Method getMethod(String name) {
		// Assumes no overloading of test methods...
		for (Method candidate : getClass().getMethods()) {
			if (candidate.getName().equals(name)) {
				return candidate;
			}
		}
		throw new AssertionError("Bad test specification, no method '" + name + "' found in test class");
	}

	private void assertParameterNames(Method method, String pointcut, String[] parameterNames) {
		assertParameterNames(method, pointcut, null, null, parameterNames);
	}

	private void assertParameterNames(
			Method method, String pointcut, String returning, String throwing, String[] parameterNames) {

		assertThat(parameterNames.length).as("bad test specification, must have same number of parameter names as method arguments").isEqualTo(method.getParameterCount());

		AspectJAdviceParameterNameDiscoverer discoverer = new AspectJAdviceParameterNameDiscoverer(pointcut);
		discoverer.setRaiseExceptions(true);
		discoverer.setReturningName(returning);
		discoverer.setThrowingName(throwing);
		String[] discoveredNames = discoverer.getParameterNames(method);

		String formattedExpectedNames = Arrays.toString(parameterNames);
		String formattedActualNames = Arrays.toString(discoveredNames);

		assertThat(discoveredNames.length).as("Expecting " + parameterNames.length + " parameter names in return set '" +
				formattedExpectedNames + "', but found " + discoveredNames.length +
				" '" + formattedActualNames + "'").isEqualTo(parameterNames.length);

		for (int i = 0; i < discoveredNames.length; i++) {
			assertThat(discoveredNames[i]).as("Parameter names must never be null").isNotNull();
			assertThat(discoveredNames[i]).as("Expecting parameter " + i + " to be named '" +
						parameterNames[i] + "' but was '" + discoveredNames[i] + "'").isEqualTo(parameterNames[i]);
		}
	}

	private void assertException(Method method, String pointcut, Class<? extends Throwable> exceptionType, String message) {
		assertException(method, pointcut, null, null, exceptionType, message);
	}

	private void assertException(Method method, String pointcut, String returning,
			String throwing, Class<? extends Throwable> exceptionType, String message) {

		AspectJAdviceParameterNameDiscoverer discoverer = new AspectJAdviceParameterNameDiscoverer(pointcut);
		discoverer.setRaiseExceptions(true);
		discoverer.setReturningName(returning);
		discoverer.setThrowingName(throwing);
		assertThatExceptionOfType(exceptionType)
			.isThrownBy(() -> discoverer.getParameterNames(method))
			.withMessageContaining(message);
	}


	// Methods to discover parameter names for

	public void noArgs() {
	}

	public void tjp(JoinPoint jp) {
	}

	public void tjpsp(JoinPoint.StaticPart tjpsp) {
	}

	public void twoJoinPoints(JoinPoint jp1, JoinPoint jp2) {
	}

	public void oneThrowable(Exception ex) {
	}

	public void jpAndOneThrowable(JoinPoint jp, Exception ex) {
	}

	public void jpAndTwoThrowables(JoinPoint jp, Exception ex, Error err) {
	}

	public void oneObject(Object x) {
	}

	public void twoObjects(Object x, Object y) {
	}

	public void onePrimitive(int x) {
	}

	public void oneObjectOnePrimitive(Object x, int y) {
	}

	public void oneThrowableOnePrimitive(Throwable x, int y) {
	}

	public void theBigOne(JoinPoint jp, Throwable x, int y, Object foo) {
	}

	public void oneAnnotation(MyAnnotation ann) {}

	public void twoAnnotations(MyAnnotation ann, MyAnnotation anotherAnn) {}

	public void annotationMedley(Throwable t, Object foo, int x, MyAnnotation ma) {}

	public void pjpAndAnAnnotation(ProceedingJoinPoint pjp, MyAnnotation ann) {}

	@interface MyAnnotation {}

}
