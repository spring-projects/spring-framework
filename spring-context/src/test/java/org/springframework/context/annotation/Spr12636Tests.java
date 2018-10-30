/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class Spr12636Tests {

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void orderOnImplementation() {
		this.context = new AnnotationConfigApplicationContext(
				UserServiceTwo.class, UserServiceOne.class, UserServiceCollector.class);
		UserServiceCollector bean = this.context.getBean(UserServiceCollector.class);
		assertSame(context.getBean("serviceOne", UserService.class), bean.userServices.get(0));
		assertSame(context.getBean("serviceTwo", UserService.class), bean.userServices.get(1));

	}

	@Test
	public void orderOnImplementationWithProxy() {
		this.context = new AnnotationConfigApplicationContext(
				UserServiceTwo.class, UserServiceOne.class, UserServiceCollector.class, AsyncConfig.class);

		// Validate those beans are indeed wrapped by a proxy
		UserService serviceOne = this.context.getBean("serviceOne", UserService.class);
		UserService serviceTwo = this.context.getBean("serviceTwo", UserService.class);
		assertTrue(AopUtils.isAopProxy(serviceOne));
		assertTrue(AopUtils.isAopProxy(serviceTwo));

		UserServiceCollector bean = this.context.getBean(UserServiceCollector.class);
		assertSame(serviceOne, bean.userServices.get(0));
		assertSame(serviceTwo, bean.userServices.get(1));
	}

	@Configuration
	@EnableAsync
	static class AsyncConfig {
	}


	@Component
	static class UserServiceCollector {

		public final List<UserService> userServices;

		@Autowired
		UserServiceCollector(List<UserService> userServices) {
			this.userServices = userServices;
		}
	}

	interface UserService {

		void doIt();
	}

	@Component("serviceOne")
	@Order(1)
	static class UserServiceOne implements UserService {

		@Async
		@Override
		public void doIt() {

		}
	}

	@Component("serviceTwo")
	@Order(2)
	static class UserServiceTwo implements UserService {

		@Async
		@Override
		public void doIt() {

		}
	}
}
