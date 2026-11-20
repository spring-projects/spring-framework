/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.util.List;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Extension of the standard JPA PersistenceUnitInfo interface, for advanced collaboration
 * between Spring's {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * and {@link PersistenceUnitManager} implementations.
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 * @see PersistenceUnitManager
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
 */
public interface SmartPersistenceUnitInfo extends PersistenceUnitInfo {

	/**
	 * Return a list of managed Java packages, to be introspected by the persistence provider.
	 * Typically found through scanning but not exposable through {@link #getManagedClassNames()}.
	 * @return a list of names of managed Java packages (potentially empty)
	 * @since 4.1
	 */
	List<String> getManagedPackages();

	/**
	 * Set the persistence provider's own package name, for exclusion from class transformation.
	 * @see #addTransformer(jakarta.persistence.spi.ClassTransformer)
	 * @see #getNewTempClassLoader()
	 */
	void setPersistenceProviderPackageName(String persistenceProviderPackageName);

	/**
	 * Determine whether this persistence unit is configured for JTA transactions.
	 * <p>This allows for a quick check without referring to the JPA transaction type enum
	 * (primarily for achieving compatibility between JPA 3.2 and 4.0).
	 * @since 7.0
	 * @see jakarta.persistence.PersistenceUnitTransactionType#JTA
	 */
	boolean isConfiguredForJta();

}
