/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.net.URL;
import java.util.List;

import jakarta.persistence.spi.PersistenceUnitInfo;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Provide the list of managed persistent types that an entity manager should
 * consider.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface PersistenceManagedTypes {

	/**
	 * Return the class names the persistence provider must add to its set of
	 * managed classes.
	 * @return the managed class names
	 * @see PersistenceUnitInfo#getManagedClassNames()
	 */
	List<String> getManagedClassNames();

	/**
	 * Return a list of managed Java packages, to be introspected by the
	 * persistence provider.
	 * @return the managed packages
	 */
	List<String> getManagedPackages();

	/**
	 * Return the persistence unit root url or {@code null} if it could not be
	 * determined.
	 * @return the persistence unit root url
	 * @see PersistenceUnitInfo#getPersistenceUnitRootUrl()
	 */
	@Nullable URL getPersistenceUnitRootUrl();


	/**
	 * Create an instance using the specified managed class names.
	 * @param managedClassNames the managed class names
	 * @return a {@link PersistenceManagedTypes}
	 */
	static PersistenceManagedTypes of(String... managedClassNames) {
		Assert.notNull(managedClassNames, "'managedClassNames' must not be null");
		return new SimplePersistenceManagedTypes(List.of(managedClassNames), List.of());
	}

	/**
	 * Create an instance using the specified managed class names and packages.
	 * @param managedClassNames the managed class names
	 * @param managedPackages the managed packages
	 * @return a {@link PersistenceManagedTypes}
	 */
	static PersistenceManagedTypes of(List<String> managedClassNames, List<String> managedPackages) {
		return new SimplePersistenceManagedTypes(managedClassNames, managedPackages);
	}

}
