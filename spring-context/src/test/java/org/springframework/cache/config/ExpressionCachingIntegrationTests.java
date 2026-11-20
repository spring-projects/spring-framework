/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.core.ParameterizedTypeReference;

/**
 * @author Stephane Nicoll
 * @author Yanming Zhou
 */
class ExpressionCachingIntegrationTests {

	@Test  // SPR-11692
	void expressionIsCacheBasedOnActualMethod() {
		ConfigurableApplicationContext context =
				new AnnotationConfigApplicationContext(SharedConfig.class, Spr11692Config.class);

		BaseDao<User> userDao = context.getBean("userDao", new ParameterizedTypeReference<>() {});
		BaseDao<Order> orderDao = context.getBean("orderDao", new ParameterizedTypeReference<>() {});

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
