/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.orm.jpa.support;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.EntityManagerPlus;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} that exposes a shared JPA {@link javax.persistence.EntityManager}
 * reference for a given EntityManagerFactory. Typically used for an EntityManagerFactory
 * created by {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean},
 * as direct alternative to a JNDI lookup for a Java EE 5 server's EntityManager reference.
 *
 * <p>The shared EntityManager will behave just like an EntityManager fetched from an
 * application server's JNDI environment, as defined by the JPA specification.
 * It will delegate all calls to the current transactional EntityManager, if any;
 * otherwise, it will fall back to a newly created EntityManager per operation.
 *
 * <p>Can be passed to DAOs that expect a shared EntityManager reference rather than an
 * EntityManagerFactory. Note that Spring's {@link org.springframework.orm.jpa.JpaTransactionManager}
 * always needs an EntityManagerFactory in order to create new transactional EntityManager instances.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setEntityManagerFactory
 * @see #setEntityManagerInterface
 * @see org.springframework.orm.jpa.LocalEntityManagerFactoryBean
 * @see org.springframework.orm.jpa.JpaTransactionManager
 */
public class SharedEntityManagerBean extends EntityManagerFactoryAccessor
		implements FactoryBean<EntityManager>, InitializingBean {

	private Class<? extends EntityManager> entityManagerInterface;

	private boolean synchronizedWithTransaction = true;

	private EntityManager shared;


	/**
	 * Specify the EntityManager interface to expose.
	 * <p>Default is the EntityManager interface as defined by the
	 * EntityManagerFactoryInfo, if available. Else, the standard
	 * {@code javax.persistence.EntityManager} interface will be used.
	 * @see org.springframework.orm.jpa.EntityManagerFactoryInfo#getEntityManagerInterface()
	 * @see javax.persistence.EntityManager
	 */
	public void setEntityManagerInterface(Class<? extends EntityManager> entityManagerInterface) {
		Assert.notNull(entityManagerInterface, "'entityManagerInterface' must not be null");
		Assert.isAssignable(EntityManager.class, entityManagerInterface);
		this.entityManagerInterface = entityManagerInterface;
	}

	/**
	 * Set whether to automatically join ongoing transactions (according
	 * to the JPA 2.1 SynchronizationType rules). Default is "true".
	 */
	public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
		this.synchronizedWithTransaction = synchronizedWithTransaction;
	}


	public final void afterPropertiesSet() {
		EntityManagerFactory emf = getEntityManagerFactory();
		if (emf == null) {
			throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
		}
		Class[] ifcs = null;
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = emfInfo.getEntityManagerInterface();
				if (this.entityManagerInterface == null) {
					this.entityManagerInterface = EntityManager.class;
				}
			}
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null && jpaDialect.supportsEntityManagerPlusOperations()) {
				ifcs = new Class[] {this.entityManagerInterface, EntityManagerPlus.class};
			}
			else {
				ifcs = new Class[] {this.entityManagerInterface};
			}
		}
		else {
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = EntityManager.class;
			}
			ifcs = new Class[] {this.entityManagerInterface};
		}
		this.shared = SharedEntityManagerCreator.createSharedEntityManager(
				emf, getJpaPropertyMap(), this.synchronizedWithTransaction, ifcs);
	}


	public EntityManager getObject() {
		return this.shared;
	}

	public Class<? extends EntityManager> getObjectType() {
		return (this.entityManagerInterface != null ? this.entityManagerInterface : EntityManager.class);
	}

	public boolean isSingleton() {
		return true;
	}

}
