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

import java.util.NoSuchElementException;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.Person;
import org.springframework.tests.sample.beans.SerializablePerson;
import org.springframework.tests.sample.beans.SideEffectBean;
import org.springframework.util.SerializationTestUtils;

import static org.junit.Assert.*;

/**
 * Tests for pooling invoker interceptor.
 * TODO: need to make these tests stronger: it's hard to
 * make too many assumptions about a pool.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 */
@SuppressWarnings("deprecation")
public class CommonsPoolTargetSourceTests {

	/**
	 * Initial count value set in bean factory XML
	 */
	private static final int INITIAL_COUNT = 10;

	private DefaultListableBeanFactory beanFactory;

	@Before
	public void setUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource(getClass().getSimpleName() + "-context.xml", getClass()));
	}

	/**
	 * We must simulate container shutdown, which should clear threads.
	 */
	@After
	public void tearDown() {
		// Will call pool.close()
		this.beanFactory.destroySingletons();
	}

	private void testFunctionality(String name) {
		SideEffectBean pooled = (SideEffectBean) beanFactory.getBean(name);
		assertEquals(INITIAL_COUNT, pooled.getCount());
		pooled.doWork();
		assertEquals(INITIAL_COUNT + 1, pooled.getCount());

		pooled = (SideEffectBean) beanFactory.getBean(name);
		// Just check that it works--we can't make assumptions
		// about the count
		pooled.doWork();
		//assertEquals(INITIAL_COUNT + 1, apartment.getCount() );
	}

	@Test
	public void testFunctionality() {
		testFunctionality("pooled");
	}

	@Test
	public void testFunctionalityWithNoInterceptors() {
		testFunctionality("pooledNoInterceptors");
	}

	@Test
	public void testConfigMixin() {
		SideEffectBean pooled = (SideEffectBean) beanFactory.getBean("pooledWithMixin");
		assertEquals(INITIAL_COUNT, pooled.getCount());
		PoolingConfig conf = (PoolingConfig) beanFactory.getBean("pooledWithMixin");
		// TODO one invocation from setup
		//assertEquals(1, conf.getInvocations());
		pooled.doWork();
		//	assertEquals("No objects active", 0, conf.getActive());
		assertEquals("Correct target source", 25, conf.getMaxSize());
		//	assertTrue("Some free", conf.getFree() > 0);
		//assertEquals(2, conf.getInvocations());
		assertEquals(25, conf.getMaxSize());
	}

	@Test
	public void testTargetSourceSerializableWithoutConfigMixin() throws Exception {
		CommonsPoolTargetSource cpts = (CommonsPoolTargetSource) beanFactory.getBean("personPoolTargetSource");

		SingletonTargetSource serialized = (SingletonTargetSource) SerializationTestUtils.serializeAndDeserialize(cpts);
		assertTrue(serialized.getTarget() instanceof Person);
	}


	@Test
	public void testProxySerializableWithoutConfigMixin() throws Exception {
		Person pooled = (Person) beanFactory.getBean("pooledPerson");

		//System.out.println(((Advised) pooled).toProxyConfigString());
		assertTrue(((Advised) pooled).getTargetSource() instanceof CommonsPoolTargetSource);

		//((Advised) pooled).setTargetSource(new SingletonTargetSource(new SerializablePerson()));
		Person serialized = (Person) SerializationTestUtils.serializeAndDeserialize(pooled);
		assertTrue(((Advised) serialized).getTargetSource() instanceof SingletonTargetSource);
		serialized.setAge(25);
		assertEquals(25, serialized.getAge());
	}

	@Test
	public void testHitMaxSize() throws Exception {
		int maxSize = 10;

		CommonsPoolTargetSource targetSource = new CommonsPoolTargetSource();
		targetSource.setMaxSize(maxSize);
		targetSource.setMaxWait(1);
		prepareTargetSource(targetSource);

		Object[] pooledInstances = new Object[maxSize];

		for (int x = 0; x < maxSize; x++) {
			Object instance = targetSource.getTarget();
			assertNotNull(instance);
			pooledInstances[x] = instance;
		}

		// should be at maximum now
		try {
			targetSource.getTarget();
			fail("Should throw NoSuchElementException");
		}
		catch (NoSuchElementException ex) {
			// desired
		}

		// lets now release an object and try to accquire a new one
		targetSource.releaseTarget(pooledInstances[9]);
		pooledInstances[9] = targetSource.getTarget();

		// release all objects
		for (int i = 0; i < pooledInstances.length; i++) {
			targetSource.releaseTarget(pooledInstances[i]);
		}
	}

	@Test
	public void testHitMaxSizeLoadedFromContext() throws Exception {
		Advised person = (Advised) beanFactory.getBean("maxSizePooledPerson");
		CommonsPoolTargetSource targetSource = (CommonsPoolTargetSource) person.getTargetSource();

		int maxSize = targetSource.getMaxSize();
		Object[] pooledInstances = new Object[maxSize];

		for (int x = 0; x < maxSize; x++) {
			Object instance = targetSource.getTarget();
			assertNotNull(instance);
			pooledInstances[x] = instance;
		}

		// should be at maximum now
		try {
			targetSource.getTarget();
			fail("Should throw NoSuchElementException");
		}
		catch (NoSuchElementException ex) {
			// desired
		}

		// lets now release an object and try to accquire a new one
		targetSource.releaseTarget(pooledInstances[9]);
		pooledInstances[9] = targetSource.getTarget();

		// release all objects
		for (int i = 0; i < pooledInstances.length; i++) {
			targetSource.releaseTarget(pooledInstances[i]);
		}
	}

	@Test
	public void testSetWhenExhaustedAction() {
		CommonsPoolTargetSource targetSource = new CommonsPoolTargetSource();
		targetSource.setWhenExhaustedActionName("WHEN_EXHAUSTED_BLOCK");
		assertEquals(GenericObjectPool.WHEN_EXHAUSTED_BLOCK, targetSource.getWhenExhaustedAction());
	}

	private void prepareTargetSource(CommonsPoolTargetSource targetSource) {
		String beanName = "target";

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerPrototype(beanName, SerializablePerson.class);

		targetSource.setTargetBeanName(beanName);
		targetSource.setBeanFactory(applicationContext);
	}

}