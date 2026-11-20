/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

/**
 * Generic interface to be implemented by resource holders.
 * Allows Spring's transaction infrastructure to introspect
 * and reset the holder when necessary.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see ResourceHolderSupport
 * @see ResourceHolderSynchronization
 */
public interface ResourceHolder {

	/**
	 * Reset the transactional state of this holder.
	 */
	void reset();

	/**
	 * Notify this holder that it has been unbound from transaction synchronization.
	 */
	void unbound();

	/**
	 * Determine whether this holder is considered as 'void',
	 * i.e. as a leftover from a previous thread.
	 */
	boolean isVoid();

}
