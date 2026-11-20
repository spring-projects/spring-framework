/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.support;

/**
 * Definition for sorting bean instances by a property.
 *
 * @author Juergen Hoeller
 * @since 26.05.2003
 * @deprecated as severely outdated and superseded by more modern solutions,
 * for example in Spring Data Commons
 */
@Deprecated(since = "7.0.3", forRemoval = true)
public interface SortDefinition {

	/**
	 * Return the name of the bean property to compare.
	 * Can also be a nested bean property path.
	 */
	String getProperty();

	/**
	 * Return whether upper and lower case in String values should be ignored.
	 */
	boolean isIgnoreCase();

	/**
	 * Return whether to sort ascending (true) or descending (false).
	 */
	boolean isAscending();

}
