/*
 * Copyright 2002-2015 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.aspectj.weaver.tools.UnsupportedPointcutPrimitiveException;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.tests.sample.beans.IOther;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.subpkg.DeepBean;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class AspectJExpressionPointcutTests {

	public static final String MATCH_ALL_METHODS = "execution(* *(..))";

	private Method getAge;

	private Method setAge;

	private Method setSomeNumber;


	@Before
	public void setUp() throws NoSuchMethodException {
		getAge = TestBean.class.getMethod("getAge");
		setAge = TestBean.class.getMethod("setAge", int.class);
		setSomeNumber = TestBean.class.getMethod("setSomeNumber", Number.class);
	}


	@Test
	public void testMatchExplicit() {
		String expression = "execution(int org.springframework.tests.sample.beans.TestBean.getAge())";

		Pointcut pointcut = getPointcut(expression);
		ClassFilter classFilter = pointcut.getClassFilter();
		MethodMatcher methodMatcher = pointcut.getMethodMatcher();

		assertMatchesTestBeanClass(classFilter);

		// not currently testable in a reliable fashion
		//assertDoesNotMatchStringClass(classFilter);

		assertFalse("Should not be a runtime match", methodMatcher.isRuntime());
		assertMatchesGetAge(methodMatcher);
		assertFalse("Expression should match setAge() method", methodMatcher.matches(setAge, TestBean.class));
	}

	@Test
	public void testMatchWithTypePattern() throws Exception {
		String expression = "execution(* *..TestBean.*Age(..))";

		Pointcut pointcut = getPointcut(expression);
		ClassFilter classFilter = pointcut.getClassFilter();
		MethodMatcher methodMatcher = pointcut.getMethodMatcher();

		assertMatchesTestBeanClass(classFilter);

		// not currently testable in a reliable fashion
		//assertDoesNotMatchStringClass(classFilter);

		assertFalse("Should not be a runtime match", methodMatcher.isRuntime());
		assertMatchesGetAge(methodMatcher);
		assertTrue("Expression should match setAge(int) method", methodMatcher.matches(setAge, TestBean.class));
	}


	@Test
	public void testThis() throws SecurityException, NoSuchMethodException{
		testThisOrTarget("this");
	}

	@Test
	public void testTarget() throws SecurityException, NoSuchMethodException {
		testThisOrTarget("target");
	}

	/**
	 * This and target are equivalent. Really instanceof pointcuts.
	 * @param which this or target
	 */
	private void testThisOrTarget(String which) throws SecurityException, NoSuchMethodException {
		String matchesTestBean = which + "(org.springframework.tests.sample.beans.TestBean)";
		String matchesIOther = which + "(org.springframework.tests.sample.beans.IOther)";
		AspectJExpressionPointcut testBeanPc = new AspectJExpressionPointcut();
		testBeanPc.setExpression(matchesTestBean);

		AspectJExpressionPointcut iOtherPc = new AspectJExpressionPointcut();
		iOtherPc.setExpression(matchesIOther);

		assertTrue(testBeanPc.matches(TestBean.class));
		assertTrue(testBeanPc.matches(getAge, TestBean.class));
		assertTrue(iOtherPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class));
		assertFalse(testBeanPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class));
	}

	@Test
	public void testWithinRootPackage() throws SecurityException, NoSuchMethodException {
		testWithinPackage(false);
	}

	@Test
	public void testWithinRootAndSubpackages() throws SecurityException, NoSuchMethodException {
		testWithinPackage(true);
	}

	private void testWithinPackage(boolean matchSubpackages) throws SecurityException, NoSuchMethodException {
		String withinBeansPackage = "within(org.springframework.tests.sample.beans.";
		// Subpackages are matched by **
		if (matchSubpackages) {
			withinBeansPackage += ".";
		}
		withinBeansPackage = withinBeansPackage + "*)";
		AspectJExpressionPointcut withinBeansPc = new AspectJExpressionPointcut();
		withinBeansPc.setExpression(withinBeansPackage);

		assertTrue(withinBeansPc.matches(TestBean.class));
		assertTrue(withinBeansPc.matches(getAge, TestBean.class));
		assertEquals(matchSubpackages, withinBeansPc.matches(DeepBean.class));
		assertEquals(matchSubpackages, withinBeansPc.matches(
				DeepBean.class.getMethod("aMethod", String.class), DeepBean.class));
		assertFalse(withinBeansPc.matches(String.class));
		assertFalse(withinBeansPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class));
	}

	@Test
	public void testFriendlyErrorOnNoLocationClassMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		try {
			pc.matches(ITestBean.class);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("expression"));
		}
	}

	@Test
	public void testFriendlyErrorOnNoLocation2ArgMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		try {
			pc.matches(getAge, ITestBean.class);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("expression"));
		}
	}

	@Test
	public void testFriendlyErrorOnNoLocation3ArgMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		try {
			pc.matches(getAge, ITestBean.class, (Object[]) null);
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("expression"));
		}
	}


	@Test
	public void testMatchWithArgs() throws Exception {
		String expression = "execution(void org.springframework.tests.sample.beans.TestBean.setSomeNumber(Number)) && args(Double)";

		Pointcut pointcut = getPointcut(expression);
		ClassFilter classFilter = pointcut.getClassFilter();
		MethodMatcher methodMatcher = pointcut.getMethodMatcher();

		assertMatchesTestBeanClass(classFilter);

		// not currently testable in a reliable fashion
		//assertDoesNotMatchStringClass(classFilter);

		assertTrue("Should match with setSomeNumber with Double input",
				methodMatcher.matches(setSomeNumber, TestBean.class, new Double(12)));
		assertFalse("Should not match setSomeNumber with Integer input",
				methodMatcher.matches(setSomeNumber, TestBean.class, new Integer(11)));
		assertFalse("Should not match getAge", methodMatcher.matches(getAge, TestBean.class));
		assertTrue("Should be a runtime match", methodMatcher.isRuntime());
	}

	@Test
	public void testSimpleAdvice() {
		String expression = "execution(int org.springframework.tests.sample.beans.TestBean.getAge())";

		CallCountingInterceptor interceptor = new CallCountingInterceptor();

		TestBean testBean = getAdvisedProxy(expression, interceptor);

		assertEquals("Calls should be 0", 0, interceptor.getCount());

		testBean.getAge();

		assertEquals("Calls should be 1", 1, interceptor.getCount());

		testBean.setAge(90);

		assertEquals("Calls should still be 1", 1, interceptor.getCount());
	}

	@Test
	public void testDynamicMatchingProxy() {
		String expression = "execution(void org.springframework.tests.sample.beans.TestBean.setSomeNumber(Number)) && args(Double)";

		CallCountingInterceptor interceptor = new CallCountingInterceptor();

		TestBean testBean = getAdvisedProxy(expression, interceptor);

		assertEquals("Calls should be 0", 0, interceptor.getCount());

		testBean.setSomeNumber(new Double(30));

		assertEquals("Calls should be 1", 1, interceptor.getCount());

		testBean.setSomeNumber(new Integer(90));

		assertEquals("Calls should be 1", 1, interceptor.getCount());
	}

	@Test
	public void testInvalidExpression() {
		String expression = "execution(void org.springframework.tests.sample.beans.TestBean.setSomeNumber(Number) && args(Double)";

		try {
			getPointcut(expression).getClassFilter();  // call to getClassFilter forces resolution
			fail("Invalid expression should throw IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(true);
		}
	}

	private TestBean getAdvisedProxy(String pointcutExpression, CallCountingInterceptor interceptor) {
		TestBean target = new TestBean();

		Pointcut pointcut = getPointcut(pointcutExpression);

		DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
		advisor.setAdvice(interceptor);
		advisor.setPointcut(pointcut);

		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(target);
		pf.addAdvisor(advisor);

		return (TestBean) pf.getProxy();
	}

	private void assertMatchesGetAge(MethodMatcher methodMatcher) {
		assertTrue("Expression should match getAge() method", methodMatcher.matches(getAge, TestBean.class));
	}

	private void assertMatchesTestBeanClass(ClassFilter classFilter) {
		assertTrue("Expression should match TestBean class", classFilter.matches(TestBean.class));
	}

	@Test
	public void testWithUnsupportedPointcutPrimitive() throws Exception {
		String expression = "call(int org.springframework.tests.sample.beans.TestBean.getAge())";

		try {
			getPointcut(expression).getClassFilter(); // call to getClassFilter forces resolution...
			fail("Should not support call pointcuts");
		}
		catch (UnsupportedPointcutPrimitiveException ex) {
			assertEquals("Should not support call pointcut", PointcutPrimitive.CALL, ex.getUnsupportedPrimitive());
		}

	}

	@Test
	public void testAndSubstitution() {
		Pointcut pc = getPointcut("execution(* *(..)) and args(String)");
		PointcutExpression expr = ((AspectJExpressionPointcut) pc).getPointcutExpression();
		assertEquals("execution(* *(..)) && args(String)",expr.getPointcutExpression());
	}

	@Test
	public void testMultipleAndSubstitutions() {
		Pointcut pc = getPointcut("execution(* *(..)) and args(String) and this(Object)");
		PointcutExpression expr = ((AspectJExpressionPointcut) pc).getPointcutExpression();
		assertEquals("execution(* *(..)) && args(String) && this(Object)",expr.getPointcutExpression());
	}

	private Pointcut getPointcut(String expression) {
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(expression);
		return pointcut;
	}


	public static class OtherIOther implements IOther {

		@Override
		public void absquatulate() {
			// Empty
		}
	}
}


class CallCountingInterceptor implements MethodInterceptor {

	private int count;

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		count++;
		return methodInvocation.proceed();
	}

	public int getCount() {
		return count;
	}

	public void reset() {
		this.count = 0;
	}

}
