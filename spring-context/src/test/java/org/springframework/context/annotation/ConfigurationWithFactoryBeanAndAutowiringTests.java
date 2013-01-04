/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Tests cornering bug SPR-8514.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ConfigurationWithFactoryBeanAndAutowiringTests {

	@Test
	public void withConcreteFactoryBeanImplementationAsReturnType() {
		AnnotationConfigApplicationContext ctx =
			new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(ConcreteFactoryBeanImplementationConfig.class);
		ctx.refresh();
	}

	@Test
	public void withParameterizedFactoryBeanImplementationAsReturnType() {
		AnnotationConfigApplicationContext ctx =
			new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(ParameterizedFactoryBeanImplementationConfig.class);
		ctx.refresh();
	}

	@Test
	public void withParameterizedFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx =
			new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(ParameterizedFactoryBeanInterfaceConfig.class);
		ctx.refresh();
	}

	@Test
	public void withNonPublicParameterizedFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx =
			new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(NonPublicParameterizedFactoryBeanInterfaceConfig.class);
		ctx.refresh();
	}

	@Test(expected=BeanCreationException.class)
	public void withRawFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx =
			new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(RawFactoryBeanInterfaceConfig.class);
		ctx.refresh();
	}

	@Test(expected=BeanCreationException.class)
	public void withWildcardParameterizedFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx =
			new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(WildcardParameterizedFactoryBeanInterfaceConfig.class);
		ctx.refresh();
	}

}


class DummyBean {
}


class MyFactoryBean implements FactoryBean<String> {
	@Override
	public String getObject() throws Exception {
		return "foo";
	}
	@Override
	public Class<String> getObjectType() {
		return String.class;
	}
	@Override
	public boolean isSingleton() {
		return true;
	}
}


class MyParameterizedFactoryBean<T> implements FactoryBean<T> {

	private final T obj;

	public MyParameterizedFactoryBean(T obj) {
		this.obj = obj;
	}

	@Override
	public T getObject() throws Exception {
		return obj;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getObjectType() {
		return (Class<T>)obj.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}


@Configuration
class AppConfig {
	@Bean
	public DummyBean dummyBean() {
		return new DummyBean();
	}
}


@Configuration
class ConcreteFactoryBeanImplementationConfig {
	@Autowired
	private DummyBean dummyBean;

	@Bean
	public MyFactoryBean factoryBean() {
		Assert.notNull(dummyBean, "DummyBean was not injected.");
		return new MyFactoryBean();
	}
}


@Configuration
class ParameterizedFactoryBeanImplementationConfig {
	@Autowired
	private DummyBean dummyBean;

	@Bean
	public MyParameterizedFactoryBean<String> factoryBean() {
		Assert.notNull(dummyBean, "DummyBean was not injected.");
		return new MyParameterizedFactoryBean<String>("whatev");
	}
}


@Configuration
class ParameterizedFactoryBeanInterfaceConfig {
	@Autowired
	private DummyBean dummyBean;

	@Bean
	public FactoryBean<String> factoryBean() {
		Assert.notNull(dummyBean, "DummyBean was not injected.");
		return new MyFactoryBean();
	}
}


@Configuration
class NonPublicParameterizedFactoryBeanInterfaceConfig {
	@Autowired
	private DummyBean dummyBean;

	@Bean
	FactoryBean<String> factoryBean() {
		Assert.notNull(dummyBean, "DummyBean was not injected.");
		return new MyFactoryBean();
	}
}


@Configuration
class RawFactoryBeanInterfaceConfig {
	@Autowired
	private DummyBean dummyBean;

	@Bean
	@SuppressWarnings("rawtypes")
	public FactoryBean factoryBean() {
		Assert.notNull(dummyBean, "DummyBean was not injected.");
		return new MyFactoryBean();
	}
}


@Configuration
class WildcardParameterizedFactoryBeanInterfaceConfig {
	@Autowired
	private DummyBean dummyBean;

	@Bean
	public FactoryBean<?> factoryBean() {
		Assert.notNull(dummyBean, "DummyBean was not injected.");
		return new MyFactoryBean();
	}
}
