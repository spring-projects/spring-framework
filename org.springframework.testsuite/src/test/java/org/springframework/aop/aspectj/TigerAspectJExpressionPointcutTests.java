/*
 * Copyright 2002-2005 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSourceTests;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSourceTests.TestBean3;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSourceTests.TestBean4;
import org.springframework.transaction.annotation.Transactional;

/** 
 * Java5-specific AspectJExpressionPointcutTests.
 *
 * @author Rod Johnson
 */
public class TigerAspectJExpressionPointcutTests extends TestCase {

	// TODO factor into static in AspectJExpressionPointcut
	private Method getAge;

	private Map<String,Method> methodsOnHasGeneric = new HashMap<String,Method>();


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
		public void setPartners(List partners) {
		}
		public void setPhoneNumbers(List<String> numbers) {
		}
	}

	public void testMatchGenericArgument() {
		String expression = "execution(* set*(java.util.List<org.springframework.beans.TestBean>) )";
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
	
	public void testMatchVarargs() throws SecurityException, NoSuchMethodException {
		String expression = "execution(int *.*(String, Object...) )";
		AspectJExpressionPointcut jdbcVarArgs = new AspectJExpressionPointcut();
		jdbcVarArgs.setExpression(expression);
		
		assertFalse(jdbcVarArgs.matches(
				JdbcTemplate.class.getMethod("queryForInt", String.class, Object[].class),
				JdbcTemplate.class));
		
		assertTrue(jdbcVarArgs.matches(
				SimpleJdbcTemplate.class.getMethod("queryForInt", String.class, Object[].class),
				SimpleJdbcTemplate.class));
		
		Method takesGenericList = methodsOnHasGeneric.get("setFriends");
		assertFalse(jdbcVarArgs.matches(takesGenericList, HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(methodsOnHasGeneric.get("setEnemies"), HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPartners"), HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPhoneNumbers"), HasGeneric.class));
		assertFalse(jdbcVarArgs.matches(getAge, TestBean.class));
	}
	
	public void testMatchAnnotationOnClassWithAtWithin() throws SecurityException, NoSuchMethodException {
		String expression = "@within(org.springframework.transaction.annotation.Transactional)";
		testMatchAnnotationOnClass(expression);
	}
	
	public void testMatchAnnotationOnClassWithoutBinding() throws SecurityException, NoSuchMethodException {
		String expression = "within(@org.springframework.transaction.annotation.Transactional *)";
		testMatchAnnotationOnClass(expression);
	}
	
	public void testMatchAnnotationOnClassWithSubpackageWildcard() throws SecurityException, NoSuchMethodException {
		String expression = "within(@(org.springframework..*) *)";
		AspectJExpressionPointcut springAnnotatedPc = testMatchAnnotationOnClass(expression);
		assertFalse(springAnnotatedPc.matches(TestBean.class.getMethod("setName", String.class), 
				TestBean.class));
		assertTrue(springAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo", (Class[]) null), 
				SpringAnnotated.class));
		
		expression = "within(@(org.springframework.transaction..*) *)";
		AspectJExpressionPointcut springTxAnnotatedPc = testMatchAnnotationOnClass(expression);
		assertFalse(springTxAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo", (Class[]) null), 
				SpringAnnotated.class));
	}
	
	public void testMatchAnnotationOnClassWithExactPackageWildcard() throws SecurityException, NoSuchMethodException {
		String expression = "within(@(org.springframework.transaction.annotation.*) *)";
		testMatchAnnotationOnClass(expression);
	}
	
	private AspectJExpressionPointcut testMatchAnnotationOnClass(String expression) throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);
		
		assertFalse(ajexp.matches(getAge, TestBean.class));
		assertTrue(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertTrue(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertTrue(ajexp.matches(AnnotationTransactionAttributeSourceTests.TestBean4.class.getMethod("setName", String.class), TestBean4.class));
		assertFalse(ajexp.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
		return ajexp;
	}
	
	public void testAnnotationOnMethodWithFQN() throws SecurityException, NoSuchMethodException {
		String expression = "@annotation(org.springframework.transaction.annotation.Transactional)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(expression);
		
		assertFalse(ajexp.matches(getAge, TestBean.class));
		assertFalse(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(ajexp.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
		assertTrue(ajexp.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("getAge", (Class[]) null), TestBean3.class));
		assertFalse(ajexp.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
	}
	
	public void testAnnotationOnMethodWithWildcard() throws SecurityException, NoSuchMethodException {
		String expression = "execution(@(org.springframework..*) * *(..))";
		AspectJExpressionPointcut anySpringMethodAnnotation = new AspectJExpressionPointcut();
		anySpringMethodAnnotation.setExpression(expression);
		
		assertFalse(anySpringMethodAnnotation.matches(getAge, TestBean.class));
		assertFalse(anySpringMethodAnnotation.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(anySpringMethodAnnotation.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(anySpringMethodAnnotation.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
		assertTrue(anySpringMethodAnnotation.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("getAge", (Class[]) null), TestBean3.class));
		assertFalse(anySpringMethodAnnotation.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
	}

	public void testAnnotationOnMethodArgumentsWithFQN() throws SecurityException, NoSuchMethodException {
		String expression = "@args(*, org.springframework.aop.aspectj.TigerAspectJExpressionPointcutTests.EmptySpringAnnotation))";
		AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
		takesSpringAnnotatedArgument2.setExpression(expression);
		
		assertFalse(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("getAge", (Class[]) null), TestBean3.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
		
		assertTrue(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
				ProcessesSpringAnnotatedParameters.class));
		
		// True because it maybeMatches with potential argument subtypes
		assertTrue(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, TestBean3.class),
				ProcessesSpringAnnotatedParameters.class));
		
		assertFalse(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, TestBean3.class),
				ProcessesSpringAnnotatedParameters.class,
				new Object[] { new TestBean(), new TestBean3()})
		);
	}
	
	public void testAnnotationOnMethodArgumentsWithWildcards() throws SecurityException, NoSuchMethodException {
		String expression = "execution(* *(*, @(org.springframework..*) *))";
		AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
		takesSpringAnnotatedArgument2.setExpression(expression);
		
		assertFalse(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("foo", (Class[]) null), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("getAge", (Class[]) null), TestBean3.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(AnnotationTransactionAttributeSourceTests.TestBean3.class.getMethod("setName", String.class), TestBean3.class));
		
		assertTrue(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
				ProcessesSpringAnnotatedParameters.class));
		assertFalse(takesSpringAnnotatedArgument2.matches(
				ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, TestBean3.class),
				ProcessesSpringAnnotatedParameters.class));
	}


	public static class ProcessesSpringAnnotatedParameters {

		public void takesAnnotatedParameters(TestBean tb, SpringAnnotated sa) {
		}

		public void takesNoAnnotatedParameters(TestBean tb, TestBean3 tb3) {
		}
	}


	@Transactional
	public static class HasTransactionalAnnotation {

		public void foo() {
		}
		public Object bar(String foo) {
			throw new UnsupportedOperationException();
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface EmptySpringAnnotation {

	}


	@EmptySpringAnnotation
	public static class SpringAnnotated {
		public void foo() {
		}
	}

}
