/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.jta;

import java.util.List;
import javax.transaction.Status;
import javax.transaction.Synchronization;

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
			case Status.STATUS_COMMITTED:
				try {
					TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
				}
				finally {
					TransactionSynchronizationUtils.invokeAfterCompletion(
							this.synchronizations, TransactionSynchronization.STATUS_COMMITTED);
				}
				break;
			case Status.STATUS_ROLLEDBACK:
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
				break;
			default:
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
		}
	}
}
