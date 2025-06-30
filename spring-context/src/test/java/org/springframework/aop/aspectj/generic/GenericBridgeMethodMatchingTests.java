/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.aspectj.generic;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

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
 * @author Chris Beams
 */
class GenericBridgeMethodMatchingTests {

	private ClassPathXmlApplicationContext ctx;

	protected DerivedInterface<String> testBean;

	protected GenericCounterAspect counterAspect;


	@BeforeEach
	@SuppressWarnings("unchecked")
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		counterAspect = ctx.getBean("counterAspect", GenericCounterAspect.class);
		counterAspect.count = 0;

		testBean = (DerivedInterface<String>) ctx.getBean("testBean");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void testGenericDerivedInterfaceMethodThroughInterface() {
		testBean.genericDerivedInterfaceMethod("");
		assertThat(counterAspect.count).isEqualTo(1);
	}

	@Test
	void testGenericBaseInterfaceMethodThroughInterface() {
		testBean.genericBaseInterfaceMethod("");
		assertThat(counterAspect.count).isEqualTo(1);
	}

}


interface BaseInterface<T> {

	void genericBaseInterfaceMethod(T t);
}


interface DerivedInterface<T> extends BaseInterface<T> {

	void genericDerivedInterfaceMethod(T t);
}


class DerivedStringParameterizedClass implements DerivedInterface<String> {

	@Override
	public void genericDerivedInterfaceMethod(String t) {
	}

	@Override
	public void genericBaseInterfaceMethod(String t) {
	}
}

@Aspect
class GenericCounterAspect {

	public int count;

	@Before("execution(* *..BaseInterface+.*(..))")
	public void increment() {
		count++;
	}

}

