/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Written with the intention of reproducing SPR-7318.
 *
 * @author Chris Beams
 */
public class FactoryBeanLookupTests {
	private BeanFactory beanFactory;

	@Before
	public void setUp() {
		beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader((BeanDefinitionRegistry) beanFactory).loadBeanDefinitions(
				new ClassPathResource("FactoryBeanLookupTests-context.xml", this.getClass()));
	}

	@Test
	public void factoryBeanLookupByNameDereferencing() {
		Object fooFactory = beanFactory.getBean("&fooFactory");
		assertThat(fooFactory, instanceOf(FooFactoryBean.class));
	}

	@Test
	public void factoryBeanLookupByType() {
		FooFactoryBean fooFactory = beanFactory.getBean(FooFactoryBean.class);
		assertNotNull(fooFactory);
	}

	@Test
	public void factoryBeanLookupByTypeAndNameDereference() {
		FooFactoryBean fooFactory = beanFactory.getBean("&fooFactory", FooFactoryBean.class);
		assertNotNull(fooFactory);
	}

	@Test
	public void factoryBeanObjectLookupByName() {
		Object fooFactory = beanFactory.getBean("fooFactory");
		assertThat(fooFactory, instanceOf(Foo.class));
	}

	@Test
	public void factoryBeanObjectLookupByNameAndType() {
		Foo foo = beanFactory.getBean("fooFactory", Foo.class);
		assertNotNull(foo);
	}
}

class FooFactoryBean extends AbstractFactoryBean<Foo> {
	@Override
	protected Foo createInstance() throws Exception {
		return new Foo();
	}

	@Override
	public Class<?> getObjectType() {
		return Foo.class;
	}
}

class Foo { }
