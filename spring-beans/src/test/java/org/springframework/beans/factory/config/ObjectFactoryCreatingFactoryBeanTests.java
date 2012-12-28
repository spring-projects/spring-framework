/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.util.Date;
import javax.inject.Provider;

import static org.easymock.EasyMock.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static test.util.TestResourceUtils.*;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.SerializationTestUtils;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 */
public class ObjectFactoryCreatingFactoryBeanTests {

	private static final Resource CONTEXT =
		qualifiedResource(ObjectFactoryCreatingFactoryBeanTests.class, "context.xml");

	private XmlBeanFactory beanFactory;

	@Before
	public void setUp() {
		this.beanFactory = new XmlBeanFactory(CONTEXT);
		this.beanFactory.setSerializationId("test");
	}

	@After
	public void tearDown() {
		this.beanFactory.setSerializationId(null);
	}

	@Test
	public void testFactoryOperation() throws Exception {
		FactoryTestBean testBean = beanFactory.getBean("factoryTestBean", FactoryTestBean.class);
		ObjectFactory<?> objectFactory = testBean.getObjectFactory();

		Date date1 = (Date) objectFactory.getObject();
		Date date2 = (Date) objectFactory.getObject();
		assertTrue(date1 != date2);
	}

	@Test
	public void testFactorySerialization() throws Exception {
		FactoryTestBean testBean = beanFactory.getBean("factoryTestBean", FactoryTestBean.class);
		ObjectFactory<?> objectFactory = testBean.getObjectFactory();

		objectFactory = (ObjectFactory) SerializationTestUtils.serializeAndDeserialize(objectFactory);

		Date date1 = (Date) objectFactory.getObject();
		Date date2 = (Date) objectFactory.getObject();
		assertTrue(date1 != date2);
	}

	@Test
	public void testProviderOperation() throws Exception {
		ProviderTestBean testBean = beanFactory.getBean("providerTestBean", ProviderTestBean.class);
		Provider<?> provider = testBean.getProvider();

		Date date1 = (Date) provider.get();
		Date date2 = (Date) provider.get();
		assertTrue(date1 != date2);
	}

	@Test
	public void testProviderSerialization() throws Exception {
		ProviderTestBean testBean = beanFactory.getBean("providerTestBean", ProviderTestBean.class);
		Provider<?> provider = testBean.getProvider();

		provider = (Provider) SerializationTestUtils.serializeAndDeserialize(provider);

		Date date1 = (Date) provider.get();
		Date date2 = (Date) provider.get();
		assertTrue(date1 != date2);
	}

	@Test
	public void testDoesNotComplainWhenTargetBeanNameRefersToSingleton() throws Exception {
		final String targetBeanName = "singleton";
		final String expectedSingleton = "Alicia Keys";

		BeanFactory beanFactory = createMock(BeanFactory.class);
		expect(beanFactory.getBean(targetBeanName)).andReturn(expectedSingleton);
		replay(beanFactory);

		ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
		factory.setTargetBeanName(targetBeanName);
		factory.setBeanFactory(beanFactory);
		factory.afterPropertiesSet();
		ObjectFactory<?> objectFactory = factory.getObject();
		Object actualSingleton = objectFactory.getObject();
		assertSame(expectedSingleton, actualSingleton);

		verify(beanFactory);
	}

	@Test
	public void testWhenTargetBeanNameIsNull() throws Exception {
		try {
			new ObjectFactoryCreatingFactoryBean().afterPropertiesSet();
			fail("Must have thrown an IllegalArgumentException; 'targetBeanName' property not set.");
		}
		catch (IllegalArgumentException expected) {}
	}

	@Test
	public void testWhenTargetBeanNameIsEmptyString() throws Exception {
		try {
			ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
			factory.setTargetBeanName("");
			factory.afterPropertiesSet();
			fail("Must have thrown an IllegalArgumentException; 'targetBeanName' property set to (invalid) empty string.");
		}
		catch (IllegalArgumentException expected) {}
	}

	@Test
	public void testWhenTargetBeanNameIsWhitespacedString() throws Exception {
		try {
			ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
			factory.setTargetBeanName("  \t");
			factory.afterPropertiesSet();
			fail("Must have thrown an IllegalArgumentException; 'targetBeanName' property set to (invalid) only-whitespace string.");
		}
		catch (IllegalArgumentException expected) {}
	}

	@Test
	public void testEnsureOFBFBReportsThatItActuallyCreatesObjectFactoryInstances() throws Exception {
		assertEquals("Must be reporting that it creates ObjectFactory instances (as per class contract).",
			ObjectFactory.class, new ObjectFactoryCreatingFactoryBean().getObjectType());
	}


	public static class FactoryTestBean {

		private ObjectFactory<?> objectFactory;

		public ObjectFactory<?> getObjectFactory() {
			return objectFactory;
		}

		public void setObjectFactory(ObjectFactory<?> objectFactory) {
			this.objectFactory = objectFactory;
		}
	}


	public static class ProviderTestBean {

		private Provider<?> provider;

		public Provider<?> getProvider() {
			return provider;
		}

		public void setProvider(Provider<?> provider) {
			this.provider = provider;
		}
	}

}
