/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.jta;

import java.util.List;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * Adapter for a JTA Synchronization, invoking the {@code afterCommit} /
 * {@code afterCompletion} callbacks of Spring {@link TransactionSynchronization}
 * objects callbacks after the outer JTA transaction has completed.
 * Applied when participating in an existing (non-Spring) JTA transaction.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see TransactionSynchronization#afterCommit
 * @see TransactionSynchronization#afterCompletion
 */
public class JtaAfterCompletionSynchronization implements Synchronization {

	private final List<TransactionSynchronization> synchronizations;


	/**
	 * Create a new JtaAfterCompletionSynchronization for the given synchronization objects.
	 * @param synchronizations the List of TransactionSynchronization objects
	 * @see org.springframework.transaction.support.TransactionSynchronization
	 */
	public JtaAfterCompletionSynchronization(List<TransactionSynchronization> synchronizations) {
		this.synchronizations = synchronizations;
	}


	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCompletion(int status) {
		switch (status) {
			case Status.STATUS_COMMITTED -> {
				try {
					TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
				}
				finally {
					TransactionSynchronizationUtils.invokeAfterCompletion(
							this.synchronizations, TransactionSynchronization.STATUS_COMMITTED);
				}
			}
			case Status.STATUS_ROLLEDBACK -> {
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
			}
			default -> {
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
			}
		}
	}
}
