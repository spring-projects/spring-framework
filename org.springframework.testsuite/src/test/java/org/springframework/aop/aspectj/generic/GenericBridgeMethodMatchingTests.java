/*
 * Copyright 2002-2007 the original author or authors.
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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests for AspectJ pointcut expression matching when working with bridge methods.
 *
 * <p>Depending on the caller's static type either the bridge method or the user-implemented method
 * gets called as the way into the proxy. Therefore, we need tests for calling a bean with
 * static type set to type with generic method and to type with specific non-generic implementation.
 *
 * <p>This class focuses on JDK proxy, while a subclass, GenericBridgeMethodMatchingClassProxyTests,
 * focuses on class proxying.
 *
 * See SPR-3556 for more details.
 *
 * @author Ramnivas Laddad
 */
public class GenericBridgeMethodMatchingTests extends AbstractDependencyInjectionSpringContextTests {

	protected DerivedInterface<String> testBean;

	protected CounterAspect counterAspect;


	public GenericBridgeMethodMatchingTests() {
		setPopulateProtectedVariables(true);
	}

	@Override
	protected String getConfigPath() {
		return "genericBridgeMethodMatchingTests-context.xml";
	}

	@Override
	protected void onSetUp() throws Exception {
		counterAspect.count = 0;
		super.onSetUp();
	}

	public void testGenericDerivedInterfaceMethodThroughInterface() {
		testBean.genericDerivedInterfaceMethod("");
		assertEquals(1, counterAspect.count);
	}

	public void testGenericBaseInterfaceMethodThroughInterface() {
		testBean.genericBaseInterfaceMethod("");
		assertEquals(1, counterAspect.count);
	}


	public interface BaseInterface<T> {

		void genericBaseInterfaceMethod(T t);
	}


	public interface DerivedInterface<T> extends BaseInterface<T> {

		public void genericDerivedInterfaceMethod(T t);
	}


	public static class DerivedStringParameterizedClass implements DerivedInterface<String> {

		public void genericDerivedInterfaceMethod(String t) {
		}

		public void genericBaseInterfaceMethod(String t) {
		}
	}
	
	@Aspect
	public static class CounterAspect {

		public int count;

		@Before("execution(* *..BaseInterface+.*(..))")
		public void increment() {
			count++;
		}

	}

}
