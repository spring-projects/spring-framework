/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cache.config;

import org.junit.jupiter.api.Test;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Stephane Nicoll
 */
class ExpressionCachingIntegrationTests {

	@Test  // SPR-11692
	@SuppressWarnings("unchecked")
	public void expressionIsCacheBasedOnActualMethod() {
		ConfigurableApplicationContext context =
				new AnnotationConfigApplicationContext(SharedConfig.class, Spr11692Config.class);

		BaseDao<User> userDao = (BaseDao<User>) context.getBean("userDao");
		BaseDao<Order> orderDao = (BaseDao<Order>) context.getBean("orderDao");

		userDao.persist(new User("1"));
		orderDao.persist(new Order("2"));

		context.close();
	}


	@Configuration
	static class Spr11692Config {

		@Bean
		public BaseDao<User> userDao() {
			return new UserDaoImpl();
		}

		@Bean
		public BaseDao<Order> orderDao() {
			return new OrderDaoImpl();
		}
	}


	private interface BaseDao<T> {

		T persist(T t);
	}


	private static class UserDaoImpl implements BaseDao<User> {

		@Override
		@CachePut(value = "users", key = "#user.id")
		public User persist(User user) {
			return user;
		}
	}


	private static class OrderDaoImpl implements BaseDao<Order> {

		@Override
		@CachePut(value = "orders", key = "#order.id")
		public Order persist(Order order) {
			return order;
		}
	}


	private static class User {

		private final String id;

		public User(String id) {
			this.id = id;
		}

		@SuppressWarnings("unused")
		public String getId() {
			return this.id;
		}
	}


	private static class Order {

		private final String id;

		public Order(String id) {
			this.id = id;
		}

		@SuppressWarnings("unused")
		public String getId() {
			return this.id;
		}
	}


	@Configuration
	@EnableCaching
	static class SharedConfig implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}
	}

}
