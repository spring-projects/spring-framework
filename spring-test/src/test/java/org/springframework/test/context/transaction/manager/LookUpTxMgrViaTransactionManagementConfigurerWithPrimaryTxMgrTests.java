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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the behavior for transaction manager lookups
 * when one transaction manager is {@link Primary @Primary} and an additional
 * transaction manager is configured via the
 * {@link TransactionManagementConfigurer} API.
 *
 * @author Sam Brannen
 * @since 5.2.6
 */
@SpringJUnitConfig
@Transactional
class LookUpTxMgrViaTransactionManagementConfigurerWithPrimaryTxMgrTests {

	@Autowired
	CallCountingTransactionManager primary;

	@Autowired
	@Qualifier("annotationDrivenTransactionManager")
	CallCountingTransactionManager annotationDriven;


	@Test
	void transactionalTest() {
		assertThat(primary.begun).isEqualTo(0);
		assertThat(primary.inflight).isEqualTo(0);
		assertThat(primary.commits).isEqualTo(0);
		assertThat(primary.rollbacks).isEqualTo(0);

		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.inflight).isEqualTo(1);
		assertThat(annotationDriven.commits).isEqualTo(0);
		assertThat(annotationDriven.rollbacks).isEqualTo(0);
	}

	@AfterTransaction
	void afterTransaction() {
		assertThat(primary.begun).isEqualTo(0);
		assertThat(primary.inflight).isEqualTo(0);
		assertThat(primary.commits).isEqualTo(0);
		assertThat(primary.rollbacks).isEqualTo(0);

		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.inflight).isEqualTo(0);
		assertThat(annotationDriven.commits).isEqualTo(0);
		assertThat(annotationDriven.rollbacks).isEqualTo(1);
	}


	@Configuration
	static class Config implements TransactionManagementConfigurer {

		@Bean
		@Primary
		PlatformTransactionManager primary() {
			return new CallCountingTransactionManager();
		}

		@Bean
		@Override
		public TransactionManager annotationDrivenTransactionManager() {
			return new CallCountingTransactionManager();
		}

	}

}
