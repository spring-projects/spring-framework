/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.util.Assert;

/**
 * Extension of {@link CompletableFuture} which allows for cancelling
 * a delegate along with the {@link CompletableFuture} itself.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @param <T> the result type returned by this Future's {@code get} method
 */
class DelegatingCompletableFuture<T> extends CompletableFuture<T> {

	private final Future<T> delegate;


	public DelegatingCompletableFuture(Future<T> delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean result = this.delegate.cancel(mayInterruptIfRunning);
		super.cancel(mayInterruptIfRunning);
		return result;
	}

}
