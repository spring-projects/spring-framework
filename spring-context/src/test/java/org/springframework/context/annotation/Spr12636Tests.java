/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class Spr12636Tests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void orderOnImplementation() {
		this.context = new AnnotationConfigApplicationContext(
				UserServiceTwo.class, UserServiceOne.class, UserServiceCollector.class);
		UserServiceCollector bean = this.context.getBean(UserServiceCollector.class);
		assertThat(bean.userServices).containsExactly(
				context.getBean("serviceOne", UserService.class),
				context.getBean("serviceTwo", UserService.class));

	}

	@Test
	void orderOnImplementationWithProxy() {
		this.context = new AnnotationConfigApplicationContext(
				UserServiceTwo.class, UserServiceOne.class, UserServiceCollector.class, AsyncConfig.class);

		// Validate those beans are indeed wrapped by a proxy
		UserService serviceOne = this.context.getBean("serviceOne", UserService.class);
		UserService serviceTwo = this.context.getBean("serviceTwo", UserService.class);
		assertThat(AopUtils.isAopProxy(serviceOne)).isTrue();
		assertThat(AopUtils.isAopProxy(serviceTwo)).isTrue();

		UserServiceCollector bean = this.context.getBean(UserServiceCollector.class);
		assertThat(bean.userServices).containsExactly(serviceOne, serviceTwo);
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
