/*
 * Copyright 2002-2024 the original author or authors.
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
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;

import org.springframework.lang.Nullable;

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
	@Nullable
	public DataSource getDataSource() {
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
		PersistenceProvider provider = getPersistenceProvider();
		if (provider != null) {
			// Create EntityManagerFactory directly through PersistenceProvider.
			EntityManagerFactory emf = provider.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
			if (emf == null) {
				throw new IllegalStateException(
						"PersistenceProvider [" + provider + "] did not return an EntityManagerFactory for name '" +
						getPersistenceUnitName() + "'");
			}
			return emf;
		}
		else {
			// Let JPA perform its standard PersistenceProvider autodetection.
			return Persistence.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
		}
	}

}
