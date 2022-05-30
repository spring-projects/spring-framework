/*
 * Copyright 2002-2020 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.testfixture.interceptor.SerializableNopInterceptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.SerializablePerson;
import org.springframework.beans.testfixture.beans.SideEffectBean;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class HotSwappableTargetSourceTests {

	/** Initial count value set in bean factory XML */
	private static final int INITIAL_COUNT = 10;

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	public void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				qualifiedResource(HotSwappableTargetSourceTests.class, "context.xml"));
	}

	/**
	 * We must simulate container shutdown, which should clear threads.
	 */
	@AfterEach
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
		assertThat(proxied.getCount()).isEqualTo(INITIAL_COUNT);
		proxied.doWork();
		assertThat(proxied.getCount()).isEqualTo((INITIAL_COUNT + 1));

		proxied = (SideEffectBean) beanFactory.getBean("swappable");
		proxied.doWork();
		assertThat(proxied.getCount()).isEqualTo((INITIAL_COUNT + 2));
	}

	@Test
	public void testValidSwaps() {
		SideEffectBean target1 = (SideEffectBean) beanFactory.getBean("target1");
		SideEffectBean target2 = (SideEffectBean) beanFactory.getBean("target2");

		SideEffectBean proxied = (SideEffectBean) beanFactory.getBean("swappable");
		assertThat(proxied.getCount()).isEqualTo(target1.getCount());
		proxied.doWork();
		assertThat(proxied.getCount()).isEqualTo((INITIAL_COUNT + 1));

		HotSwappableTargetSource swapper = (HotSwappableTargetSource) beanFactory.getBean("swapper");
		Object old = swapper.swap(target2);
		assertThat(old).as("Correct old target was returned").isEqualTo(target1);

		// TODO should be able to make this assertion: need to fix target handling
		// in AdvisedSupport
		//assertEquals(target2, ((Advised) proxied).getTarget());

		assertThat(proxied.getCount()).isEqualTo(20);
		proxied.doWork();
		assertThat(target2.getCount()).isEqualTo(21);

		// Swap it back
		swapper.swap(target1);
		assertThat(proxied.getCount()).isEqualTo(target1.getCount());
	}

	@Test
	public void testRejectsSwapToNull() {
		HotSwappableTargetSource swapper = (HotSwappableTargetSource) beanFactory.getBean("swapper");
		assertThatIllegalArgumentException().as("Shouldn't be able to swap to invalid value").isThrownBy(() ->
				swapper.swap(null))
			.withMessageContaining("null");
		// It shouldn't be corrupted, it should still work
		testBasicFunctionality();
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

		assertThat(p.getName()).isEqualTo(sp1.getName());
		hts.swap(sp2);
		assertThat(p.getName()).isEqualTo(sp2.getName());

		p = SerializationTestUtils.serializeAndDeserialize(p);
		// We need to get a reference to the client-side targetsource
		hts = (HotSwappableTargetSource) ((Advised) p).getTargetSource();
		assertThat(p.getName()).isEqualTo(sp2.getName());
		hts.swap(sp1);
		assertThat(p.getName()).isEqualTo(sp1.getName());

	}

}
