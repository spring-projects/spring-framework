/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.annotation;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for combining {@link Retryable @Retryable} with
 * {@link Transactional @Transactional}.
 *
 * @author Juhwan Lee
 * @since 7.1
 */
class RetryableTransactionTests {

	@Test
	void withTransactionalAnnotation() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		TransactionalAnnotatedBean proxy = ctx.getBean(TransactionalAnnotatedBean.class);
		TransactionalAnnotatedBean target = (TransactionalAnnotatedBean) AopProxyUtils.getSingletonTarget(proxy);
		CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);

		// 2 = 1 initial invocation + 1 retry attempt
		String result = proxy.retryOperation();
		assertThat(result).isEqualTo("result");
		assertThat(target.counter).hasValue(2);
		// 1 rollback for the initial failure, 1 commit for the successful retry
		assertThat(txManager.rollbacks).isEqualTo(1);
		assertThat(txManager.commits).isEqualTo(1);

		ctx.close();
	}


	static class TransactionalAnnotatedBean {

		AtomicInteger counter = new AtomicInteger();

		@Transactional
		@Retryable(maxRetries = 2, delay = 10)
		public String retryOperation() {
			if (counter.incrementAndGet() < 2) {
				throw new IllegalStateException();
			}
			return "result";
		}
	}


	@Configuration
	@EnableTransactionManagement
	@EnableResilientMethods
	static class Config {

		@Bean
		TransactionalAnnotatedBean transactionalAnnotatedBean() {
			return new TransactionalAnnotatedBean();
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new CallCountingTransactionManager();
		}
	}

}
