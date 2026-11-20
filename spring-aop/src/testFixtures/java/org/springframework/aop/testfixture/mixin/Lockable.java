/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.mixin;

/**
 * Simple interface to use for mixins.
 *
 * @author Rod Johnson
 */
public interface Lockable {

	void lock();

	void unlock();

	boolean locked();
}
