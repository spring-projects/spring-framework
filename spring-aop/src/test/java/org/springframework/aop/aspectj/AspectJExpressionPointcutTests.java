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

package org.springframework.aop.aspectj;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.annotation.EmptySpringAnnotation;
import test.annotation.transaction.Tx;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.testfixture.beans.IOther;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.subpkg.DeepBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class AspectJExpressionPointcutTests {

	public static final String MATCH_ALL_METHODS = "execution(* *(..))";

	private Method getAge;

	private Method setAge;

	private Method setSomeNumber;

	private final Map<String, Method> methodsOnHasGeneric = new HashMap<>();


	@BeforeEach
	public void setup() throws NoSuchMethodException {
		getAge = TestBean.class.getMethod("getAge");
		setAge = TestBean.class.getMethod("setAge", int.class);
		setSomeNumber = TestBean.class.getMethod("setSomeNumber", Number.class);

		// Assumes no overloading
		for (Method method : HasGeneric.class.getMethods()) {
			methodsOnHasGeneric.put(method.getName(), method);
		}
	}


	@Test
	public void testMatchExplicit() {
		String expression = "execution(int org.springframework.beans.testfixture.beans.TestBean.getAge())";

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
		String matchesTestBean = which + "(org.springframework.beans.testfixture.beans.TestBean)";
		String matchesIOther = which + "(org.springframework.beans.testfixture.beans.IOther)";
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
		String withinBeansPackage = "within(org.springframework.beans.testfixture.beans.";
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
		assertThatIllegalStateException()
				.isThrownBy(() -> pc.getClassFilter().matches(ITestBean.class))
				.withMessageContaining("expression");
	}

	@Test
	public void testFriendlyErrorOnNoLocation2ArgMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		assertThatIllegalStateException()
				.isThrownBy(() -> pc.getMethodMatcher().matches(getAge, ITestBean.class))
				.withMessageContaining("expression");
	}

	@Test
	public void testFriendlyErrorOnNoLocation3ArgMatching() {
		AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
		assertThatIllegalStateException()
				.isThrownBy(() -> pc.getMethodMatcher().matches(getAge, ITestBean.class, (Object[]) null))
				.withMessageContaining("expression");
	}


	@Test
	public void testMatchWithArgs() throws Exception {
		String expression = "execution(void org.springframework.beans.testfixture.beans.TestBean.setSomeNumber(Number)) && args(Double)";

		Pointcut pointcut = getPointcut(expression);
		ClassFilter classFilter = pointcut.getClassFilter();
		MethodMatcher methodMatcher = pointcut.getMethodMatcher();

		assertMatchesTestBeanClass(classFilter);

		// not currently testable in a reliable fashion
		//assertDoesNotMatchStringClass(classFilter);

		assertThat(methodMatcher.matches(setSomeNumber, TestBean.class, 12D))
				.as("Should match with setSomeNumber with Double input").isTrue();
		assertThat(methodMatcher.matches(setSomeNumber, TestBean.class, 11))
				.as("Should not match setSomeNumber with Integer input").isFalse();
		assertThat(methodMatcher.matches(getAge, TestBean.class)).as("Should not match getAge").isFalse();
		assertThat(methodMatcher.isRuntime()).as("Should be a runtime match").isTrue();
	}

	@Test
	public void testSimpleAdvice() {
		String expression = "execution(int org.springframework.beans.testfixture.beans.TestBean.getAge())";
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
		String expression = "execution(void org.springframework.beans.testfixture.beans.TestBean.setSomeNumber(Number)) && args(Double)";
		CallCountingInterceptor interceptor = new CallCountingInterceptor();
		TestBean testBean = getAdvisedProxy(expression, interceptor);

		assertThat(interceptor.getCount()).as("Calls should be 0").isEqualTo(0);
		testBean.setSomeNumber(30D);
		assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);

		testBean.setSomeNumber(90);
		assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);
	}

	@Test
	public void testInvalidExpression() {
		String expression = "execution(void org.springframework.beans.testfixture.beans.TestBean.setSomeNumber(Number) && args(Double)";
		assertThat(getPointcut(expression).getClassFilter().matches(Object.class)).isFalse();
	}

	private TestBean getAdvisedProxy(String pointcutExpression, CallCountingInterceptor interceptor) {
		TestBean target = new TestBean();

		AspectJExpressionPointcut pointcut = getPointcut(pointcutExpression);

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
		String expression = "call(int org.springframework.beans.testfixture.beans.TestBean.getAge())";
		assertThat(getPointcut(expression).getClassFilter().matches(Object.class)).isFalse();
	}

	@Test
	public void testAndSubstitution() {
		AspectJExpressionPointcut pc = getPointcut("execution(* *(..)) and args(String)");
		String expr = pc.getPointcutExpression().getPointcutExpression();
		assertThat(expr).isEqualTo("execution(* *(..)) && args(String)");
	}

	@Test
	public void testMultipleAndSubstitutions() {
		AspectJExpressionPointcut pc = getPointcut("execution(* *(..)) and args(String) and this(Object)");
		String expr = pc.getPointcutExpression().getPointcutExpression();
		assertThat(expr).isEqualTo("execution(* *(..)) && args(String) && this(Object)");
	}

	private AspectJExpressionPointcut getPointcut(String expression) {
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(expression);
		return pointcut;
	}

	@Test
	public void testMatchGenericArgument() {
		String expression = "execution(* set*(java.util.List<org.springframework.beans.testfixture.beans.TestBean>) )";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		// TODO this will currently map, would be nice for optimization
		//assertTrue(ajexp.matches(HasGeneric.class));
		//assertFalse(ajexp.matches(TestBean.class));

		Method takesGenericList = methodsOnHasGeneric.get("setFriends");
		assertThat(ajexp.matches(takesGenericList, HasGeneric.class)).isTrue();
		assertThat(ajexp.matches(methodsOnHasGeneric.get("setEnemies"), HasGeneric.class)).isTrue();
		assertThat(ajexp.matches(methodsOnHasGeneric.get("setPartners"), HasGeneric.class)).isFalse();
		assertThat(ajexp.matches(methodsOnHasGeneric.get("setPhoneNumbers"), HasGeneric.class)).isFalse();

		assertThat(ajexp.matches(getAge, TestBean.class)).isFalse();
	}

	@Test
	public void testMatchVarargs() throws Exception {

		@SuppressWarnings("unused")
		class MyTemplate {
			public int queryForInt(String sql, Object... params) {
				return 0;
			}
		}

		String expression = "execution(int *.*(String, Object...))";
		AspectJExpressionPointcut jdbcVarArgs = new AspectJExpressionPointcut();
		jdbcVarArgs.setExpression(expression);

		assertThat(jdbcVarArgs.matches(
				MyTemplate.class.getMethod("queryForInt", String.class, Object[].class),
				MyTemplate.class)).isTrue();

		Method takesGenericList = methodsOnHasGeneric.get("setFriends");
		assertThat(jdbcVarArgs.matches(takesGenericList, HasGeneric.class)).isFalse();
		assertThat(jdbcVarArgs.matches(methodsOnHasGeneric.get("setEnemies"), HasGeneric.class)).isFalse();
		assertThat(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPartners"), HasGeneric.class)).isFalse();
		assertThat(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPhoneNumbers"), HasGeneric.class)).isFalse();
		assertThat(jdbcVarArgs.matches(getAge, TestBean.class)).isFalse();
	}

	@Test
	public void testMatchAnnotationOnClassWithAtWithin() throws Exception {
		String expression = "@within(test.annotation.transaction.Tx)";
		testMatchAnnotationOnClass(expression);
	}

	@Test
	public void testMatchAnnotationOnClassWithoutBinding() throws Exception {
		String expression = "within(@test.annotation.transaction.Tx *)";
		testMatchAnnotationOnClass(expression);
	}

	@Test
	public void testMatchAnnotationOnClassWithSubpackageWildcard() throws Exception {
		String expression = "within(@(test.annotation..*) *)";
		AspectJExpressionPointcut springAnnotatedPc = testMatchAnnotationOnClass(expression);
		assertThat(springAnnotatedPc.matches(TestBean.class.getMethod("setName", String.class), TestBean.class)).isFalse();
		assertThat(springAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo"), SpringAnnotated.class)).isTrue();

		expression = "within(@(test.annotation.transaction..*) *)";
		AspectJExpressionPointcut springTxAnnotatedPc = testMatchAnnotationOnClass(expression);
		assertThat(springTxAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo"), SpringAnnotated.class)).isFalse();
	}

	@Test
	public void testMatchAnnotationOnClassWithExactPackageWildcard() throws Exception {
		String expression = "within(@(test.annotation.transaction.*) *)";
		testMatchAnnotationOnClass(expression);
	}

	private AspectJExpressionPointcut testMatchAnnotationOnClass(String expression) throws Exception {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		assertThat(ajexp.matches(getAge, TestBean.class)).isFalse();
		assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isTrue();
		assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isTrue();
		assertThat(ajexp.matches(BeanB.class.getMethod("setName", String.class), BeanB.class)).isTrue();
		assertThat(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
		return ajexp;
	}

	@Test
	public void testAnnotationOnMethodWithFQN() throws Exception {
		String expression = "@annotation(test.annotation.transaction.Tx)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		assertThat(ajexp.matches(getAge, TestBean.class)).isFalse();
		assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
		assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
		assertThat(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
		assertThat(ajexp.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isTrue();
		assertThat(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
	}

	@Test
	public void testAnnotationOnCglibProxyMethod() throws Exception {
		String expression = "@annotation(test.annotation.transaction.Tx)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		ProxyFactory factory = new ProxyFactory(new BeanA());
		factory.setProxyTargetClass(true);
		BeanA proxy = (BeanA) factory.getProxy();
		assertThat(ajexp.matches(BeanA.class.getMethod("getAge"), proxy.getClass())).isTrue();
	}

	@Test
	public void testAnnotationOnDynamicProxyMethod() throws Exception {
		String expression = "@annotation(test.annotation.transaction.Tx)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		ProxyFactory factory = new ProxyFactory(new BeanA());
		factory.setProxyTargetClass(false);
		IBeanA proxy = (IBeanA) factory.getProxy();
		assertThat(ajexp.matches(IBeanA.class.getMethod("getAge"), proxy.getClass())).isTrue();
	}

	@Test
	public void testAnnotationOnMethodWithWildcard() throws Exception {
		String expression = "execution(@(test.annotation..*) * *(..))";
		AspectJExpressionPointcut anySpringMethodAnnotation = new AspectJExpressionPointcut();
		anySpringMethodAnnotation.setExpression(expression);

		assertThat(anySpringMethodAnnotation.matches(getAge, TestBean.class)).isFalse();
		assertThat(anySpringMethodAnnotation.matches(
				HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
		assertThat(anySpringMethodAnnotation.matches(
				HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
		assertThat(anySpringMethodAnnotation.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
		assertThat(anySpringMethodAnnotation.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isTrue();
		assertThat(anySpringMethodAnnotation.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
	}

	@Test
	public void testAnnotationOnMethodArgumentsWithFQN() throws Exception {
		String expression = "@args(*, test.annotation.EmptySpringAnnotation))";
		AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
		takesSpringAnnotatedArgument2.setExpression(expression);

		assertThat(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(
				HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(
				HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();

		assertThat(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
				ProcessesSpringAnnotatedParameters.class)).isTrue();

		// True because it maybeMatches with potential argument subtypes
		assertThat(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
				ProcessesSpringAnnotatedParameters.class)).isTrue();

		assertThat(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
				ProcessesSpringAnnotatedParameters.class, new TestBean(), new BeanA())).isFalse();
	}

	@Test
	public void testAnnotationOnMethodArgumentsWithWildcards() throws Exception {
		String expression = "execution(* *(*, @(test..*) *))";
		AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
		takesSpringAnnotatedArgument2.setExpression(expression);

		assertThat(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(
				HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(
				HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isFalse();
		assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();

		assertThat(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
				ProcessesSpringAnnotatedParameters.class)).isTrue();
		assertThat(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
				ProcessesSpringAnnotatedParameters.class)).isFalse();
	}


	public static class OtherIOther implements IOther {

		@Override
		public void absquatulate() {
			// Empty
		}
	}


	public static class HasGeneric {

		public void setFriends(List<TestBean> friends) {
		}
		public void setEnemies(List<TestBean> enemies) {
		}
		public void setPartners(List<?> partners) {
		}
		public void setPhoneNumbers(List<String> numbers) {
		}
	}


	public static class ProcessesSpringAnnotatedParameters {

		public void takesAnnotatedParameters(TestBean tb, SpringAnnotated sa) {
		}

		public void takesNoAnnotatedParameters(TestBean tb, BeanA tb3) {
		}
	}


	@Tx
	public static class HasTransactionalAnnotation {

		public void foo() {
		}
		public Object bar(String foo) {
			throw new UnsupportedOperationException();
		}
	}


	@EmptySpringAnnotation
	public static class SpringAnnotated {

		public void foo() {
		}
	}


	interface IBeanA {

		@Tx
		int getAge();
	}


	static class BeanA implements IBeanA {

		@SuppressWarnings("unused")
		private String name;

		private int age;

		public void setName(String name) {
			this.name = name;
		}

		@Tx
		@Override
		public int getAge() {
			return age;
		}
	}


	@Tx
	static class BeanB {

		@SuppressWarnings("unused")
		private String name;

		public void setName(String name) {
			this.name = name;
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
