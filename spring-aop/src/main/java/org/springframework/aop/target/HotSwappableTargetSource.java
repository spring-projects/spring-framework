/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.TargetSource;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.aop.TargetSource} implementation that
 * caches a local target object, but allows the target to be swapped
 * while the application is running.
 *
 * <p>If configuring an object of this class in a Spring IoC container,
 * use constructor injection.
 *
 * <p>This TargetSource is serializable if the target is at the time
 * of serialization.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class HotSwappableTargetSource implements TargetSource, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 7497929212653839187L;


	/** The current target object. */
	@SuppressWarnings("serial")
	private Object target;


	/**
	 * Create a new HotSwappableTargetSource with the given initial target object.
	 * @param initialTarget the initial target object
	 */
	public HotSwappableTargetSource(Object initialTarget) {
		Assert.notNull(initialTarget, "Target object must not be null");
		this.target = initialTarget;
	}


	/**
	 * Return the type of the current target object.
	 * <p>The returned type should usually be constant across all target objects.
	 */
	@Override
	public synchronized Class<?> getTargetClass() {
		return this.target.getClass();
	}

	@Override
	public synchronized Object getTarget() {
		return this.target;
	}


	/**
	 * Swap the target, returning the old target object.
	 * @param newTarget the new target object
	 * @return the old target object
	 * @throws IllegalArgumentException if the new target is invalid
	 */
	public synchronized Object swap(Object newTarget) throws IllegalArgumentException {
		Assert.notNull(newTarget, "Target object must not be null");
		Object old = this.target;
		this.target = newTarget;
		return old;
	}


	/**
	 * Two HotSwappableTargetSources are equal if the current target objects are equal.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof HotSwappableTargetSource that &&
				this.target.equals(that.target)));
	}

	@Override
	public int hashCode() {
		return HotSwappableTargetSource.class.hashCode();
	}

	@Override
	public String toString() {
		return "HotSwappableTargetSource for target: " + this.target;
	}

}
