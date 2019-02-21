/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class Spr15275Tests {

	@Test
	public void testWithFactoryBean() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithFactoryBean.class);
		assertEquals("x", context.getBean(Bar.class).foo.toString());
		assertSame(context.getBean(FooInterface.class), context.getBean(Bar.class).foo);
	}

	@Test
	public void testWithAbstractFactoryBean() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithAbstractFactoryBean.class);
		assertEquals("x", context.getBean(Bar.class).foo.toString());
		assertSame(context.getBean(FooInterface.class), context.getBean(Bar.class).foo);
	}

	@Test
	public void testWithAbstractFactoryBeanForInterface() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithAbstractFactoryBeanForInterface.class);
		assertEquals("x", context.getBean(Bar.class).foo.toString());
		assertSame(context.getBean(FooInterface.class), context.getBean(Bar.class).foo);
	}

	@Test
	public void testWithAbstractFactoryBeanAsReturnType() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithAbstractFactoryBeanAsReturnType.class);
		assertEquals("x", context.getBean(Bar.class).foo.toString());
		assertSame(context.getBean(FooInterface.class), context.getBean(Bar.class).foo);
	}

	@Test
	public void testWithFinalFactoryBean() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithFinalFactoryBean.class);
		assertEquals("x", context.getBean(Bar.class).foo.toString());
		assertSame(context.getBean(FooInterface.class), context.getBean(Bar.class).foo);
	}

	@Test
	public void testWithFinalFactoryBeanAsReturnType() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithFinalFactoryBeanAsReturnType.class);
		assertEquals("x", context.getBean(Bar.class).foo.toString());
		// not same due to fallback to raw FinalFactoryBean instance with repeated getObject() invocations
		assertNotSame(context.getBean(FooInterface.class), context.getBean(Bar.class).foo);
	}


	@Configuration
	protected static class ConfigWithFactoryBean {

		@Bean
		public FactoryBean<Foo> foo() {
			return new FactoryBean<Foo>() {
				@Override
				public Foo getObject() {
					return new Foo("x");
				}
				@Override
				public Class<?> getObjectType() {
					return Foo.class;
				}
			};
		}

		@Bean
		public Bar bar() throws Exception {
			assertTrue(foo().isSingleton());
			return new Bar(foo().getObject());
		}
	}


	@Configuration
	protected static class ConfigWithAbstractFactoryBean {

		@Bean
		public FactoryBean<Foo> foo() {
			return new AbstractFactoryBean<Foo>() {
				@Override
				public Foo createInstance() {
					return new Foo("x");
				}
				@Override
				public Class<?> getObjectType() {
					return Foo.class;
				}
			};
		}

		@Bean
		public Bar bar() throws Exception {
			assertTrue(foo().isSingleton());
			return new Bar(foo().getObject());
		}
	}


	@Configuration
	protected static class ConfigWithAbstractFactoryBeanForInterface {

		@Bean
		public FactoryBean<FooInterface> foo() {
			return new AbstractFactoryBean<FooInterface>() {
				@Override
				public FooInterface createInstance() {
					return new Foo("x");
				}
				@Override
				public Class<?> getObjectType() {
					return FooInterface.class;
				}
			};
		}

		@Bean
		public Bar bar() throws Exception {
			assertTrue(foo().isSingleton());
			return new Bar(foo().getObject());
		}
	}


	@Configuration
	protected static class ConfigWithAbstractFactoryBeanAsReturnType {

		@Bean
		public AbstractFactoryBean<FooInterface> foo() {
			return new AbstractFactoryBean<FooInterface>() {
				@Override
				public FooInterface createInstance() {
					return new Foo("x");
				}
				@Override
				public Class<?> getObjectType() {
					return Foo.class;
				}
			};
		}

		@Bean
		public Bar bar() throws Exception {
			assertTrue(foo().isSingleton());
			return new Bar(foo().getObject());
		}
	}


	@Configuration
	protected static class ConfigWithFinalFactoryBean {

		@Bean
		public FactoryBean<FooInterface> foo() {
			return new FinalFactoryBean();
		}

		@Bean
		public Bar bar() throws Exception {
			assertTrue(foo().isSingleton());
			return new Bar(foo().getObject());
		}
	}


	@Configuration
	protected static class ConfigWithFinalFactoryBeanAsReturnType {

		@Bean
		public FinalFactoryBean foo() {
			return new FinalFactoryBean();
		}

		@Bean
		public Bar bar() throws Exception {
			assertTrue(foo().isSingleton());
			return new Bar(foo().getObject());
		}
	}


	private static final class FinalFactoryBean implements FactoryBean<FooInterface> {

		@Override
		public Foo getObject() {
			return new Foo("x");
		}

		@Override
		public Class<?> getObjectType() {
			return FooInterface.class;
		}
	}


	protected interface FooInterface {
	}


	protected static class Foo implements FooInterface {

		private final String value;

		public Foo(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}


	protected static class Bar {

		public final FooInterface foo;

		public Bar(FooInterface foo) {
			this.foo = foo;
		}
	}

}
