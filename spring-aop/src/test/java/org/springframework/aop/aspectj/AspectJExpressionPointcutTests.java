/*
 * Copyright 2002-2019 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.tests.sample.beans.IOther;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.subpkg.DeepBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AspectJExpressionPointcutTests {

	public static final String MATCH_ALL_METHODS = "execution(* *(..))";

	private Method getAge;

	private Method setAge;

	private Method setSomeNumber;


	@BeforeEach
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

		assertThat(methodMatcher.isRuntime()).as("Should not be a runtime match").isFalse();
		assertMatchesGetAge(methodMatcher);
		assertThat(methodMatcher.matches(setAge, TestBean.class)).as("Expression should match setAge() method").isFalse();
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

		assertThat(methodMatcher.isRuntime()).as("Should not be a runtime match").isFalse();
		assertMatchesGetAge(methodMatcher);
		assertThat(methodMatcher.matches(setAge, TestBean.class)).as("Expression should match setAge(int) method").isTrue();
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

		assertThat(testBeanPc.matches(TestBean.class)).isTrue();
		assertThat(testBeanPc.matches(getAge, TestBean.class)).isTrue();
		assertThat(iOtherPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class)).isTrue();
		assertThat(testBeanPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class)).isFalse();
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

		assertThat(withinBeansPc.matches(TestBean.class)).isTrue();
		assertThat(withinBeansPc.matches(getAge, TestBean.class)).isTrue();
		assertThat(withinBeansPc.matches(DeepBean.class)).isEqualTo(matchSubpackages);
		assertThat(withinBeansPc.matches(
				DeepBean.class.getMethod("aMethod", String.class), DeepBean.class)).isEqualTo(matchSubpackages);
		assertThat(withinBeansPc.matches(String.class)).isFalse();
		assertThat(withinBeansPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class)).isFalse();
	}

	@Test
	public void testFriendlyErrorOnNoLocationClassMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		assertThatIllegalStateException().isThrownBy(() ->
				pc.matches(ITestBean.class))
			.withMessageContaining("expression");
	}

	@Test
	public void testFriendlyErrorOnNoLocation2ArgMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		assertThatIllegalStateException().isThrownBy(() ->
				pc.matches(getAge, ITestBean.class))
			.withMessageContaining("expression");
	}

	@Test
	public void testFriendlyErrorOnNoLocation3ArgMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		assertThatIllegalStateException().isThrownBy(() ->
				pc.matches(getAge, ITestBean.class, (Object[]) null))
			.withMessageContaining("expression");
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

		assertThat(methodMatcher.matches(setSomeNumber, TestBean.class, new Double(12))).as("Should match with setSomeNumber with Double input").isTrue();
		assertThat(methodMatcher.matches(setSomeNumber, TestBean.class, new Integer(11))).as("Should not match setSomeNumber with Integer input").isFalse();
		assertThat(methodMatcher.matches(getAge, TestBean.class)).as("Should not match getAge").isFalse();
		assertThat(methodMatcher.isRuntime()).as("Should be a runtime match").isTrue();
	}

	@Test
	public void testSimpleAdvice() {
		String expression = "execution(int org.springframework.tests.sample.beans.TestBean.getAge())";
		CallCountingInterceptor interceptor = new CallCountingInterceptor();
		TestBean testBean = getAdvisedProxy(expression, interceptor);

		assertThat(interceptor.getCount()).as("Calls should be 0").isEqualTo(0);
		testBean.getAge();
		assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);
		testBean.setAge(90);
		assertThat(interceptor.getCount()).as("Calls should still be 1").isEqualTo(1);
	}

	@Test
	public void testDynamicMatchingProxy() {
		String expression = "execution(void org.springframework.tests.sample.beans.TestBean.setSomeNumber(Number)) && args(Double)";
		CallCountingInterceptor interceptor = new CallCountingInterceptor();
		TestBean testBean = getAdvisedProxy(expression, interceptor);

		assertThat(interceptor.getCount()).as("Calls should be 0").isEqualTo(0);
		testBean.setSomeNumber(new Double(30));
		assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);

		testBean.setSomeNumber(new Integer(90));
		assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);
	}

	@Test
	public void testInvalidExpression() {
		String expression = "execution(void org.springframework.tests.sample.beans.TestBean.setSomeNumber(Number) && args(Double)";
		assertThatIllegalArgumentException().isThrownBy(
				getPointcut(expression)::getClassFilter);  // call to getClassFilter forces resolution
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
		assertThat(methodMatcher.matches(getAge, TestBean.class)).as("Expression should match getAge() method").isTrue();
	}

	private void assertMatchesTestBeanClass(ClassFilter classFilter) {
		assertThat(classFilter.matches(TestBean.class)).as("Expression should match TestBean class").isTrue();
	}

	@Test
	public void testWithUnsupportedPointcutPrimitive() {
		String expression = "call(int org.springframework.tests.sample.beans.TestBean.getAge())";
		assertThatExceptionOfType(UnsupportedPointcutPrimitiveException.class).isThrownBy(() ->
				getPointcut(expression).getClassFilter()) // call to getClassFilter forces resolution...
			.satisfies(ex -> assertThat(ex.getUnsupportedPrimitive()).isEqualTo(PointcutPrimitive.CALL));
	}

	@Test
	public void testAndSubstitution() {
		Pointcut pc = getPointcut("execution(* *(..)) and args(String)");
		PointcutExpression expr = ((AspectJExpressionPointcut) pc).getPointcutExpression();
		assertThat(expr.getPointcutExpression()).isEqualTo("execution(* *(..)) && args(String)");
	}

	@Test
	public void testMultipleAndSubstitutions() {
		Pointcut pc = getPointcut("execution(* *(..)) and args(String) and this(Object)");
		PointcutExpression expr = ((AspectJExpressionPointcut) pc).getPointcutExpression();
		assertThat(expr.getPointcutExpression()).isEqualTo("execution(* *(..)) && args(String) && this(Object)");
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
