/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture;

/**
 * This interface can be implemented by cacheable objects or cache entries,
 * to enable the freshness of objects to be checked.
 *
 * @author Rod Johnson
 */
public interface TimeStamped {

	/**
	 * Return the timestamp for this object.
	 * @return long the timestamp for this object,
	 * as returned by System.currentTimeMillis()
	 */
	long getTimeStamp();

}
