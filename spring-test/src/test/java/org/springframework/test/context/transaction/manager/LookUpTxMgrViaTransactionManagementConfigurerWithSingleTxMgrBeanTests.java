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

package org.springframework.test.context.transaction.manager;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the behavior for transaction manager lookups
 * when only one transaction manager is configured as a bean in the application
 * context and a non-bean transaction manager is configured via the
 * {@link TransactionManagementConfigurer} API.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@SpringJUnitConfig
@Transactional
class LookUpTxMgrViaTransactionManagementConfigurerWithSingleTxMgrBeanTests {

	@Autowired
	CallCountingTransactionManager txManager;

	@Autowired
	Config config;


	@Test
	void transactionalTest() {
		assertThat(txManager.begun).isEqualTo(0);
		assertThat(txManager.inflight).isEqualTo(0);
		assertThat(txManager.commits).isEqualTo(0);
		assertThat(txManager.rollbacks).isEqualTo(0);

		CallCountingTransactionManager annotationDriven = config.annotationDriven;
		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.inflight).isEqualTo(1);
		assertThat(annotationDriven.commits).isEqualTo(0);
		assertThat(annotationDriven.rollbacks).isEqualTo(0);
	}

	@AfterTransaction
	void afterTransaction() {
		assertThat(txManager.begun).isEqualTo(0);
		assertThat(txManager.inflight).isEqualTo(0);
		assertThat(txManager.commits).isEqualTo(0);
		assertThat(txManager.rollbacks).isEqualTo(0);

		CallCountingTransactionManager annotationDriven = config.annotationDriven;
		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.inflight).isEqualTo(0);
		assertThat(annotationDriven.commits).isEqualTo(0);
		assertThat(annotationDriven.rollbacks).isEqualTo(1);
	}


	@Configuration
	static class Config implements TransactionManagementConfigurer {

		final CallCountingTransactionManager annotationDriven = new CallCountingTransactionManager();

		@Bean
		TransactionManager txManager() {
			return new CallCountingTransactionManager();
		}

		@Override
		public TransactionManager annotationDrivenTransactionManager() {
			return annotationDriven;
		}

	}

}
