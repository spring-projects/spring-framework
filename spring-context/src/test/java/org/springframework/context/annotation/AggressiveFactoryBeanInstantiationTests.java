/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;

/**
 * @author Andy Wilkinson
 */
public class AggressiveFactoryBeanInstantiationTests {

	@Test
	public void directlyRegisteredFactoryBean() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(SimpleFactoryBean.class);
			context.addBeanFactoryPostProcessor((factory) -> {
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, String.class);
			});
			context.refresh();
		}
	}

	@Test
	public void beanMethodFactoryBean() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(BeanMethodConfiguration.class);
			context.addBeanFactoryPostProcessor((factory) -> {
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, String.class);
			});
			context.refresh();
		}
	}


	@Configuration
	static class BeanMethodConfiguration {

		@Bean
		public SimpleFactoryBean simpleFactoryBean(ApplicationContext applicationContext) {
			return new SimpleFactoryBean(applicationContext);
		}
	}


	static class SimpleFactoryBean implements FactoryBean<Object> {

		public SimpleFactoryBean(ApplicationContext applicationContext) {
		}

		@Override
		public Object getObject() {
			return new Object();
		}

		@Override
		public Class<?> getObjectType() {
			return Object.class;
		}
	}

}
