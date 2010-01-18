package org.springframework.context.annotation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests to verify that FactoryBean semantics are the same in Configuration
 * classes as in XML.
 *
 * @author Chris Beams
 */
public class Spr6602Tests {
	@Test
	public void testXmlBehavior() throws Exception {
		doAssertions(new ClassPathXmlApplicationContext("Spr6602Tests-context.xml", Spr6602Tests.class));
	}
	
	@Test
	public void testConfigurationClassBehavior() throws Exception {
		doAssertions(new AnnotationConfigApplicationContext(FooConfig.class));
	}
	
	private void doAssertions(ApplicationContext ctx) throws Exception {
		Foo foo = ctx.getBean(Foo.class);
		
		Bar bar1 = ctx.getBean(Bar.class);
		Bar bar2 = ctx.getBean(Bar.class);
		assertThat(bar1, is(bar2));
		assertThat(bar1, is(foo.bar));
		
		BarFactory barFactory1 = ctx.getBean(BarFactory.class);
		BarFactory barFactory2 = ctx.getBean(BarFactory.class);
		assertThat(barFactory1, is(barFactory2));
		
		Bar bar3 = barFactory1.getObject();
		Bar bar4 = barFactory1.getObject();
		assertThat(bar3, is(not(bar4)));
	}
	
}


@Configuration
class FooConfig {
	public @Bean Foo foo() throws Exception {
		return new Foo(barFactory().getObject());
	}
	
	public @Bean BarFactory barFactory() {
		return new BarFactory();
	}
}

class Foo { final Bar bar; public Foo(Bar bar) { this.bar = bar; } }
class Bar { }

class BarFactory implements FactoryBean<Bar> {

	public Bar getObject() throws Exception {
		return new Bar();
	}

	public Class<? extends Bar> getObjectType() {
		return Bar.class;
	}
	
	public boolean isSingleton() {
		return true;
	}

}
