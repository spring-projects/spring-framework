/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

/**
 * @author Juergen Hoeller
 */
@Configuration
public class TransactionManagerConfiguration {

	@Bean
	@Qualifier("synch")
	public PlatformTransactionManager transactionManager1() {
		return new CallCountingTransactionManager();
	}

	@Bean
	@NoSynch
	public PlatformTransactionManager transactionManager2() {
		CallCountingTransactionManager tm = new CallCountingTransactionManager();
		tm.setTransactionSynchronization(CallCountingTransactionManager.SYNCHRONIZATION_NEVER);
		return tm;
	}

}
