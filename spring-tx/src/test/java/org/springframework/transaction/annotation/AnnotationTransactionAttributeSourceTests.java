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

package org.springframework.transaction.annotation;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import jakarta.ejb.TransactionAttributeType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationTransactionAttributeSource}.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class AnnotationTransactionAttributeSourceTests {

	private final AnnotationTransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource();

	@Test
	void serializable() throws Exception {
		TestBean1 tb = new TestBean1();
		CallCountingTransactionManager ptm = new CallCountingTransactionManager();
		TransactionInterceptor ti = new TransactionInterceptor((TransactionManager) ptm, this.attributeSource);

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setInterfaces(ITestBean1.class);
		proxyFactory.addAdvice(ti);
		proxyFactory.setTarget(tb);
		ITestBean1 proxy = (ITestBean1) proxyFactory.getProxy();
		proxy.getAge();
		assertThat(ptm.commits).isEqualTo(1);

		ITestBean1 serializedProxy = SerializationTestUtils.serializeAndDeserialize(proxy);
		serializedProxy.getAge();
		Advised advised = (Advised) serializedProxy;
		TransactionInterceptor serializedTi = (TransactionInterceptor) advised.getAdvisors()[0].getAdvice();
		CallCountingTransactionManager serializedPtm =
				(CallCountingTransactionManager) serializedTi.getTransactionManager();
		assertThat(serializedPtm.commits).isEqualTo(2);
	}

	@Test
	void nullOrEmpty() {
		Method method = getMethod(Empty.class, "getAge");
		assertThat(this.attributeSource.getTransactionAttribute(method, null)).isNull();

		// Try again in case of caching
		assertThat(this.attributeSource.getTransactionAttribute(method, null)).isNull();
	}

	/**
	 * Test the important case where the invocation is on a proxied interface method
	 * but the attribute is defined on the target class.
	 */
	@Test
	void transactionAttributeDeclaredOnClassMethod() {
		TransactionAttribute actual = getTransactionAttribute(TestBean1.class, ITestBean1.class, "getAge");
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class)));
	}

	/**
	 * Test the important case where the invocation is on a proxied interface method
	 * but the attribute is defined on the target class.
	 */
	@Test
	void transactionAttributeDeclaredOnCglibClassMethod() {
		TestBean1 tb = new TestBean1();
		ProxyFactory pf = new ProxyFactory(tb);
		pf.setProxyTargetClass(true);
		Object proxy = pf.getProxy();

		TransactionAttribute actual = getTransactionAttribute(proxy.getClass(), ITestBean1.class, "getAge");
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class)));
	}

	/**
	 * Test case where attribute is on the interface method.
	 */
	@Test
	void transactionAttributeDeclaredOnInterfaceMethodOnly() {
		TransactionAttribute actual = getTransactionAttribute(TestBean2.class, ITestBean2.class, "getAge");
		assertThat(actual).satisfies(hasNoRollbackRule());
	}

	/**
	 * Test that when an attribute exists on both class and interface, class takes precedence.
	 */
	@Test
	void transactionAttributeOnTargetClassMethodOverridesAttributeOnInterfaceMethod() {
		this.attributeSource.setEmbeddedValueResolver(strVal -> ("${myTimeout}".equals(strVal) ? "5" : strVal));

		TransactionAttribute actual = getTransactionAttribute(TestBean3.class, ITestBean3.class, "getAge");
		assertThat(actual.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRES_NEW);
		assertThat(actual.getIsolationLevel()).isEqualTo(TransactionAttribute.ISOLATION_REPEATABLE_READ);
		assertThat(actual.getTimeout()).isEqualTo(5);
		assertThat(actual.isReadOnly()).isTrue();
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));

		TransactionAttribute actual2 = getTransactionAttribute(TestBean3.class, ITestBean3.class, "setAge", int.class);
		assertThat(actual2.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRES_NEW);
		assertThat(actual2.getIsolationLevel()).isEqualTo(TransactionAttribute.ISOLATION_REPEATABLE_READ);
		assertThat(actual2.getTimeout()).isEqualTo(5);
		assertThat(actual2.isReadOnly()).isTrue();
		assertThat(actual2).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));

		TransactionAttribute actual3 = getTransactionAttribute(TestBean3.class, ITestBean3.class, "getName");
		assertThat(actual3.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
	}

	@Test
	void rollbackRulesAreApplied() {
		Method method = getMethod(TestBean3.class, "getAge");

		TransactionAttribute actual = getTransactionAttribute(TestBean3.class, method);
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute("java.lang.Exception"),
				new NoRollbackRuleAttribute(IOException.class)));
		assertThat(actual.rollbackOn(new Exception())).isTrue();
		assertThat(actual.rollbackOn(new IOException())).isFalse();

		TransactionAttribute actual2 = getTransactionAttribute(method.getDeclaringClass(), method);
		assertThat(actual2).satisfies(hasRollbackRules(new RollbackRuleAttribute("java.lang.Exception"),
				new NoRollbackRuleAttribute(IOException.class)));
		assertThat(actual2.rollbackOn(new Exception())).isTrue();
		assertThat(actual2.rollbackOn(new IOException())).isFalse();
	}

	@Test
	void labelsAreApplied() {
		TransactionAttribute actual = getTransactionAttribute(TestBean11.class, TestBean11.class, "getAge");
		assertThat(actual.getLabels()).containsOnly("retryable", "long-running");

		TransactionAttribute actual2 = getTransactionAttribute(TestBean11.class, TestBean11.class, "setAge", int.class);
		assertThat(actual2.getLabels()).containsOnly("short-running");
	}

	/**
	 * Test that transaction attribute is inherited from class
	 * if not specified on method.
	 */
	@Test
	void defaultsToClassTransactionAttribute() {
		TransactionAttribute actual = getTransactionAttribute(TestBean4.class, TestBean4.class, "getAge");
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));
	}

	@Test
	void customClassAttributeDetected() {
		TransactionAttribute actual = getTransactionAttribute(TestBean5.class, "getAge");
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));
	}

	@Test
	void customMethodAttributeDetected() {
		TransactionAttribute actual = getTransactionAttribute(TestBean6.class, "getAge");
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));
	}

	@Test
	void customClassAttributeWithReadOnlyOverrideDetected() {
		TransactionAttribute actual = getTransactionAttribute(TestBean7.class, "getAge");
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));
		assertThat(actual.isReadOnly()).isTrue();
	}

	@Test
	void customMethodAttributeWithReadOnlyOverrideDetected() {
		TransactionAttribute actual = getTransactionAttribute(TestBean8.class, "getAge");
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));
		assertThat(actual.isReadOnly()).isTrue();
	}

	@Test
	void customClassAttributeWithReadOnlyOverrideOnInterface() {
		Method method = getMethod(TestInterface9.class, "getAge");

		Transactional annotation = AnnotationUtils.findAnnotation(method, Transactional.class);
		assertThat(annotation).as("AnnotationUtils.findAnnotation should not find @Transactional for TestBean9.getAge()").isNull();
		annotation = AnnotationUtils.findAnnotation(TestBean9.class, Transactional.class);
		assertThat(annotation).as("AnnotationUtils.findAnnotation failed to find @Transactional for TestBean9").isNotNull();

		TransactionAttribute actual = getTransactionAttribute(TestBean9.class, method);
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));
		assertThat(actual.isReadOnly()).isTrue();
	}

	@Test
	void customMethodAttributeWithReadOnlyOverrideOnInterface() {
		Method method = getMethod(TestInterface10.class, "getAge");

		Transactional annotation = AnnotationUtils.findAnnotation(method, Transactional.class);
		assertThat(annotation).as("AnnotationUtils.findAnnotation failed to find @Transactional for TestBean10.getAge()").isNotNull();
		annotation = AnnotationUtils.findAnnotation(TestBean10.class, Transactional.class);
		assertThat(annotation).as("AnnotationUtils.findAnnotation should not find @Transactional for TestBean10").isNull();

		TransactionAttribute actual = getTransactionAttribute(TestBean10.class, method);
		assertThat(actual).satisfies(hasRollbackRules(new RollbackRuleAttribute(Exception.class),
				new NoRollbackRuleAttribute(IOException.class)));
		assertThat(actual.isReadOnly()).isTrue();
	}

	@Nested
	class JtaAttributeTests {

		@Test
		void transactionAttributeDeclaredOnClassMethod() {
			TransactionAttribute getAgeAttr = getTransactionAttribute(JtaAnnotatedBean1.class, ITestBean1.class, "getAge");
			assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			TransactionAttribute getNameAttr = getTransactionAttribute(JtaAnnotatedBean1.class, ITestBean1.class, "getName");
			assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
		}

		@Test
		void transactionAttributeDeclaredOnClass() {
			TransactionAttribute getAgeAttr = getTransactionAttribute(JtaAnnotatedBean2.class, ITestBean1.class, "getAge");
			assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			TransactionAttribute getNameAttr = getTransactionAttribute(JtaAnnotatedBean2.class, ITestBean1.class, "getName");
			assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
		}

		@Test
		void transactionAttributeDeclaredOnInterface() {
			TransactionAttribute getAgeAttr = getTransactionAttribute(JtaAnnotatedBean3.class, ITestJta.class, "getAge");
			assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			TransactionAttribute getNameAttr = getTransactionAttribute(JtaAnnotatedBean3.class, ITestJta.class, "getName");
			assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
		}

		static class JtaAnnotatedBean1 implements ITestBean1 {

			private String name;

			private int age;

			@Override
			@jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.SUPPORTS)
			public String getName() {
				return name;
			}

			@Override
			public void setName(String name) {
				this.name = name;
			}

			@Override
			@jakarta.transaction.Transactional
			public int getAge() {
				return age;
			}

			@Override
			public void setAge(int age) {
				this.age = age;
			}
		}


		@jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.SUPPORTS)
		static class JtaAnnotatedBean2 implements ITestBean1 {

			private String name;

			private int age;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public void setName(String name) {
				this.name = name;
			}

			@Override
			@jakarta.transaction.Transactional
			public int getAge() {
				return age;
			}

			@Override
			public void setAge(int age) {
				this.age = age;
			}
		}

		static class JtaAnnotatedBean3 implements ITestJta {

			private String name;

			private int age;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public void setName(String name) {
				this.name = name;
			}

			@Override
			public int getAge() {
				return age;
			}

			@Override
			public void setAge(int age) {
				this.age = age;
			}
		}


		@jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.SUPPORTS)
		interface ITestJta {

			@jakarta.transaction.Transactional
			int getAge();

			void setAge(int age);

			String getName();

			void setName(String name);
		}

	}

	@Nested
	class Ejb3AttributeTests {

		@Test
		void transactionAttributeDeclaredOnClassMethod() {
			TransactionAttribute getAgeAttr = getTransactionAttribute(Ejb3AnnotatedBean1.class, ITestBean1.class, "getAge");
			assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			TransactionAttribute getNameAttr = getTransactionAttribute(Ejb3AnnotatedBean1.class, ITestBean1.class, "getName");
			assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
		}

		@Test
		void transactionAttributeDeclaredOnClass() {
			TransactionAttribute getAgeAttr = getTransactionAttribute(Ejb3AnnotatedBean2.class, ITestBean1.class, "getAge");
			assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			TransactionAttribute getNameAttr = getTransactionAttribute(Ejb3AnnotatedBean2.class, ITestBean1.class, "getName");
			assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
		}

		@Test
		void transactionAttributeDeclaredOnInterface() {
			TransactionAttribute getAgeAttr = getTransactionAttribute(Ejb3AnnotatedBean3.class, ITestEjb.class, "getAge");
			assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			TransactionAttribute getNameAttr = getTransactionAttribute(Ejb3AnnotatedBean3.class, ITestEjb.class, "getName");
			assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
		}


		@jakarta.ejb.TransactionAttribute(TransactionAttributeType.SUPPORTS)
		interface ITestEjb {

			@jakarta.ejb.TransactionAttribute
			int getAge();

			void setAge(int age);

			String getName();

			void setName(String name);
		}

		static class Ejb3AnnotatedBean1 implements ITestBean1 {

			private String name;

			private int age;

			@Override
			@jakarta.ejb.TransactionAttribute(TransactionAttributeType.SUPPORTS)
			public String getName() {
				return name;
			}

			@Override
			public void setName(String name) {
				this.name = name;
			}

			@Override
			@jakarta.ejb.TransactionAttribute
			public int getAge() {
				return age;
			}

			@Override
			public void setAge(int age) {
				this.age = age;
			}
		}


		@jakarta.ejb.TransactionAttribute(TransactionAttributeType.SUPPORTS)
		static class Ejb3AnnotatedBean2 implements ITestBean1 {

			private String name;

			private int age;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public void setName(String name) {
				this.name = name;
			}

			@Override
			@jakarta.ejb.TransactionAttribute
			public int getAge() {
				return age;
			}

			@Override
			public void setAge(int age) {
				this.age = age;
			}
		}

		static class Ejb3AnnotatedBean3 implements ITestEjb {

			private String name;

			private int age;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public void setName(String name) {
				this.name = name;
			}

			@Override
			public int getAge() {
				return age;
			}

			@Override
			public void setAge(int age) {
				this.age = age;
			}
		}
	}

	@Nested
	class GroovyTests {

		@Test
		void transactionAttributeDeclaredOnGroovyClass() {
			TransactionAttribute getAgeAttr = getTransactionAttribute(GroovyTestBean.class, ITestBean1.class, "getAge");
			assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			TransactionAttribute getNameAttr = getTransactionAttribute(GroovyTestBean.class, ITestBean1.class, "getName");
			assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
			Method getMetaClassMethod = getMethod(GroovyObject.class, "getMetaClass");
			assertThat(attributeSource.getTransactionAttribute(getMetaClassMethod, GroovyTestBean.class)).isNull();
		}

		@Transactional
		static class GroovyTestBean implements ITestBean1, GroovyObject {

			private String name;

			private int age;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public void setName(String name) {
				this.name = name;
			}

			@Override
			public int getAge() {
				return age;
			}

			@Override
			public void setAge(int age) {
				this.age = age;
			}

			@Override
			public Object invokeMethod(String name, Object args) {
				return null;
			}

			@Override
			public Object getProperty(String propertyName) {
				return null;
			}

			@Override
			public void setProperty(String propertyName, Object newValue) {
			}

			@Override
			public MetaClass getMetaClass() {
				return null;
			}

			@Override
			public void setMetaClass(MetaClass metaClass) {
			}
		}
	}

	private Consumer<TransactionAttribute> hasRollbackRules(RollbackRuleAttribute... rollbackRuleAttributes) {
		return transactionAttribute -> {
			RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
			rbta.getRollbackRules().addAll(Arrays.asList(rollbackRuleAttributes));
			assertThat(transactionAttribute).isInstanceOfSatisfying(RuleBasedTransactionAttribute.class,
					attribute -> assertThat(attribute.getRollbackRules()).isEqualTo(rbta.getRollbackRules()));
		};
	}

	private Consumer<TransactionAttribute> hasNoRollbackRule() {
		return hasRollbackRules();
	}

	private TransactionAttribute getTransactionAttribute(Class<?> targetType, Method method) {
		TransactionAttribute transactionAttribute = this.attributeSource.getTransactionAttribute(method, targetType);
		assertThat(transactionAttribute).isNotNull();
		return transactionAttribute;
	}

	private TransactionAttribute getTransactionAttribute(Class<?> targetType, Class<?> declaringType, String methodName, Class<?>... parameterTypes) {
		return getTransactionAttribute(targetType, getMethod(declaringType, methodName, parameterTypes));
	}

	private TransactionAttribute getTransactionAttribute(Class<?> declaringType, String methodName, Class<?>... parameterTypes) {
		return getTransactionAttribute(declaringType, declaringType, methodName, parameterTypes);
	}

	private Method getMethod(Class<?> declaringType, String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(declaringType, methodName, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}


	interface ITestBean1 {

		int getAge();

		void setAge(int age);

		String getName();

		void setName(String name);
	}


	interface ITestBean2 {

		@Transactional
		int getAge();

		void setAge(int age);
	}


	interface ITestBean2X extends ITestBean2 {

		String getName();

		void setName(String name);
	}


	@Transactional
	interface ITestBean3 {

		int getAge();

		void setAge(int age);

		String getName();

		void setName(String name);
	}


	static class Empty implements ITestBean1 {

		private String name;

		private int age;

		public Empty() {
		}

		public Empty(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int getAge() {
			return age;
		}

		@Override
		public void setAge(int age) {
			this.age = age;
		}
	}


	@SuppressWarnings("serial")
	static class TestBean1 implements ITestBean1, Serializable {

		private String name;

		private int age;

		public TestBean1() {
		}

		public TestBean1(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		@Transactional(rollbackFor = Exception.class)
		public int getAge() {
			return age;
		}

		@Override
		public void setAge(int age) {
			this.age = age;
		}
	}


	static class TestBean2 implements ITestBean2X {

		private String name;

		private int age;

		public TestBean2() {
		}

		public TestBean2(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int getAge() {
			return age;
		}

		@Override
		public void setAge(int age) {
			this.age = age;
		}
	}


	static class TestBean3 implements ITestBean3 {

		private String name;

		private int age;

		public TestBean3() {
		}

		public TestBean3(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		@Transactional(propagation = Propagation.REQUIRES_NEW, isolation=Isolation.REPEATABLE_READ,
				timeout = 5, readOnly = true, rollbackFor = Exception.class, noRollbackFor = IOException.class)
		public int getAge() {
			return age;
		}

		@Override
		@Transactional(propagation = Propagation.REQUIRES_NEW, isolation=Isolation.REPEATABLE_READ,
				timeoutString = "${myTimeout}", readOnly = true, rollbackFor = Exception.class,
				noRollbackFor = IOException.class)
		public void setAge(int age) {
			this.age = age;
		}
	}


	@Transactional(rollbackFor = Exception.class, noRollbackFor = IOException.class)
	static class TestBean4 implements ITestBean3 {

		private String name;

		private int age;

		public TestBean4() {
		}

		public TestBean4(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int getAge() {
			return age;
		}

		@Override
		public void setAge(int age) {
			this.age = age;
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Transactional(rollbackFor = Exception.class, noRollbackFor = IOException.class)
	@interface Tx {
	}


	@Tx
	static class TestBean5 {

		public int getAge() {
			return 10;
		}
	}


	static class TestBean6 {

		@Tx
		public int getAge() {
			return 10;
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Transactional(rollbackFor = Exception.class, noRollbackFor = IOException.class)
	@interface TxWithAttribute {

		@AliasFor(annotation = Transactional.class)
		boolean readOnly();
	}


	@TxWithAttribute(readOnly = true)
	static class TestBean7 {

		public int getAge() {
			return 10;
		}
	}


	static class TestBean8 {

		@TxWithAttribute(readOnly = true)
		public int getAge() {
			return 10;
		}
	}


	@TxWithAttribute(readOnly = true)
	interface TestInterface9 {

		int getAge();
	}


	static class TestBean9 implements TestInterface9 {

		@Override
		public int getAge() {
			return 10;
		}
	}


	interface TestInterface10 {

		@TxWithAttribute(readOnly = true)
		int getAge();
	}


	static class TestBean10 implements TestInterface10 {

		@Override
		public int getAge() {
			return 10;
		}
	}

	@Transactional(label = {"retryable", "long-running"})
	static class TestBean11 {

		private int age = 10;

		@Transactional(label = "short-running")
		public void setAge(int age) {
			this.age = age;
		}

		public int getAge() {
			return age;
		}
	}

}
