/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.*;
import static test.util.TestResourceUtils.qualifiedResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.Resource;

import test.aop.SerializableNopInterceptor;
import test.beans.Person;
import test.beans.SerializablePerson;
import test.beans.SideEffectBean;
import test.util.SerializationTestUtils;


/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class HotSwappableTargetSourceTests {

	private static final Resource CONTEXT = qualifiedResource(HotSwappableTargetSourceTests.class, "context.xml");

	/** Initial count value set in bean factory XML */
	private static final int INITIAL_COUNT = 10;

	private XmlBeanFactory beanFactory;

	@Before
	public void setUp() throws Exception {
		this.beanFactory = new XmlBeanFactory(CONTEXT);
	}

	/**
	 * We must simulate container shutdown, which should clear threads.
	 */
	@After
	public void tearDown() {
		// Will call pool.close()
		this.beanFactory.destroySingletons();
	}

	/**
	 * Check it works like a normal invoker
	 *
	 */
	@Test
	public void testBasicFunctionality() {
		SideEffectBean proxied = (SideEffectBean) beanFactory.getBean("swappable");
		assertEquals(INITIAL_COUNT, proxied.getCount() );
		proxied.doWork();
		assertEquals(INITIAL_COUNT + 1, proxied.getCount() );

		proxied = (SideEffectBean) beanFactory.getBean("swappable");
		proxied.doWork();
		assertEquals(INITIAL_COUNT + 2, proxied.getCount() );
	}

	@Test
	public void testValidSwaps() {
		SideEffectBean target1 = (SideEffectBean) beanFactory.getBean("target1");
		SideEffectBean target2 = (SideEffectBean) beanFactory.getBean("target2");

		SideEffectBean proxied = (SideEffectBean) beanFactory.getBean("swappable");
		assertEquals(target1.getCount(), proxied.getCount() );
		proxied.doWork();
		assertEquals(INITIAL_COUNT + 1, proxied.getCount() );

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


	/**
	 *
	 * @param invalid
	 * @return the message
	 */
	private IllegalArgumentException testRejectsSwapToInvalidValue(Object invalid) {
		HotSwappableTargetSource swapper = (HotSwappableTargetSource) beanFactory.getBean("swapper");
		IllegalArgumentException aopex = null;
		try {
			swapper.swap(invalid);
			fail("Shouldn't be able to swap to invalid value [" + invalid + "]");
		}
		catch (IllegalArgumentException ex) {
			// Ok
			aopex = ex;
		}

		// It shouldn't be corrupted, it should still work
		testBasicFunctionality();
		return aopex;
	}

	@Test
	public void testRejectsSwapToNull() {
		IllegalArgumentException ex = testRejectsSwapToInvalidValue(null);
		assertTrue(ex.getMessage().indexOf("null") != -1);
	}

	// TODO test reject swap to wrong interface or class?
	// how to decide what's valid?


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
