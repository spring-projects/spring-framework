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

package org.springframework.aop.target.dynamic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;

/**
 * Abstract {@link org.springframework.aop.TargetSource} implementation that
 * wraps a refreshable target object. Subclasses can determine whether a
 * refresh is required, and need to provide fresh target objects.
 *
 * <p>Implements the {@link Refreshable} interface in order to allow for
 * explicit control over the refresh status.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see #requiresRefresh()
 * @see #freshTarget()
 */
public abstract class AbstractRefreshableTargetSource implements TargetSource, Refreshable {

	/** Logger available to subclasses */
	protected Log logger = LogFactory.getLog(getClass());

	protected Object targetObject;

	private long refreshCheckDelay = -1;

	private long lastRefreshCheck = -1;

	private long lastRefreshTime = -1;

	private long refreshCount = 0;


	/**
	 * Set the delay between refresh checks, in milliseconds.
	 * Default is -1, indicating no refresh checks at all.
	 * <p>Note that an actual refresh will only happen when
	 * {@link #requiresRefresh()} returns {@code true}.
	 */
	public void setRefreshCheckDelay(long refreshCheckDelay) {
		this.refreshCheckDelay = refreshCheckDelay;
	}


	@Override
	public synchronized Class<?> getTargetClass() {
		if (this.targetObject == null) {
			refresh();
		}
		return this.targetObject.getClass();
	}

	/**
	 * Not static.
	 */
	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public final synchronized Object getTarget() {
		if ((refreshCheckDelayElapsed() && requiresRefresh()) || this.targetObject == null) {
			refresh();
		}
		return this.targetObject;
	}

	/**
	 * No need to release target.
	 */
	@Override
	public void releaseTarget(Object object) {
	}


	@Override
	public final synchronized void refresh() {
		logger.debug("Attempting to refresh target");

		this.targetObject = freshTarget();
		this.refreshCount++;
		this.lastRefreshTime = System.currentTimeMillis();

		logger.debug("Target refreshed successfully");
	}

	@Override
	public synchronized long getRefreshCount() {
		return this.refreshCount;
	}

	@Override
	public synchronized long getLastRefreshTime() {
		return this.lastRefreshTime;
	}


	private boolean refreshCheckDelayElapsed() {
		if (this.refreshCheckDelay < 0) {
			return false;
		}

		long currentTimeMillis = System.currentTimeMillis();

		if (this.lastRefreshCheck < 0 || currentTimeMillis - this.lastRefreshCheck > this.refreshCheckDelay) {
			// Going to perform a refresh check - update the timestamp.
			this.lastRefreshCheck = currentTimeMillis;
			logger.debug("Refresh check delay elapsed - checking whether refresh is required");
			return true;
		}

		return false;
	}


	/**
	 * Determine whether a refresh is required.
	 * Invoked for each refresh check, after the refresh check delay has elapsed.
	 * <p>The default implementation always returns {@code true}, triggering
	 * a refresh every time the delay has elapsed. To be overridden by subclasses
	 * with an appropriate check of the underlying target resource.
	 * @return whether a refresh is required
	 */
	protected boolean requiresRefresh() {
		return true;
	}

	/**
	 * Obtain a fresh target object.
	 * <p>Only invoked if a refresh check has found that a refresh is required
	 * (that is, {@link #requiresRefresh()} has returned {@code true}).
	 * @return the fresh target object
	 */
	protected abstract Object freshTarget();

}
