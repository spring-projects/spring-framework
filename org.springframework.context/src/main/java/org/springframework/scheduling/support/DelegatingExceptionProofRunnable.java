/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.scheduling.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Runnable wrapper that catches any exception or error thrown
 * from its delegate Runnable. Used for continuing scheduled
 * execution even after an exception thrown from a task's Runnable.
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 */
public class DelegatingExceptionProofRunnable implements Runnable {

	private static final Log logger = LogFactory.getLog(DelegatingExceptionProofRunnable.class);

	private final Runnable delegate;

	private final boolean propagateException;


	/**
	 * Create a new DelegatingExceptionProofRunnable that logs the exception
	 * but isn't propagating it (in order to continue scheduled execution).
	 * @param delegate the Runnable implementation to delegate to
	 */
	public DelegatingExceptionProofRunnable(Runnable delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		this.propagateException = false;
	}

	/**
	 * Create a new DelegatingExceptionProofRunnable.
	 * @param delegate the Runnable implementation to delegate to
	 * @param propagateException whether to propagate the exception after logging
	 * (note: this will typically cancel scheduled execution of the runnable)
	 */
	public DelegatingExceptionProofRunnable(Runnable delegate, boolean propagateException) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		this.propagateException = propagateException;
	}

	/**
	 * Return the wrapped Runnable implementation.
	 */
	public final Runnable getDelegate() {
		return this.delegate;
	}


	public void run() {
		try {
			this.delegate.run();
		}
		catch (Throwable ex) {
			logger.error("Unexpected exception thrown from Runnable: " + this.delegate, ex);
			if (this.propagateException) {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
		}
	}

}
