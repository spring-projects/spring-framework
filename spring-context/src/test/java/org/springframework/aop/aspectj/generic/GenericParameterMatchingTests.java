/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.generic;

import java.util.Collection;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that pointcut matching is correct with generic method parameter.
 * See SPR-3904 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
class GenericParameterMatchingTests {

	private ClassPathXmlApplicationContext ctx;

	private CounterAspect counterAspect;

	private GenericInterface<String> testBean;


	@BeforeEach
	@SuppressWarnings("unchecked")
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		counterAspect = (CounterAspect) ctx.getBean("counterAspect");
		testBean = (GenericInterface<String>) ctx.getBean("testBean");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void genericInterfaceGenericArgExecution() {
		testBean.save("");
		assertThat(counterAspect.genericInterfaceGenericArgExecutionCount).isEqualTo(1);
	}

	@Test
	void genericInterfaceGenericCollectionArgExecution() {
		testBean.saveAll(null);
		assertThat(counterAspect.genericInterfaceGenericCollectionArgExecutionCount).isEqualTo(1);
	}

	@Test
	void genericInterfaceSubtypeGenericCollectionArgExecution() {
		testBean.saveAll(null);
		assertThat(counterAspect.genericInterfaceSubtypeGenericCollectionArgExecutionCount).isEqualTo(1);
	}


	interface GenericInterface<T> {

		void save(T bean);

		void saveAll(Collection<T> beans);
	}


	static class GenericImpl<T> implements GenericInterface<T> {

		@Override
		public void save(T bean) {
		}

		@Override
		public void saveAll(Collection<T> beans) {
		}
	}


	@Aspect
	static class CounterAspect {

		int genericInterfaceGenericArgExecutionCount;
		int genericInterfaceGenericCollectionArgExecutionCount;
		int genericInterfaceSubtypeGenericCollectionArgExecutionCount;

		public void reset() {
			genericInterfaceGenericArgExecutionCount = 0;
			genericInterfaceGenericCollectionArgExecutionCount = 0;
			genericInterfaceSubtypeGenericCollectionArgExecutionCount = 0;
		}

		@Pointcut("execution(* org.springframework.aop.aspectj.generic.GenericParameterMatchingTests.GenericInterface.save(..))")
		public void genericInterfaceGenericArgExecution() {}

		@Pointcut("execution(* org.springframework.aop.aspectj.generic.GenericParameterMatchingTests.GenericInterface.saveAll(..))")
		public void GenericInterfaceGenericCollectionArgExecution() {}

		@Pointcut("execution(* org.springframework.aop.aspectj.generic.GenericParameterMatchingTests.GenericInterface+.saveAll(..))")
		public void genericInterfaceSubtypeGenericCollectionArgExecution() {}

		@Before("genericInterfaceGenericArgExecution()")
		public void incrementGenericInterfaceGenericArgExecution() {
			genericInterfaceGenericArgExecutionCount++;
		}

		@Before("GenericInterfaceGenericCollectionArgExecution()")
		public void incrementGenericInterfaceGenericCollectionArgExecution() {
			genericInterfaceGenericCollectionArgExecutionCount++;
		}

		@Before("genericInterfaceSubtypeGenericCollectionArgExecution()")
		public void incrementGenericInterfaceSubtypeGenericCollectionArgExecution() {
			genericInterfaceSubtypeGenericCollectionArgExecutionCount++;
		}
	}

}
