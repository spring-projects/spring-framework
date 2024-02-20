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

package org.springframework.aop.framework;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.testfixture.advice.CountingBeforeAdvice;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.aop.testfixture.interceptor.TimestampIntroductionInterceptor;
import org.springframework.beans.testfixture.beans.IOther;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.testfixture.TimeStamped;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Also tests AdvisedSupport and ProxyCreatorSupport superclasses.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 14.05.2003
 */
class ProxyFactoryTests {

	@Test
	void indexOfMethods() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		Advisor advisor = new DefaultPointcutAdvisor(new CountingBeforeAdvice());
		Advised advised = (Advised) pf.getProxy();
		// Can use advised and ProxyFactory interchangeably
		advised.addAdvice(nop);
		pf.addAdvisor(advisor);
		assertThat(pf.indexOf(new NopInterceptor())).isEqualTo(-1);
		assertThat(pf.indexOf(nop)).isEqualTo(0);
		assertThat(pf.indexOf(advisor)).isEqualTo(1);
		assertThat(advised.indexOf(new DefaultPointcutAdvisor(null))).isEqualTo(-1);
	}

	@Test
	void removeAdvisorByReference() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		CountingBeforeAdvice cba = new CountingBeforeAdvice();
		Advisor advisor = new DefaultPointcutAdvisor(cba);
		pf.addAdvice(nop);
		pf.addAdvisor(advisor);
		ITestBean proxied = (ITestBean) pf.getProxy();
		proxied.setAge(5);
		assertThat(cba.getCalls()).isEqualTo(1);
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(pf.removeAdvisor(advisor)).isTrue();
		assertThat(proxied.getAge()).isEqualTo(5);
		assertThat(cba.getCalls()).isEqualTo(1);
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(pf.removeAdvisor(new DefaultPointcutAdvisor(null))).isFalse();
	}

	@Test
	void removeAdvisorByIndex() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		CountingBeforeAdvice cba = new CountingBeforeAdvice();
		Advisor advisor = new DefaultPointcutAdvisor(cba);
		pf.addAdvice(nop);
		pf.addAdvisor(advisor);
		NopInterceptor nop2 = new NopInterceptor();
		pf.addAdvice(nop2);
		ITestBean proxied = (ITestBean) pf.getProxy();
		proxied.setAge(5);
		assertThat(cba.getCalls()).isEqualTo(1);
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(nop2.getCount()).isEqualTo(1);
		// Removes counting before advisor
		pf.removeAdvisor(1);
		assertThat(proxied.getAge()).isEqualTo(5);
		assertThat(cba.getCalls()).isEqualTo(1);
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(nop2.getCount()).isEqualTo(2);
		// Removes Nop1
		pf.removeAdvisor(0);
		assertThat(proxied.getAge()).isEqualTo(5);
		assertThat(cba.getCalls()).isEqualTo(1);
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(nop2.getCount()).isEqualTo(3);

		// Check out of bounds
		try {
			pf.removeAdvisor(-1);
		}
		catch (AopConfigException ex) {
			// Ok
		}

		try {
			pf.removeAdvisor(2);
		}
		catch (AopConfigException ex) {
			// Ok
		}

		assertThat(proxied.getAge()).isEqualTo(5);
		assertThat(nop2.getCount()).isEqualTo(4);
	}

	@Test
	void replaceAdvisor() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		CountingBeforeAdvice cba1 = new CountingBeforeAdvice();
		CountingBeforeAdvice cba2 = new CountingBeforeAdvice();
		Advisor advisor1 = new DefaultPointcutAdvisor(cba1);
		Advisor advisor2 = new DefaultPointcutAdvisor(cba2);
		pf.addAdvisor(advisor1);
		pf.addAdvice(nop);
		ITestBean proxied = (ITestBean) pf.getProxy();
		// Use the type cast feature
		// Replace etc methods on advised should be same as on ProxyFactory
		Advised advised = (Advised) proxied;
		proxied.setAge(5);
		assertThat(cba1.getCalls()).isEqualTo(1);
		assertThat(cba2.getCalls()).isEqualTo(0);
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(advised.replaceAdvisor(new DefaultPointcutAdvisor(new NopInterceptor()), advisor2)).isFalse();
		assertThat(advised.replaceAdvisor(advisor1, advisor2)).isTrue();
		assertThat(pf.getAdvisors()[0]).isEqualTo(advisor2);
		assertThat(proxied.getAge()).isEqualTo(5);
		assertThat(cba1.getCalls()).isEqualTo(1);
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(cba2.getCalls()).isEqualTo(1);
		assertThat(pf.replaceAdvisor(new DefaultPointcutAdvisor(null), advisor1)).isFalse();
	}

	@Test
	void addRepeatedInterface() {
		TimeStamped tst = () -> {
			throw new UnsupportedOperationException("getTimeStamp");
		};
		ProxyFactory pf = new ProxyFactory(tst);
		// We've already implicitly added this interface.
		// This call should be ignored without error
		pf.addInterface(TimeStamped.class);
		// All cool
		assertThat(pf.getProxy()).isInstanceOf(TimeStamped.class);
	}

	@Test
	void getsAllInterfaces() {
		// Extend to get new interface
		class TestBeanSubclass extends TestBean implements Comparable<Object> {
			@Override
			public int compareTo(Object arg0) {
				throw new UnsupportedOperationException("compareTo");
			}
		}
		TestBeanSubclass raw = new TestBeanSubclass();
		ProxyFactory factory = new ProxyFactory(raw);
		//System.out.println("Proxied interfaces are " + StringUtils.arrayToDelimitedString(factory.getProxiedInterfaces(), ","));
		assertThat(factory.getProxiedInterfaces()).as("Found correct number of interfaces").hasSize(5);
		ITestBean tb = (ITestBean) factory.getProxy();
		assertThat(tb).as("Picked up secondary interface").isInstanceOf(IOther.class);
		raw.setAge(25);
		assertThat(tb.getAge()).isEqualTo(raw.getAge());

		long t = 555555L;
		TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor(t);

		Class<?>[] oldProxiedInterfaces = factory.getProxiedInterfaces();

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));

		Class<?>[] newProxiedInterfaces = factory.getProxiedInterfaces();
		assertThat(newProxiedInterfaces).as("Advisor proxies one more interface after introduction").hasSize(oldProxiedInterfaces.length + 1);

		TimeStamped ts = (TimeStamped) factory.getProxy();
		assertThat(ts.getTimeStamp()).isEqualTo(t);
		// Shouldn't fail;
		((IOther) ts).absquatulate();
	}

	@Test
	void interceptorInclusionMethods() {
		class MyInterceptor implements MethodInterceptor {
			@Override
			public Object invoke(MethodInvocation invocation) {
				throw new UnsupportedOperationException();
			}
		}

		NopInterceptor di = new NopInterceptor();
		NopInterceptor diUnused = new NopInterceptor();
		ProxyFactory factory = new ProxyFactory(new TestBean());
		factory.addAdvice(0, di);
		assertThat(factory.getProxy()).isInstanceOf(ITestBean.class);
		assertThat(factory.adviceIncluded(di)).isTrue();
		assertThat(factory.adviceIncluded(diUnused)).isFalse();
		assertThat(factory.countAdvicesOfType(NopInterceptor.class)).isEqualTo(1);
		assertThat(factory.countAdvicesOfType(MyInterceptor.class)).isEqualTo(0);

		factory.addAdvice(0, diUnused);
		assertThat(factory.adviceIncluded(diUnused)).isTrue();
		assertThat(factory.countAdvicesOfType(NopInterceptor.class)).isEqualTo(2);
	}

	@Test
	void sealedInterfaceExclusion() {
		// String implements ConstantDesc on JDK 12+, sealed as of JDK 17
		ProxyFactory factory = new ProxyFactory("");
		NopInterceptor di = new NopInterceptor();
		factory.addAdvice(0, di);
		Object proxy = factory.getProxy();
		assertThat(proxy).isInstanceOf(CharSequence.class);
	}

	/**
	 * Should see effect immediately on behavior.
	 */
	@Test
	void canAddAndRemoveAspectInterfacesOnSingleton() {
		ProxyFactory config = new ProxyFactory(new TestBean());

		assertThat(config.getProxy()).as("Shouldn't implement TimeStamped before manipulation")
				.isNotInstanceOf(TimeStamped.class);

		long time = 666L;
		TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor();
		ti.setTime(time);

		// Add to front of interceptor chain
		int oldCount = config.getAdvisors().length;
		config.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));

		assertThat(config.getAdvisors()).hasSize(oldCount + 1);

		TimeStamped ts = (TimeStamped) config.getProxy();
		assertThat(ts.getTimeStamp()).isEqualTo(time);

		// Can remove
		config.removeAdvice(ti);

		assertThat(config.getAdvisors()).hasSize(oldCount);

		assertThatRuntimeException()
				.as("Existing object won't implement this interface any more")
				.isThrownBy(ts::getTimeStamp); // Existing reference will fail

		assertThat(config.getProxy()).as("Should no longer implement TimeStamped").isNotInstanceOf(TimeStamped.class);

		// Now check non-effect of removing interceptor that isn't there
		config.removeAdvice(new DebugInterceptor());

		assertThat(config.getAdvisors()).hasSize(oldCount);

		ITestBean it = (ITestBean) ts;
		DebugInterceptor debugInterceptor = new DebugInterceptor();
		config.addAdvice(0, debugInterceptor);
		it.getSpouse();
		assertThat(debugInterceptor.getCount()).isEqualTo(1);
		config.removeAdvice(debugInterceptor);
		it.getSpouse();
		// not invoked again
		assertThat(debugInterceptor.getCount()).isEqualTo(1);
	}

	@Test
	void proxyTargetClassWithInterfaceAsTarget() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTargetClass(ITestBean.class);
		Object proxy = pf.getProxy();
		assertThat(AopUtils.isJdkDynamicProxy(proxy)).as("Proxy is a JDK proxy").isTrue();
		assertThat(proxy).isInstanceOf(ITestBean.class);
		assertThat(AopProxyUtils.ultimateTargetClass(proxy)).isEqualTo(ITestBean.class);

		ProxyFactory pf2 = new ProxyFactory(proxy);
		Object proxy2 = pf2.getProxy();
		assertThat(AopUtils.isJdkDynamicProxy(proxy2)).as("Proxy is a JDK proxy").isTrue();
		assertThat(proxy2).isInstanceOf(ITestBean.class);
		assertThat(AopProxyUtils.ultimateTargetClass(proxy2)).isEqualTo(ITestBean.class);
	}

	@Test
	void proxyTargetClassWithConcreteClassAsTarget() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTargetClass(TestBean.class);
		Object proxy = pf.getProxy();
		assertThat(AopUtils.isCglibProxy(proxy)).as("Proxy is a CGLIB proxy").isTrue();
		assertThat(proxy).isInstanceOf(TestBean.class);
		assertThat(AopProxyUtils.ultimateTargetClass(proxy)).isEqualTo(TestBean.class);

		ProxyFactory pf2 = new ProxyFactory(proxy);
		pf2.setProxyTargetClass(true);
		Object proxy2 = pf2.getProxy();
		assertThat(AopUtils.isCglibProxy(proxy2)).as("Proxy is a CGLIB proxy").isTrue();
		assertThat(proxy2).isInstanceOf(TestBean.class);
		assertThat(AopProxyUtils.ultimateTargetClass(proxy2)).isEqualTo(TestBean.class);
	}

	@Test
	void interfaceProxiesCanBeOrderedThroughAnnotations() {
		Object proxy1 = new ProxyFactory(new A()).getProxy();
		Object proxy2 = new ProxyFactory(new B()).getProxy();
		List<Object> list = new ArrayList<>(2);
		list.add(proxy1);
		list.add(proxy2);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(proxy2, proxy1);
	}

	@Test
	void targetClassProxiesCanBeOrderedThroughAnnotations() {
		ProxyFactory pf1 = new ProxyFactory(new A());
		pf1.setProxyTargetClass(true);
		ProxyFactory pf2 = new ProxyFactory(new B());
		pf2.setProxyTargetClass(true);
		Object proxy1 = pf1.getProxy();
		Object proxy2 = pf2.getProxy();
		List<Object> list = new ArrayList<>(2);
		list.add(proxy1);
		list.add(proxy2);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(proxy2, proxy1);
	}

	@Test
	void interceptorWithoutJoinpoint() {
		final TestBean target = new TestBean("tb");
		ITestBean proxy = ProxyFactory.getProxy(ITestBean.class, (MethodInterceptor) invocation -> {
			assertThat(invocation.getThis()).isNull();
			return invocation.getMethod().invoke(target, invocation.getArguments());
		});
		assertThat(proxy.getName()).isEqualTo("tb");
	}

	@Test
	void interfaceProxy() {
		CharSequence target = "test";
		ProxyFactory pf = new ProxyFactory(target);
		ClassLoader cl = target.getClass().getClassLoader();
		CharSequence proxy = (CharSequence) pf.getProxy(cl);
		assertThat(proxy).asString().isEqualTo(target);
		assertThat(pf.getProxyClass(cl)).isSameAs(proxy.getClass());
	}

	@Test
	void dateProxy() {
		MyDate target = new MyDate();
		ProxyFactory pf = new ProxyFactory(target);
		pf.setProxyTargetClass(true);
		ClassLoader cl = target.getClass().getClassLoader();
		MyDate proxy = (MyDate) pf.getProxy(cl);
		assertThat(proxy.getTime()).isEqualTo(target.getTime());
		assertThat(pf.getProxyClass(cl)).isSameAs(proxy.getClass());
	}

	@Test
	void jdbcSavepointProxy() throws SQLException {
		Savepoint target = new Savepoint() {
			@Override
			public int getSavepointId() {
				return 1;
			}
			@Override
			public String getSavepointName() {
				return "sp";
			}
		};
		ProxyFactory pf = new ProxyFactory(target);
		ClassLoader cl = Savepoint.class.getClassLoader();
		Savepoint proxy = (Savepoint) pf.getProxy(cl);
		assertThat(proxy.getSavepointName()).isEqualTo("sp");
		assertThat(pf.getProxyClass(cl)).isSameAs(proxy.getClass());
	}


	// Emulates java.util.Date locally, since we cannot automatically proxy the
	// java.util.Date class.
	static class MyDate {

		private final long time = System.currentTimeMillis();

		public long getTime() {
			return time;
		}
	}


	@Order(2)
	static class A implements Runnable {

		@Override
		public void run() {
		}
	}


	@Order(1)
	static class B implements Runnable {

		@Override
		public void run() {
		}
	}

}
