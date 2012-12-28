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

package org.springframework.orm.hibernate3;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Filter;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.criterion.DetachedCriteria;

import org.springframework.dao.DataAccessException;

/**
 * Interface that specifies a basic set of Hibernate operations,
 * implemented by {@link HibernateTemplate}. Not often used, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Defines {@code HibernateTemplate}'s data access methods that
 * mirror various {@link org.hibernate.Session} methods. Users are
 * strongly encouraged to read the Hibernate {@code Session} javadocs
 * for details on the semantics of those methods.
 *
 * <p>Note that operations that return an {@link java.util.Iterator} (i.e.
 * {@code iterate(..)}) are supposed to be used within Spring-driven
 * or JTA-driven transactions (with {@link HibernateTransactionManager},
 * {@link org.springframework.transaction.jta.JtaTransactionManager},
 * or EJB CMT). Else, the {@code Iterator} won't be able to read
 * results from its {@link java.sql.ResultSet} anymore, as the underlying
 * Hibernate {@code Session} will already have been closed.
 *
 * <p>Note that lazy loading will just work with an open Hibernate
 * {@code Session}, either within a transaction or within
 * {@link org.springframework.orm.hibernate3.support.OpenSessionInViewFilter}/
 * {@link org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor}.
 * Furthermore, some operations just make sense within transactions,
 * for example: {@code contains}, {@code evict}, {@code lock},
 * {@code flush}, {@code clear}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see HibernateTemplate
 * @see org.hibernate.Session
 * @see HibernateTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewFilter
 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
 */
public interface HibernateOperations {

	/**
	 * Execute the action specified by the given action object within a
	 * {@link org.hibernate.Session}.
	 * <p>Application exceptions thrown by the action object get propagated
	 * to the caller (can only be unchecked). Hibernate exceptions are
	 * transformed into appropriate DAO ones. Allows for returning a result
	 * object, that is a domain object or a collection of domain objects.
	 * <p>Note: Callback code is not supposed to handle transactions itself!
	 * Use an appropriate transaction manager like
	 * {@link HibernateTransactionManager}. Generally, callback code must not
	 * touch any {@code Session} lifecycle methods, like close,
	 * disconnect, or reconnect, to let the template do its work.
	 * @param action callback object that specifies the Hibernate action
	 * @return a result object returned by the action, or {@code null}
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see HibernateTransactionManager
	 * @see org.hibernate.Session
	 */
	<T> T execute(HibernateCallback<T> action) throws DataAccessException;

