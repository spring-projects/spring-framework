/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests that poitncut matching is correct with generic method parameter.
 * See SPR-3904 for more details.
 *
 * @author Ramnivas Laddad
 */
public class GenericParameterMatchingTests extends AbstractDependencyInjectionSpringContextTests {

	protected CounterAspect counterAspect;

	protected GenericInterface<String> testBean;


	public GenericParameterMatchingTests() {
		setPopulateProtectedVariables(true);
	}

	@Override
	protected String getConfigPath() {
		return "genericParameterMatchingTests-context.xml";
	}

	@Override
	protected void onSetUp() throws Exception {
		counterAspect.reset();
		super.onSetUp();
	}


	public void testGenericInterfaceGenericArgExecution() {
		testBean.save("");
		assertEquals(1, counterAspect.genericInterfaceGenericArgExecutionCount);
	}

	public void testGenericInterfaceGenericCollectionArgExecution() {
		testBean.saveAll(null);
		// TODO: uncomment once we officially update to AspectJ 1.6.0
		//assertEquals(1, counterAspect.genericInterfaceGenericCollectionArgExecutionCount);
	}
	
	public void testGenericInterfaceSubtypeGenericCollectionArgExecution() {
		testBean.saveAll(null);
		assertEquals(1, counterAspect.genericInterfaceSubtypeGenericCollectionArgExecutionCount);
	}


	static interface GenericInterface<T> {

		public void save(T bean);

		public void saveAll(Collection<T> beans);
	}


	static class GenericImpl<T> implements GenericInterface<T> {

		public void save(T bean) {
		}

		public void saveAll(Collection<T> beans) {
		}
	}


	@Aspect
	public static class CounterAspect {

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
