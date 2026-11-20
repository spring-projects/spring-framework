/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target.dynamic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

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

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	protected @Nullable Object targetObject;

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
	public synchronized @Nullable Class<?> getTargetClass() {
		if (this.targetObject == null) {
			refresh();
		}
		return (this.targetObject != null ? this.targetObject.getClass() : null);
	}

	@Override
	public final synchronized @Nullable Object getTarget() {
		if ((refreshCheckDelayElapsed() && requiresRefresh()) || this.targetObject == null) {
			refresh();
		}
		return this.targetObject;
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
