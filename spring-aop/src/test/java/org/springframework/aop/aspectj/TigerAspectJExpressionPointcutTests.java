/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import test.annotation.EmptySpringAnnotation;
import test.annotation.transaction.Tx;
import org.springframework.tests.sample.beans.TestBean;

/**
 * Java5-specific {@link AspectJExpressionPointcutTests}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class TigerAspectJExpressionPointcutTests {

	// TODO factor into static in AspectJExpressionPointcut
	private Method getAge;

	private Map<String,Method> methodsOnHasGeneric = new HashMap<String,Method>();


	@Before
	public void setUp() throws NoSuchMethodException {
		getAge = TestBean.class.getMethod("getAge", (Class[]) null);
		// Assumes no overloading
		for (Method m : HasGeneric.class.getMethods()) {
			methodsOnHasGeneric.put(m.getName(), m);
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

	@Test
	public void testMatchGenericArgument() {
		String expression = "execution(* set*(java.util.List<org.springframework.tests.sample.beans.TestBean>) )";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		// TODO this will currently map, would be nice for optimization
		//assertTrue(ajexp.matches(HasGeneric.class));
		//assertFalse(ajexp.matches(TestBean.class));

		Method takesGenericList = methodsOnHasGeneric.get("setFriends");
		assertTrue(ajexp.matches(takesGenericList, HasGeneric.class));
		assertTrue(ajexp.matches(methodsOnHasGeneric.get("setEnemies"), HasGeneric.class));
		assertFalse(ajexp.matches(methodsOnHasGeneric.get("setPartners"), HasGeneric.class));
		assertFalse(ajexp.matches(methodsOnHasGeneric.get("setPhoneNumbers"), HasGeneric.class));

		assertFalse(ajexp.matches(getAge, TestBean.class));
	}

	@Test
	public void testMatchVarargs() throws SecurityException, NoSuchMethodException {

		@SuppressWarnings("unused")
		class MyTemplate {
			public int queryForInt(String sql, Object... params) {
				return 0;
			}
		}

		String expression = "execution(int *.*(String, Object...))";
		AspectJExpressionPointcut jdbcVarArgs = new AspectJExpressionPointcut();
		jdbcVarArgs.setExpression(expression);

		// TODO: the expression above no longer matches Object[]
		// assertFalse(jdbcVarArgs.matches(
		//	JdbcTemplate.class.getMethod("queryForInt", String.class, Object[].class),
		//	JdbcTemplate.class));

		assertTrue(jdbcVarArgs.matches(
				MyTemplate.class.getMethod("queryForInt", String.class, Object[].class),
				MyTemplate.class));

		Method takesGenericList = methodsOnHasGeneric.get("setFriends");
		assertFalse(jdbcVarArgs.matches(takesGenericList, HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(methodsOnHasGeneric.get("setEnemies"), HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPartners"), HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPhoneNumbers"), HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(getAge, TestBean.class));
	}

	@Test
	public void testMatchAnnotationOnClassWithAtWithin() throws SecurityException, NoSuchMethodException {
		String expression = "@within(test.annotation.transaction.Tx)";
		testMatchAnnotationOnClass(expression);
	}

	@Test
	public void testMatchAnnotationOnClassWithoutBinding() throws SecurityException, NoSuchMethodException {
		String expression = "within(@test.annotation.transaction.Tx *)";
		testMatchAnnotationOnClass(expression);
	}

	@Test
	public void testMatchAnnotationOnClassWithSubpackageWildcard() throws SecurityException, NoSuchMethodException {
		String expression = "within(@(test.annotation..*) *)";
		AspectJExpressionPointcut springAnnotatedPc = testMatchAnnotationOnClass(expression);
		assertFalse(springAnnotatedPc.matches(TestBean.class.getMethod("setName", String.class),
				TestBean.class));
		assertTrue(springAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo", (Class[]) null),
				SpringAnnotated.class));

		expression = "within(@(test.annotation.transaction..*) *)";
		AspectJExpressionPointcut springTxAnnotatedPc = testMatchAnnotationOnClass(expression);
		assertFalse(springTxAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo", (Class[]) null),
				SpringAnnotated.class));
	}

	@Test
	public void testMatchAnnotationOnClassWithExactPackageWildcard() throws SecurityException, NoSuchMethodException {
		String expression = "within(@(test.annotation.transaction.*) *)";
		testMatchAnnotationOnClass(expression);
	}

	private AspectJExpressionPointcut testMatchAnnotationOnClass(String expression) throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		assertFalse(ajexp.matches(getAge, TestBean.class));
		assertTrue(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertTrue(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertTrue(ajexp.matches(BeanB.class.getMethod("setName", String.class), BeanB.class));
		assertFalse(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));
		return ajexp;
	}

	@Test
	public void testAnnotationOnMethodWithFQN() throws SecurityException, NoSuchMethodException {
		String expression = "@annotation(test.annotation.transaction.Tx)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);

		assertFalse(ajexp.matches(getAge, TestBean.class));
		assertFalse(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));
		assertTrue(ajexp.matches(BeanA.class.getMethod("getAge", (Class[]) null), BeanA.class));
		assertFalse(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));
	}

	@Test
	public void testAnnotationOnMethodWithWildcard() throws SecurityException, NoSuchMethodException {
		String expression = "execution(@(test.annotation..*) * *(..))";
		AspectJExpressionPointcut anySpringMethodAnnotation = new AspectJExpressionPointcut();
		anySpringMethodAnnotation.setExpression(expression);

		assertFalse(anySpringMethodAnnotation.matches(getAge, TestBean.class));
		assertFalse(anySpringMethodAnnotation.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(anySpringMethodAnnotation.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(anySpringMethodAnnotation.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));
		assertTrue(anySpringMethodAnnotation.matches(BeanA.class.getMethod("getAge", (Class[]) null), BeanA.class));
		assertFalse(anySpringMethodAnnotation.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));
	}

	@Test
	public void testAnnotationOnMethodArgumentsWithFQN() throws SecurityException, NoSuchMethodException {
		String expression = "@args(*, test.annotation.EmptySpringAnnotation))";
		AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
		takesSpringAnnotatedArgument2.setExpression(expression);

		assertFalse(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("getAge", (Class[]) null), BeanA.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));

		assertTrue(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
				ProcessesSpringAnnotatedParameters.class));

		// True because it maybeMatches with potential argument subtypes
		assertTrue(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
				ProcessesSpringAnnotatedParameters.class));

		assertFalse(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
				ProcessesSpringAnnotatedParameters.class,
				new Object[] { new TestBean(), new BeanA()})
		);
	}

	@Test
	public void testAnnotationOnMethodArgumentsWithWildcards() throws SecurityException, NoSuchMethodException {
		String expression = "execution(* *(*, @(test..*) *))";
		AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
		takesSpringAnnotatedArgument2.setExpression(expression);

		assertFalse(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("getAge", (Class[]) null), BeanA.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class));

		assertTrue(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
				ProcessesSpringAnnotatedParameters.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
				ProcessesSpringAnnotatedParameters.class));
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


	static class BeanA {
		private String name;

		private int age;

		public void setName(String name) {
			this.name = name;
		}

		@Tx
		public int getAge() {
			return age;
		}
	}


	@Tx
	static class BeanB {
		private String name;

		public void setName(String name) {
			this.name = name;
		}
	}

}
