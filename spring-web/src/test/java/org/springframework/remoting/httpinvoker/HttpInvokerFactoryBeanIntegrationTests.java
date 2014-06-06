/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.remoting.httpinvoker;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.junit.Assert.*;

/**
 *
 * @author Stephane Nicoll
 */
public class HttpInvokerFactoryBeanIntegrationTests {

	@Test
	public void foo() {
		ApplicationContext context = new AnnotationConfigApplicationContext(InvokerAutowiringConfig.class);
		MyBean myBean = context.getBean(MyBean.class);
		assertSame(context.getBean("myService"), myBean.myService);
	}


	public interface MyService {
	}


	@Component
	public static class MyBean {

		@Autowired
		private MyService myService;
	}


	@Configuration
	@ComponentScan
	public static class InvokerAutowiringConfig {

		@Bean
		public HttpInvokerProxyFactoryBean myService() {
			HttpInvokerProxyFactoryBean factory = new HttpInvokerProxyFactoryBean();
			factory.setServiceUrl("/svc/dummy");
			factory.setServiceInterface(MyService.class);
			return factory;
		}
	}

}
