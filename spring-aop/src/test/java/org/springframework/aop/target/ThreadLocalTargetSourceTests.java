/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.SideEffectBean;

import static org.junit.Assert.*;
import static org.springframework.tests.TestResourceUtils.*;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ThreadLocalTargetSourceTests {

	private static final Resource CONTEXT = qualifiedResource(ThreadLocalTargetSourceTests.class, "context.xml");

	/** Initial count value set in bean factory XML */
	private static final int INITIAL_COUNT = 10;

	private DefaultListableBeanFactory beanFactory;

	@Before
	public void setUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(CONTEXT);
	}

	/**
	 * We must simulate container shutdown, which should clear threads.
	 */
	protected void tearDown() {
		this.beanFactory.destroySingletons();
	}

	/**
	 * Check we can use two different ThreadLocalTargetSources
	 * managing objects of different types without them interfering
	 * with one another.
	 */
	@Test
	public void testUseDifferentManagedInstancesInSameThread() {
		SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
		assertEquals(INITIAL_COUNT, apartment.getCount());
		apartment.doWork();
		assertEquals(INITIAL_COUNT + 1, apartment.getCount());

		ITestBean test = (ITestBean) beanFactory.getBean("threadLocal2");
		assertEquals("Rod", test.getName());
		assertEquals("Kerry", test.getSpouse().getName());
	}

	@Test
	public void testReuseInSameThread() {
		SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
		assertEquals(INITIAL_COUNT, apartment.getCount());
		apartment.doWork();
		assertEquals(INITIAL_COUNT + 1, apartment.getCount());

		apartment = (SideEffectBean) beanFactory.getBean("apartment");
		assertEquals(INITIAL_COUNT + 1, apartment.getCount());
	}

	/**
	 * Relies on introduction.
	 */
	@Test
	public void testCanGetStatsViaMixin() {
		ThreadLocalTargetSourceStats stats = (ThreadLocalTargetSourceStats) beanFactory.getBean("apartment");
		// +1 because creating target for stats call counts
		assertEquals(1, stats.getInvocationCount());
		SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
		apartment.doWork();
		// +1 again
		assertEquals(3, stats.getInvocationCount());
		// + 1 for states call!
		assertEquals(3, stats.getHitCount());
		apartment.doWork();
		assertEquals(6, stats.getInvocationCount());
		assertEquals(6, stats.getHitCount());
		// Only one thread so only one object can have been bound
		assertEquals(1, stats.getObjectCount());
	}

	@Test
	public void testNewThreadHasOwnInstance() throws InterruptedException {
		SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
		assertEquals(INITIAL_COUNT, apartment.getCount());
		apartment.doWork();
		apartment.doWork();
		apartment.doWork();
		assertEquals(INITIAL_COUNT + 3, apartment.getCount());

		class Runner implements Runnable {
			public SideEffectBean mine;
			@Override
			public void run() {
				this.mine = (SideEffectBean) beanFactory.getBean("apartment");
				assertEquals(INITIAL_COUNT, mine.getCount());
				mine.doWork();
				assertEquals(INITIAL_COUNT + 1, mine.getCount());
			}
		}
		Runner r = new Runner();
		Thread t = new Thread(r);
		t.start();
		t.join();

		assertNotNull(r);

		// Check it didn't affect the other thread's copy
		assertEquals(INITIAL_COUNT + 3, apartment.getCount());

		// When we use other thread's copy in this thread
		// it should behave like ours
		assertEquals(INITIAL_COUNT + 3, r.mine.getCount());

		// Bound to two threads
		assertEquals(2, ((ThreadLocalTargetSourceStats) apartment).getObjectCount());
	}

	/**
	 * Test for SPR-1442. Destroyed target should re-associated with thread and not throw NPE
	 */
	@Test
	public void testReuseDestroyedTarget() {
		ThreadLocalTargetSource source = (ThreadLocalTargetSource)this.beanFactory.getBean("threadLocalTs");

		// try first time
		source.getTarget();
		source.destroy();

		// try second time
		try {
			source.getTarget();
		}
		catch (NullPointerException ex) {
			fail("Should not throw NPE");
		}
	}

}
