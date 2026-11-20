/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 */
class DuplicatePostProcessingTests {

	@Test
	@SuppressWarnings("resource")
	void withFactoryBeanAndEventListener() {
		new AnnotationConfigApplicationContext(Config.class).getBean(ExampleBean.class);
	}


	static class Config {

		@Bean
		public ExampleFactoryBean exampleFactory() {
			return new ExampleFactoryBean();
		}

		@Bean
		public static ExampleBeanPostProcessor exampleBeanPostProcessor() {
			return new ExampleBeanPostProcessor();
		}

		@Bean
		public ExampleApplicationEventListener exampleApplicationEventListener() {
			return new ExampleApplicationEventListener();
		}
	}


	static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		private final ExampleBean exampleBean = new ExampleBean();

		@Override
		public ExampleBean getObject() {
			return this.exampleBean;
		}

		@Override
		public Class<?> getObjectType() {
			return ExampleBean.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}


	static class ExampleBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

		private ApplicationContext applicationContext;

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof ExampleBean) {
				this.applicationContext.publishEvent(new ExampleApplicationEvent(this));
			}
			return bean;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}
	}


	@SuppressWarnings("serial")
	static class ExampleApplicationEvent extends ApplicationEvent {

		public ExampleApplicationEvent(Object source) {
			super(source);
		}
	}


	static class ExampleApplicationEventListener implements ApplicationListener<ExampleApplicationEvent>, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public void onApplicationEvent(ExampleApplicationEvent event) {
			this.beanFactory.getBean(ExampleBean.class);
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}
	}


	static class ExampleBean {
	}

}
