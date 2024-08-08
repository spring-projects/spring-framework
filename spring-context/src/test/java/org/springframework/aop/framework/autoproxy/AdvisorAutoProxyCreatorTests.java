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

package org.springframework.aop.framework.autoproxy;

import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.autoproxy.target.AbstractBeanFactoryBasedTargetSourceCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.CommonsPool2TargetSource;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.aop.target.PrototypeTargetSource;
import org.springframework.aop.target.ThreadLocalTargetSource;
import org.springframework.aop.testfixture.advice.CountingBeforeAdvice;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.aop.testfixture.mixin.Lockable;
import org.springframework.beans.testfixture.beans.CountingTestBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for auto proxy creation by advisor recognition.
 *
 * @author Rod Johnson
 * @author Dave Syer
 * @author Chris Beams
 * @see org.springframework.aop.framework.autoproxy.AdvisorAutoProxyCreatorIntegrationTests
 */
class AdvisorAutoProxyCreatorTests {

	private static final String CLASSNAME = AdvisorAutoProxyCreatorTests.class.getSimpleName();
	private static final String COMMON_INTERCEPTORS_CONTEXT = CLASSNAME + "-common-interceptors.xml";
	private static final String CUSTOM_TARGETSOURCE_CONTEXT = CLASSNAME + "-custom-targetsource.xml";
	private static final String QUICK_TARGETSOURCE_CONTEXT = CLASSNAME + "-quick-targetsource.xml";
	private static final String OPTIMIZED_CONTEXT = CLASSNAME + "-optimized.xml";


	/**
	 * Check that we can provide a common interceptor that will
	 * appear in the chain before "specific" interceptors,
	 * which are sourced from matching advisors
	 */
	@Test
	void commonInterceptorAndAdvisor() {
		ClassPathXmlApplicationContext ctx = context(COMMON_INTERCEPTORS_CONTEXT);
		ITestBean test1 = (ITestBean) ctx.getBean("test1");
		assertThat(AopUtils.isAopProxy(test1)).isTrue();

		Lockable lockable1 = (Lockable) test1;
		NopInterceptor nop1 = (NopInterceptor) ctx.getBean("nopInterceptor");
		NopInterceptor nop2 = (NopInterceptor) ctx.getBean("pointcutAdvisor", Advisor.class).getAdvice();

		ITestBean test2 = (ITestBean) ctx.getBean("test2");
		Lockable lockable2 = (Lockable) test2;

		// Locking should be independent; nop is shared
		assertThat(lockable1.locked()).isFalse();
		assertThat(lockable2.locked()).isFalse();
		// equals 2 calls on shared nop, because it's first and sees calls
		// against the Lockable interface introduced by the specific advisor
		assertThat(nop1.getCount()).isEqualTo(2);
		assertThat(nop2.getCount()).isEqualTo(0);
		lockable1.lock();
		assertThat(lockable1.locked()).isTrue();
		assertThat(lockable2.locked()).isFalse();
		assertThat(nop1.getCount()).isEqualTo(5);
		assertThat(nop2.getCount()).isEqualTo(0);

		PackageVisibleMethod packageVisibleMethod = (PackageVisibleMethod) ctx.getBean("packageVisibleMethod");
		assertThat(nop1.getCount()).isEqualTo(5);
		assertThat(nop2.getCount()).isEqualTo(0);
		packageVisibleMethod.doSomething();
		assertThat(nop1.getCount()).isEqualTo(6);
		assertThat(nop2.getCount()).isEqualTo(1);
		assertThat(packageVisibleMethod).isInstanceOf(Lockable.class);
		Lockable lockable3 = (Lockable) packageVisibleMethod;
		lockable3.lock();
		assertThat(lockable3.locked()).isTrue();
		lockable3.unlock();
		assertThat(lockable3.locked()).isFalse();
		ctx.close();
	}

	/**
	 * We have custom TargetSourceCreators but there's no match, and
	 * hence no proxying, for this bean
	 */
	@Test
	void customTargetSourceNoMatch() {
		ClassPathXmlApplicationContext ctx = context(CUSTOM_TARGETSOURCE_CONTEXT);
		ITestBean test = (ITestBean) ctx.getBean("test");
		assertThat(AopUtils.isAopProxy(test)).isFalse();
		assertThat(test.getName()).isEqualTo("Rod");
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
		ctx.close();
	}

