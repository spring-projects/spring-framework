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

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests lookup methods wrapped by a CGLIB proxy (see SPR-391).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class LookupMethodWrappedByCglibProxyTests {

	private static final Class<?> CLASS = LookupMethodWrappedByCglibProxyTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final String CONTEXT = CLASSNAME + "-context.xml";

	private ApplicationContext applicationContext;

	@BeforeEach
	void setUp() {
		this.applicationContext = new ClassPathXmlApplicationContext(CONTEXT, CLASS);
		resetInterceptor();
	}

	@Test
	void testAutoProxiedLookup() {
		OverloadLookup olup = (OverloadLookup) applicationContext.getBean("autoProxiedOverload");
		ITestBean jenny = olup.newTestBean();
		assertThat(jenny.getName()).isEqualTo("Jenny");
		assertThat(olup.testMethod()).isEqualTo("foo");
		assertInterceptorCount(2);
	}

	@Test
	void testRegularlyProxiedLookup() {
		OverloadLookup olup = (OverloadLookup) applicationContext.getBean("regularlyProxiedOverload");
		ITestBean jenny = olup.newTestBean();
		assertThat(jenny.getName()).isEqualTo("Jenny");
		assertThat(olup.testMethod()).isEqualTo("foo");
		assertInterceptorCount(2);
	}

	private void assertInterceptorCount(int count) {
		DebugInterceptor interceptor = getInterceptor();
		assertThat(interceptor.getCount()).as("Interceptor count is incorrect").isEqualTo(count);
	}

	private void resetInterceptor() {
		DebugInterceptor interceptor = getInterceptor();
		interceptor.resetCount();
	}

	private DebugInterceptor getInterceptor() {
		return (DebugInterceptor) applicationContext.getBean("interceptor");
	}

}


abstract class OverloadLookup {

	public abstract ITestBean newTestBean();

	public String testMethod() {
		return "foo";
	}
}

