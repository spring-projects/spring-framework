/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.aop.aspectj;

import static org.junit.Assert.*;

import org.aspectj.lang.JoinPoint;
import org.junit.Test;
import org.springframework.aop.aspectj.AspectJAdviceParameterNameDiscoverer.AmbiguousBindingException;

import java.lang.reflect.Method;

/**
 * Unit tests for the {@link AspectJAdviceParameterNameDiscoverer} class.
 *
 * <p>See also {@link TigerAspectJAdviceParameterNameDiscovererTests} in
 * the 'tiger' tree for tests relating to annotations.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AspectJAdviceParameterNameDiscovererTests {

	// methods to discover parameter names for
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


	@Test
	public void testNoArgs() {
		assertParameterNames(getMethod("noArgs"), "execution(* *(..))", new String[0]);
	}

	@Test
	public void testJoinPointOnly() {
		assertParameterNames(getMethod("tjp"), "execution(* *(..))", new String[]{"thisJoinPoint"});
	}

	@Test
	public void testJoinPointStaticPartOnly() {
		assertParameterNames(getMethod("tjpsp"), "execution(* *(..))", new String[]{"thisJoinPointStaticPart"});
	}

	@Test
	public void testTwoJoinPoints() {
		assertException(getMethod("twoJoinPoints"), "foo()", IllegalStateException.class, "Failed to bind all argument names: 1 argument(s) could not be bound");
	}

	@Test
	public void testOneThrowable() {
		assertParameterNames(getMethod("oneThrowable"), "foo()", null, "ex", new String[]{"ex"});
	}

	@Test
	public void testOneJPAndOneThrowable() {
		assertParameterNames(getMethod("jpAndOneThrowable"), "foo()", null, "ex", new String[]{"thisJoinPoint", "ex"});
	}

	@Test
	public void testOneJPAndTwoThrowables() {
		assertException(getMethod("jpAndTwoThrowables"), "foo()", null, "ex", AmbiguousBindingException.class,
				"Binding of throwing parameter 'ex' is ambiguous: could be bound to argument 1 or argument 2");
	}

	@Test
	public void testThrowableNoCandidates() {
		assertException(getMethod("noArgs"), "foo()", null, "ex", IllegalStateException.class,
				"Not enough arguments in method to satisfy binding of returning and throwing variables");
	}

	@Test
	public void testReturning() {
		assertParameterNames(getMethod("oneObject"), "foo()", "obj", null, new String[]{"obj"});
	}

	@Test
	public void testAmbiguousReturning() {
		assertException(getMethod("twoObjects"), "foo()", "obj", null, AmbiguousBindingException.class,
				"Binding of returning parameter 'obj' is ambiguous, there are 2 candidates.");
	}

	@Test
	public void testReturningNoCandidates() {
		assertException(getMethod("noArgs"), "foo()", "obj", null, IllegalStateException.class,
				"Not enough arguments in method to satisfy binding of returning and throwing variables");
	}

	@Test
	public void testThisBindingOneCandidate() {
		assertParameterNames(getMethod("oneObject"), "this(x)", new String[]{"x"});
	}

	@Test
	public void testThisBindingWithAlternateTokenizations() {
		assertParameterNames(getMethod("oneObject"), "this( x )", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "this( x)", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "this (x )", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "this(x )", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "foo() && this(x)", new String[]{"x"});
	}

	@Test
	public void testThisBindingTwoCandidates() {
		assertException(getMethod("oneObject"), "this(x) || this(y)", AmbiguousBindingException.class,
				"Found 2 candidate this(), target() or args() variables but only one unbound argument slot");
	}

	@Test
	public void testThisBindingWithBadPointcutExpressions() {
		assertException(getMethod("oneObject"), "this(", IllegalStateException.class,
				"Failed to bind all argument names: 1 argument(s) could not be bound");
		assertException(getMethod("oneObject"), "this(x && foo()", IllegalStateException.class,
				"Failed to bind all argument names: 1 argument(s) could not be bound");
	}

	@Test
	public void testTargetBindingOneCandidate() {
		assertParameterNames(getMethod("oneObject"), "target(x)", new String[]{"x"});
	}

	@Test
	public void testTargetBindingWithAlternateTokenizations() {
		assertParameterNames(getMethod("oneObject"), "target( x )", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "target( x)", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "target (x )", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "target(x )", new String[]{"x"});
		assertParameterNames(getMethod("oneObject"), "foo() && target(x)", new String[]{"x"});
	}

	@Test
	public void testTargetBindingTwoCandidates() {
		assertException(getMethod("oneObject"), "target(x) || target(y)", AmbiguousBindingException.class,
				"Found 2 candidate this(), target() or args() variables but only one unbound argument slot");
	}

	@Test
	public void testTargetBindingWithBadPointcutExpressions() {
		assertException(getMethod("oneObject"), "target(", IllegalStateException.class,
				"Failed to bind all argument names: 1 argument(s) could not be bound");
		assertException(getMethod("oneObject"), "target(x && foo()", IllegalStateException.class,
				"Failed to bind all argument names: 1 argument(s) could not be bound");
	}

	@Test
	public void testArgsBindingOneObject() {
		assertParameterNames(getMethod("oneObject"), "args(x)", new String[]{"x"});
	}

	@Test
	public void testArgsBindingOneObjectTwoCandidates() {
		assertException(getMethod("oneObject"), "args(x,y)", AmbiguousBindingException.class,
				"Found 2 candidate this(), target() or args() variables but only one unbound argument slot");
	}

	@Test
	public void testAmbiguousArgsBinding() {
		assertException(getMethod("twoObjects"), "args(x,y)", AmbiguousBindingException.class,
				"Still 2 unbound args at this(),target(),args() binding stage, with no way to determine between them");
	}

	@Test
	public void testArgsOnePrimitive() {
		assertParameterNames(getMethod("onePrimitive"), "args(count)", new String[]{"count"});
	}

	@Test
	public void testArgsOnePrimitiveOneObject() {
		assertException(getMethod("oneObjectOnePrimitive"), "args(count,obj)", AmbiguousBindingException.class,
				"Found 2 candidate variable names but only one candidate binding slot when matching primitive args");
	}

	@Test
	public void testThisAndPrimitive() {
		assertParameterNames(getMethod("oneObjectOnePrimitive"), "args(count) && this(obj)", new String[]{"obj", "count"});
	}

	@Test
	public void testTargetAndPrimitive() {
		assertParameterNames(getMethod("oneObjectOnePrimitive"), "args(count) && target(obj)", new String[]{"obj", "count"});
	}

	@Test
	public void testThrowingAndPrimitive() {
		assertParameterNames(getMethod("oneThrowableOnePrimitive"), "args(count)", null, "ex", new String[]{"ex", "count"});
	}

	@Test
	public void testAllTogetherNow() {
		assertParameterNames(getMethod("theBigOne"), "this(foo) && args(x)", null, "ex", new String[]{"thisJoinPoint", "ex", "x", "foo"});
	}

	@Test
	public void testReferenceBinding() {
		assertParameterNames(getMethod("onePrimitive"),"somepc(foo)",new String[] {"foo"});
	}

	@Test
	public void testReferenceBindingWithAlternateTokenizations() {
		assertParameterNames(getMethod("onePrimitive"),"call(bar *) && somepc(foo)",new String[] {"foo"});
		assertParameterNames(getMethod("onePrimitive"),"somepc ( foo )",new String[] {"foo"});
		assertParameterNames(getMethod("onePrimitive"),"somepc( foo)",new String[] {"foo"});
	}


	protected Method getMethod(String name) {
		// assumes no overloading of test methods...
		Method[] candidates = this.getClass().getMethods();
		for (int i = 0; i < candidates.length; i++) {
			if (candidates[i].getName().equals(name)) {
				return candidates[i];
			}
		}
		fail("Bad test specification, no method '" + name + "' found in test class");
		return null;
	}

	protected void assertParameterNames(Method m, String pointcut, String[] parameterNames) {
		assertParameterNames(m, pointcut, null, null, parameterNames);
	}

	protected void assertParameterNames(Method m, String pointcut, String returning, String throwing, String[] parameterNames) {
		assertEquals("bad test specification, must have same number of parameter names as method arguments",
				m.getParameterTypes().length, parameterNames.length);

		AspectJAdviceParameterNameDiscoverer discoverer = new AspectJAdviceParameterNameDiscoverer(pointcut);
		discoverer.setRaiseExceptions(true);
		discoverer.setReturningName(returning);
		discoverer.setThrowingName(throwing);
		String[] discoveredNames = discoverer.getParameterNames(m);

		String formattedExpectedNames = format(parameterNames);
		String formattedActualNames = format(discoveredNames);

		assertEquals("Expecting " + parameterNames.length + " parameter names in return set '" +
				formattedExpectedNames + "', but found " + discoveredNames.length +
				" '" + formattedActualNames + "'",
				parameterNames.length, discoveredNames.length);

		for (int i = 0; i < discoveredNames.length; i++) {
			assertNotNull("Parameter names must never be null", discoveredNames[i]);
			assertEquals("Expecting parameter " + i + " to be named '" +
					parameterNames[i] + "' but was '" + discoveredNames[i] + "'",
					parameterNames[i], discoveredNames[i]);
		}
	}

	protected void assertException(Method m, String pointcut, Class<?> exceptionType, String message) {
		assertException(m, pointcut, null, null, exceptionType, message);
	}

	protected void assertException(Method m, String pointcut, String returning, String throwing, Class<?> exceptionType, String message) {
		AspectJAdviceParameterNameDiscoverer discoverer = new AspectJAdviceParameterNameDiscoverer(pointcut);
		discoverer.setRaiseExceptions(true);
		discoverer.setReturningName(returning);
		discoverer.setThrowingName(throwing);

		try {
			discoverer.getParameterNames(m);
			fail("Expecting " + exceptionType.getName() + " with message '" + message + "'");
		} catch (RuntimeException expected) {
			assertEquals("Expecting exception of type " + exceptionType.getName(),
					exceptionType, expected.getClass());
			assertEquals("Exception message does not match expected", message, expected.getMessage());
		}
	}


	private static String format(String[] names) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		for (int i = 0; i < names.length; i++) {
			sb.append(names[i]);
			if ((i + 1) < names.length) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}

}
