/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.mixin;

/**
 * Simple implementation of Lockable interface for use in mixins.
 *
 * @author Rod Johnson
 */
public class DefaultLockable implements Lockable {

	private boolean locked;

	@Override
	public void lock() {
		this.locked = true;
	}

	@Override
	public void unlock() {
		this.locked = false;
	}

	@Override
	public boolean locked() {
		return this.locked;
	}

}
