package org.springframework.context.debug;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * A minimal Spring application to debug the startup process.
 * <p>
 * Usage: Run the main method and set breakpoints in:
 * <ul>
 *     <li>{@link org.springframework.context.support.AbstractApplicationContext#refresh()}</li>
 *     <li>{@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton(String, boolean)}</li>
 *     <li>{@link org.springframework.aop.framework.JdkDynamicAopProxy#invoke(Object, java.lang.reflect.Method, Object[])}</li>
 * </ul>
 */
public class MinimalSpringApp {

	public static void main(String[] args) {
		System.out.println(">>> Spring Application Starting...");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		
		ServiceA serviceA = context.getBean(ServiceA.class);
		serviceA.sayHello();
		
		context.close();
		System.out.println(">>> Spring Application Stopped.");
	}

	@Configuration
	static class AppConfig {

		@Bean
		public ServiceA serviceA(ServiceB serviceB) {
			return new ServiceA(serviceB);
		}

		@Bean
		public ServiceB serviceB() {
			return new ServiceB();
		}

		@Bean
		public StartupLoggerPostProcessor startupLoggerPostProcessor() {
			return new StartupLoggerPostProcessor();
		}
	}

	static class ServiceA {
		private final ServiceB serviceB;

		public ServiceA(ServiceB serviceB) {
			this.serviceB = serviceB;
		}

		public void sayHello() {
			System.out.println("ServiceA says: Hello! (ServiceB is " + serviceB + ")");
		}
	}

	static class ServiceB {
		@Override
		public String toString() {
			return "ServiceBInstance";
		}
	}

	static class StartupLoggerPostProcessor implements BeanPostProcessor {
		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			System.out.println("--- [BPP] Before Initialization: " + beanName);
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			System.out.println("--- [BPP] After Initialization: " + beanName);
			return bean;
		}
	}
}
