/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jdbc.support.lob;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Helper class for registering a transaction synchronization for closing
 * a LobCreator, preferring Spring transaction synchronization and falling
 * back to plain JTA transaction synchronization.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SpringLobCreatorSynchronization
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @see JtaLobCreatorSynchronization
 * @see javax.transaction.Transaction#registerSynchronization
 */
public abstract class LobCreatorUtils {

	private static final Log logger = LogFactory.getLog(LobCreatorUtils.class);


	/**
	 * Register a transaction synchronization for closing the given LobCreator,
	 * preferring Spring transaction synchronization and falling back to
	 * plain JTA transaction synchronization.
	 * @param lobCreator the LobCreator to close after transaction completion
	 * @param jtaTransactionManager the JTA TransactionManager to fall back to
	 * when no Spring transaction synchronization is active (may be {@code null})
	 * @throws IllegalStateException if there is neither active Spring transaction
	 * synchronization nor active JTA transaction synchronization
	 */
	public static void registerTransactionSynchronization(
			LobCreator lobCreator, TransactionManager jtaTransactionManager) throws IllegalStateException {

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering Spring transaction synchronization for LobCreator");
			TransactionSynchronizationManager.registerSynchronization(
				new SpringLobCreatorSynchronization(lobCreator));
		}
		else {
			if (jtaTransactionManager != null) {
				try {
					int jtaStatus = jtaTransactionManager.getStatus();
					if (jtaStatus == Status.STATUS_ACTIVE || jtaStatus == Status.STATUS_MARKED_ROLLBACK) {
						logger.debug("Registering JTA transaction synchronization for LobCreator");
						jtaTransactionManager.getTransaction().registerSynchronization(
								new JtaLobCreatorSynchronization(lobCreator));
						return;
					}
				}
				catch (Throwable ex) {
					throw new TransactionSystemException(
							"Could not register synchronization with JTA TransactionManager", ex);
				}
			}
			throw new IllegalStateException("Active Spring transaction synchronization or active " +
				"JTA transaction with specified [javax.transaction.TransactionManager] required");
		}
	}

}
