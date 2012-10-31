/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.beans.factory.annotation;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Test cases for using injection annotations ({@link Autowired}, {@link Value}) as meta-annotations.
 *
 * @author Oliver Gierke
 */
public class InjectionAnnotationsAsMetaAnnotationsTests {

	@Test
	public void valueInjectionWorksForAtValueUsedAsMetaAnnotation() {

		Dependency dependency = new Dependency();

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("foo", 1);
		beanFactory.registerSingleton("dependency", dependency);
		beanFactory.registerBeanDefinition("client", new RootBeanDefinition(Client.class));

		AutowiredAnnotationBeanPostProcessor postProcessor = new AutowiredAnnotationBeanPostProcessor();
		postProcessor.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(postProcessor);

		Client client = beanFactory.getBean(Client.class);
		assertThat(client.value, is(1));
		assertThat(client.dependency, is(dependency));
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Value("foo")
	@interface MySpecialValue {

	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Autowired
	@interface MySpecialAutowired {

	}

	static class Client {

		@MySpecialValue
		Integer value;

		@MySpecialAutowired
		Dependency dependency;
	}

	static class Dependency {

	}
}
