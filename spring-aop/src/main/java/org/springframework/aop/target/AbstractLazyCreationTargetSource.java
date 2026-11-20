/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.TargetSource;

/**
 * {@link org.springframework.aop.TargetSource} implementation that will
 * lazily create a user-managed object.
 *
 * <p>Creation of the lazy target object is controlled by the user by implementing
 * the {@link #createObject()} method. This {@code TargetSource} will invoke
 * this method the first time the proxy is accessed.
 *
 * <p>Useful when you need to pass a reference to some dependency to an object
 * but you don't actually want the dependency to be created until it is first used.
 * A typical scenario for this is a connection to a remote resource.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see #isInitialized()
 * @see #createObject()
 */
public abstract class AbstractLazyCreationTargetSource implements TargetSource {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** The lazily initialized target object. */
	private @Nullable Object lazyTarget;


	/**
	 * Return whether the lazy target object of this TargetSource
	 * has already been fetched.
	 */
	public synchronized boolean isInitialized() {
		return (this.lazyTarget != null);
	}

	/**
	 * This default implementation returns {@code null} if the
	 * target is {@code null} (it is hasn't yet been initialized),
	 * or the target class if the target has already been initialized.
	 * <p>Subclasses may wish to override this method in order to provide
	 * a meaningful value when the target is still {@code null}.
	 * @see #isInitialized()
	 */
	@Override
	public synchronized @Nullable Class<?> getTargetClass() {
		return (this.lazyTarget != null ? this.lazyTarget.getClass() : null);
	}

	/**
	 * Returns the lazy-initialized target object,
	 * creating it on-the-fly if it doesn't exist already.
	 * @see #createObject()
	 */
	@Override
	public synchronized Object getTarget() throws Exception {
		if (this.lazyTarget == null) {
			logger.debug("Initializing lazy target object");
			this.lazyTarget = createObject();
		}
		return this.lazyTarget;
	}


	/**
	 * Subclasses should implement this method to return the lazy initialized object.
	 * Called the first time the proxy is invoked.
	 * @return the created object
	 * @throws Exception if creation failed
	 */
	protected abstract Object createObject() throws Exception;

}
