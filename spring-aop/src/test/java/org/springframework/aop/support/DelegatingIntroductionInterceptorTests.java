/*
 * Copyright 2002-2018 the original author or authors.
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
import org.junit.Test;

import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.tests.TimeStamped;
import org.springframework.tests.aop.interceptor.SerializableNopInterceptor;
import org.springframework.tests.sample.beans.INestedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.NestedTestBean;
import org.springframework.tests.sample.beans.Person;
import org.springframework.tests.sample.beans.SerializablePerson;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SerializationTestUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 13.05.2003
 */
public class DelegatingIntroductionInterceptorTests {

	@Test(expected = IllegalArgumentException.class)
	public void testNullTarget() throws Exception {
		// Shouldn't accept null target
		new DelegatingIntroductionInterceptor(null);
	}

	@Test
	public void testIntroductionInterceptorWithDelegation() throws Exception {
		TestBean raw = new TestBean();
		assertTrue(! (raw instanceof TimeStamped));
		ProxyFactory factory = new ProxyFactory(raw);

		TimeStamped ts = mock(TimeStamped.class);
		long timestamp = 111L;
		given(ts.getTimeStamp()).willReturn(timestamp);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts)));

		TimeStamped tsp = (TimeStamped) factory.getProxy();
		assertTrue(tsp.getTimeStamp() == timestamp);
	}

	@Test
	public void testIntroductionInterceptorWithInterfaceHierarchy() throws Exception {
		TestBean raw = new TestBean();
		assertTrue(! (raw instanceof SubTimeStamped));
		ProxyFactory factory = new ProxyFactory(raw);

		TimeStamped ts = mock(SubTimeStamped.class);
		long timestamp = 111L;
		given(ts.getTimeStamp()).willReturn(timestamp);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts), SubTimeStamped.class));

		SubTimeStamped tsp = (SubTimeStamped) factory.getProxy();
		assertTrue(tsp.getTimeStamp() == timestamp);
	}

	@Test
	public void testIntroductionInterceptorWithSuperInterface() throws Exception {
		TestBean raw = new TestBean();
		assertTrue(! (raw instanceof TimeStamped));
		ProxyFactory factory = new ProxyFactory(raw);

		TimeStamped ts = mock(SubTimeStamped.class);
		long timestamp = 111L;
		given(ts.getTimeStamp()).willReturn(timestamp);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts), TimeStamped.class));

		TimeStamped tsp = (TimeStamped) factory.getProxy();
		assertTrue(!(tsp instanceof SubTimeStamped));
		assertTrue(tsp.getTimeStamp() == timestamp);
	}

	@Test
	public void testAutomaticInterfaceRecognitionInDelegate() throws Exception {
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

		assertTrue(ts.getTimeStamp() == t);
		((ITester) ts).foo();

		((ITestBean) ts).getAge();
	}


	@Test
	public void testAutomaticInterfaceRecognitionInSubclass() throws Exception {
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
		assertTrue(ia.isPerInstance());
		pf.addAdvisor(0, ia);

		//assertTrue(Arrays.binarySearch(pf.getProxiedInterfaces(), TimeStamped.class) != -1);
		TimeStamped ts = (TimeStamped) pf.getProxy();

		assertThat(ts, instanceOf(TimeStamped.class));
		// Shouldn't proxy framework interfaces
		assertTrue(!(ts instanceof MethodInterceptor));
		assertTrue(!(ts instanceof IntroductionInterceptor));

		assertTrue(ts.getTimeStamp() == t);
		((ITester) ts).foo();
		((ITestBean) ts).getAge();

		// Test removal
		ii.suppressInterface(TimeStamped.class);
		// Note that we need to construct a new proxy factory,
		// or suppress the interface on the proxy factory
		pf = new ProxyFactory(target);
		pf.addAdvisor(0, new DefaultIntroductionAdvisor(ii));
		Object o = pf.getProxy();
		assertTrue(!(o instanceof TimeStamped));
	}

	@SuppressWarnings("serial")
	@Test
	public void testIntroductionInterceptorDoesntReplaceToString() throws Exception {
		TestBean raw = new TestBean();
		assertTrue(! (raw instanceof TimeStamped));
		ProxyFactory factory = new ProxyFactory(raw);

		TimeStamped ts = new SerializableTimeStamped(0);

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts) {
			@Override
			public String toString() {
				throw new UnsupportedOperationException("Shouldn't be invoked");
			}
		}));

		TimeStamped tsp = (TimeStamped) factory.getProxy();
		assertEquals(0, tsp.getTimeStamp());

		assertEquals(raw.toString(), tsp.toString());
	}

	@Test
	public void testDelegateReturnsThisIsMassagedToReturnProxy() {
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

		assertEquals(company, proxy.getCompany());
		ITestBean introduction = (ITestBean) proxy;
		assertSame("Introduced method returning delegate returns proxy", introduction, introduction.getSpouse());
		assertTrue("Introduced method returning delegate returns proxy", AopUtils.isAopProxy(introduction.getSpouse()));
	}

	@Test
	public void testSerializableDelegatingIntroductionInterceptorSerializable() throws Exception {
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

		assertEquals(name, p.getName());
		assertEquals(time, ((TimeStamped) p).getTimeStamp());

		Person p1 = (Person) SerializationTestUtils.serializeAndDeserialize(p);
		assertEquals(name, p1.getName());
		assertEquals(time, ((TimeStamped) p1).getTimeStamp());
	}

	// Test when target implements the interface: should get interceptor by preference.
	@Test
	public void testIntroductionMasksTargetImplementation() throws Exception {
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
		assertTrue(ts.getTimeStamp() == t);
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


	private static interface SubTimeStamped extends TimeStamped {
	}

}
