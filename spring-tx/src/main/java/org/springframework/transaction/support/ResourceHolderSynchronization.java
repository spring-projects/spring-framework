/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.support;

/**
 * {@link TransactionSynchronization} implementation that manages a
 * {@link ResourceHolder} bound through {@link TransactionSynchronizationManager}.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public abstract class ResourceHolderSynchronization<H extends ResourceHolder, K>
		implements TransactionSynchronization {

	private final H resourceHolder;

	private final K resourceKey;

	private volatile boolean holderActive = true;


	/**
	 * Create a new ResourceHolderSynchronization for the given holder.
	 * @param resourceHolder the ResourceHolder to manage
	 * @param resourceKey the key to bind the ResourceHolder for
	 * @see TransactionSynchronizationManager#bindResource
	 */
	public ResourceHolderSynchronization(H resourceHolder, K resourceKey) {
		this.resourceHolder = resourceHolder;
		this.resourceKey = resourceKey;
	}


	@Override
	public void suspend() {
		if (this.holderActive) {
			TransactionSynchronizationManager.unbindResource(this.resourceKey);
		}
	}

	@Override
	public void resume() {
		if (this.holderActive) {
			TransactionSynchronizationManager.bindResource(this.resourceKey, this.resourceHolder);
		}
	}

	@Override
	public void flush() {
		flushResource(this.resourceHolder);
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
		if (shouldUnbindAtCompletion()) {
			TransactionSynchronizationManager.unbindResource(this.resourceKey);
			this.holderActive = false;
			if (shouldReleaseBeforeCompletion()) {
				releaseResource(this.resourceHolder, this.resourceKey);
			}
		}
	}

	@Override
	public void afterCommit() {
		if (!shouldReleaseBeforeCompletion()) {
			processResourceAfterCommit(this.resourceHolder);
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (shouldUnbindAtCompletion()) {
			boolean releaseNecessary = false;
			if (this.holderActive) {
				// The thread-bound resource holder might not be available anymore,
				// since afterCompletion might get called from a different thread.
				this.holderActive = false;
				TransactionSynchronizationManager.unbindResourceIfPossible(this.resourceKey);
				this.resourceHolder.unbound();
				releaseNecessary = true;
			}
			else {
				releaseNecessary = shouldReleaseAfterCompletion(this.resourceHolder);
			}
			if (releaseNecessary) {
				releaseResource(this.resourceHolder, this.resourceKey);
			}
		}
		else {
			// Probably a pre-bound resource...
			cleanupResource(this.resourceHolder, this.resourceKey, (status == STATUS_COMMITTED));
		}
		this.resourceHolder.reset();
	}


	/**
	 * Return whether this holder should be unbound at completion
	 * (or should rather be left bound to the thread after the transaction).
	 * <p>The default implementation returns {@code true}.
	 */
	protected boolean shouldUnbindAtCompletion() {
		return true;
	}

	/**
	 * Return whether this holder's resource should be released before
	 * transaction completion ({@code true}) or rather after
	 * transaction completion ({@code false}).
	 * <p>Note that resources will only be released when they are
	 * unbound from the thread ({@link #shouldUnbindAtCompletion()}).
	 * <p>The default implementation returns {@code true}.
	 * @see #releaseResource
	 */
	protected boolean shouldReleaseBeforeCompletion() {
		return true;
	}

	/**
	 * Return whether this holder's resource should be released after
	 * transaction completion ({@code true}).
	 * <p>The default implementation returns {@code !shouldReleaseBeforeCompletion()},
	 * releasing after completion if no attempt was made before completion.
	 * @see #releaseResource
	 */
	protected boolean shouldReleaseAfterCompletion(H resourceHolder) {
		return !shouldReleaseBeforeCompletion();
	}

	/**
	 * Flush callback for the given resource holder.
	 * @param resourceHolder the resource holder to flush
	 */
	protected void flushResource(H resourceHolder) {
	}

	/**
	 * After-commit callback for the given resource holder.
	 * Only called when the resource hasn't been released yet
	 * ({@link #shouldReleaseBeforeCompletion()}).
	 * @param resourceHolder the resource holder to process
	 */
	protected void processResourceAfterCommit(H resourceHolder) {
	}

	/**
	 * Release the given resource (after it has been unbound from the thread).
	 * @param resourceHolder the resource holder to process
	 * @param resourceKey the key that the ResourceHolder was bound for
	 */
	protected void releaseResource(H resourceHolder, K resourceKey) {
	}

	/**
	 * Perform a cleanup on the given resource (which is left bound to the thread).
	 * @param resourceHolder the resource holder to process
	 * @param resourceKey the key that the ResourceHolder was bound for
	 * @param committed whether the transaction has committed ({@code true})
	 * or rolled back ({@code false})
	 */
	protected void cleanupResource(H resourceHolder, K resourceKey, boolean committed) {
	}

}
