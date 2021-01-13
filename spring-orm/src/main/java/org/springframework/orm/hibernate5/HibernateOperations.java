/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.orm.hibernate5;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Filter;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.criterion.DetachedCriteria;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Interface that specifies a common set of Hibernate operations as well as
 * a general {@link #execute} method for Session-based lambda expressions.
 * Implemented by {@link HibernateTemplate}. Not often used, but a useful option
 * to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Defines {@code HibernateTemplate}'s data access methods that mirror various
 * {@link org.hibernate.Session} methods. Users are strongly encouraged to read the
 * Hibernate {@code Session} javadocs for details on the semantics of those methods.
 *
 * <p><b>A deprecation note:</b> While {@link HibernateTemplate} and this operations
 * interface are being kept around for backwards compatibility in terms of the data
 * access implementation style in Spring applications, we strongly recommend the use
 * of native {@link org.hibernate.Session} access code for non-trivial interactions.
 * This in particular affects parameterized queries where - on Java 8+ - a custom
 * {@link HibernateCallback} lambda code block with {@code createQuery} and several
 * {@code setParameter} calls on the {@link org.hibernate.query.Query} interface
 * is an elegant solution, to be executed via the general {@link #execute} method.
 * All such operations which benefit from a lambda variant have been marked as
 * {@code deprecated} on this interface.
 *
 * <p><b>A Hibernate compatibility note:</b> {@link HibernateTemplate} and the
 * operations on this interface generally aim to be applicable across all Hibernate
 * versions. In terms of binary compatibility, Spring ships a variant for each major
 * generation of Hibernate (in the present case: Hibernate ORM 5.x). However, due to
 * refactorings and removals in Hibernate ORM 5.3, some variants - in particular
 * legacy positional parameters starting from index 0 - do not work anymore.
 * All affected operations are marked as deprecated; please replace them with the
 * general {@link #execute} method and custom lambda blocks creating the queries,
 * ideally setting named parameters through {@link org.hibernate.query.Query}.
 * <b>Please be aware that deprecated operations are known to work with Hibernate
 * ORM 5.2 but may not work with Hibernate ORM 5.3 and higher anymore.</b>
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see HibernateTemplate
 * @see org.hibernate.Session
 * @see HibernateTransactionManager
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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see HibernateTransactionManager
	 * @see org.hibernate.Session
	 */
	@Nullable
	<T> T execute(HibernateCallback<T> action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for loading individual objects
	//-------------------------------------------------------------------------

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(Class, Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance, or {@code null} if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, Serializable)
	 */
	@Nullable
	<T> T get(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * <p>Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(Class, Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance, or {@code null} if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, Serializable, LockMode)
	 */
	@Nullable
	<T> T get(Class<T> entityClass, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(String, Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance, or {@code null} if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, Serializable)
	 */
	@Nullable
	Object get(String entityName, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, or {@code null} if not found.
	 * Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#get(String, Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance, or {@code null} if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#get(Class, Serializable, LockMode)
	 */
	@Nullable
	Object get(String entityName, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(Class, Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	<T> T load(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(Class, Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityClass a persistent class
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	<T> T load(Class<T> entityClass, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(String, Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	Object load(String entityName, Serializable id) throws DataAccessException;

	/**
	 * Return the persistent instance of the given entity class
	 * with the given identifier, throwing an exception if not found.
	 * <p>Obtains the specified lock mode if the instance exists.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(String, Serializable, LockMode)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entityName the name of the persistent entity
	 * @param id the identifier of the persistent instance
	 * @param lockMode the lock mode to obtain
	 * @return the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Class, Serializable)
	 */
	Object load(String entityName, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * Return all persistent instances of the given entity class.
	 * Note: Use queries or criteria for retrieving a specific subset.
	 * @param entityClass a persistent class
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws DataAccessException if there is a Hibernate error
	 * @see org.hibernate.Session#createCriteria
	 */
	<T> List<T> loadAll(Class<T> entityClass) throws DataAccessException;

	/**
	 * Load the persistent instance with the given identifier
	 * into the given object, throwing an exception if not found.
	 * <p>This method is a thin wrapper around
	 * {@link org.hibernate.Session#load(Object, Serializable)} for convenience.
	 * For an explanation of the exact semantics of this method, please do refer to
	 * the Hibernate API documentation in the first instance.
	 * @param entity the object (of the target class) to load into
	 * @param id the identifier of the persistent instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#load(Object, Serializable)
	 */
	void load(Object entity, Serializable id) throws DataAccessException;

	/**
	 * Re-read the state of the given persistent instance.
	 * @param entity the persistent instance to re-read
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#refresh(Object)
	 */
	void refresh(Object entity) throws DataAccessException;

	/**
	 * Re-read the state of the given persistent instance.
	 * Obtains the specified lock mode for the instance.
	 * @param entity the persistent instance to re-read
	 * @param lockMode the lock mode to obtain
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#refresh(Object, LockMode)
	 */
	void refresh(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Check whether the given object is in the Session cache.
	 * @param entity the persistence instance to check
	 * @return whether the given object is in the Session cache
	 * @throws DataAccessException if there is a Hibernate error
	 * @see org.hibernate.Session#contains
	 */
	boolean contains(Object entity) throws DataAccessException;

	/**
	 * Remove the given object from the {@link org.hibernate.Session} cache.
	 * @param entity the persistent instance to evict
	 * @throws DataAccessException in case of Hibernate errors
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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#lock(Object, LockMode)
	 */
	void lock(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Obtain the specified lock level upon the given object, implicitly
	 * checking whether the corresponding database entry still exists.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to lock
	 * @param lockMode the lock mode to obtain
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if not found
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#lock(String, Object, LockMode)
	 */
	void lock(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Persist the given transient instance.
	 * @param entity the transient instance to persist
	 * @return the generated identifier
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#save(Object)
	 */
	Serializable save(Object entity) throws DataAccessException;

	/**
	 * Persist the given transient instance.
	 * @param entityName the name of the persistent entity
	 * @param entity the transient instance to persist
	 * @return the generated identifier
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#save(String, Object)
	 */
	Serializable save(String entityName, Object entity) throws DataAccessException;

	/**
	 * Update the given persistent instance,
	 * associating it with the current Hibernate {@link org.hibernate.Session}.
	 * @param entity the persistent instance to update
	 * @throws DataAccessException in case of Hibernate errors
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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#update(Object)
	 */
	void update(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Update the given persistent instance,
	 * associating it with the current Hibernate {@link org.hibernate.Session}.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to update
	 * @throws DataAccessException in case of Hibernate errors
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
	 * @throws DataAccessException in case of Hibernate errors
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
	 * Persist the state of the given detached instance according to the
	 * given replication mode, reusing the current identifier value.
	 * @param entity the persistent object to replicate
	 * @param replicationMode the Hibernate ReplicationMode
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#replicate(Object, ReplicationMode)
	 */
	void replicate(Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * Persist the state of the given detached instance according to the
	 * given replication mode, reusing the current identifier value.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent object to replicate
	 * @param replicationMode the Hibernate ReplicationMode
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#replicate(String, Object, ReplicationMode)
	 */
	void replicate(String entityName, Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * Persist the given transient instance. Follows JSR-220 semantics.
	 * <p>Similar to {@code save}, associating the given object
	 * with the current Hibernate {@link org.hibernate.Session}.
	 * @param entity the persistent instance to persist
	 * @throws DataAccessException in case of Hibernate errors
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
	 * @throws DataAccessException in case of Hibernate errors
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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#merge(Object)
	 * @see #saveOrUpdate
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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#merge(String, Object)
	 * @see #saveOrUpdate
	 */
	<T> T merge(String entityName, T entity) throws DataAccessException;

	/**
	 * Delete the given persistent instance.
	 * @param entity the persistent instance to delete
	 * @throws DataAccessException in case of Hibernate errors
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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Delete the given persistent instance.
	 * @param entityName the name of the persistent entity
	 * @param entity the persistent instance to delete
	 * @throws DataAccessException in case of Hibernate errors
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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void delete(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * Delete all given persistent instances.
	 * <p>This can be combined with any of the find methods to delete by query
	 * in two lines of code.
	 * @param entities the persistent instances to delete
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#delete(Object)
	 */
	void deleteAll(Collection<?> entities) throws DataAccessException;

	/**
	 * Flush all pending saves, updates and deletes to the database.
	 * <p>Only invoke this for selective eager flushing, for example when
	 * JDBC code needs to see certain changes within the same transaction.
	 * Else, it is preferable to rely on auto-flushing at transaction
	 * completion.
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#flush
	 */
	void flush() throws DataAccessException;

	/**
	 * Remove all objects from the {@link org.hibernate.Session} cache, and
	 * cancel all pending saves, updates and deletes.
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#clear
	 */
	void clear() throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for detached criteria
	//-------------------------------------------------------------------------

	/**
	 * Execute a query based on a given Hibernate criteria object.
	 * @param criteria the detached Hibernate criteria object.
	 * <b>Note: Do not reuse criteria objects! They need to recreated per execution,
	 * due to the suboptimal design of Hibernate's criteria facility.</b>
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws DataAccessException in case of Hibernate errors
	 * @see DetachedCriteria#getExecutableCriteria(org.hibernate.Session)
	 */
	List<?> findByCriteria(DetachedCriteria criteria) throws DataAccessException;

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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see DetachedCriteria#getExecutableCriteria(org.hibernate.Session)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	List<?> findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults) throws DataAccessException;

	/**
	 * Execute a query based on the given example entity object.
	 * @param exampleEntity an instance of the desired entity,
	 * serving as example for "query-by-example"
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 */
	<T> List<T> findByExample(T exampleEntity) throws DataAccessException;

	/**
	 * Execute a query based on the given example entity object.
	 * @param entityName the name of the persistent entity
	 * @param exampleEntity an instance of the desired entity,
	 * serving as example for "query-by-example"
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 */
	<T> List<T> findByExample(String entityName, T exampleEntity) throws DataAccessException;

	/**
	 * Execute a query based on a given example entity object.
	 * @param exampleEntity an instance of the desired entity,
	 * serving as example for "query-by-example"
	 * @param firstResult the index of the first result object to be retrieved
	 * (numbered from 0)
	 * @param maxResults the maximum number of result objects to retrieve
	 * (or <=0 for no limit)
	 * @return a {@link List} containing 0 or more persistent instances
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	<T> List<T> findByExample(T exampleEntity, int firstResult, int maxResults) throws DataAccessException;

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
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.criterion.Example#create(Object)
	 * @see org.hibernate.Criteria#setFirstResult(int)
	 * @see org.hibernate.Criteria#setMaxResults(int)
	 */
	<T> List<T> findByExample(String entityName, T exampleEntity, int firstResult, int maxResults)
			throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for HQL strings
	//-------------------------------------------------------------------------

	/**
	 * Execute an HQL query, binding a number of values to "?" parameters
	 * in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> find(String queryString, Object... values) throws DataAccessException;

	/**
	 * Execute an HQL query, binding one value to a ":" named parameter
	 * in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedParam(String queryString, String paramName, Object value) throws DataAccessException;

	/**
	 * Execute an HQL query, binding a number of values to ":" named
	 * parameters in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param paramNames the names of the parameters
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedParam(String queryString, String[] paramNames, Object[] values) throws DataAccessException;

	/**
	 * Execute an HQL query, binding the properties of the given bean to
	 * <i>named</i> parameters in the query string.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param valueBean the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Query#setProperties
	 * @see org.hibernate.Session#createQuery
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByValueBean(String queryString, Object valueBean) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for named queries
	//-------------------------------------------------------------------------

	/**
	 * Execute a named query binding a number of values to "?" parameters
	 * in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQuery(String queryName, Object... values) throws DataAccessException;

	/**
	 * Execute a named query, binding one value to a ":" named parameter
	 * in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param paramName the name of parameter
	 * @param value the value of the parameter
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
			throws DataAccessException;

	/**
	 * Execute a named query, binding a number of values to ":" named
	 * parameters in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param paramNames the names of the parameters
	 * @param values the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQueryAndNamedParam(String queryName, String[] paramNames, Object[] values)
			throws DataAccessException;

	/**
	 * Execute a named query, binding the properties of the given bean to
	 * ":" named parameters in the query string.
	 * <p>A named query is defined in a Hibernate mapping file.
	 * @param queryName the name of a Hibernate query in a mapping file
	 * @param valueBean the values of the parameters
	 * @return a {@link List} containing the results of the query execution
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Query#setProperties
	 * @see org.hibernate.Session#getNamedQuery(String)
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	List<?> findByNamedQueryAndValueBean(String queryName, Object valueBean) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience query methods for iteration and bulk updates/deletes
	//-------------------------------------------------------------------------

	/**
	 * Execute a query for persistent instances, binding a number of
	 * values to "?" parameters in the query string.
	 * <p>Returns the results as an {@link Iterator}. Entities returned are
	 * initialized on demand. See the Hibernate API documentation for details.
	 * @param queryString a query expressed in Hibernate's query language
	 * @param values the values of the parameters
	 * @return an {@link Iterator} containing 0 or more persistent instances
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#iterate
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	Iterator<?> iterate(String queryString, Object... values) throws DataAccessException;

	/**
	 * Immediately close an {@link Iterator} created by any of the various
	 * {@code iterate(..)} operations, instead of waiting until the
	 * session is closed or disconnected.
	 * @param it the {@code Iterator} to close
	 * @throws DataAccessException if the {@code Iterator} could not be closed
	 * @see org.hibernate.Hibernate#close
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	void closeIterator(Iterator<?> it) throws DataAccessException;

	/**
	 * Update/delete all objects according to the given query, binding a number of
	 * values to "?" parameters in the query string.
	 * @param queryString an update/delete query expressed in Hibernate's query language
	 * @param values the values of the parameters
	 * @return the number of instances updated/deleted
	 * @throws DataAccessException in case of Hibernate errors
	 * @see org.hibernate.Session#createQuery
	 * @see org.hibernate.Query#executeUpdate
	 * @deprecated as of 5.0.4, in favor of a custom {@link HibernateCallback}
	 * lambda code block passed to the general {@link #execute} method
	 */
	@Deprecated
	int bulkUpdate(String queryString, Object... values) throws DataAccessException;

}
