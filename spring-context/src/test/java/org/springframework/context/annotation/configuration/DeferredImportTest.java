/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DeferredImport;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DeferredImport} annotation support.
 * @author Johannes Edmeier
 * @since 4.2.3
 */
public class DeferredImportTest {

	@Test
	public void deferredImport() {
		DefaultListableBeanFactory beanFactory = spy(new DefaultListableBeanFactory());
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(beanFactory);
		context.register(Config1.class);
		context.register(Config2.class);
		context.refresh();
		context.getBean(Config1.class);
		context.getBean(Config2.class);

		InOrder ordered = inOrder(beanFactory);
		ordered.verify(beanFactory).registerBeanDefinition(eq("a"), (BeanDefinition) anyObject());
		ordered.verify(beanFactory).registerBeanDefinition(eq("b"), (BeanDefinition) anyObject());
		ordered.verify(beanFactory).registerBeanDefinition(eq("c"), (BeanDefinition) anyObject());
		ordered.verify(beanFactory).registerBeanDefinition(eq("d"), (BeanDefinition) anyObject());
		ordered.verify(beanFactory).registerBeanDefinition(eq("e"), (BeanDefinition) anyObject());
	}

	@Configuration
	@DeferredImport(Deferred1.class)
	static class Config1 {
	}

	@Configuration
	@Import(ImportedDeferred1.class)
	static class Deferred1 {
		@Bean
		public String e() {
			return "e";
		}
	}

	@Configuration
	static class ImportedDeferred1 {
		@Bean
		public String d() {
			return "d";
		}
	}

	@Configuration
	@Import(Imported.class)
	@DeferredImport(value = {Deferred2.class}, order = Ordered.LOWEST_PRECEDENCE -1)
	static class Config2 {
	}

	@Configuration
	static class Imported {
		@Bean
		public String a() {
			return "a";
		}
	}

	@Configuration
	@DeferredImport(DeferredDeferred2.class)
	static class Deferred2 {
		@Bean
		public String c() {
			return "c";
		}
	}

	@Configuration
	static class DeferredDeferred2 {
		@Bean
		public String b() {
			return "b";
		}
	}

}
