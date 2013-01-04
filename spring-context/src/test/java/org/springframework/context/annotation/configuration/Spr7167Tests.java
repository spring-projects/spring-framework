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

package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

public class Spr7167Tests {
	@Test
	public void test() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		assertThat("someDependency was not post processed",
				ctx.getBeanFactory().getBeanDefinition("someDependency").getDescription(),
				equalTo("post processed by MyPostProcessor"));

		MyConfig config = ctx.getBean(MyConfig.class);
		assertTrue("Config class was not enhanced", ClassUtils.isCglibProxy(config));
	}
}

@Configuration
class MyConfig {

	@Bean
	public Dependency someDependency() {
		return new Dependency();
	}

	@Bean
	public BeanFactoryPostProcessor thePostProcessor() {
		return new MyPostProcessor(someDependency());
	}
}

class Dependency {
}

class MyPostProcessor implements BeanFactoryPostProcessor {

	public MyPostProcessor(Dependency someDependency) {
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		AbstractBeanDefinition bd = (AbstractBeanDefinition) beanFactory.getBeanDefinition("someDependency");
		bd.setDescription("post processed by MyPostProcessor");
	}
}
