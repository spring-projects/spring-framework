/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction.manager;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the behavior requested in
 * <a href="https://jira.spring.io/browse/SPR-9645">SPR-9645</a>.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig
class LookUpTxMgrNonTransactionalTests {

	@Autowired
	CallCountingTransactionManager txManager;


	@Test
	void nonTransactionalTest() {
		assertThat(txManager.begun).isEqualTo(0);
		assertThat(txManager.inflight).isEqualTo(0);
		assertThat(txManager.commits).isEqualTo(0);
		assertThat(txManager.rollbacks).isEqualTo(0);
	}


	@Configuration
	static class Config {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new CallCountingTransactionManager();
		}

	}

}
