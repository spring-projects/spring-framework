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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
class Spr11202Tests {

	@Test
	void withImporter() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Wrapper.class);
		assertThat(context.getBean("value")).isEqualTo("foo");
		context.close();
	}

	@Test
	void withoutImporter() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		assertThat(context.getBean("value")).isEqualTo("foo");
		context.close();
	}


	@Configuration
	@Import(Selector.class)
	protected static class Wrapper {
	}


	protected static class Selector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] {Config.class.getName()};
		}
	}


	@Configuration
	protected static class Config {

		@Bean
		public FooFactoryBean foo() {
			return new FooFactoryBean();
		}

		@Bean
		public String value() {
			String name = foo().getObject().getName();
			Assert.state(name != null, "Name cannot be null");
			return name;
		}

		@Bean
		@Conditional(NoBarCondition.class)
		public String bar() {
			return "bar";
		}
	}


	protected static class NoBarCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (context.getBeanFactory().getBeanNamesForAnnotation(Bar.class).length > 0) {
				return false;
			}
			return true;
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.TYPE)
	protected @interface Bar {
	}


	protected static class FooFactoryBean implements FactoryBean<Foo>, InitializingBean {

		private Foo foo = new Foo();

		@Override
		public Foo getObject() {
			return foo;
		}

		@Override
		public Class<?> getObjectType() {
			return Foo.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void afterPropertiesSet() {
			this.foo.name = "foo";
		}
	}


	protected static class Foo {

		private String name;

		public String getName() {
			return name;
		}
	}

}
