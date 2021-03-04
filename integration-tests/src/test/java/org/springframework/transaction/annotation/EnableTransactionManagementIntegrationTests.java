/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for the @EnableTransactionManagement annotation.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("resource")
class EnableTransactionManagementIntegrationTests {

	@Test
	void repositoryIsNotTxProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

		assertThat(isTxProxy(ctx.getBean(FooRepository.class))).isFalse();
	}

	@Test
	void repositoryIsTxProxy_withDefaultTxManagerName() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, DefaultTxManagerNameConfig.class);

		assertTxProxying(ctx);
	}

	@Test
	void repositoryIsTxProxy_withCustomTxManagerName() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, CustomTxManagerNameConfig.class);

		assertTxProxying(ctx);
	}

	@Test
	void repositoryIsTxProxy_withNonConventionalTxManagerName_fallsBackToByTypeLookup() {
		assertTxProxying(new AnnotationConfigApplicationContext(Config.class, NonConventionalTxManagerNameConfig.class));
	}

	@Test
	void repositoryIsClassBasedTxProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, ProxyTargetClassTxConfig.class);

		assertTxProxying(ctx);
		assertThat(AopUtils.isCglibProxy(ctx.getBean(FooRepository.class))).isTrue();
	}

	@Test
	void repositoryUsesAspectJAdviceMode() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class, AspectJTxConfig.class);
		// this test is a bit fragile, but gets the job done, proving that an
		// attempt was made to look up the AJ aspect. It's due to classpath issues
		// in .integration-tests that it's not found.
		assertThatExceptionOfType(Exception.class)
			.isThrownBy(ctx::refresh)
			.withMessageContaining("AspectJJtaTransactionManagementConfiguration");
	}

	@Test
	void implicitTxManager() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImplicitTxManagerConfig.class);

		FooRepository fooRepository = ctx.getBean(FooRepository.class);
		fooRepository.findAll();

		CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
		assertThat(txManager.begun).isEqualTo(1);
		assertThat(txManager.commits).isEqualTo(1);
		assertThat(txManager.rollbacks).isEqualTo(0);
	}

	@Test
	void explicitTxManager() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ExplicitTxManagerConfig.class);

		FooRepository fooRepository = ctx.getBean(FooRepository.class);
		fooRepository.findAll();

		CallCountingTransactionManager txManager1 = ctx.getBean("txManager1", CallCountingTransactionManager.class);
		assertThat(txManager1.begun).isEqualTo(1);
		assertThat(txManager1.commits).isEqualTo(1);
		assertThat(txManager1.rollbacks).isEqualTo(0);

		CallCountingTransactionManager txManager2 = ctx.getBean("txManager2", CallCountingTransactionManager.class);
		assertThat(txManager2.begun).isEqualTo(0);
		assertThat(txManager2.commits).isEqualTo(0);
		assertThat(txManager2.rollbacks).isEqualTo(0);
	}

	@Test
	void apcEscalation() {
		new AnnotationConfigApplicationContext(EnableTxAndCachingConfig.class);
	}


	private void assertTxProxying(AnnotationConfigApplicationContext ctx) {
		FooRepository repo = ctx.getBean(FooRepository.class);
		assertThat(isTxProxy(repo)).isTrue();
		// trigger a transaction
		repo.findAll();
	}

	private boolean isTxProxy(FooRepository repo) {
		if (!AopUtils.isAopProxy(repo)) {
			return false;
		}
		return Arrays.stream(((Advised) repo).getAdvisors())
				.anyMatch(BeanFactoryTransactionAttributeSourceAdvisor.class::isInstance);
	}


	@Configuration
	@EnableTransactionManagement
	@ImportResource("org/springframework/transaction/annotation/enable-caching.xml")
	static class EnableTxAndCachingConfig {

		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public FooRepository fooRepository() {
			return new DummyFooRepository();
		}

		@Bean
		public CacheManager cacheManager() {
			SimpleCacheManager mgr = new SimpleCacheManager();
			ArrayList<Cache> caches = new ArrayList<>();
			caches.add(new ConcurrentMapCache(""));
			mgr.setCaches(caches);
			return mgr;
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class ImplicitTxManagerConfig {

		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public FooRepository fooRepository() {
			return new DummyFooRepository();
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class ExplicitTxManagerConfig implements TransactionManagementConfigurer {

		@Bean
		public PlatformTransactionManager txManager1() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public PlatformTransactionManager txManager2() {
			return new CallCountingTransactionManager();
		}

		@Override
		public PlatformTransactionManager annotationDrivenTransactionManager() {
			return txManager1();
		}

		@Bean
		public FooRepository fooRepository() {
			return new DummyFooRepository();
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class DefaultTxManagerNameConfig {

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class CustomTxManagerNameConfig {

		@Bean
		PlatformTransactionManager txManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class NonConventionalTxManagerNameConfig {

		@Bean
		PlatformTransactionManager txManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}


	@Configuration
	@EnableTransactionManagement(proxyTargetClass=true)
	static class ProxyTargetClassTxConfig {

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}


	@Configuration
	@EnableTransactionManagement(mode=AdviceMode.ASPECTJ)
	static class AspectJTxConfig {

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}


	@Configuration
	static class Config {

		@Bean
		FooRepository fooRepository() {
			JdbcFooRepository repos = new JdbcFooRepository();
			repos.setDataSource(dataSource());
			return repos;
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.HSQL)
				.build();
		}
	}


	interface FooRepository {

		List<Object> findAll();
	}


	@Repository
	static class JdbcFooRepository implements FooRepository {

		public void setDataSource(DataSource dataSource) {
		}

		@Override
		@Transactional
		public List<Object> findAll() {
			return Collections.emptyList();
		}
	}


	@Repository
	static class DummyFooRepository implements FooRepository {

		@Override
		@Transactional
		public List<Object> findAll() {
			return Collections.emptyList();
		}
	}

}
