/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.testfixture.interceptor.SerializableNopInterceptor;
import org.springframework.beans.testfixture.beans.INestedTestBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.NestedTestBean;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.SerializablePerson;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.testfixture.TimeStamped;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 13.05.2003
 */
class DelegatingIntroductionInterceptorTests {

	@Test
	void testNullTarget() throws Exception {
		// Shouldn't accept null target
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DelegatingIntroductionInterceptor(null));
	}

	@Test
	void testIntroductionInterceptorWithDelegation() throws Exception {
		TestBean raw = new TestBean();
		assertThat(raw).isNotInstanceOf(TimeStamped.class);
		ProxyFactory factory = new ProxyFactory(raw);

		TimeStamped ts = mock();
		long timestamp = 111L;
		given(ts.getTimeStamp()).willReturn(timestamp);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts)));

		TimeStamped tsp = (TimeStamped) factory.getProxy();
		assertThat(tsp.getTimeStamp()).isEqualTo(timestamp);
	}

	@Test
	void testIntroductionInterceptorWithInterfaceHierarchy() throws Exception {
		TestBean raw = new TestBean();
		assertThat(raw).isNotInstanceOf(SubTimeStamped.class);
		ProxyFactory factory = new ProxyFactory(raw);

		SubTimeStamped ts = mock();
		long timestamp = 111L;
		given(ts.getTimeStamp()).willReturn(timestamp);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts), SubTimeStamped.class));

		SubTimeStamped tsp = (SubTimeStamped) factory.getProxy();
		assertThat(tsp.getTimeStamp()).isEqualTo(timestamp);
	}

	@Test
	void testIntroductionInterceptorWithSuperInterface() throws Exception {
		TestBean raw = new TestBean();
		assertThat(raw).isNotInstanceOf(TimeStamped.class);
		ProxyFactory factory = new ProxyFactory(raw);

		SubTimeStamped ts = mock();
		long timestamp = 111L;
		given(ts.getTimeStamp()).willReturn(timestamp);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts), TimeStamped.class));

		TimeStamped tsp = (TimeStamped) factory.getProxy();
		assertThat(tsp).isNotInstanceOf(SubTimeStamped.class);
		assertThat(tsp.getTimeStamp()).isEqualTo(timestamp);
	}

	@Test
	void testAutomaticInterfaceRecognitionInDelegate() throws Exception {
		final long t = 1001L;
		class Tester implements TimeStamped, ITester {
			@Override
			public void foo() throws Exception {
			}
			@Override
			public long getTimeStamp() {
				return t;
			}
		}

		DelegatingIntroductionInterceptor ii = new DelegatingIntroductionInterceptor(new Tester());

		TestBean target = new TestBean();

		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(0, new DefaultIntroductionAdvisor(ii));

		//assertTrue(Arrays.binarySearch(pf.getProxiedInterfaces(), TimeStamped.class) != -1);
		TimeStamped ts = (TimeStamped) pf.getProxy();

		assertThat(ts.getTimeStamp()).isEqualTo(t);
		((ITester) ts).foo();

		((ITestBean) ts).getAge();
	}


	@Test
	void testAutomaticInterfaceRecognitionInSubclass() throws Exception {
		final long t = 1001L;
		@SuppressWarnings("serial")
		class TestII extends DelegatingIntroductionInterceptor implements TimeStamped, ITester {
			@Override
			public void foo() throws Exception {
			}
			@Override
			public long getTimeStamp() {
				return t;
			}
		}

		DelegatingIntroductionInterceptor ii = new TestII();

		TestBean target = new TestBean();

		ProxyFactory pf = new ProxyFactory(target);
		IntroductionAdvisor ia = new DefaultIntroductionAdvisor(ii);
		assertThat(ia.isPerInstance()).isTrue();
		pf.addAdvisor(0, ia);

		//assertTrue(Arrays.binarySearch(pf.getProxiedInterfaces(), TimeStamped.class) != -1);
		TimeStamped ts = (TimeStamped) pf.getProxy();

		assertThat(ts).isInstanceOf(TimeStamped.class);
		// Shouldn't proxy framework interfaces
		assertThat(ts).isNotInstanceOf(MethodInterceptor.class);
		assertThat(ts).isNotInstanceOf(IntroductionInterceptor.class);

		assertThat(ts.getTimeStamp()).isEqualTo(t);
		((ITester) ts).foo();
		((ITestBean) ts).getAge();

		// Test removal
		ii.suppressInterface(TimeStamped.class);
		// Note that we need to construct a new proxy factory,
		// or suppress the interface on the proxy factory
		pf = new ProxyFactory(target);
		pf.addAdvisor(0, new DefaultIntroductionAdvisor(ii));
		Object o = pf.getProxy();
		assertThat(o).isNotInstanceOf(TimeStamped.class);
	}

	@SuppressWarnings("serial")
	@Test
	void testIntroductionInterceptorDoesntReplaceToString() throws Exception {
		TestBean raw = new TestBean();
		assertThat(raw).isNotInstanceOf(TimeStamped.class);
		ProxyFactory factory = new ProxyFactory(raw);

		TimeStamped ts = new SerializableTimeStamped(0);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts) {
			@Override
			public String toString() {
				throw new UnsupportedOperationException("Shouldn't be invoked");
			}
		}));

		TimeStamped tsp = (TimeStamped) factory.getProxy();
		assertThat(tsp.getTimeStamp()).isEqualTo(0);

		assertThat(tsp.toString()).isEqualTo(raw.toString());
	}

	@Test
	void testDelegateReturnsThisIsMassagedToReturnProxy() {
		NestedTestBean target = new NestedTestBean();
		String company = "Interface21";
		target.setCompany(company);
		TestBean delegate = new TestBean() {
			@Override
			public ITestBean getSpouse() {
				return this;
			}
		};
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new DelegatingIntroductionInterceptor(delegate));
		INestedTestBean proxy = (INestedTestBean) pf.getProxy();

		assertThat(proxy.getCompany()).isEqualTo(company);
		ITestBean introduction = (ITestBean) proxy;
		assertThat(introduction.getSpouse()).as("Introduced method returning delegate returns proxy").isSameAs(introduction);
		assertThat(AopUtils.isAopProxy(introduction.getSpouse())).as("Introduced method returning delegate returns proxy").isTrue();
	}

	@Test
	void testSerializableDelegatingIntroductionInterceptorSerializable() throws Exception {
		SerializablePerson serializableTarget = new SerializablePerson();
		String name = "Tony";
		serializableTarget.setName("Tony");

		ProxyFactory factory = new ProxyFactory(serializableTarget);
		factory.addInterface(Person.class);
		long time = 1000;
		TimeStamped ts = new SerializableTimeStamped(time);

		factory.addAdvisor(new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts)));
		factory.addAdvice(new SerializableNopInterceptor());

		Person p = (Person) factory.getProxy();

		assertThat(p.getName()).isEqualTo(name);
		assertThat(((TimeStamped) p).getTimeStamp()).isEqualTo(time);

		Person p1 = SerializationTestUtils.serializeAndDeserialize(p);
		assertThat(p1.getName()).isEqualTo(name);
		assertThat(((TimeStamped) p1).getTimeStamp()).isEqualTo(time);
	}

	// Test when target implements the interface: should get interceptor by preference.
	@Test
	void testIntroductionMasksTargetImplementation() throws Exception {
		final long t = 1001L;
		@SuppressWarnings("serial")
		class TestII extends DelegatingIntroductionInterceptor implements TimeStamped {
			@Override
			public long getTimeStamp() {
				return t;
			}
		}

		DelegatingIntroductionInterceptor ii = new TestII();

		// != t
		TestBean target = new TargetClass(t + 1);

		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(0, new DefaultIntroductionAdvisor(ii));

		TimeStamped ts = (TimeStamped) pf.getProxy();
		// From introduction interceptor, not target
		assertThat(ts.getTimeStamp()).isEqualTo(t);
	}


	@SuppressWarnings("serial")
	private static class SerializableTimeStamped implements TimeStamped, Serializable {

		private final long ts;

		public SerializableTimeStamped(long ts) {
			this.ts = ts;
		}

		@Override
		public long getTimeStamp() {
			return ts;
		}
	}


	public static class TargetClass extends TestBean implements TimeStamped {

		long t;

		public TargetClass(long t) {
			this.t = t;
		}

		@Override
		public long getTimeStamp() {
			return t;
		}
	}


	public interface ITester {

		void foo() throws Exception;
	}


	private interface SubTimeStamped extends TimeStamped {
	}

}
