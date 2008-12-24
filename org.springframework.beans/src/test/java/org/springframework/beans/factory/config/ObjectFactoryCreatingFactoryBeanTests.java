/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static test.util.TestResourceUtils.qualifiedResource;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.Resource;

/**
 * Unit tests for {@link ObjectFactoryCreatingFactoryBean}.
 *
 * @author Colin Sampaleanu
 * @author Rick Evans
 * @author Chris Beams
 * @since 2004-05-11
 */
public final class ObjectFactoryCreatingFactoryBeanTests {

	private static final Resource CONTEXT =
		qualifiedResource(ObjectFactoryCreatingFactoryBeanTests.class, "context.xml");
	
	private BeanFactory beanFactory;

	@Before
	public void setUp() {
		this.beanFactory = new XmlBeanFactory(CONTEXT);
	}

	@Test
	public void testBasicOperation() throws BeansException {
		TestBean testBean = (TestBean) beanFactory.getBean("testBean");
		ObjectFactory<?> objectFactory = testBean.getObjectFactory();

		Date date1 = (Date) objectFactory.getObject();
		Date date2 = (Date) objectFactory.getObject();
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
		ObjectFactory<?> objectFactory = (ObjectFactory<?>) factory.getObject();
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


	public static class TestBean {

		public ObjectFactory<?> objectFactory;


		public ObjectFactory<?> getObjectFactory() {
			return objectFactory;
		}

		public void setObjectFactory(ObjectFactory<?> objectFactory) {
			this.objectFactory = objectFactory;
		}

	}

}
