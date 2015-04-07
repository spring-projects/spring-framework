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

package org.springframework.scheduling.annotation;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.stereotype.Repository;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Integration tests cornering bug SPR-8651, which revealed that @Scheduled methods may
 * not work well with beans that have already been proxied for other reasons such
 * as @Transactional or @Async processing.
 *
 * @author Chris Beams
 * @since 3.1
 */
@SuppressWarnings("resource")
public class ScheduledAndTransactionalAnnotationIntegrationTests {

	@Before
	public void setUp() {
		Assume.group(TestGroup.PERFORMANCE);
	}

	@Test
	public void failsWhenJdkProxyAndScheduledMethodNotPresentOnInterface() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class, JdkProxyTxConfig.class, RepoConfigA.class);
		try {
			ctx.refresh();
			fail("expected exception");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getRootCause().getMessage().startsWith("@Scheduled method 'scheduled' found"));
		}
	}

	@Test
	public void succeedsWhenSubclassProxyAndScheduledMethodNotPresentOnInterface() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class, SubclassProxyTxConfig.class, RepoConfigA.class);
		ctx.refresh();

		Thread.sleep(100); // allow @Scheduled method to be called several times

		MyRepository repository = ctx.getBean(MyRepository.class);
		CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
		assertThat("repository is not a proxy", AopUtils.isAopProxy(repository), equalTo(true));
		assertThat("@Scheduled method never called", repository.getInvocationCount(), greaterThan(0));
		assertThat("no transactions were committed", txManager.commits, greaterThan(0));
	}

	@Test
	public void succeedsWhenJdkProxyAndScheduledMethodIsPresentOnInterface() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class, JdkProxyTxConfig.class, RepoConfigB.class);
		ctx.refresh();

		Thread.sleep(50); // allow @Scheduled method to be called several times

		MyRepositoryWithScheduledMethod repository = ctx.getBean(MyRepositoryWithScheduledMethod.class);
		CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
		assertThat("repository is not a proxy", AopUtils.isAopProxy(repository), is(true));
		assertThat("@Scheduled method never called", repository.getInvocationCount(), greaterThan(0));
		assertThat("no transactions were committed", txManager.commits, greaterThan(0));
	}


	@Configuration
	@EnableTransactionManagement
	static class JdkProxyTxConfig { }

	@Configuration
	@EnableTransactionManagement(proxyTargetClass=true)
	static class SubclassProxyTxConfig { }

	@Configuration
	static class RepoConfigA {
		@Bean
		public MyRepository repository() {
			return new MyRepositoryImpl();
		}
	}

	@Configuration
	static class RepoConfigB {
		@Bean
		public MyRepositoryWithScheduledMethod repository() {
			return new MyRepositoryWithScheduledMethodImpl();
		}
	}

	@Configuration
	@EnableScheduling
	static class Config {

		@Bean
		public PersistenceExceptionTranslationPostProcessor peTranslationPostProcessor() {
			return new PersistenceExceptionTranslationPostProcessor();
		}

		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public PersistenceExceptionTranslator peTranslator() {
			PersistenceExceptionTranslator txlator = mock(PersistenceExceptionTranslator.class);
			return txlator;
		}
	}

	public interface MyRepository {
		int getInvocationCount();
	}

	@Repository
	static class MyRepositoryImpl implements MyRepository {

		private final AtomicInteger count = new AtomicInteger(0);

		@Transactional
		@Scheduled(fixedDelay = 5)
		public void scheduled() {
			this.count.incrementAndGet();
		}

		@Override
		public int getInvocationCount() {
			return this.count.get();
		}
	}

	public interface MyRepositoryWithScheduledMethod {
		int getInvocationCount();
		public void scheduled();
	}

	@Repository
	static class MyRepositoryWithScheduledMethodImpl implements MyRepositoryWithScheduledMethod {

		private final AtomicInteger count = new AtomicInteger(0);

		@Override
		@Transactional
		@Scheduled(fixedDelay = 5)
		public void scheduled() {
			this.count.incrementAndGet();
		}

		@Override
		public int getInvocationCount() {
			return this.count.get();
		}
	}

}
