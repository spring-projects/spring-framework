/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

/**
 * Interface for objects that may participate in a phased
 * process such as lifecycle management.
 *
 * @author Mark Fisher
 * @since 3.0
 * @see SmartLifecycle
 */
public interface Phased {

	/**
	 * Return the phase value of this object.
	 */
	int getPhase();

}
