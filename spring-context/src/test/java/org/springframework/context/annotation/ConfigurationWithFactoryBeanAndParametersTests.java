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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test case cornering the bug initially raised with SPR-8762, in which a
 * NullPointerException would be raised if a FactoryBean-returning @Bean method also
 * accepts parameters
 *
 * @author Chris Beams
 * @since 3.1
 */
class ConfigurationWithFactoryBeanAndParametersTests {

	@Test
	void test() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, Bar.class);
		assertThat(ctx.getBean(Bar.class).foo).isNotNull();
		ctx.close();
	}


	@Configuration
	static class Config {

		@Bean
		public FactoryBean<Foo> fb(@Value("42") String answer) {
			return new FooFactoryBean();
		}
	}

	static class Foo {
	}

	static class Bar {

		Foo foo;

		@Autowired
		public Bar(Foo foo) {
			this.foo = foo;
		}
	}

	static class FooFactoryBean implements FactoryBean<Foo> {

		@Override
		public Foo getObject() {
			return new Foo();
		}

		@Override
		public Class<Foo> getObjectType() {
			return Foo.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}
