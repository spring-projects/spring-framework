/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.dao.support.DaoSupport;
import org.springframework.orm.jpa.JpaTemplate;

/**
 * Convenient super class for JPA data access objects. Intended for
 * JpaTemplate usage. Alternatively, JPA-based DAOs can be coded
 * against the plain JPA EntityManagerFactory/EntityManager API.
 *
 * <p>Requires an EntityManagerFactory or EntityManager to be set,
 * providing a JpaTemplate based on it to subclasses. Can alternatively
 * be initialized directly via a JpaTemplate, to reuse the latter's
 * settings such as the EntityManagerFactory, JpaDialect, flush mode, etc.
 *
 * <p>This class will create its own JpaTemplate if an EntityManagerFactory
 * or EntityManager reference is passed in. A custom JpaTemplate instance
 * can be used through overriding <code>createJpaTemplate</code>.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setEntityManagerFactory
 * @see #setEntityManager
 * @see #createJpaTemplate
 * @see #setJpaTemplate
 * @see org.springframework.orm.jpa.JpaTemplate
 */
public abstract class JpaDaoSupport extends DaoSupport {

	private JpaTemplate jpaTemplate;


	/**
	 * Set the JPA EntityManagerFactory to be used by this DAO.
	 * Will automatically create a JpaTemplate for the given EntityManagerFactory.
	 * @see #createJpaTemplate
	 * @see #setJpaTemplate
	 */
	public final void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		if (this.jpaTemplate == null || entityManagerFactory != this.jpaTemplate.getEntityManagerFactory()) {
		  this.jpaTemplate = createJpaTemplate(entityManagerFactory);
		}
	}

	/**
	 * Create a JpaTemplate for the given EntityManagerFactory.
	 * Only invoked if populating the DAO with a EntityManagerFactory reference!
	 * <p>Can be overridden in subclasses to provide a JpaTemplate instance
	 * with different configuration, or a custom JpaTemplate subclass.
	 * @param entityManagerFactory the JPA EntityManagerFactory to create a JpaTemplate for
	 * @return the new JpaTemplate instance
	 * @see #setEntityManagerFactory
	 */
	protected JpaTemplate createJpaTemplate(EntityManagerFactory entityManagerFactory) {
		return new JpaTemplate(entityManagerFactory);
	}

	/**
	 * Set the JPA EntityManager to be used by this DAO.
	 * Will automatically create a JpaTemplate for the given EntityManager.
	 * @see #createJpaTemplate
	 * @see #setJpaTemplate
	 */
	public final void setEntityManager(EntityManager entityManager) {
	  this.jpaTemplate = createJpaTemplate(entityManager);
	}

	/**
	 * Create a JpaTemplate for the given EntityManager.
	 * Only invoked if populating the DAO with a EntityManager reference!
	 * <p>Can be overridden in subclasses to provide a JpaTemplate instance
	 * with different configuration, or a custom JpaTemplate subclass.
	 * @param entityManager the JPA EntityManager to create a JpaTemplate for
	 * @return the new JpaTemplate instance
	 * @see #setEntityManagerFactory
	 */
	protected JpaTemplate createJpaTemplate(EntityManager entityManager) {
		return new JpaTemplate(entityManager);
	}

	/**
	 * Set the JpaTemplate for this DAO explicitly,
	 * as an alternative to specifying a EntityManagerFactory.
	 * @see #setEntityManagerFactory
	 */
	public final void setJpaTemplate(JpaTemplate jpaTemplate) {
		this.jpaTemplate = jpaTemplate;
	}

	/**
	 * Return the JpaTemplate for this DAO, pre-initialized
	 * with the EntityManagerFactory or set explicitly.
	 */
	public final JpaTemplate getJpaTemplate() {
	  return jpaTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.jpaTemplate == null) {
			throw new IllegalArgumentException("entityManagerFactory or jpaTemplate is required");
		}
	}

}
