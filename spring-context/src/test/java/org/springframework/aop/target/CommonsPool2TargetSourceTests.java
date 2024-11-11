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

package org.springframework.aop.target;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.SerializablePerson;
import org.springframework.beans.testfixture.beans.SideEffectBean;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for pooling invoker interceptor.
 *
 * TODO: need to make these tests stronger: it's hard to
 * make too many assumptions about a pool.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 * @author Stephane Nicoll
 */
class CommonsPool2TargetSourceTests {

	/**
	 * Initial count value set in bean factory XML
	 */
	private static final int INITIAL_COUNT = 10;

	private DefaultListableBeanFactory beanFactory;

	@BeforeEach
	void setUp() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource(getClass().getSimpleName() + "-context.xml", getClass()));
	}

	/**
	 * We must simulate container shutdown, which should clear threads.
	 */
	@AfterEach
	void tearDown() {
		// Will call pool.close()
		this.beanFactory.destroySingletons();
	}

	private void testFunctionality(String name) {
		SideEffectBean pooled = (SideEffectBean) beanFactory.getBean(name);
		assertThat(pooled.getCount()).isEqualTo(INITIAL_COUNT);
		pooled.doWork();
		assertThat(pooled.getCount()).isEqualTo((INITIAL_COUNT + 1));

		pooled = (SideEffectBean) beanFactory.getBean(name);
		// Just check that it works--we can't make assumptions
		// about the count
		pooled.doWork();
		//assertEquals(INITIAL_COUNT + 1, apartment.getCount());
	}

	@Test
	void testFunctionality() {
		testFunctionality("pooled");
	}

	@Test
	void testFunctionalityWithNoInterceptors() {
		testFunctionality("pooledNoInterceptors");
	}

	@Test
	void testConfigMixin() {
		SideEffectBean pooled = (SideEffectBean) beanFactory.getBean("pooledWithMixin");
		assertThat(pooled.getCount()).isEqualTo(INITIAL_COUNT);
		PoolingConfig conf = (PoolingConfig) beanFactory.getBean("pooledWithMixin");
		// TODO one invocation from setup
		//assertEquals(1, conf.getInvocations());
		pooled.doWork();
		//	assertEquals("No objects active", 0, conf.getActive());
		assertThat(conf.getMaxSize()).as("Correct target source").isEqualTo(25);
		//	assertTrue("Some free", conf.getFree() > 0);
		//assertEquals(2, conf.getInvocations());
		assertThat(conf.getMaxSize()).isEqualTo(25);
	}

	@Test
	void testTargetSourceSerializableWithoutConfigMixin() throws Exception {
		CommonsPool2TargetSource cpts = (CommonsPool2TargetSource) beanFactory.getBean("personPoolTargetSource");

		SingletonTargetSource serialized = SerializationTestUtils.serializeAndDeserialize(cpts, SingletonTargetSource.class);
		assertThat(serialized.getTarget()).isInstanceOf(Person.class);
	}

	@Test
	void testProxySerializableWithoutConfigMixin() throws Exception {
		Person pooled = (Person) beanFactory.getBean("pooledPerson");

		boolean condition1 = ((Advised) pooled).getTargetSource() instanceof CommonsPool2TargetSource;
		assertThat(condition1).isTrue();

		//((Advised) pooled).setTargetSource(new SingletonTargetSource(new SerializablePerson()));
		Person serialized = SerializationTestUtils.serializeAndDeserialize(pooled);
		boolean condition = ((Advised) serialized).getTargetSource() instanceof SingletonTargetSource;
		assertThat(condition).isTrue();
		serialized.setAge(25);
		assertThat(serialized.getAge()).isEqualTo(25);
	}

	@Test
	void testHitMaxSize() throws Exception {
		int maxSize = 10;

		CommonsPool2TargetSource targetSource = new CommonsPool2TargetSource();
		targetSource.setMaxSize(maxSize);
		targetSource.setMaxWait(1);
		prepareTargetSource(targetSource);

		Object[] pooledInstances = new Object[maxSize];

		for (int x = 0; x < maxSize; x++) {
			Object instance = targetSource.getTarget();
			assertThat(instance).isNotNull();
			pooledInstances[x] = instance;
		}

		// should be at maximum now
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
				targetSource::getTarget);

		// let's now release an object and try to acquire a new one
		targetSource.releaseTarget(pooledInstances[9]);
		pooledInstances[9] = targetSource.getTarget();

		// release all objects
		for (Object element : pooledInstances) {
			targetSource.releaseTarget(element);
		}
	}

	@Test
	void testHitMaxSizeLoadedFromContext() throws Exception {
		Advised person = (Advised) beanFactory.getBean("maxSizePooledPerson");
		CommonsPool2TargetSource targetSource = (CommonsPool2TargetSource) person.getTargetSource();

		int maxSize = targetSource.getMaxSize();
		Object[] pooledInstances = new Object[maxSize];

		for (int x = 0; x < maxSize; x++) {
			Object instance = targetSource.getTarget();
			assertThat(instance).isNotNull();
			pooledInstances[x] = instance;
		}

		// should be at maximum now
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
				targetSource::getTarget);

		// let's now release an object and try to acquire a new one
		targetSource.releaseTarget(pooledInstances[9]);
		pooledInstances[9] = targetSource.getTarget();

		// release all objects
		for (Object pooledInstance : pooledInstances) {
			targetSource.releaseTarget(pooledInstance);
		}
	}

	@Test
	void testSetWhenExhaustedAction() {
		CommonsPool2TargetSource targetSource = new CommonsPool2TargetSource();
		targetSource.setBlockWhenExhausted(true);
		assertThat(targetSource.isBlockWhenExhausted()).isTrue();
	}

	@Test
	void referenceIdentityByDefault() throws Exception {
		CommonsPool2TargetSource targetSource = new CommonsPool2TargetSource();
		targetSource.setMaxWait(1);
		prepareTargetSource(targetSource);

		Object first = targetSource.getTarget();
		Object second = targetSource.getTarget();
		boolean condition1 = first instanceof SerializablePerson;
		assertThat(condition1).isTrue();
		boolean condition = second instanceof SerializablePerson;
		assertThat(condition).isTrue();
		assertThat(second).isEqualTo(first);

		targetSource.releaseTarget(first);
		targetSource.releaseTarget(second);
	}

	private void prepareTargetSource(CommonsPool2TargetSource targetSource) {
		String beanName = "target";

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerPrototype(beanName, SerializablePerson.class);

		targetSource.setTargetBeanName(beanName);
		targetSource.setBeanFactory(applicationContext);
	}

}
