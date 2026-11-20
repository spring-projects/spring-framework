/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test;

/**
 * A simple marker interface used to identify an event or an event listener
 *
 * @author Stephane Nicoll
 */
public interface Identifiable {

	/**
	 * Return a unique global id used to identify this instance.
	 */
	String getId();

}
