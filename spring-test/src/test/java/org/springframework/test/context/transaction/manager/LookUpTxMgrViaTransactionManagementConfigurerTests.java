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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the behavior requested in
 * <a href="https://jira.spring.io/browse/SPR-9604">SPR-9604</a>.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig
@Transactional
class LookUpTxMgrViaTransactionManagementConfigurerTests {

	@Autowired
	CallCountingTransactionManager txManager1;

	@Autowired
	CallCountingTransactionManager txManager2;


	@Test
	void transactionalTest() {
		assertThat(txManager1.begun).isEqualTo(1);
		assertThat(txManager1.inflight).isEqualTo(1);
		assertThat(txManager1.commits).isEqualTo(0);
		assertThat(txManager1.rollbacks).isEqualTo(0);

		assertThat(txManager2.begun).isEqualTo(0);
		assertThat(txManager2.inflight).isEqualTo(0);
		assertThat(txManager2.commits).isEqualTo(0);
		assertThat(txManager2.rollbacks).isEqualTo(0);
	}

	@AfterTransaction
	void afterTransaction() {
		assertThat(txManager1.begun).isEqualTo(1);
		assertThat(txManager1.inflight).isEqualTo(0);
		assertThat(txManager1.commits).isEqualTo(0);
		assertThat(txManager1.rollbacks).isEqualTo(1);

		assertThat(txManager2.begun).isEqualTo(0);
		assertThat(txManager2.inflight).isEqualTo(0);
		assertThat(txManager2.commits).isEqualTo(0);
		assertThat(txManager2.rollbacks).isEqualTo(0);
	}


	@Configuration
	static class Config implements TransactionManagementConfigurer {

		@Override
		public PlatformTransactionManager annotationDrivenTransactionManager() {
			return txManager1();
		}

		@Bean
		PlatformTransactionManager txManager1() {
			return new CallCountingTransactionManager();
		}

		@Bean
		PlatformTransactionManager txManager2() {
			return new CallCountingTransactionManager();
		}

	}

}
