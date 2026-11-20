/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.net.URL;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A simple {@link PersistenceManagedTypes} implementation that holds the list
 * of managed entities.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
class SimplePersistenceManagedTypes implements PersistenceManagedTypes {

	private final List<String> managedClassNames;

	private final List<String> managedPackages;

	private final @Nullable URL persistenceUnitRootUrl;


	SimplePersistenceManagedTypes(List<String> managedClassNames, List<String> managedPackages,
			@Nullable URL persistenceUnitRootUrl) {

		this.managedClassNames = managedClassNames;
		this.managedPackages = managedPackages;
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	SimplePersistenceManagedTypes(List<String> managedClassNames, List<String> managedPackages) {
		this(managedClassNames, managedPackages, null);
	}


	@Override
	public List<String> getManagedClassNames() {
		return this.managedClassNames;
	}

	@Override
	public List<String> getManagedPackages() {
		return this.managedPackages;
	}

	@Override
	public @Nullable URL getPersistenceUnitRootUrl() {
		return this.persistenceUnitRootUrl;
	}

}
