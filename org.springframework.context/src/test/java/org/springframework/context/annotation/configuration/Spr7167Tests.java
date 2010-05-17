package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class Spr7167Tests {
	@Test
	public void test() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		assertThat("someDependency was not post processed",
				ctx.getBeanFactory().getBeanDefinition("someDependency").getDescription(),
				equalTo("post processed by MyPostProcessor"));

		MyConfig config = ctx.getBean(MyConfig.class);
		assertTrue("Config class was not enhanced", AopUtils.isCglibProxyClass(config.getClass()));
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

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		AbstractBeanDefinition bd = (AbstractBeanDefinition) beanFactory.getBeanDefinition("someDependency");
		bd.setDescription("post processed by MyPostProcessor");
	}
}