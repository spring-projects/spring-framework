/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.target;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.SideEffectBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ThreadLocalTargetSourceTests {

	/** Initial count value set in bean factory XML */
	private static final int INITIAL_COUNT = 10;

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	public void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				qualifiedResource(ThreadLocalTargetSourceTests.class, "context.xml"));
	}

	/**
	 * We must simulate container shutdown, which should clear threads.
	 */
	protected void close() {
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
		assertThat(apartment.getCount()).isEqualTo(INITIAL_COUNT);
		apartment.doWork();
		assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 1));

		ITestBean test = (ITestBean) beanFactory.getBean("threadLocal2");
		assertThat(test.getName()).isEqualTo("Rod");
		assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
	}

	@Test
	public void testReuseInSameThread() {
		SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
		assertThat(apartment.getCount()).isEqualTo(INITIAL_COUNT);
		apartment.doWork();
		assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 1));

		apartment = (SideEffectBean) beanFactory.getBean("apartment");
		assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 1));
	}

	/**
	 * Relies on introduction.
	 */
	@Test
	public void testCanGetStatsViaMixin() {
		ThreadLocalTargetSourceStats stats = (ThreadLocalTargetSourceStats) beanFactory.getBean("apartment");
		// +1 because creating target for stats call counts
		assertThat(stats.getInvocationCount()).isEqualTo(1);
		SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
		apartment.doWork();
		// +1 again
		assertThat(stats.getInvocationCount()).isEqualTo(3);
		// + 1 for states call!
		assertThat(stats.getHitCount()).isEqualTo(3);
		apartment.doWork();
		assertThat(stats.getInvocationCount()).isEqualTo(6);
		assertThat(stats.getHitCount()).isEqualTo(6);
		// Only one thread so only one object can have been bound
		assertThat(stats.getObjectCount()).isEqualTo(1);
	}

	@Test
	public void testNewThreadHasOwnInstance() throws InterruptedException {
		SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
		assertThat(apartment.getCount()).isEqualTo(INITIAL_COUNT);
		apartment.doWork();
		apartment.doWork();
		apartment.doWork();
		assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 3));

		class Runner implements Runnable {
			public SideEffectBean mine;
			@Override
			public void run() {
				this.mine = (SideEffectBean) beanFactory.getBean("apartment");
				assertThat(mine.getCount()).isEqualTo(INITIAL_COUNT);
				mine.doWork();
				assertThat(mine.getCount()).isEqualTo((INITIAL_COUNT + 1));
			}
		}
		Runner r = new Runner();
		Thread t = new Thread(r);
		t.start();
		t.join();

		assertThat(r).isNotNull();

		// Check it didn't affect the other thread's copy
		assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 3));

		// When we use other thread's copy in this thread
		// it should behave like ours
		assertThat(r.mine.getCount()).isEqualTo((INITIAL_COUNT + 3));

		// Bound to two threads
		assertThat(((ThreadLocalTargetSourceStats) apartment).getObjectCount()).isEqualTo(2);
	}

	/**
	 * Test for SPR-1442. Destroyed target should re-associated with thread and not throw NPE.
	 */
	@Test
	public void testReuseDestroyedTarget() {
		ThreadLocalTargetSource source = (ThreadLocalTargetSource)this.beanFactory.getBean("threadLocalTs");

		// try first time
		source.getTarget();
		source.destroy();

		// try second time
		source.getTarget(); // Should not throw NPE
	}

}
