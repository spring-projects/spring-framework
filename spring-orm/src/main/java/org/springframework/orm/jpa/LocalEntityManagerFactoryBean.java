/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a JPA
 * {@link jakarta.persistence.EntityManagerFactory} according to JPA's standard
 * <i>standalone</i> bootstrap contract. This is the simplest way to set up a
 * shared JPA EntityManagerFactory in a Spring application context; the
 * EntityManagerFactory can then be passed to JPA-based DAOs via
 * dependency injection. Note that switching to a JNDI lookup or to a
 * {@link LocalContainerEntityManagerFactoryBean} definition based on the
 * JPA container contract is just a matter of configuration!
 *
 * <p>Configuration settings are usually read from a {@code META-INF/persistence.xml}
 * config file, residing in the class path, according to the JPA standalone bootstrap
 * contract. See the Java Persistence API specification and your persistence provider
 * documentation for setup details. Additionally, JPA properties can also be added
 * on this FactoryBean via {@link #setJpaProperties}/{@link #setJpaPropertyMap}.
 *
 * <p><b>Note:</b> This FactoryBean has limited configuration power in terms of
 * the configuration that it is able to pass to the JPA provider. If you need
 * more flexible configuration options, consider using Spring's more powerful
 * {@link LocalContainerEntityManagerFactoryBean} instead.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see #setJpaProperties
 * @see #setJpaVendorAdapter
 * @see JpaTransactionManager#setEntityManagerFactory
 * @see LocalContainerEntityManagerFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
 * @see jakarta.persistence.Persistence#createEntityManagerFactory
 * @see jakarta.persistence.spi.PersistenceProvider#createEntityManagerFactory
 */
@SuppressWarnings("serial")
public class LocalEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

	private static final String DATASOURCE_PROPERTY = "jakarta.persistence.dataSource";

	private @Nullable PersistenceConfiguration configuration;


	/**
	 * Create a {@code LocalEntityManagerFactoryBean}.
	 * <p>As of 7.0, This uses "default" for the default persistence unit name.
	 * @see #setPersistenceUnitName
	 * @see #setPersistenceConfiguration
	 */
	public LocalEntityManagerFactoryBean() {
		setPersistenceUnitName(DefaultPersistenceUnitManager.ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME);
	}

	/**
	 * Create a {@code LocalEntityManagerFactoryBean} for the given persistence unit.
	 * @param persistenceUnitName the name of the persistence unit
	 * @since 7.0
	 */
	public LocalEntityManagerFactoryBean(String persistenceUnitName) {
		setPersistenceUnitName(persistenceUnitName);
	}

	/**
	 * Create a {@code LocalEntityManagerFactoryBean} for the given persistence unit.
	 * @param configuration the configuration for the persistence unit
	 * @since 7.0
	 */
	public LocalEntityManagerFactoryBean(PersistenceConfiguration configuration) {
		setPersistenceConfiguration(configuration);
	}


	/**
	 * Set a local JPA 3.2 {@link PersistenceConfiguration} to use for creating
	 * the EntityManagerFactory. This can be a provider-specific subclass such as
	 * {@link org.hibernate.jpa.HibernatePersistenceConfiguration}, exposing a
	 * complete programmatic persistence unit configuration which replaces
	 * {@code persistence.xml} (including provider-specific classpath scanning).
	 * <p>Note: {@link PersistenceConfiguration} includes a persistence unit name,
	 * so this effectively overrides the {@link #setPersistenceUnitName} method.
	 * In contrast, locally specified JPA properties ({@link #setJpaProperties})
	 * will get merged into the given {@code PersistenceConfiguration} instance.
	 * @since 7.0
	 * @see #getPersistenceConfiguration()
	 * @see #getPersistenceUnitName()
	 */
	public void setPersistenceConfiguration(PersistenceConfiguration configuration) {
		Assert.notNull(configuration, "PersistenceConfiguration must not be null");
		this.configuration = configuration;
		setPersistenceUnitName(configuration.name());
	}

	/**
	 * Set a local JPA 3.2 {@link PersistenceConfiguration} to use for creating
	 * the EntityManagerFactory. If none is in use yet, a new plain
	 * {@link PersistenceConfiguration} for the configured persistence unit name
	 * will be created and returned.
	 * @since 7.0
	 * @see #setPersistenceConfiguration
	 * @see #setPersistenceUnitName
	 */
	public PersistenceConfiguration getPersistenceConfiguration() {
		if (this.configuration == null) {
			this.configuration = new PersistenceConfiguration(getPersistenceUnitName());
		}
		return this.configuration;
	}

	@Override
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		Assert.state(this.configuration == null || this.configuration.name().equals(persistenceUnitName),
				"Cannot change setPersistenceUnitName when PersistenceConfiguration has been set");
		super.setPersistenceUnitName(persistenceUnitName);
	}

	/**
	 * Specify the JDBC DataSource that the JPA persistence provider is supposed
	 * to use for accessing the database. This is an alternative to keeping the
	 * JDBC configuration in {@code persistence.xml}, passing in a Spring-managed
	 * DataSource through the "jakarta.persistence.dataSource" property instead.
	 * <p>When configured here, the JDBC DataSource will also get autodetected by
	 * {@link JpaTransactionManager} for exposing JPA transactions to JDBC accessors.
	 * @since 6.2
	 * @see #getJpaPropertyMap()
	 * @see JpaTransactionManager#setDataSource
	 */
	public void setDataSource(@Nullable DataSource dataSource) {
		if (dataSource != null) {
			getJpaPropertyMap().put(DATASOURCE_PROPERTY, dataSource);
		}
		else {
			getJpaPropertyMap().remove(DATASOURCE_PROPERTY);
		}
	}

	/**
	 * Expose the JDBC DataSource from the "jakarta.persistence.dataSource"
	 * property, if any.
	 * @since 6.2
	 * @see #getJpaPropertyMap()
	 */
	@Override
	public @Nullable DataSource getDataSource() {
		return (DataSource) getJpaPropertyMap().get(DATASOURCE_PROPERTY);
	}


	/**
	 * Initialize the EntityManagerFactory for the given configuration.
	 * @throws jakarta.persistence.PersistenceException in case of JPA initialization errors
	 */
	@Override
	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
		if (logger.isDebugEnabled()) {
			logger.debug("Building JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}

		if (this.configuration != null) {
			this.configuration.properties(getJpaPropertyMap());
		}

		PersistenceProvider provider = getPersistenceProvider();
		if (provider != null) {
			// Create EntityManagerFactory directly through PersistenceProvider.
			EntityManagerFactory emf = (this.configuration != null ?
					provider.createEntityManagerFactory(this.configuration) :
					provider.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap()));
			if (emf == null) {
				throw new IllegalStateException(
						"PersistenceProvider [" + provider + "] did not return an EntityManagerFactory for name '" +
						getPersistenceUnitName() + "'");
			}
			return emf;
		}
		else {
			// Let JPA perform its standard PersistenceProvider autodetection.
			return (this.configuration != null ?
					Persistence.createEntityManagerFactory(this.configuration) :
					Persistence.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap()));
		}
	}

}
