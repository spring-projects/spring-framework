/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling.timer;

import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Simple {@link java.util.TimerTask} adapter that delegates to a
 * given {@link java.lang.Runnable}.
 *
 * <p>This is often preferable to deriving from TimerTask, to be able to
 * implement an interface rather than extend an abstract base class.
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @deprecated as of Spring 3.0, in favor of the <code>scheduling.concurrent</code>
 * package which is based on Java 5's <code>java.util.concurrent.ExecutorService</code>
 */
@Deprecated
public class DelegatingTimerTask extends TimerTask {

	private static final Log logger = LogFactory.getLog(DelegatingTimerTask.class);

	private final Runnable delegate;


	/**
	 * Create a new DelegatingTimerTask.
	 * @param delegate the Runnable implementation to delegate to
	 */
	public DelegatingTimerTask(Runnable delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Return the wrapped Runnable implementation.
	 */
	public final Runnable getDelegate() {
		return this.delegate;
	}


	/**
	 * Delegates execution to the underlying Runnable, catching any exception
	 * or error thrown in order to continue scheduled execution.
	 */
	@Override
	public void run() {
		try {
			this.delegate.run();
		}
		catch (Throwable ex) {
			logger.error("Unexpected exception thrown from Runnable: " + this.delegate, ex);
			// Do not throw the exception, else the main loop of the Timer might stop!
		}
	}

}
