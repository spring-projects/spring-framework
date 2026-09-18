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
