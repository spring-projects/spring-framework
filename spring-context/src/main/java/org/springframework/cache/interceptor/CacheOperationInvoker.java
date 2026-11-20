/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import org.jspecify.annotations.Nullable;

/**
 * Abstract the invocation of a cache operation.
 *
 * <p>Does not provide a way to transmit checked exceptions but
 * provides a special exception that should be used to wrap any
 * exception that was thrown by the underlying invocation.
 * Callers are expected to handle this issue type specifically.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface CacheOperationInvoker {

	/**
	 * Invoke the cache operation defined by this instance. Wraps any exception
	 * that is thrown during the invocation in a {@link ThrowableWrapper}.
	 * @return the result of the operation
	 * @throws ThrowableWrapper if an error occurred while invoking the operation
	 */
	@Nullable Object invoke() throws ThrowableWrapper;


	/**
	 * Wrap any exception thrown while invoking {@link #invoke()}.
	 */
	@SuppressWarnings("serial")
	class ThrowableWrapper extends RuntimeException {

		private final Throwable original;

		public ThrowableWrapper(Throwable original) {
			super(original.getMessage(), original);
			this.original = original;
		}

		public Throwable getOriginal() {
			return this.original;
		}
	}

}
