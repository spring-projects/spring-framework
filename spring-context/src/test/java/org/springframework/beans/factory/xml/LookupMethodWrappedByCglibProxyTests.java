/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void autoProxiedLookup() {
		OverloadLookup olup = (OverloadLookup) applicationContext.getBean("autoProxiedOverload");
		ITestBean jenny = olup.newTestBean();
		assertThat(jenny.getName()).isEqualTo("Jenny");
		assertThat(olup.testMethod()).isEqualTo("foo");
		assertInterceptorCount(2);
	}

	@Test
	void regularlyProxiedLookup() {
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

