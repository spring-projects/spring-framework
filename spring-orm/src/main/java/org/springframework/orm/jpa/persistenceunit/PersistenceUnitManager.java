/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Interface that defines an abstraction for finding and managing
 * JPA PersistenceUnitInfos. Used by
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * in order to obtain a {@link jakarta.persistence.spi.PersistenceUnitInfo}
 * for building a concrete {@link jakarta.persistence.EntityManagerFactory}.
 *
 * <p>Obtaining a PersistenceUnitInfo instance is an exclusive process.
 * A PersistenceUnitInfo instance is not available for further calls
 * anymore once it has been obtained.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DefaultPersistenceUnitManager
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitManager
 */
public interface PersistenceUnitManager {

	/**
	 * Obtain the default PersistenceUnitInfo from this manager.
	 * @return the PersistenceUnitInfo (never {@code null})
	 * @throws IllegalStateException if there is no default PersistenceUnitInfo defined
	 * or it has already been obtained
	 */
	PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException;

	/**
	 * Obtain the specified PersistenceUnitInfo from this manager.
	 * @param persistenceUnitName the name of the desired persistence unit
	 * @return the PersistenceUnitInfo (never {@code null})
	 * @throws IllegalArgumentException if no PersistenceUnitInfo with the given
	 * name is defined
	 * @throws IllegalStateException if the PersistenceUnitInfo with the given
	 * name has already been obtained
	 */
	PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName)
			throws IllegalArgumentException, IllegalStateException;

}
