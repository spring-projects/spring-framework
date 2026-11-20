/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

class Spr7167Tests {

	@Test
	@SuppressWarnings({ "deprecation", "resource" })
	void test() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		assertThat(ctx.getBeanFactory().getBeanDefinition("someDependency").getDescription())
				.as("someDependency was not post processed")
				.isEqualTo("post processed by MyPostProcessor");

		MyConfig config = ctx.getBean(MyConfig.class);
		assertThat(ClassUtils.isCglibProxy(config)).as("Config class was not enhanced").isTrue();
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
