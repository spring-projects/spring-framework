/*
 * Copyright 2002-present the original author or authors.
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
	void testXmlBehavior() throws Exception {
		doAssertions(new ClassPathXmlApplicationContext("Spr6602Tests-context.xml", Spr6602Tests.class));
	}

	@Test
	void testConfigurationClassBehavior() throws Exception {
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
