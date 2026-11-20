/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
