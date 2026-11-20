/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Written with the intention of reproducing SPR-7318.
 *
 * @author Chris Beams
 */
class FactoryBeanLookupTests {

	private final BeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	void setUp() {
		new XmlBeanDefinitionReader((BeanDefinitionRegistry) beanFactory).loadBeanDefinitions(
				new ClassPathResource("FactoryBeanLookupTests-context.xml", getClass()));
	}

	@Test
	void factoryBeanLookupByNameDereferencing() {
		Object fooFactory = beanFactory.getBean("&fooFactory");
		assertThat(fooFactory).isInstanceOf(FooFactoryBean.class);
	}

	@Test
	void factoryBeanLookupByType() {
		FooFactoryBean fooFactory = beanFactory.getBean(FooFactoryBean.class);
		assertThat(fooFactory).isNotNull();
	}

	@Test
	void factoryBeanLookupByTypeAndNameDereference() {
		FooFactoryBean fooFactory = beanFactory.getBean("&fooFactory", FooFactoryBean.class);
		assertThat(fooFactory).isNotNull();
	}

	@Test
	void factoryBeanObjectLookupByName() {
		Object fooFactory = beanFactory.getBean("fooFactory");
		assertThat(fooFactory).isInstanceOf(Foo.class);
	}

	@Test
	void factoryBeanObjectLookupByNameAndType() {
		Foo foo = beanFactory.getBean("fooFactory", Foo.class);
		assertThat(foo).isNotNull();
	}

}

class FooFactoryBean extends AbstractFactoryBean<Foo> {
	@Override
	protected Foo createInstance() {
		return new Foo();
	}

	@Override
	public Class<?> getObjectType() {
		return Foo.class;
	}
}

class Foo { }
