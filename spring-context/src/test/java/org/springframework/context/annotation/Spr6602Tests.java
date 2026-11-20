/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests to verify that FactoryBean semantics are the same in Configuration
 * classes as in XML.
 *
 * @author Chris Beams
 */
class Spr6602Tests {

	@Test
	void xmlBehavior() throws Exception {
		doAssertions(new ClassPathXmlApplicationContext("Spr6602Tests-context.xml", Spr6602Tests.class));
	}

	@Test
	void configurationClassBehavior() throws Exception {
		doAssertions(new AnnotationConfigApplicationContext(FooConfig.class));
	}

	private void doAssertions(ApplicationContext ctx) {
		Foo foo = ctx.getBean(Foo.class);

		Bar bar1 = ctx.getBean(Bar.class);
		Bar bar2 = ctx.getBean(Bar.class);
		assertThat(bar1).isEqualTo(bar2);
		assertThat(bar1).isEqualTo(foo.bar);

		BarFactory barFactory1 = ctx.getBean(BarFactory.class);
		BarFactory barFactory2 = ctx.getBean(BarFactory.class);
		assertThat(barFactory1).isEqualTo(barFactory2);

		Bar bar3 = barFactory1.getObject();
		Bar bar4 = barFactory1.getObject();
		assertThat(bar3).isNotEqualTo(bar4);
	}


	@Configuration
	public static class FooConfig {

		@Bean
		public Foo foo() {
			return new Foo(barFactory().getObject());
		}

		@Bean
		public BarFactory barFactory() {
			return new BarFactory();
		}
	}


	public static class Foo {

		final Bar bar;

		public Foo(Bar bar) {
			this.bar = bar;
		}
	}


	public static class Bar {
	}


	public static class BarFactory implements FactoryBean<Bar> {

		@Override
		public Bar getObject() {
			return new Bar();
		}

		@Override
		public Class<? extends Bar> getObjectType() {
			return Bar.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

}
