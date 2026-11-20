/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

/**
 * Strategy interface to filter the list of persistent managed types to include
 * in the persistence unit. Only class names that match the filter are managed.
 *
 * @author Stephane Nicoll
 * @since 6.1.4
 * @see DefaultPersistenceUnitManager#setManagedClassNameFilter
 */
@FunctionalInterface
public interface ManagedClassNameFilter {

	/**
	 * Test if the given class name matches the filter.
	 * @param className the fully qualified class name of the persistent type to test
	 * @return {@code true} if the class name matches
	 */
	boolean matches(String className);

}
