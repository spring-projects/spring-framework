/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.config;

import org.springframework.transaction.testfixture.CallCountingTransactionManager;

/**
 * @author Juergen Hoeller
 */
@NoSynch
@SuppressWarnings("serial")
public class NoSynchTransactionManager extends CallCountingTransactionManager {

	public NoSynchTransactionManager() {
		setTransactionSynchronization(CallCountingTransactionManager.SYNCHRONIZATION_NEVER);
	}

}
