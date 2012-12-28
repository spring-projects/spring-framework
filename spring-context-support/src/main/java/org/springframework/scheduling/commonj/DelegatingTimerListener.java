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

package org.springframework.scheduling.commonj;

import commonj.timers.Timer;
import commonj.timers.TimerListener;

import org.springframework.util.Assert;

/**
 * Simple TimerListener adapter that delegates to a given Runnable.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see commonj.timers.TimerListener
 * @see java.lang.Runnable
 */
public class DelegatingTimerListener implements TimerListener {

	private final Runnable runnable;


	/**
	 * Create a new DelegatingTimerListener.
	 * @param runnable the Runnable implementation to delegate to
	 */
	public DelegatingTimerListener(Runnable runnable) {
		Assert.notNull(runnable, "Runnable is required");
		this.runnable = runnable;
	}


	/**
	 * Delegates execution to the underlying Runnable.
	 */
	@Override
	public void timerExpired(Timer timer) {
		this.runnable.run();
	}

}
