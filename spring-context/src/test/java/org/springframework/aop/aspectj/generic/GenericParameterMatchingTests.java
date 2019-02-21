/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.aspectj.generic;

import java.util.Collection;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

/**
 * Tests that poitncut matching is correct with generic method parameter.
 * See SPR-3904 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class GenericParameterMatchingTests {

	private CounterAspect counterAspect;

	private GenericInterface<String> testBean;


	@SuppressWarnings("unchecked")
	@org.junit.Before
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		counterAspect = (CounterAspect) ctx.getBean("counterAspect");
		counterAspect.reset();

		testBean = (GenericInterface<String>) ctx.getBean("testBean");
	}


	@Test
	public void testGenericInterfaceGenericArgExecution() {
		testBean.save("");
		assertEquals(1, counterAspect.genericInterfaceGenericArgExecutionCount);
	}

	@Test
	public void testGenericInterfaceGenericCollectionArgExecution() {
		testBean.saveAll(null);
		assertEquals(1, counterAspect.genericInterfaceGenericCollectionArgExecutionCount);
	}

	@Test
	public void testGenericInterfaceSubtypeGenericCollectionArgExecution() {
		testBean.saveAll(null);
		assertEquals(1, counterAspect.genericInterfaceSubtypeGenericCollectionArgExecutionCount);
	}


	static interface GenericInterface<T> {

		public void save(T bean);

		public void saveAll(Collection<T> beans);
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
