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

package org.springframework.aop.target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** The lazily initialized target object */
	private Object lazyTarget;


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
	public synchronized Class<?> getTargetClass() {
		return (this.lazyTarget != null ? this.lazyTarget.getClass() : null);
	}

	public boolean isStatic() {
		return false;
	}

	/**
	 * Returns the lazy-initialized target object,
	 * creating it on-the-fly if it doesn't exist already.
	 * @see #createObject()
	 */
	public synchronized Object getTarget() throws Exception {
		if (this.lazyTarget == null) {
			logger.debug("Initializing lazy target object");
			this.lazyTarget = createObject();
		}
		return this.lazyTarget;
	}

	public void releaseTarget(Object target) throws Exception {
		// nothing to do
	}


	/**
	 * Subclasses should implement this method to return the lazy initialized object.
	 * Called the first time the proxy is invoked.
	 * @return the created object
	 * @throws Exception if creation failed
	 */
	protected abstract Object createObject() throws Exception;

}
