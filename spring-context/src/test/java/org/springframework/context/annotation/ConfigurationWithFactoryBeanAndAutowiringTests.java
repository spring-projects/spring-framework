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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests cornering bug SPR-8514.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
class ConfigurationWithFactoryBeanAndAutowiringTests {

	@Test
	void withConcreteFactoryBeanImplementationAsReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(ConcreteFactoryBeanImplementationConfig.class);
		ctx.refresh();
		ctx.close();
	}

	@Test
	void withParameterizedFactoryBeanImplementationAsReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(ParameterizedFactoryBeanImplementationConfig.class);
		ctx.refresh();
		ctx.close();
	}

	@Test
	void withParameterizedFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(ParameterizedFactoryBeanInterfaceConfig.class);
		ctx.refresh();
		ctx.close();
	}

	@Test
	void withNonPublicParameterizedFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(NonPublicParameterizedFactoryBeanInterfaceConfig.class);
		ctx.refresh();
		ctx.close();
	}

	@Test
	void withRawFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(RawFactoryBeanInterfaceConfig.class);
		ctx.refresh();
		ctx.close();
	}

	@Test
	void withWildcardParameterizedFactoryBeanInterfaceAsReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(WildcardParameterizedFactoryBeanInterfaceConfig.class);
		ctx.refresh();
		ctx.close();
	}

	@Test
	void withFactoryBeanCallingBean() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		ctx.register(FactoryBeanCallingConfig.class);
		ctx.refresh();
		assertThat(ctx.getBean("myString")).isEqualTo("true");
		ctx.close();
	}


	static class DummyBean {
	}


	static class MyFactoryBean implements FactoryBean<String>, InitializingBean {

		private boolean initialized = false;

		@Override
		public void afterPropertiesSet() {
			this.initialized = true;
		}

		@Override
		public String getObject() {
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

		public String getString() {
			return Boolean.toString(this.initialized);
		}
	}


	static class MyParameterizedFactoryBean<T> implements FactoryBean<T> {

		private final T obj;

		public MyParameterizedFactoryBean(T obj) {
			this.obj = obj;
		}

		@Override
		public T getObject() {
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
	static class AppConfig {

		@Bean
		public DummyBean dummyBean() {
			return new DummyBean();
		}
	}


	@Configuration
	static class ConcreteFactoryBeanImplementationConfig {

		@Autowired
		private DummyBean dummyBean;

		@Bean
		public MyFactoryBean factoryBean() {
			Assert.notNull(dummyBean, "DummyBean was not injected.");
			return new MyFactoryBean();
		}
	}


	@Configuration
	static class ParameterizedFactoryBeanImplementationConfig {

		@Autowired
		private DummyBean dummyBean;

		@Bean
		public MyParameterizedFactoryBean<String> factoryBean() {
			Assert.notNull(dummyBean, "DummyBean was not injected.");
			return new MyParameterizedFactoryBean<>("whatev");
		}
	}


	@Configuration
	static class ParameterizedFactoryBeanInterfaceConfig {

		@Autowired
		private DummyBean dummyBean;

		@Bean
		public FactoryBean<String> factoryBean() {
			Assert.notNull(dummyBean, "DummyBean was not injected.");
			return new MyFactoryBean();
		}
	}


	@Configuration
	static class NonPublicParameterizedFactoryBeanInterfaceConfig {

		@Autowired
		private DummyBean dummyBean;

		@Bean
		FactoryBean<String> factoryBean() {
			Assert.notNull(dummyBean, "DummyBean was not injected.");
			return new MyFactoryBean();
		}
	}


	@Configuration
	static class RawFactoryBeanInterfaceConfig {

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
	static class WildcardParameterizedFactoryBeanInterfaceConfig {

		@Autowired
		private DummyBean dummyBean;

		@Bean
		public FactoryBean<?> factoryBean() {
			Assert.notNull(dummyBean, "DummyBean was not injected.");
			return new MyFactoryBean();
		}
	}


	@Configuration
	static class FactoryBeanCallingConfig {

		@Autowired
		private DummyBean dummyBean;

		@Bean
		public MyFactoryBean factoryBean() {
			Assert.notNull(dummyBean, "DummyBean was not injected.");
			return new MyFactoryBean();
		}

		@Bean
		public String myString() {
			return factoryBean().getString();
		}
	}

}
