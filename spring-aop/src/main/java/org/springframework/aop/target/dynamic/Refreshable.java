/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target.dynamic;

/**
 * Interface to be implemented by dynamic target objects,
 * which support reloading and optionally polling for updates.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 2.0
 */
public interface Refreshable {

	/**
	 * Refresh the underlying target object.
	 */
	void refresh();

	/**
	 * Return the number of actual refreshes since startup.
	 */
	long getRefreshCount();

	/**
	 * Return the last time an actual refresh happened (as timestamp).
	 */
	long getLastRefreshTime();

}
