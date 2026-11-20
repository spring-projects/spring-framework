/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

import org.springframework.core.Ordered;

/**
 * Simple {@link TransactionSynchronization} adapter containing empty
 * method implementations, for easier overriding of single methods.
 *
 * <p>Also implements the {@link Ordered} interface to enable the execution
 * order of synchronizations to be controlled declaratively. The default
 * {@link #getOrder() order} is {@link Ordered#LOWEST_PRECEDENCE}, indicating
 * late execution; return a lower value for earlier execution.
 *
 * @author Juergen Hoeller
 * @since 22.01.2004
 * @deprecated as of 5.3, in favor of the default methods on the
 * {@link TransactionSynchronization} interface
 */
@Deprecated(since = "5.3")
public abstract class TransactionSynchronizationAdapter implements TransactionSynchronization, Ordered {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void suspend() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCommit() {
	}

	@Override
	public void afterCompletion(int status) {
	}

}