	@Test
	void customPrototypeTargetSource() {
		CountingTestBean.count = 0;
		ClassPathXmlApplicationContext ctx = context(CUSTOM_TARGETSOURCE_CONTEXT);
		ITestBean test = (ITestBean) ctx.getBean("prototypeTest");
		assertThat(AopUtils.isAopProxy(test)).isTrue();
		Advised advised = (Advised) test;
		assertThat(advised.getTargetSource()).isInstanceOf(PrototypeTargetSource.class);
		assertThat(test.getName()).isEqualTo("Rod");
		// Check that references survived prototype creation
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
		assertThat(CountingTestBean.count).as("Only 2 CountingTestBeans instantiated").isEqualTo(2);
		CountingTestBean.count = 0;
		ctx.close();
	}

	@Test
	void lazyInitTargetSource() {
		CountingTestBean.count = 0;
		ClassPathXmlApplicationContext ctx = context(CUSTOM_TARGETSOURCE_CONTEXT);
		ITestBean test = (ITestBean) ctx.getBean("lazyInitTest");
		assertThat(AopUtils.isAopProxy(test)).isTrue();
		Advised advised = (Advised) test;
		assertThat(advised.getTargetSource()).isInstanceOf(LazyInitTargetSource.class);
		assertThat(CountingTestBean.count).as("No CountingTestBean instantiated yet").isEqualTo(0);
		assertThat(test.getName()).isEqualTo("Rod");
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
		assertThat(CountingTestBean.count).as("Only 1 CountingTestBean instantiated").isEqualTo(1);
		CountingTestBean.count = 0;
		ctx.close();
	}

	@Test
	void quickTargetSourceCreator() {
		ClassPathXmlApplicationContext ctx = context(QUICK_TARGETSOURCE_CONTEXT);
		ITestBean test = (ITestBean) ctx.getBean("test");
		assertThat(AopUtils.isAopProxy(test)).isFalse();
		assertThat(test.getName()).isEqualTo("Rod");
		// Check that references survived pooling
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

		// Now test the pooled one
		test = (ITestBean) ctx.getBean(":test");
		assertThat(AopUtils.isAopProxy(test)).isTrue();
		Advised advised = (Advised) test;
		assertThat(advised.getTargetSource()).isInstanceOf(CommonsPool2TargetSource.class);
		assertThat(test.getName()).isEqualTo("Rod");
		// Check that references survived pooling
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

		// Now test the ThreadLocal one
		test = (ITestBean) ctx.getBean("%test");
		assertThat(AopUtils.isAopProxy(test)).isTrue();
		advised = (Advised) test;
		assertThat(advised.getTargetSource()).isInstanceOf(ThreadLocalTargetSource.class);
		assertThat(test.getName()).isEqualTo("Rod");
		// Check that references survived pooling
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

		// Now test the Prototype TargetSource
		test = (ITestBean) ctx.getBean("!test");
		assertThat(AopUtils.isAopProxy(test)).isTrue();
		advised = (Advised) test;
		assertThat(advised.getTargetSource()).isInstanceOf(PrototypeTargetSource.class);
		assertThat(test.getName()).isEqualTo("Rod");
		// Check that references survived pooling
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

		ITestBean test2 = (ITestBean) ctx.getBean("!test");
		assertThat(test).as("Prototypes cannot be the same object").isNotSameAs(test2);
		assertThat(test2.getName()).isEqualTo("Rod");
		assertThat(test2.getSpouse().getName()).isEqualTo("Kerry");
		ctx.close();
	}

	@Test
	void withOptimizedProxy() {
		ClassPathXmlApplicationContext ctx = context(OPTIMIZED_CONTEXT);

		ITestBean testBean = (ITestBean) ctx.getBean("optimizedTestBean");
		assertThat(AopUtils.isAopProxy(testBean)).isTrue();

		testBean.setAge(23);
		testBean.getAge();

		CountingBeforeAdvice beforeAdvice = (CountingBeforeAdvice) ctx.getBean("countingAdvice");
		assertThat(beforeAdvice.getCalls()).as("Incorrect number of calls to proxy").isEqualTo(2);
		ctx.close();
	}


	private ClassPathXmlApplicationContext context(String filename) {
		return new ClassPathXmlApplicationContext(filename, getClass());
	}

}


class SelectivePrototypeTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	@Override
	protected AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {
		if (!beanName.startsWith("prototype")) {
			return null;
		}
		return new PrototypeTargetSource();
	}

}

