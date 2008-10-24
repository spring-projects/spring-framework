/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.orm.toplink;

import java.util.Collection;
import java.util.List;

import oracle.toplink.expressions.Expression;
import oracle.toplink.queryframework.Call;
import oracle.toplink.queryframework.DatabaseQuery;
import oracle.toplink.sessions.ObjectCopyingPolicy;

import org.springframework.dao.DataAccessException;

/**
 * Interface that specifies a basic set of TopLink operations,
 * implemented by {@link TopLinkTemplate}. Not often used, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Defines <code>TopLinkTemplate</code>'s data access methods that
 * mirror various TopLink {@link oracle.toplink.sessions.Session} /
 * {@link oracle.toplink.sessions.UnitOfWork} methods. Users are
 * strongly encouraged to read the TopLink javadocs for details
 * on the semantics of those methods.
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public interface TopLinkOperations {

	/**
	 * Execute the action specified by the given action object within a
	 * TopLink Session. Application exceptions thrown by the action object
	 * get propagated to the caller (can only be unchecked). TopLink exceptions
	 * are transformed into appropriate DAO ones. Allows for returning a
	 * result object, i.e. a domain object or a collection of domain objects.
	 * <p>Note: Callback code is not supposed to handle transactions itself!
	 * Use an appropriate transaction manager like TopLinkTransactionManager.
	 * @param action callback object that specifies the TopLink action
	 * @return a result object returned by the action, or <code>null</code>
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see TopLinkTransactionManager
	 * @see org.springframework.dao
	 * @see org.springframework.transaction
	 * @see oracle.toplink.sessions.Session
	 */
	Object execute(TopLinkCallback action) throws DataAccessException;

	/**
	 * Execute the specified action assuming that the result object is a
	 * Collection. This is a convenience method for executing TopLink queries
	 * within an action.
	 * @param action callback object that specifies the TopLink action
	 * @return a Collection result returned by the action, or <code>null</code>
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 */
	List executeFind(TopLinkCallback action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for executing generic queries
	//-------------------------------------------------------------------------

	/**
	 * Execute a given named query with the given arguments.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class that has the named query descriptor
	 * @param queryName the name of the query
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(String, Class)
	 */
	Object executeNamedQuery(Class entityClass, String queryName) throws DataAccessException;

	/**
	 * Execute a given named query with the given arguments.
	 * @param entityClass the entity class that has the named query descriptor
	 * @param queryName the name of the query
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(String, Class)
	 */
	Object executeNamedQuery(Class entityClass, String queryName, boolean enforceReadOnly)
			throws DataAccessException;

	/**
	 * Execute a given named query with the given arguments.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class that has the named query descriptor
	 * @param queryName the name of the query
	 * @param args the arguments for the query (can be <code>null</code>)
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(String, Class, java.util.Vector)
	 */
	Object executeNamedQuery(Class entityClass, String queryName, Object[] args) throws DataAccessException;

	/**
	 * Execute a given named query with the given arguments.
	 * @param entityClass the entity class that has the named query descriptor
	 * @param queryName the name of the query
	 * @param args the arguments for the query (can be <code>null</code>)
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(String, Class, java.util.Vector)
	 */
	Object executeNamedQuery(Class entityClass, String queryName, Object[] args, boolean enforceReadOnly)
			throws DataAccessException;

	/**
	 * Execute the given query object with the given arguments.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param query the query object to execute (for example,
	 * a ReadObjectQuery or ReadAllQuery instance)
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(oracle.toplink.queryframework.DatabaseQuery)
	 */
	Object executeQuery(DatabaseQuery query) throws DataAccessException;

	/**
	 * Execute the given query object with the given arguments.
	 * @param query the query object to execute (for example,
	 * a ReadObjectQuery or ReadAllQuery instance)
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(oracle.toplink.queryframework.DatabaseQuery)
	 */
	Object executeQuery(DatabaseQuery query, boolean enforceReadOnly) throws DataAccessException;

	/**
	 * Execute the given query object with the given arguments.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param query the query object to execute (for example,
	 * a ReadObjectQuery or ReadAllQuery instance)
	 * @param args the arguments for the query (can be <code>null</code>)
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(oracle.toplink.queryframework.DatabaseQuery, java.util.Vector)
	 */
	Object executeQuery(DatabaseQuery query, Object[] args) throws DataAccessException;

	/**
	 * Execute the given query object with the given arguments.
	 * @param query the query object to execute (for example,
	 * a ReadObjectQuery or ReadAllQuery instance)
	 * @param args the arguments for the query (can be <code>null</code>)
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the result object or list of result objects for the query
	 * (can be cast to the entity class or Collection/List, respectively)
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#executeQuery(oracle.toplink.queryframework.DatabaseQuery, java.util.Vector)
	 */
	Object executeQuery(DatabaseQuery query, Object[] args, boolean enforceReadOnly)
			throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for reading a specific set of objects
	//-------------------------------------------------------------------------

	/**
	 * Read all entity instances of the given class.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @return the list of entity instances
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class)
	 */
	List readAll(Class entityClass) throws DataAccessException;

	/**
	 * Read all entity instances of the given class.
	 * @param entityClass the entity class
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the list of entity instances
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class)
	 */
	List readAll(Class entityClass, boolean enforceReadOnly) throws DataAccessException;

	/**
	 * Read all entity instances of the given class that match the given expression.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param expression the TopLink expression to match,
	 * usually built through the TopLink ExpressionBuilder
	 * @return the list of matching entity instances
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.expressions.Expression)
	 * @see oracle.toplink.expressions.ExpressionBuilder
	 */
	List readAll(Class entityClass, Expression expression) throws DataAccessException;

	/**
	 * Read all entity instances of the given class that match the given expression.
	 * @param entityClass the entity class
	 * @param expression the TopLink expression to match,
	 * usually built through the TopLink ExpressionBuilder
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the list of matching entity instances
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.expressions.Expression)
	 * @see oracle.toplink.expressions.ExpressionBuilder
	 */
	List readAll(Class entityClass, Expression expression, boolean enforceReadOnly)
			throws DataAccessException;

	/**
	 * Read all entity instances of the given class, as returned by the given call.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param call the TopLink Call object to apply (either a SQLCall or an EJBQLCall)
	 * @return the list of matching entity instances
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.queryframework.Call)
	 * @see oracle.toplink.queryframework.SQLCall
	 * @see oracle.toplink.queryframework.EJBQLCall
	 */
	List readAll(Class entityClass, Call call) throws DataAccessException;

	/**
	 * Read all entity instances of the given class, as returned by the given call.
	 * @param entityClass the entity class
	 * @param call the TopLink Call object to apply (either a SQLCall or an EJBQLCall)
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the list of matching entity instances
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.expressions.Expression)
	 * @see oracle.toplink.queryframework.SQLCall
	 * @see oracle.toplink.queryframework.EJBQLCall
	 */
	List readAll(Class entityClass, Call call, boolean enforceReadOnly) throws DataAccessException;

	/**
	 * Read an entity instance of the given class that matches the given expression.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param expression the TopLink expression to match,
	 * usually built through the TopLink ExpressionBuilder
	 * @return the matching entity instance, or <code>null</code> if none found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.expressions.Expression)
	 * @see oracle.toplink.expressions.ExpressionBuilder
	 */
	Object read(Class entityClass, Expression expression) throws DataAccessException;

	/**
	 * Read an entity instance of the given class that matches the given expression.
	 * @param entityClass the entity class
	 * @param expression the TopLink expression to match,
	 * usually built through the TopLink ExpressionBuilder
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return a matching entity instance, or <code>null</code> if none found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.expressions.Expression)
	 * @see oracle.toplink.expressions.ExpressionBuilder
	 */
	Object read(Class entityClass, Expression expression, boolean enforceReadOnly)
			throws DataAccessException;

	/**
	 * Read an entity instance of the given class, as returned by the given call.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param call the TopLink Call object to apply (either a SQLCall or an EJBQLCall)
	 * @return a matching entity instance, or <code>null</code> if none found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.queryframework.Call)
	 * @see oracle.toplink.queryframework.SQLCall
	 * @see oracle.toplink.queryframework.EJBQLCall
	 */
	Object read(Class entityClass, Call call) throws DataAccessException;

	/**
	 * Read an entity instance of the given class, as returned by the given call.
	 * @param entityClass the entity class
	 * @param call the TopLink Call object to apply (either a SQLCall or an EJBQLCall)
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return a matching entity instance, or <code>null</code> if none found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#readAllObjects(Class, oracle.toplink.expressions.Expression)
	 * @see oracle.toplink.queryframework.SQLCall
	 * @see oracle.toplink.queryframework.EJBQLCall
	 */
	Object read(Class entityClass, Call call, boolean enforceReadOnly)
			throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for reading an individual object by id
	//-------------------------------------------------------------------------

	/**
	 * Read the entity instance of the given class with the given id,
	 * throwing an exception if not found.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param id the id of the desired object
	 * @return the entity instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 */
	Object readById(Class entityClass, Object id) throws DataAccessException;

	/**
	 * Read the entity instance of the given class with the given id,
	 * throwing an exception if not found.
	 * @param entityClass the entity class
	 * @param id the id of the desired object
	 * @return the entity instance
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 */
	Object readById(Class entityClass, Object id, boolean enforceReadOnly) throws DataAccessException;

	/**
	 * Read the entity instance of the given class with the given composite id,
	 * throwing an exception if not found.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param keys the composite id elements of the desired object
	 * @return the entity instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 */
	Object readById(Class entityClass, Object[] keys) throws DataAccessException;

	/**
	 * Read the entity instance of the given class with the given composite id,
	 * throwing an exception if not found.
	 * @param entityClass the entity class
	 * @param keys the composite id elements of the desired object
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the entity instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 */
	Object readById(Class entityClass, Object[] keys, boolean enforceReadOnly) throws DataAccessException;

	/**
	 * Read the entity instance of the given class with the given id,
	 * throwing an exception if not found. A detached copy of the entity object
	 * will be returned, allowing for modifications outside the current transaction,
	 * with the changes to be merged into a later transaction.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param id the id of the desired object
	 * @return a copy of the entity instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 * @see oracle.toplink.sessions.Session#copyObject(Object)
	 */
	Object readAndCopy(Class entityClass, Object id) throws DataAccessException;

	/**
	 * Read the entity instance of the given class with the given id,
	 * throwing an exception if not found. A detached copy of the entity object
	 * will be returned, allowing for modifications outside the current transaction,
	 * with the changes to be merged into a later transaction.
	 * @param entityClass the entity class
	 * @param id the id of the desired object
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return a copy of the entity instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 * @see oracle.toplink.sessions.Session#copyObject(Object)
	 */
	Object readAndCopy(Class entityClass, Object id, boolean enforceReadOnly) throws DataAccessException;

	/**
	 * Read the entity instance of the given class with the given composite id,
	 * throwing an exception if not found. A detached copy of the entity object
	 * will be returned, allowing for modifications outside the current transaction,
	 * with the changes to be merged into a later transaction.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entityClass the entity class
	 * @param keys the composite id elements of the desired object
	 * @return a copy of the entity instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 * @see oracle.toplink.sessions.Session#copyObject(Object)
	 */
	Object readAndCopy(Class entityClass, Object[] keys) throws DataAccessException;

	/**
	 * Read the entity instance of the given class with the given composite id,
	 * throwing an exception if not found. A detached copy of the entity object
	 * will be returned, allowing for modifications outside the current transaction,
	 * with the changes to be merged into a later transaction.
	 * @param entityClass the entity class
	 * @param keys the composite id elements of the desired object
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return a copy of the entity instance
	 * @throws org.springframework.orm.ObjectRetrievalFailureException if not found
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.queryframework.ReadObjectQuery#setSelectionKey(java.util.Vector)
	 * @see oracle.toplink.sessions.Session#copyObject(Object)
	 */
	Object readAndCopy(Class entityClass, Object[] keys, boolean enforceReadOnly) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for copying and refreshing objects
	//-------------------------------------------------------------------------

	/**
	 * Create a detached copy of the given entity object,
	 * using TopLink's default ObjectCopyingPolicy.
	 * @param entity the entity object to copy
	 * @return the copy of the entity object
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#copyObject(Object)
	 */
	Object copy(Object entity) throws DataAccessException;

	/**
	 * Create a detached copy of the given entity object.
	 * @param entity the entity object to copy
	 * @param copyingPolicy the TopLink ObjectCopyingPolicy to apply
	 * @return the copy of the entity object
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#copyObject(Object, oracle.toplink.sessions.ObjectCopyingPolicy)
	 */
	Object copy(Object entity, ObjectCopyingPolicy copyingPolicy) throws DataAccessException;

	/**
	 * Create detached copies of all given entity objects,
	 * using TopLink's default ObjectCopyingPolicy.
	 * @param entities the entity objects to copy
	 * @return the copies of the entity objects
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#copyObject(Object)
	 */
	List copyAll(Collection entities) throws DataAccessException;

	/**
	 * Create detached copies of all given entity objects.
	 * @param entities the entity objects to copy
	 * @param copyingPolicy the TopLink ObjectCopyingPolicy to apply
	 * @return the copies of the entity objects
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#copyObject(Object)
	 */
	List copyAll(Collection entities, ObjectCopyingPolicy copyingPolicy) throws DataAccessException;

	/**
	 * Refresh the given entity object, returning the refreshed object.
	 * <p>The returned object will only be different from the passed-in object
	 * if the passed-in object is not the currently registered version of
	 * the corresponding entity.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entity the entity object to refresh
	 * @return the refreshed version of the entity object
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#refreshObject(Object)
	 */
	Object refresh(Object entity) throws DataAccessException;

	/**
	 * Refresh the given entity object, returning the refreshed object.
	 * <p>The returned object will only be different from the passed-in object
	 * if the passed-in object is not the currently registered version of
	 * the corresponding entity.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entity the entity object to refresh
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the refreshed version of the entity object
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#refreshObject(Object)
	 */
	Object refresh(Object entity, boolean enforceReadOnly) throws DataAccessException;

	/**
	 * Refresh the given entity objects, returning the corresponding refreshed objects.
	 * <p>A returned object will only be different from the corresponding passed-in
	 * object if the passed-in object is not the currently registered version of
	 * the corresponding entity.
	 * <p>Retrieves read-write objects from the TopLink UnitOfWork in case of a
	 * non-read-only transaction, and read-only objects else.
	 * @param entities the entity objects to refresh
	 * @return the refreshed versions of the entity objects
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#refreshObject(Object)
	 */
	List refreshAll(Collection entities) throws DataAccessException;

	/**
	 * Refresh the given entity objects, returning the corresponding refreshed objects.
	 * <p>A returned object will only be different from the corresponding passed-in
	 * object if the passed-in object is not the currently registered version of
	 * the corresponding entity.
	 * @param entities the entity objects to refresh
	 * @param enforceReadOnly whether to always retrieve read-only objects from
	 * the plain TopLink Session (else, read-write objects will be retrieved
	 * from the TopLink UnitOfWork in case of a non-read-only transaction)
	 * @return the refreshed versions of the entity objects
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.Session#refreshObject(Object)
	 */
	List refreshAll(Collection entities, boolean enforceReadOnly) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for persisting and deleting objects
	//-------------------------------------------------------------------------

	/**
	 * Register the given (new or existing) entity with the current UnitOfWork.
	 * <p>The entity will be checked for existence, according to TopLink's
	 * configured existence checking policy. To avoid the (potentially costly)
	 * existence check, consider using the specific <code>registerNew</code>
	 * or <code>registerExisting</code> method.
	 * <b>Do not edit the passed-in object any further afterwards.</b>
	 * @param entity the entity to register
	 * @return the registered clone of the original object,
	 * which needs to be used for further editing
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#registerObject(Object)
	 * @see #registerNew(Object)
	 * @see #registerExisting(Object)
	 */
	Object register(Object entity);

	/**
	 * Register all given entities with the current UnitOfWork.
	 * <b>Do not edit the passed-in objects any further afterwards.</b>
	 * @param entities the entities to register
	 * @return the registered clones of the original objects,
	 * which need to be used for further editing
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#registerAllObjects(java.util.Collection)
	 */
	List registerAll(Collection entities);

	/**
	 * Register the given new entity with the current UnitOfWork.
	 * The passed-in object can be edited further afterwards.
	 * @param entity the new entity to register
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#registerNewObject(Object)
	 */
	void registerNew(Object entity);

	/**
	 * Register the given existing entity with the current UnitOfWork.
	 * <b>Do not edit the passed-in object any further afterwards.</b>
	 * @param entity the existing entity to register
	 * @return the registered clone of the original object,
	 * which needs to be used for further editing
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#registerExistingObject(Object)
	 */
	Object registerExisting(Object entity);

	/**
	 * Reassociate the given entity copy with the current UnitOfWork,
	 * using simple merging.
	 * <p>The given object will not be reassociated itself: instead, the state
	 * will be copied onto the persistent object with the same identifier.
	 * In case of a new entity, merge will copy to a registered object as well,
	 * but will also update the identifier of the passed-in object.
	 * @param entity the updated copy to merge
	 * @return the updated, registered persistent instance
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#mergeClone(Object)
	 */
	Object merge(Object entity) throws DataAccessException;

	/**
	 * Reassociate the given entity copy with the current UnitOfWork,
	 * using deep merging of all contained entities.
	 * <p>The given object will not be reassociated itself: instead, the state
	 * will be copied onto the persistent object with the same identifier.
	 * In case of a new entity, merge will register a copy as well,
	 * but will also update the identifier of the passed-in object.
	 * @param entity the updated copy to merge
	 * @return the updated, registered persistent instance
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#deepMergeClone(Object)
	 */
	Object deepMerge(Object entity) throws DataAccessException;

	/**
	 * Reassociate the given entity copy with the current UnitOfWork,
	 * using shallow merging of the entity instance.
	 * <p>The given object will not be reassociated itself: instead, the state
	 * will be copied onto the persistent object with the same identifier.
	 * In case of a new entity, merge will register a copy as well,
	 * but will also update the identifier of the passed-in object.
	 * @param entity the updated copy to merge
	 * @return the updated, registered persistent instance
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#shallowMergeClone(Object)
	 */
	Object shallowMerge(Object entity) throws DataAccessException;

	/**
	 * Reassociate the given entity copy with the current UnitOfWork,
	 * using merging with all references from this clone.
	 * <p>The given object will not be reassociated itself: instead, the state
	 * will be copied onto the persistent object with the same identifier.
	 * In case of a new entity, merge will register a copy as well,
	 * but will also update the identifier of the passed-in object.
	 * @param entity the updated copy to merge
	 * @return the updated, registered persistent instance
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#mergeCloneWithReferences(Object)
	 */
	Object mergeWithReferences(Object entity) throws DataAccessException;

	/**
	 * Delete the given entity.
	 * @param entity the entity to delete
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#deleteObject(Object)
	 */
	void delete(Object entity) throws DataAccessException;

	/**
	 * Delete all given entities.
	 * @param entities the entities to delete
	 * @throws org.springframework.dao.DataAccessException in case of TopLink errors
	 * @see oracle.toplink.sessions.UnitOfWork#deleteAllObjects(java.util.Collection)
	 */
	void deleteAll(Collection entities) throws DataAccessException;

}
