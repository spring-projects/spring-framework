/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.target;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.tests.aop.interceptor.SerializableNopInterceptor;
import org.springframework.tests.sample.beans.Person;
import org.springframework.tests.sample.beans.SerializablePerson;
import org.springframework.tests.sample.beans.SideEffectBean;
import org.springframework.util.SerializationTestUtils;

import static org.junit.Assert.*;
import static org.springframework.tests.TestResourceUtils.*;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class HotSwappableTargetSourceTests {

	/** Initial count value set in bean factory XML */
	private static final int INITIAL_COUNT = 10;

	private DefaultListableBeanFactory beanFactory;


	@Before
	public void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				qualifiedResource(HotSwappableTargetSourceTests.class, "context.xml"));
	}

	/**
	 * We must simulate container shutdown, which should clear threads.
	 */
	@After
	public void close() {
		// Will call pool.close()
		this.beanFactory.destroySingletons();
	}


	/**
	 * Check it works like a normal invoker
	 */
	@Test
	public void testBasicFunctionality() {
		SideEffectBean proxied = (SideEffectBean) beanFactory.getBean("swappable");
		assertEquals(INITIAL_COUNT, proxied.getCount());
		proxied.doWork();
		assertEquals(INITIAL_COUNT + 1, proxied.getCount());

		proxied = (SideEffectBean) beanFactory.getBean("swappable");
		proxied.doWork();
		assertEquals(INITIAL_COUNT + 2, proxied.getCount());
	}

	@Test
	public void testValidSwaps() {
		SideEffectBean target1 = (SideEffectBean) beanFactory.getBean("target1");
		SideEffectBean target2 = (SideEffectBean) beanFactory.getBean("target2");

		SideEffectBean proxied = (SideEffectBean) beanFactory.getBean("swappable");
		assertEquals(target1.getCount(), proxied.getCount());
		proxied.doWork();
		assertEquals(INITIAL_COUNT + 1, proxied.getCount());

		HotSwappableTargetSource swapper = (HotSwappableTargetSource) beanFactory.getBean("swapper");
		Object old = swapper.swap(target2);
		assertEquals("Correct old target was returned", target1, old);

		// TODO should be able to make this assertion: need to fix target handling
		// in AdvisedSupport
		//assertEquals(target2, ((Advised) proxied).getTarget());

		assertEquals(20, proxied.getCount());
		proxied.doWork();
		assertEquals(21, target2.getCount());

		// Swap it back
		swapper.swap(target1);
		assertEquals(target1.getCount(), proxied.getCount());
	}

	@Test
	public void testRejectsSwapToNull() {
		HotSwappableTargetSource swapper = (HotSwappableTargetSource) beanFactory.getBean("swapper");
		IllegalArgumentException aopex = null;
		try {
			swapper.swap(null);
			fail("Shouldn't be able to swap to invalid value");
		}
		catch (IllegalArgumentException ex) {
			// Ok
			aopex = ex;
		}

		// It shouldn't be corrupted, it should still work
		testBasicFunctionality();
		assertTrue(aopex.getMessage().contains("null"));
	}

	@Test
	public void testSerialization() throws Exception {
		SerializablePerson sp1 = new SerializablePerson();
		sp1.setName("Tony");
		SerializablePerson sp2 = new SerializablePerson();
		sp1.setName("Gordon");

		HotSwappableTargetSource hts = new HotSwappableTargetSource(sp1);
		ProxyFactory pf = new ProxyFactory();
		pf.addInterface(Person.class);
		pf.setTargetSource(hts);
		pf.addAdvisor(new DefaultPointcutAdvisor(new SerializableNopInterceptor()));
		Person p = (Person) pf.getProxy();

		assertEquals(sp1.getName(), p.getName());
		hts.swap(sp2);
		assertEquals(sp2.getName(), p.getName());

		p = (Person) SerializationTestUtils.serializeAndDeserialize(p);
		// We need to get a reference to the client-side targetsource
		hts = (HotSwappableTargetSource) ((Advised) p).getTargetSource();
		assertEquals(sp2.getName(), p.getName());
		hts.swap(sp1);
		assertEquals(sp1.getName(), p.getName());

	}

}
