/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation.configuration;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 * Tests for SPR-10668.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
public class Spr10668Tests {

	@Test
	public void testSelfInjectHierarchy() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ChildConfig.class);
		assertNotNull(context.getBean(MyComponent.class));
		context.close();
	}

	@Configuration
	public static class ParentConfig implements BeanFactoryAware {

		@Autowired(required = false)
		MyComponent component;

		public ParentConfig() {
			System.out.println("Parent " + getClass());
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			System.out.println("BFA " + getClass());
		}

	}

	@Configuration
	public static class ChildConfig extends ParentConfig {

		@Bean
		public MyComponentImpl myComponent() {
			return new MyComponentImpl();
		}

	}

	public static interface MyComponent {
	}

	public static class MyComponentImpl implements MyComponent {
	}
}
