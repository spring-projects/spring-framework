/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support class for throttling concurrent access to a specific resource.
 *
 * <p>Designed for use as a base class, with the subclass invoking
 * the {@link #beforeAccess()} and {@link #afterAccess()} methods at
 * appropriate points of its workflow. Note that {@code afterAccess}
 * should usually be called in a {@code finally} block!
 *
 * <p>The default concurrency limit of this support class is -1
 * ("unbounded concurrency"). Subclasses may override this default;
 * check the javadoc of the concrete class that you're using.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see #setConcurrencyLimit
 * @see #beforeAccess()
 * @see #afterAccess()
 * @see org.springframework.aop.interceptor.ConcurrencyThrottleInterceptor
 * @see java.io.Serializable
 */
@SuppressWarnings("serial")
public abstract class ConcurrencyThrottleSupport implements Serializable {

	/**
	 * Permit any number of concurrent invocations: that is, don't throttle concurrency.
	 */
	public static final int UNBOUNDED_CONCURRENCY = -1;

	/**
	 * Switch concurrency 'off': that is, don't allow any concurrent invocations.
	 */
	public static final int NO_CONCURRENCY = 0;


	/** Transient to optimize serialization. */
	protected transient Log logger = LogFactory.getLog(getClass());

	private final Lock concurrencyLock = new ReentrantLock();

	private final Condition concurrencyCondition = this.concurrencyLock.newCondition();

	private int concurrencyLimit = UNBOUNDED_CONCURRENCY;

	private int concurrencyCount = 0;


	/**
	 * Set the maximum number of concurrent access attempts allowed.
	 * The default of -1 indicates no concurrency limit at all.
	 * <p>In principle, this limit can be changed at runtime,
	 * although it is generally designed as a config time setting.
	 * <p>NOTE: Do not switch between -1 and any concrete limit at runtime,
	 * as this will lead to inconsistent concurrency counts: A limit
	 * of -1 effectively turns off concurrency counting completely.
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyLimit = concurrencyLimit;
	}

	/**
	 * Return the maximum number of concurrent access attempts allowed.
	 */
	public int getConcurrencyLimit() {
		return this.concurrencyLimit;
	}

	/**
	 * Return whether this throttle is currently active.
	 * @return {@code true} if the concurrency limit for this instance is active
	 * @see #getConcurrencyLimit()
	 */
	public boolean isThrottleActive() {
		return (this.concurrencyLimit >= 0);
	}


	/**
	 * To be invoked before the main execution logic of concrete subclasses.
	 * <p>This implementation applies the concurrency throttle.
	 * @see #onLimitReached()
	 * @see #afterAccess()
	 */
	protected void beforeAccess() {
		if (this.concurrencyLimit == NO_CONCURRENCY) {
			throw new IllegalStateException(
					"Currently no invocations allowed - concurrency limit set to NO_CONCURRENCY");
		}
		if (this.concurrencyLimit > 0) {
			this.concurrencyLock.lock();
			try {
				if (this.concurrencyCount >= this.concurrencyLimit) {
					onLimitReached();
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Entering throttle at concurrency count " + this.concurrencyCount);
				}
				this.concurrencyCount++;
			}
			finally {
				this.concurrencyLock.unlock();
			}
		}
	}

	/**
	 * Triggered by {@link #beforeAccess()} when the concurrency limit has been reached.
	 * The default implementation blocks until the concurrency count allows for entering.
	 * @since 6.2.6
	 */
	protected void onLimitReached() {
		boolean interrupted = false;
		while (this.concurrencyCount >= this.concurrencyLimit) {
			if (interrupted) {
				throw new IllegalStateException("Thread was interrupted while waiting for invocation access, " +
						"but concurrency limit still does not allow for entering");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Concurrency count " + this.concurrencyCount +
						" has reached limit " + this.concurrencyLimit + " - blocking");
			}
			try {
				this.concurrencyCondition.await();
			}
			catch (InterruptedException ex) {
				// Re-interrupt current thread, to allow other threads to react.
				Thread.currentThread().interrupt();
				interrupted = true;
			}
		}
	}

	/**
	 * To be invoked after the main execution logic of concrete subclasses.
	 * @see #beforeAccess()
	 */
	protected void afterAccess() {
		if (this.concurrencyLimit >= 0) {
			boolean debug = logger.isDebugEnabled();
			this.concurrencyLock.lock();
			try {
				this.concurrencyCount--;
				if (debug) {
					logger.debug("Returning from throttle at concurrency count " + this.concurrencyCount);
				}
				this.concurrencyCondition.signal();
			}
			finally {
				this.concurrencyLock.unlock();
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization, just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.logger = LogFactory.getLog(getClass());
	}

}