	/**
	 * Execute the specified action assuming that the result object is a
	 * {@link List}.
	 * <p>This is a convenience method for executing Hibernate find calls or
	 * queries within an action.
	 * @param action calback object that specifies the Hibernate action
	 * @return a List result returned by the action, or {@code null}
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 */
	List executeFind(HibernateCallback<?> action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for loading individual objects
	//-------------------------------------------------------------------------

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(Class, java.io.Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance, or {@code null} if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, java.io.Serializable)
	 */
	<T> T get(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * <p>Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(Class, java.io.Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance, or {@code null} if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, java.io.Serializable, org.hibernate.LockMode)
	 */
	<T> T get(Class<T> entityClass, Serializable id, LockMode lockMode)
			throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(String, java.io.Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance, or {@code null} if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, java.io.Serializable)
	 */
	Object get(String entityName, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(String, java.io.Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance, or {@code null} if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, java.io.Serializable, org.hibernate.LockMode)
	 */
	Object get(String entityName, Serializable id, LockMode lockMode)
			throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(Class, java.io.Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, java.io.Serializable)
	 */
	<T> T load(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(Class, java.io.Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, java.io.Serializable)
	 */
	<T> T load(Class<T> entityClass, Serializable id, LockMode lockMode)
			throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(String, java.io.Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, java.io.Serializable)
	 */
	Object load(String entityName, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * <p>Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(String, java.io.Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, java.io.Serializable)
	 */
	Object load(String entityName, Serializable id, LockMode lockMode)
			throws DataAccessException;

	/**
	 * Return all persistent instances of the given entity class.
	 * Note: Use queries or criteria for retrieving a specific subset.
	 * @param entityClass a persistent class
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException if there is a Hibernate error
	 * @see org.hibernate.Session#createCriteria
	 */
	<T>List<T> loadAll(Class<T> entityClass) throws DataAccessException;

	/**
	 * Load the persistent instance with the given identifier
	 * into the given object, throwing an exception if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(Object, java.io.Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entity the object (of the target class) to load into
	 * @param id the identifier of the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Object, java.io.Serializable)
	 */
	void load(Object entity, Serializable id) throws DataAccessException;

	/**
	 * Re-read the state of the given persistent instance.
	 * @param entity the persistent instance to re-read
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#refresh(Object)
	 */
	void refresh(Object entity) throws DataAccessException;

	/**
	 * Re-read the state of the given persistent instance.
	 * Obtains the specified lock mode for the instance.
	 * @param entity the persistent instance to re-read
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#refresh(Object, org.hibernate.LockMode)
	 */
	void refresh(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Check whether the given object is in the Session cache.
	 * @param entity the persistence instance to check
	 * @return whether the given object is in the Session cache
	 * @throws org.springframework.dao.DataAccessException if there is a Hibernate error
	 * @see org.hibernate.Session#contains
	 */
	boolean contains(Object entity) throws DataAccessException;

	/**
	 * Remove the given object from the {@link org.hibernate.Session} cache.
	 * @param entity the persistent instance to evict
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#evict
	 */
	void evict(Object entity) throws DataAccessException;

	/**
	 * Force initialization of a Hibernate proxy or persistent collection.
	 * @param proxy a proxy for a persistent object or a persistent collection
	 * @throws DataAccessException if we can't initialize the proxy, for example
	 * because it is not associated with an active Session
	 * @see org.hibernate.Hibernate#initialize
	 */
	void initialize(Object proxy) throws DataAccessException;

	/**
	 * Return an enabled Hibernate {@link Filter} for the given filter name.
	 * The returned {@code Filter} instance can be used to set filter parameters.
	 * @param filterName the name of the filter
	 * @return the enabled Hibernate {@code Filter} (either already
	 * enabled or enabled on the fly by this operation)
	 * @throws IllegalStateException if we are not running within a
	 * transactional Session (in which case this operation does not make sense)
	 */
	Filter enableFilter(String filterName) throws IllegalStateException;


	//-------------------------------------------------------------------------
	// Convenience methods for storing individual objects
	//-------------------------------------------------------------------------

	/**
	 * Obtain the specified lock level upon the given object, implicitly
	 * checking whether the corresponding database entry still exists.
	 * @param entity the persistent instance to lock
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#lock(Object, org.hibernate.LockMode)
	 */
	void lock(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Obtain the specified lock level upon the given object, implicitly
	 * checking whether the corresponding database entry still exists.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to lock
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#lock(String, Object, org.hibernate.LockMode)
	 */
	void lock(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Persist the given transient instance.
	 * @param entity the transient instance to persist
	 * @return the generated identifier
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#save(Object)
	 */
	Serializable save(Object entity) throws DataAccessException;

	/**
	 * Persist the given transient instance.
	 * @param entityName the name of the persistent entity
	 * @param entity the transient instance to persist
	 * @return the generated identifier
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#save(String, Object)
	 */
	Serializable save(String entityName, Object entity) throws DataAccessException;

	/**
	 * Update the given persistent instance,
	 * associating it with the current Hibernate {@link org.hibernate.Session}.
	 * @param entity the persistent instance to update
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#update(Object)
	 */
	void update(Object entity) throws DataAccessException;

	/**
	 * Update the given persistent instance,
	 * associating it with the current Hibernate {@link org.hibernate.Session}.
	 * <p>Obtains the specified lock mode if the instance exists, implicitly
	 * checking whether the corresponding database entry still exists.
	 * @param entity the persistent instance to update
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#update(Object)
	 */
	void update(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Update the given persistent instance,
	 * associating it with the current Hibernate {@link org.hibernate.Session}.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to update
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#update(String, Object)
	 */
	void update(String entityName, Object entity) throws DataAccessException;

	/**
	 * Update the given persistent instance,
	 * associating it with the current Hibernate {@link org.hibernate.Session}.
	 * <p>Obtains the specified lock mode if the instance exists, implicitly
	 * checking whether the corresponding database entry still exists.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to update
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#update(String, Object)
	 */
	void update(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Save or update the given persistent instance,
	 * according to its id (matching the configured "unsaved-value"?).
	 * Associates the instance with the current Hibernate {@link org.hibernate.Session}.
	 * @param entity the persistent instance to save or update
	 * (to be associated with the Hibernate {@code Session})
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#saveOrUpdate(Object)
	 */
	void saveOrUpdate(Object entity) throws DataAccessException;

	/**
	 * Save or update the given persistent instance,
	 * according to its id (matching the configured "unsaved-value"?).
	 * Associates the instance with the current Hibernate {@code Session}.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to save or update
	 * (to be associated with the Hibernate {@code Session})
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#saveOrUpdate(String, Object)
	 */
	void saveOrUpdate(String entityName, Object entity) throws DataAccessException;

	/**
	 * Save or update all given persistent instances,
	 * according to its id (matching the configured "unsaved-value"?).
	 * Associates the instances with the current Hibernate {@code Session}.
	 * @param entities the persistent instances to save or update
	 * (to be associated with the Hibernate {@code Session})
	 * @throws DataAccessException in case of Hibernate errors
	 * @deprecated as of Spring 2.5, in favor of individual
	 * {@code saveOrUpdate} or {@code merge} usage
	 */
	@Deprecated
	void saveOrUpdateAll(Collection entities) throws DataAccessException;

	/**
	 * Persist the state of the given detached instance according to the
	 * given replication mode, reusing the current identifier value.
	 * @param entity the persistent object to replicate
	 * @param replicationMode the Hibernate ReplicationMode
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#replicate(Object, org.hibernate.ReplicationMode)
	 */
	void replicate(Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * Persist the state of the given detached instance according to the
	 * given replication mode, reusing the current identifier value.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent object to replicate
	 * @param replicationMode the Hibernate ReplicationMode
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#replicate(String, Object, org.hibernate.ReplicationMode)
	 */
	void replicate(String entityName, Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * Persist the given transient instance. Follows JSR-220 semantics.
	 * <p>Similar to {@code save}, associating the given object
	 * with the current Hibernate {@link org.hibernate.Session}.
	 * @param entity the persistent instance to persist
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#persist(Object)
	 * @see #save
	 */
	void persist(Object entity) throws DataAccessException;

	/**
	 * Persist the given transient instance. Follows JSR-220 semantics.
	 * <p>Similar to {@code save}, associating the given object
	 * with the current Hibernate {@link org.hibernate.Session}.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to persist
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#persist(String, Object)
	 * @see #save
	 */
	void persist(String entityName, Object entity) throws DataAccessException;

	/**
	 * Copy the state of the given object onto the persistent object
	 * with the same identifier. Follows JSR-220 semantics.
	 * <p>Similar to {@code saveOrUpdate}, but never associates the given
	 * object with the current Hibernate Session. In case of a new entity,
	 * the state will be copied over as well.
	 * <p>Note that {@code merge} will <i>not</i> update the identifiers
	 * in the passed-in object graph (in contrast to TopLink)! Consider
	 * registering Spring's {@code IdTransferringMergeEventListener} if
	 * you would like to have newly assigned ids transferred to the original
	 * object graph too.
	 * @param entity the object to merge with the corresponding persistence instance
	 * @return the updated, registered persistent instance
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#merge(Object)
	 * @see #saveOrUpdate
	 * @see org.springframework.orm.hibernate3.support.IdTransferringMergeEventListener
	 */
	<T> T merge(T entity) throws DataAccessException;

	/**
	 * Copy the state of the given object onto the persistent object
	 * with the same identifier. Follows JSR-220 semantics.
	 * <p>Similar to {@code saveOrUpdate}, but never associates the given
	 * object with the current Hibernate {@link org.hibernate.Session}. In
	 * the case of a new entity, the state will be copied over as well.
	 * <p>Note that {@code merge} will <i>not</i> update the identifiers
	 * in the passed-in object graph (in contrast to TopLink)! Consider
	 * registering Spring's {@code IdTransferringMergeEventListener}
	 * if you would like to have newly assigned ids transferred to the
	 * original object graph too.
	 * @param entityName the name of the persistent entity
	 * @param entity the object to merge with the corresponding persistence instance
	 * @return the updated, registered persistent instance
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#merge(String, Object)
	 * @see #saveOrUpdate
	 */
	<T> T merge(String entityName, T entity) throws DataAccessException;

	/**
	 * Delete the given persistent instance.
	 * @param entity the persistent instance to delete
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(Object entity) throws DataAccessException;

	/**
	 * Delete the given persistent instance.
	 * <p>Obtains the specified lock mode if the instance exists, implicitly
	 * checking whether the corresponding database entry still exists.
	 * @param entity the persistent instance to delete
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Delete the given persistent instance.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to delete
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(String entityName, Object entity) throws DataAccessException;

	/**
	 * Delete the given persistent instance.
	 * <p>Obtains the specified lock mode if the instance exists, implicitly
	 * checking whether the corresponding database entry still exists.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to delete
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Delete all given persistent instances.
	 * <p>This can be combined with any of the find methods to delete by query
	 * in two lines of code.
	 * @param entities the persistent instances to delete
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void deleteAll(Collection entities) throws DataAccessException;

	/**
	 * Flush all pending saves, updates and deletes to the database.
	 * <p>Only invoke this for selective eager flushing, for example when
	 * JDBC code needs to see certain changes within the same transaction.
	 * Else, it is preferable to rely on auto-flushing at transaction
	 * completion.
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#flush
	 */
	void flush() throws DataAccessException;

	/**
	 * Remove all objects from the {@link org.hibernate.Session} cache, and
	 * cancel all pending saves, updates and deletes.
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#clear
	 */
	void clear() throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for HQL strings
	//-------------------------------------------------------------------------

	/**
	 * Execute an HQL query.
	 * @param queryString a query expressed in Hibernate's query language
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 */
	List find(String queryString) throws DataAccessException;

	/**
	 * Execute an HQL query, binding one value to a "?" parameter in the
	 * query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param value the value of the parameter
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 */
	List find(String queryString, Object value) throws DataAccessException;

	/**
	 * Execute an HQL query, binding a number of values to "?" parameters
	 * in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 */
	List find(String queryString, Object... values) throws DataAccessException;

	/**
	 * Execute an HQL query, binding one value to a ":" named parameter
	 * in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedParam(String queryString, String paramName, Object value)
			throws DataAccessException;

	/**
	 * Execute an HQL query, binding a number of values to ":" named
	 * parameters in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param paramNames the names of the parameters
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedParam(String queryString, String[] paramNames, Object[] values)
			throws DataAccessException;

	/**
	 * Execute an HQL query, binding the properties of the given bean to
	 * <i>named</i> parameters in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param valueBean the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Query#setProperties
	 * @see org.hibernate.Session#createQuery
	 */
	List findByValueBean(String queryString, Object valueBean) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for named queries
	//-------------------------------------------------------------------------

	/**
	 * Execute a named query.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedQuery(String queryName) throws DataAccessException;

	/**
	 * Execute a named query, binding one value to a "?" parameter in
	 * the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param value the value of the parameter
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedQuery(String queryName, Object value) throws DataAccessException;

	/**
	 * Execute a named query binding a number of values to "?" parameters
	 * in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedQuery(String queryName, Object... values) throws DataAccessException;

	/**
	 * Execute a named query, binding one value to a ":" named parameter
	 * in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param paramName the name of parameter
	 * @param value the value of the parameter
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
			throws DataAccessException;

	/**
	 * Execute a named query, binding a number of values to ":" named
	 * parameters in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param paramNames the names of the parameters
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedQueryAndNamedParam(String queryName, String[] paramNames, Object[] values)
			throws DataAccessException;

	/**
	 * Execute a named query, binding the properties of the given bean to
	 * ":" named parameters in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param valueBean the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Query#setProperties
	 * @see org.hibernate.Session#getNamedQuery(String)
	 */
	List findByNamedQueryAndValueBean(String queryName, Object valueBean)
			throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for detached criteria
	//-------------------------------------------------------------------------

	/**
	 * Execute a query based on a given Hibernate criteria object.
	 * @param criteria the detached Hibernate criteria object.
	 * <b>Note: Do not reuse criteria objects! They need to recreated per execution,
	 * due to the suboptimal design of Hibernate's criteria facility.</b>
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.DetachedCriteria#getExecutableCriteria(org.hibernate.Session)
	 */
	List findByCriteria(DetachedCriteria criteria) throws DataAccessException;

	/**
	 * Execute a query based on the given Hibernate criteria object.
	 * @param criteria the detached Hibernate criteria object.
	 * <b>Note: Do not reuse criteria objects! They need to recreated per execution,
	 * due to the suboptimal design of Hibernate's criteria facility.</b>
	 * @param firstResult the index of the first result object to be retrieved
	 * (numbered from 0)
	 * @param maxResults the maximum number of result objects to retrieve
	 * (or <=0 for no limit)
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.DetachedCriteria#getExecutableCriteria(org.hibernate.Session)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	List findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults) throws DataAccessException;

	/**
	 * Execute a query based on the given example entity object.
	 * @param exampleEntity an instance of the desired entity,
	 * serving as example for "query-by-example"
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 */
	List findByExample(Object exampleEntity) throws DataAccessException;

	/**
	 * Execute a query based on the given example entity object.
	 * @param entityName the name of the persistent entity
	 * @param exampleEntity an instance of the desired entity,
	 * serving as example for "query-by-example"
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 */
	List findByExample(String entityName, Object exampleEntity) throws DataAccessException;

	/**
	 * Execute a query based on a given example entity object.
	 * @param exampleEntity an instance of the desired entity,
	 * serving as example for "query-by-example"
	 * @param firstResult the index of the first result object to be retrieved
	 * (numbered from 0)
	 * @param maxResults the maximum number of result objects to retrieve
	 * (or <=0 for no limit)
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	List findByExample(Object exampleEntity, int firstResult, int maxResults) throws DataAccessException;

	/**
	 * Execute a query based on a given example entity object.
	 * @param entityName the name of the persistent entity
	 * @param exampleEntity an instance of the desired entity,
	 * serving as example for "query-by-example"
	 * @param firstResult the index of the first result object to be retrieved
	 * (numbered from 0)
	 * @param maxResults the maximum number of result objects to retrieve
	 * (or <=0 for no limit)
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	List findByExample(String entityName, Object exampleEntity, int firstResult, int maxResults)
			throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience query methods for iteration and bulk updates/deletes
	//-------------------------------------------------------------------------

	/**
	 * Execute a query for persistent instances.
	 * <p>Returns the results as an {@link Iterator}. Entities returned are
	 * initialized on demand. See the Hibernate API documentation for details.
	 * @param queryString a query expressed in Hibernate's query language
	 * @return an {@link Iterator} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#iterate
	 */
	Iterator iterate(String queryString) throws DataAccessException;

	/**
	 * Execute a query for persistent instances, binding one value
	 * to a "?" parameter in the query string.
	 * <p>Returns the results as an {@link Iterator}. Entities returned are
	 * initialized on demand. See the Hibernate API documentation for details.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param value the value of the parameter
	 * @return an {@link Iterator} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#iterate
	 */
	Iterator iterate(String queryString, Object value) throws DataAccessException;

	/**
	 * Execute a query for persistent instances, binding a number of
	 * values to "?" parameters in the query string.
	 * <p>Returns the results as an {@link Iterator}. Entities returned are
	 * initialized on demand. See the Hibernate API documentation for details.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param values the values of the parameters
	 * @return an {@link Iterator} containing 0 or more persistent instances
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#iterate
	 */
	Iterator iterate(String queryString, Object... values) throws DataAccessException;

	/**
	 * Immediately close an {@link Iterator} created by any of the various
	 * {@code iterate(..)} operations, instead of waiting until the
	 * session is closed or disconnected.
	 * @param it the {@code Iterator} to close
	 * @throws DataAccessException if the {@code Iterator} could not be closed
	 * @see org.hibernate.Hibernate#close
	 */
	void closeIterator(Iterator it) throws DataAccessException;

	/**
	 * Update/delete all objects according to the given query.
	 * @param queryString an update/delete query expressed in Hibernate's query language
	 * @return the number of instances updated/deleted
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#executeUpdate
	 */
	int bulkUpdate(String queryString) throws DataAccessException;

	/**
	 * Update/delete all objects according to the given query, binding one value
	 * to a "?" parameter in the query string.
	 * @param queryString an update/delete query expressed in Hibernate's query language
	 * @param value the value of the parameter
	 * @return the number of instances updated/deleted
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#executeUpdate
	 */
	int bulkUpdate(String queryString, Object value) throws DataAccessException;

	/**
	 * Update/delete all objects according to the given query, binding a number of
	 * values to "?" parameters in the query string.
	 * @param queryString an update/delete query expressed in Hibernate's query language
	 * @param values the values of the parameters
	 * @return the number of instances updated/deleted
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#executeUpdate
	 */
	int bulkUpdate(String queryString, Object... values) throws DataAccessException;

}
