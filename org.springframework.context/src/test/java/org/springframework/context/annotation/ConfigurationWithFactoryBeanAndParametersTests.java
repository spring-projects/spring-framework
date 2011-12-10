/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;


/**
 * Test case cornering the bug initially raised with SPR-8762, in which a
 * NullPointerException would be raised if a FactoryBean-returning @Bean method also
 * accepts parameters
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ConfigurationWithFactoryBeanAndParametersTests {
	@Test
	public void test() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, Bar.class);
		assertNotNull(ctx.getBean(Bar.class).foo);
	}
}

@Configuration
class Config {
	@Bean
	public FactoryBean<Foo> fb(@Value("42") String answer) {
		return new FooFactoryBean();
	}
}

class Foo {
}

class Bar {
	Foo foo;

	@Autowired
	public Bar(Foo foo) {
		this.foo = foo;
	}
}

class FooFactoryBean implements FactoryBean<Foo> {

	public Foo getObject() {
		return new Foo();
	}

	public Class<Foo> getObjectType() {
		return Foo.class;
	}

	public boolean isSingleton() {
		return true;
	}
}
