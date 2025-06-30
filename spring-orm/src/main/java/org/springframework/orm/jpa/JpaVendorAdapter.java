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

import java.util.Collections;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.jspecify.annotations.Nullable;

/**
 * SPI interface that allows to plug in vendor-specific behavior
 * into Spring's EntityManagerFactory creators. Serves as single
 * configuration point for all vendor-specific properties.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see AbstractEntityManagerFactoryBean#setJpaVendorAdapter
 */
public interface JpaVendorAdapter {

	/**
	 * Return the vendor-specific persistence provider.
	 */
	PersistenceProvider getPersistenceProvider();

	/**
	 * Return the name of the persistence provider's root package
	 * (for example, "oracle.toplink.essentials"). Will be used for
	 * excluding provider classes from temporary class overriding.
	 * @since 2.5.2
	 */
	default @Nullable String getPersistenceProviderRootPackage() {
		return null;
	}

	/**
	 * Return a Map of vendor-specific JPA properties for the given persistence
	 * unit, typically based on settings in this JpaVendorAdapter instance.
	 * <p>Note that there might be further JPA properties defined on the
	 * EntityManagerFactory bean, which might potentially override individual
	 * JPA property values specified here.
	 * <p>This implementation delegates to {@link #getJpaPropertyMap()} for
	 * non-unit-dependent properties. Effectively, this PersistenceUnitInfo-based
	 * variant only needs to be implemented if there is an actual need to react
	 * to unit-specific characteristics such as the transaction type.
	 * <p><b>NOTE:</b> This variant will only be invoked in case of Jakarta EE style
	 * container bootstrapping where a {@link PersistenceUnitInfo} is present
	 * (i.e. {@link LocalContainerEntityManagerFactoryBean}). In case of simple
	 * Java SE style bootstrapping via {@link jakarta.persistence.Persistence}
	 * (i.e. {@link LocalEntityManagerFactoryBean}), the parameter-less
	 * {@link #getJpaPropertyMap()} variant will be called directly.
	 * @param pui the PersistenceUnitInfo for the current persistence unit
	 * @return a Map of JPA properties, as accepted by the standard JPA bootstrap
	 * facilities, or an empty Map if there are no properties to expose
	 * @since 4.3.13
	 * @see PersistenceUnitInfo#getTransactionType()
	 * @see jakarta.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map)
	 */
	default Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return getJpaPropertyMap();
	}

	/**
	 * Return a Map of vendor-specific JPA properties,
	 * typically based on settings in this JpaVendorAdapter instance.
	 * <p>Note that there might be further JPA properties defined on the
	 * EntityManagerFactory bean, which might potentially override individual
	 * JPA property values specified here.
	 * @return a Map of JPA properties, as accepted by the standard JPA bootstrap
	 * facilities, or an empty Map if there are no properties to expose
	 * @see jakarta.persistence.Persistence#createEntityManagerFactory(String, Map)
	 */
	default Map<String, ?> getJpaPropertyMap() {
		return Collections.emptyMap();
	}

	/**
	 * Return the vendor-specific JpaDialect implementation for this
	 * provider, or {@code null} if there is none.
	 */
	default @Nullable JpaDialect getJpaDialect() {
		return null;
	}

	/**
	 * Return the vendor-specific EntityManagerFactory interface
	 * that the EntityManagerFactory proxy is supposed to implement.
	 * <p>If the provider does not offer any EntityManagerFactory extensions,
	 * the adapter should simply return the standard
	 * {@link jakarta.persistence.EntityManagerFactory} class here.
	 * @since 2.5.2
	 */
	default Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return EntityManagerFactory.class;
	}

	/**
	 * Return the vendor-specific EntityManager interface
	 * that this provider's EntityManagers will implement.
	 * <p>If the provider does not offer any EntityManager extensions,
	 * the adapter should simply return the standard
	 * {@link jakarta.persistence.EntityManager} class here.
	 */
	default Class<? extends EntityManager> getEntityManagerInterface() {
		return EntityManager.class;
	}

	/**
	 * Optional callback for post-processing the native EntityManagerFactory
	 * before active use.
	 * <p>This can be used for triggering vendor-specific initialization processes.
	 * While this is not expected to be used for most providers, it is included
	 * here as a general extension hook.
	 */
	default void postProcessEntityManagerFactory(EntityManagerFactory emf) {
	}

	/**
	 * Optional callback for post-processing the native EntityManager
	 * before active use.
	 * <p>This can be used for setting vendor-specific parameters, for example,
	 * Hibernate filters, on every new EntityManager.
	 * @since 5.3
	 */
	default void postProcessEntityManager(EntityManager em) {
	}

}
