/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.remoting.httpinvoker;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class HttpInvokerFactoryBeanIntegrationTests {

	@Test
	@SuppressWarnings("resource")
	void testLoadedConfigClass() {
		ApplicationContext context = new AnnotationConfigApplicationContext(InvokerAutowiringConfig.class);
		MyBean myBean = context.getBean("myBean", MyBean.class);
		assertThat(myBean.myService).isSameAs(context.getBean("myService"));
		myBean.myService.handle();
		myBean.myService.handleAsync();
	}

	@Test
	@SuppressWarnings("resource")
	void testNonLoadedConfigClass() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBeanDefinition("config", new RootBeanDefinition(InvokerAutowiringConfig.class.getName()));
		context.refresh();
		MyBean myBean = context.getBean("myBean", MyBean.class);
		assertThat(myBean.myService).isSameAs(context.getBean("myService"));
		myBean.myService.handle();
		myBean.myService.handleAsync();
	}

	@Test
	@SuppressWarnings("resource")
	void withConfigurationClassWithPlainFactoryBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ConfigWithPlainFactoryBean.class);
		context.refresh();
		MyBean myBean = context.getBean("myBean", MyBean.class);
		assertThat(myBean.myService).isSameAs(context.getBean("myService"));
		myBean.myService.handle();
		myBean.myService.handleAsync();
	}


	interface MyService {

		void handle();

		@Async
		public void handleAsync();
	}


	@Component("myBean")
	static class MyBean {

		@Autowired
		MyService myService;
	}


	@Configuration
	@ComponentScan
	@Lazy
	static class InvokerAutowiringConfig {

		@Bean
		AsyncAnnotationBeanPostProcessor aabpp() {
			return new AsyncAnnotationBeanPostProcessor();
		}

		@Bean
		@SuppressWarnings("deprecation")
		HttpInvokerProxyFactoryBean myService() {
			HttpInvokerProxyFactoryBean factory = new HttpInvokerProxyFactoryBean();
			factory.setServiceUrl("/svc/dummy");
			factory.setServiceInterface(MyService.class);
			factory.setHttpInvokerRequestExecutor((config, invocation) -> new RemoteInvocationResult());
			return factory;
		}

		@Bean
		FactoryBean<String> myOtherService() {
			throw new IllegalStateException("Don't ever call me");
		}
	}


	@Configuration
	static class ConfigWithPlainFactoryBean {

		@Autowired
		Environment env;

		@Bean
		MyBean myBean() {
			return new MyBean();
		}

		@Bean
		@SuppressWarnings("deprecation")
		HttpInvokerProxyFactoryBean myService() {
			String name = env.getProperty("testbean.name");
			HttpInvokerProxyFactoryBean factory = new HttpInvokerProxyFactoryBean();
			factory.setServiceUrl("/svc/" + name);
			factory.setServiceInterface(MyService.class);
			factory.setHttpInvokerRequestExecutor((config, invocation) -> new RemoteInvocationResult());
			return factory;
		}
	}

}
