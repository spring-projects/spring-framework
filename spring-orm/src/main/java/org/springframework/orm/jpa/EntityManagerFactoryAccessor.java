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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Base class for any class that needs to access a JPA {@link EntityManagerFactory},
 * usually in order to obtain a JPA {@link EntityManager}. Defines common properties.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see EntityManagerFactoryUtils
 */
public abstract class EntityManagerFactoryAccessor implements BeanFactoryAware {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private @Nullable EntityManagerFactory entityManagerFactory;

	private @Nullable String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<>();


	/**
	 * Set the JPA EntityManagerFactory that should be used to create
	 * EntityManagers.
	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager()
	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setEntityManagerFactory(@Nullable EntityManagerFactory emf) {
		this.entityManagerFactory = emf;
	}

	/**
	 * Return the JPA EntityManagerFactory that should be used to create
	 * EntityManagers.
	 */
	public @Nullable EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	/**
	 * Obtain the EntityManagerFactory for actual use.
	 * @return the EntityManagerFactory (never {@code null})
	 * @throws IllegalStateException in case of no EntityManagerFactory set
	 * @since 5.0
	 */
	protected final EntityManagerFactory obtainEntityManagerFactory() {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "No EntityManagerFactory set");
		return emf;
	}

	/**
	 * Set the name of the persistence unit to access the EntityManagerFactory for.
	 * <p>This is an alternative to specifying the EntityManagerFactory by direct reference,
	 * resolving it by its persistence unit name instead. If no EntityManagerFactory and
	 * no persistence unit name have been specified, a default EntityManagerFactory will
	 * be retrieved through finding a single unique bean of type EntityManagerFactory.
	 * @see #setEntityManagerFactory
	 */
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * Return the name of the persistence unit to access the EntityManagerFactory for, if any.
	 */
	public @Nullable String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * Specify JPA properties, to be passed into
	 * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
	 * or a "props" element in XML bean definitions.
	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * Specify JPA properties as a Map, to be passed into
	 * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaPropertyMap(@Nullable Map<String, Object> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * Allow {@code Map} access to the JPA properties to be passed to the persistence
	 * provider, with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via {@code jpaPropertyMap[myKey]}.
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * Retrieves an EntityManagerFactory by persistence unit name, if none set explicitly.
	 * Falls back to a default EntityManagerFactory bean if no persistence unit specified.
	 * @see #setPersistenceUnitName
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getEntityManagerFactory() == null) {
			if (!(beanFactory instanceof ListableBeanFactory lbf)) {
				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
						"in a non-listable BeanFactory: " + beanFactory);
			}
			setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(lbf, getPersistenceUnitName()));
		}
	}


	/**
	 * Obtain a new EntityManager from this accessor's EntityManagerFactory.
	 * <p>Can be overridden in subclasses to create specific EntityManager variants.
	 * @return a new EntityManager
	 * @throws IllegalStateException if this accessor is not configured with an EntityManagerFactory
	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager()
	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	protected EntityManager createEntityManager() throws IllegalStateException {
		EntityManagerFactory emf = obtainEntityManagerFactory();
		Map<String, Object> properties = getJpaPropertyMap();
		return (!CollectionUtils.isEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager());
	}

	/**
	 * Obtain the transactional EntityManager for this accessor's EntityManagerFactory, if any.
	 * @return the transactional EntityManager, or {@code null} if none
	 * @throws IllegalStateException if this accessor is not configured with an EntityManagerFactory
	 * @see EntityManagerFactoryUtils#getTransactionalEntityManager(jakarta.persistence.EntityManagerFactory)
	 * @see EntityManagerFactoryUtils#getTransactionalEntityManager(jakarta.persistence.EntityManagerFactory, java.util.Map)
	 */
	protected @Nullable EntityManager getTransactionalEntityManager() throws IllegalStateException{
		EntityManagerFactory emf = obtainEntityManagerFactory();
		return EntityManagerFactoryUtils.getTransactionalEntityManager(emf, getJpaPropertyMap());
	}

}
