package org.springframework.beans.factory;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;


/**
 * Written with the intention of reproducing SPR-7318.
 *
 * @author Chris Beams
 */
public class FactoryBeanLookupTests {
	private BeanFactory beanFactory;

	@Before
	public void setUp() {
		beanFactory = new XmlBeanFactory(
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
