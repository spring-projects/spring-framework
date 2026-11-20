/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

/**
 * Callback interface for post-processing a {@link MutablePersistenceUnitInfo}
 * configuration that Spring prepares for JPA persistence unit bootstrapping.
 *
 * <p>Implementations can be registered with a {@link DefaultPersistenceUnitManager}
 * or via a {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DefaultPersistenceUnitManager#setPersistenceUnitPostProcessors
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitPostProcessors
 */
public interface PersistenceUnitPostProcessor {

	/**
	 * Post-process the given {@link MutablePersistenceUnitInfo},
	 * for example registering further entity classes and jar files.
	 * @param pui the chosen persistence unit configuration, as read from
	 * {@code persistence.xml}. Passed in as MutablePersistenceUnitInfo.
	 */
	void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui);

}
