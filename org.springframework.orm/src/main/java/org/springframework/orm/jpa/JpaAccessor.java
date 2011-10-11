/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.orm.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.support.DataAccessUtils;

/**
 * Base class for JpaTemplate and JpaInterceptor, defining common
 * properties such as EntityManagerFactory and flushing behavior.
 *
 * <p>Not intended to be used directly.
 * See {@link JpaTemplate} and {@link JpaInterceptor}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setEntityManagerFactory
 * @see #setEntityManager
 * @see #setJpaDialect
 * @see #setFlushEager
 * @see JpaTemplate
 * @see JpaInterceptor
 * @see JpaDialect
 * @deprecated as of Spring 3.1, in favor of native EntityManager usage
 * (typically obtained through <code>@PersistenceContext</code>)
 */
@Deprecated
public abstract class JpaAccessor extends EntityManagerFactoryAccessor implements InitializingBean {

	private EntityManager entityManager;

	private JpaDialect jpaDialect = new DefaultJpaDialect();

	private boolean flushEager = false;


	/**
	 * Set the JPA EntityManager to use.
	 */
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * Return the JPA EntityManager to use.
	 */
	public EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * Set the JPA dialect to use for this accessor.
	 * <p>The dialect object can be used to retrieve the underlying JDBC
	 * connection, for example.
	 */
	public void setJpaDialect(JpaDialect jpaDialect) {
		this.jpaDialect = (jpaDialect != null ? jpaDialect : new DefaultJpaDialect());
	}

	/**
	 * Return the JPA dialect to use for this accessor.
	 * <p>Creates a default one for the specified EntityManagerFactory if none set.
	 */
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * Set if this accessor should flush changes to the database eagerly.
	 * <p>Eager flushing leads to immediate synchronization with the database,
	 * even if in a transaction. This causes inconsistencies to show up and throw
	 * a respective exception immediately, and JDBC access code that participates
	 * in the same transaction will see the changes as the database is already
	 * aware of them then. But the drawbacks are:
	 * <ul>
	 * <li>additional communication roundtrips with the database, instead of a
	 * single batch at transaction commit;
	 * <li>the fact that an actual database rollback is needed if the JPA
	 * transaction rolls back (due to already submitted SQL statements).
	 * </ul>
	 */
	public void setFlushEager(boolean flushEager) {
		this.flushEager = flushEager;
	}

	/**
	 * Return if this accessor should flush changes to the database eagerly.
	 */
	public boolean isFlushEager() {
		return this.flushEager;
	}

	/**
	 * Eagerly initialize the JPA dialect, creating a default one
	 * for the specified EntityManagerFactory if none set.
	 */
	public void afterPropertiesSet() {
		EntityManagerFactory emf = getEntityManagerFactory();
		if (emf == null && getEntityManager() == null) {
			throw new IllegalArgumentException("'entityManagerFactory' or 'entityManager' is required");
		}
		if (emf instanceof EntityManagerFactoryInfo) {
			JpaDialect jpaDialect = ((EntityManagerFactoryInfo) emf).getJpaDialect();
			if (jpaDialect != null) {
				setJpaDialect(jpaDialect);
			}
		}
	}


	/**
	 * Flush the given JPA entity manager if necessary.
	 * @param em the current JPA PersistenceManage
	 * @param existingTransaction if executing within an existing transaction
	 * @throws javax.persistence.PersistenceException in case of JPA flushing errors
	 */
	protected void flushIfNecessary(EntityManager em, boolean existingTransaction) throws PersistenceException {
		if (isFlushEager()) {
			logger.debug("Eagerly flushing JPA entity manager");
			em.flush();
		}
	}

	/**
	 * Convert the given runtime exception to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy if necessary, or
	 * return the exception itself if it is not persistence related
	 * <p>Default implementation delegates to the JpaDialect.
	 * May be overridden in subclasses.
	 * @param ex runtime exception that occured, which may or may not
	 * be JPA-related
	 * @return the corresponding DataAccessException instance if
	 * wrapping should occur, otherwise the raw exception
	 * @see org.springframework.dao.support.DataAccessUtils#translateIfNecessary
	 */
	public RuntimeException translateIfNecessary(RuntimeException ex) {
		return DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
	}

}
