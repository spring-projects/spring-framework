/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.persistenceunit;

import javax.persistence.spi.PersistenceUnitInfo;

/**
 * Interface that defines an abstraction for finding and managing
 * JPA PersistenceUnitInfos. Used by
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * in order to obtain a {@link javax.persistence.spi.PersistenceUnitInfo}
 * for building a concrete {@link javax.persistence.EntityManagerFactory}.
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
