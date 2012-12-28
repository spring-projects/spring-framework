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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;

/**
 * Helper class that simplifies Hibernate data access code. Automatically
 * converts HibernateExceptions into DataAccessExceptions, following the
 * {@code org.springframework.dao} exception hierarchy.
 *
 * <p>The central method is {@code execute}, supporting Hibernate access code
 * implementing the {@link HibernateCallback} interface. It provides Hibernate Session
 * handling such that neither the HibernateCallback implementation nor the calling
 * code needs to explicitly care about retrieving/closing Hibernate Sessions,
 * or handling Session lifecycle exceptions. For typical single step actions,
 * there are various convenience methods (find, load, saveOrUpdate, delete).
 *
 * <p>Can be used within a service implementation via direct instantiation
 * with a SessionFactory reference, or get prepared in an application context
 * and given to services as bean reference. Note: The SessionFactory should
 * always be configured as bean in the application context, in the first case
 * given to the service directly, in the second case to the prepared template.
 *
 * <p><b>NOTE: As of Hibernate 3.0.1, transactional Hibernate access code can
 * also be coded in plain Hibernate style. Hence, for newly started projects,
 * consider adopting the standard Hibernate3 style of coding data access objects
 * instead, based on {@link org.hibernate.SessionFactory#getCurrentSession()}.</b>
 *
 * <p>This class can be considered as direct alternative to working with the raw
 * Hibernate3 Session API (through {@code SessionFactory.getCurrentSession()}).
 * The major advantage is its automatic conversion to DataAccessExceptions as well
 * as its capability to fall back to 'auto-commit' style behavior when used outside
 * of transactions. <b>Note that HibernateTemplate will perform its own Session
 * management, not participating in a custom Hibernate CurrentSessionContext
 * unless you explicitly switch {@link #setAllowCreate "allowCreate"} to "false".</b>
 *
 * <p>{@link LocalSessionFactoryBean} is the preferred way of obtaining a reference
 * to a specific Hibernate SessionFactory, at least in a non-EJB environment.
 * The Spring application context will manage its lifecycle, initializing and
 * shutting down the factory as part of the application.
 *
 * <p>Note that operations that return an Iterator (i.e. {@code iterate})
 * are supposed to be used within Spring-driven or JTA-driven transactions
 * (with HibernateTransactionManager, JtaTransactionManager, or EJB CMT).
 * Else, the Iterator won't be able to read results from its ResultSet anymore,
 * as the underlying Hibernate Session will already have been closed.
 *
 * <p>Lazy loading will also just work with an open Hibernate Session,
 * either within a transaction or within OpenSessionInViewFilter/Interceptor.
 * Furthermore, some operations just make sense within transactions,
 * for example: {@code contains}, {@code evict}, {@code lock},
 * {@code flush}, {@code clear}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setSessionFactory
 * @see HibernateCallback
 * @see org.hibernate.Session
 * @see LocalSessionFactoryBean
 * @see HibernateTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewFilter
 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
 */
public class HibernateTemplate extends HibernateAccessor implements HibernateOperations {

	private boolean allowCreate = true;

	private boolean alwaysUseNewSession = false;

	private boolean exposeNativeSession = false;

	private boolean checkWriteOperations = true;

	private boolean cacheQueries = false;

	private String queryCacheRegion;

	private int fetchSize = 0;

	private int maxResults = 0;


	/**
	 * Create a new HibernateTemplate instance.
	 */
	public HibernateTemplate() {
	}

	/**
	 * Create a new HibernateTemplate instance.
	 * @param sessionFactory SessionFactory to create Sessions
	 */
	public HibernateTemplate(SessionFactory sessionFactory) {
		setSessionFactory(sessionFactory);
		afterPropertiesSet();
	}

	/**
	 * Create a new HibernateTemplate instance.
	 * @param sessionFactory SessionFactory to create Sessions
	 * @param allowCreate if a non-transactional Session should be created when no
	 * transactional Session can be found for the current thread
	 */
	public HibernateTemplate(SessionFactory sessionFactory, boolean allowCreate) {
		setSessionFactory(sessionFactory);
		setAllowCreate(allowCreate);
		afterPropertiesSet();
	}


	/**
	 * Set if a new {@link Session} should be created when no transactional
	 * {@code Session} can be found for the current thread.
	 * The default value is {@code true}.
	 * <p>{@code HibernateTemplate} is aware of a corresponding
	 * {@code Session} bound to the current thread, for example when using
	 * {@link HibernateTransactionManager}. If {@code allowCreate} is
	 * {@code true}, a new non-transactional {@code Session} will be
	 * created if none is found, which needs to be closed at the end of the operation.
	 * If {@code false}, an {@link IllegalStateException} will get thrown in
	 * this case.
	 * <p><b>NOTE: As of Spring 2.5, switching {@code allowCreate}
	 * to {@code false} will delegate to Hibernate's
	 * {@link org.hibernate.SessionFactory#getCurrentSession()} method,</b>
	 * which - with Spring-based setup - will by default delegate to Spring's
	 * {@code SessionFactoryUtils.getSession(sessionFactory, false)}.
	 * This mode also allows for custom Hibernate CurrentSessionContext strategies
	 * to be plugged in, whereas {@code allowCreate} set to {@code true}
	 * will always use a Spring-managed Hibernate Session.
	 * @see SessionFactoryUtils#getSession(SessionFactory, boolean)
	 */
	public void setAllowCreate(boolean allowCreate) {
		this.allowCreate = allowCreate;
	}

	/**
	 * Return if a new Session should be created if no thread-bound found.
	 */
	public boolean isAllowCreate() {
		return this.allowCreate;
	}

	/**
	 * Set whether to always use a new Hibernate Session for this template.
	 * Default is "false"; if activated, all operations on this template will
	 * work on a new Hibernate Session even in case of a pre-bound Session
	 * (for example, within a transaction or OpenSessionInViewFilter).
	 * <p>Within a transaction, a new Hibernate Session used by this template
	 * will participate in the transaction through using the same JDBC
	 * Connection. In such a scenario, multiple Sessions will participate
	 * in the same database transaction.
	 * <p>Turn this on for operations that are supposed to always execute
	 * independently, without side effects caused by a shared Hibernate Session.
	 */
	public void setAlwaysUseNewSession(boolean alwaysUseNewSession) {
		this.alwaysUseNewSession = alwaysUseNewSession;
	}

	/**
	 * Return whether to always use a new Hibernate Session for this template.
	 */
	public boolean isAlwaysUseNewSession() {
		return this.alwaysUseNewSession;
	}

	/**
	 * Set whether to expose the native Hibernate Session to
	 * HibernateCallback code.
	 * <p>Default is "false": a Session proxy will be returned, suppressing
	 * {@code close} calls and automatically applying query cache
	 * settings and transaction timeouts.
	 * @see HibernateCallback
	 * @see org.hibernate.Session
	 * @see #setCacheQueries
	 * @see #setQueryCacheRegion
	 * @see #prepareQuery
	 * @see #prepareCriteria
	 */
	public void setExposeNativeSession(boolean exposeNativeSession) {
		this.exposeNativeSession = exposeNativeSession;
	}

	/**
	 * Return whether to expose the native Hibernate Session to
	 * HibernateCallback code, or rather a Session proxy.
	 */
	public boolean isExposeNativeSession() {
		return this.exposeNativeSession;
	}

	/**
	 * Set whether to check that the Hibernate Session is not in read-only mode
	 * in case of write operations (save/update/delete).
	 * <p>Default is "true", for fail-fast behavior when attempting write operations
	 * within a read-only transaction. Turn this off to allow save/update/delete
	 * on a Session with flush mode NEVER.
	 * @see #setFlushMode
	 * @see #checkWriteOperationAllowed
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly
	 */
	public void setCheckWriteOperations(boolean checkWriteOperations) {
		this.checkWriteOperations = checkWriteOperations;
	}

	/**
	 * Return whether to check that the Hibernate Session is not in read-only
	 * mode in case of write operations (save/update/delete).
	 */
	public boolean isCheckWriteOperations() {
		return this.checkWriteOperations;
	}

	/**
	 * Set whether to cache all queries executed by this template.
	 * <p>If this is "true", all Query and Criteria objects created by
	 * this template will be marked as cacheable (including all
	 * queries through find methods).
	 * <p>To specify the query region to be used for queries cached
	 * by this template, set the "queryCacheRegion" property.
	 * @see #setQueryCacheRegion
	 * @see org.hibernate.Query#setCacheable
	 * @see org.hibernate.Criteria#setCacheable
	 */
	public void setCacheQueries(boolean cacheQueries) {
		this.cacheQueries = cacheQueries;
	}

	/**
	 * Return whether to cache all queries executed by this template.
	 */
	public boolean isCacheQueries() {
		return this.cacheQueries;
	}

	/**
	 * Set the name of the cache region for queries executed by this template.
	 * <p>If this is specified, it will be applied to all Query and Criteria objects
	 * created by this template (including all queries through find methods).
	 * <p>The cache region will not take effect unless queries created by this
	 * template are configured to be cached via the "cacheQueries" property.
	 * @see #setCacheQueries
	 * @see org.hibernate.Query#setCacheRegion
	 * @see org.hibernate.Criteria#setCacheRegion
	 */
	public void setQueryCacheRegion(String queryCacheRegion) {
		this.queryCacheRegion = queryCacheRegion;
	}

	/**
	 * Return the name of the cache region for queries executed by this template.
	 */
	public String getQueryCacheRegion() {
		return this.queryCacheRegion;
	}

	/**
	 * Set the fetch size for this HibernateTemplate. This is important for processing
	 * large result sets: Setting this higher than the default value will increase
	 * processing speed at the cost of memory consumption; setting this lower can
	 * avoid transferring row data that will never be read by the application.
	 * <p>Default is 0, indicating to use the JDBC driver's default.
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * Return the fetch size specified for this HibernateTemplate.
	 */
	public int getFetchSize() {
		return this.fetchSize;
	}

	/**
	 * Set the maximum number of rows for this HibernateTemplate. This is important
	 * for processing subsets of large result sets, avoiding to read and hold
	 * the entire result set in the database or in the JDBC driver if we're
	 * never interested in the entire result in the first place (for example,
	 * when performing searches that might return a large number of matches).
	 * <p>Default is 0, indicating to use the JDBC driver's default.
	 */
	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * Return the maximum number of rows specified for this HibernateTemplate.
	 */
	public int getMaxResults() {
		return this.maxResults;
	}


	@Override
	public <T> T execute(HibernateCallback<T> action) throws DataAccessException {
		return doExecute(action, false, false);
	}

	@Override
	public List executeFind(HibernateCallback<?> action) throws DataAccessException {
		Object result = doExecute(action, false, false);
		if (result != null && !(result instanceof List)) {
			throw new InvalidDataAccessApiUsageException(
					"Result object returned from HibernateCallback isn't a List: [" + result + "]");
		}
		return (List) result;
	}

	/**
	 * Execute the action specified by the given action object within a
	 * new {@link org.hibernate.Session}.
	 * <p>This execute variant overrides the template-wide
	 * {@link #isAlwaysUseNewSession() "alwaysUseNewSession"} setting.
	 * @param action callback object that specifies the Hibernate action
	 * @return a result object returned by the action, or {@code null}
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 */
	public <T> T executeWithNewSession(HibernateCallback<T> action) {
		return doExecute(action, true, false);
	}

	/**
	 * Execute the action specified by the given action object within a
	 * native {@link org.hibernate.Session}.
	 * <p>This execute variant overrides the template-wide
	 * {@link #isExposeNativeSession() "exposeNativeSession"} setting.
	 * @param action callback object that specifies the Hibernate action
	 * @return a result object returned by the action, or {@code null}
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 */
	public <T> T executeWithNativeSession(HibernateCallback<T> action) {
		return doExecute(action, false, true);
	}

	/**
	 * Execute the action specified by the given action object within a Session.
	 * @param action callback object that specifies the Hibernate action
	 * @param enforceNewSession whether to enforce a new Session for this template
	 * even if there is a pre-bound transactional Session
	 * @param enforceNativeSession whether to enforce exposure of the native
	 * Hibernate Session to callback code
	 * @return a result object returned by the action, or {@code null}
	 * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
	 */
	protected <T> T doExecute(HibernateCallback<T> action, boolean enforceNewSession, boolean enforceNativeSession)
			throws DataAccessException {

		Assert.notNull(action, "Callback object must not be null");

		Session session = (enforceNewSession ?
				SessionFactoryUtils.getNewSession(getSessionFactory(), getEntityInterceptor()) : getSession());
		boolean existingTransaction = (!enforceNewSession &&
				(!isAllowCreate() || SessionFactoryUtils.isSessionTransactional(session, getSessionFactory())));
		if (existingTransaction) {
			logger.debug("Found thread-bound Session for HibernateTemplate");
		}

		FlushMode previousFlushMode = null;
		try {
			previousFlushMode = applyFlushMode(session, existingTransaction);
			enableFilters(session);
			Session sessionToExpose =
					(enforceNativeSession || isExposeNativeSession() ? session : createSessionProxy(session));
			T result = action.doInHibernate(sessionToExpose);
			flushIfNecessary(session, existingTransaction);
			return result;
		}
		catch (HibernateException ex) {
			throw convertHibernateAccessException(ex);
		}
		catch (SQLException ex) {
			throw convertJdbcAccessException(ex);
		}
		catch (RuntimeException ex) {
			// Callback code threw application exception...
			throw ex;
		}
		finally {
			if (existingTransaction) {
				logger.debug("Not closing pre-bound Hibernate Session after HibernateTemplate");
				disableFilters(session);
				if (previousFlushMode != null) {
					session.setFlushMode(previousFlushMode);
				}
			}
			else {
				// Never use deferred close for an explicitly new Session.
				if (isAlwaysUseNewSession()) {
					SessionFactoryUtils.closeSession(session);
				}
				else {
					SessionFactoryUtils.closeSessionOrRegisterDeferredClose(session, getSessionFactory());
				}
			}
		}
	}

	/**
	 * Return a Session for use by this template.
	 * <p>Returns a new Session in case of "alwaysUseNewSession" (using the same
	 * JDBC Connection as a transactional Session, if applicable), a pre-bound
	 * Session in case of "allowCreate" turned off, and a pre-bound or new Session
	 * otherwise (new only if no transactional or otherwise pre-bound Session exists).
	 * @return the Session to use (never {@code null})
	 * @see SessionFactoryUtils#getSession
	 * @see SessionFactoryUtils#getNewSession
	 * @see #setAlwaysUseNewSession
	 * @see #setAllowCreate
	 */
	protected Session getSession() {
		if (isAlwaysUseNewSession()) {
			return SessionFactoryUtils.getNewSession(getSessionFactory(), getEntityInterceptor());
		}
		else if (isAllowCreate()) {
			return SessionFactoryUtils.getSession(
					getSessionFactory(), getEntityInterceptor(), getJdbcExceptionTranslator());
		}
		else if (SessionFactoryUtils.hasTransactionalSession(getSessionFactory())) {
			return SessionFactoryUtils.getSession(getSessionFactory(), false);
		}
		else {
			try {
				return getSessionFactory().getCurrentSession();
			}
			catch (HibernateException ex) {
				throw new DataAccessResourceFailureException("Could not obtain current Hibernate Session", ex);
			}
		}
	}

	/**
	 * Create a close-suppressing proxy for the given Hibernate Session.
	 * The proxy also prepares returned Query and Criteria objects.
	 * @param session the Hibernate Session to create a proxy for
	 * @return the Session proxy
	 * @see org.hibernate.Session#close()
	 * @see #prepareQuery
	 * @see #prepareCriteria
	 */
	protected Session createSessionProxy(Session session) {
		Class[] sessionIfcs = null;
		Class mainIfc = (session instanceof org.hibernate.classic.Session ?
				org.hibernate.classic.Session.class : Session.class);
		if (session instanceof EventSource) {
			sessionIfcs = new Class[] {mainIfc, EventSource.class};
		}
		else if (session instanceof SessionImplementor) {
			sessionIfcs = new Class[] {mainIfc, SessionImplementor.class};
		}
		else {
			sessionIfcs = new Class[] {mainIfc};
		}
		return (Session) Proxy.newProxyInstance(
				session.getClass().getClassLoader(), sessionIfcs,
				new CloseSuppressingInvocationHandler(session));
	}


	//-------------------------------------------------------------------------
	// Convenience methods for loading individual objects
	//-------------------------------------------------------------------------

	@Override
	public <T> T get(Class<T> entityClass, Serializable id) throws DataAccessException {
		return get(entityClass, id, null);
	}

	@Override
	public <T> T get(final Class<T> entityClass, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return (T) session.get(entityClass, id, lockMode);
				}
				else {
					return (T) session.get(entityClass, id);
				}
			}
		});
	}

	@Override
	public Object get(String entityName, Serializable id) throws DataAccessException {
		return get(entityName, id, null);
	}

	@Override
	public Object get(final String entityName, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return session.get(entityName, id, lockMode);
				}
				else {
					return session.get(entityName, id);
				}
			}
		});
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable id) throws DataAccessException {
		return load(entityClass, id, null);
	}

	@Override
	public <T> T load(final Class<T> entityClass, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return (T) session.load(entityClass, id, lockMode);
				}
				else {
					return (T) session.load(entityClass, id);
				}
			}
		});
	}

	@Override
	public Object load(String entityName, Serializable id) throws DataAccessException {
		return load(entityName, id, null);
	}

	@Override
	public Object load(final String entityName, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return session.load(entityName, id, lockMode);
				}
				else {
					return session.load(entityName, id);
				}
			}
		});
	}

	@Override
	public <T> List<T> loadAll(final Class<T> entityClass) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<List<T>>() {
			@Override
			@SuppressWarnings("unchecked")
			public List<T> doInHibernate(Session session) throws HibernateException {
				Criteria criteria = session.createCriteria(entityClass);
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				prepareCriteria(criteria);
				return criteria.list();
			}
		});
	}

	@Override
	public void load(final Object entity, final Serializable id) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.load(entity, id);
				return null;
			}
		});
	}

	@Override
	public void refresh(final Object entity) throws DataAccessException {
		refresh(entity, null);
	}

	@Override
	public void refresh(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					session.refresh(entity, lockMode);
				}
				else {
					session.refresh(entity);
				}
				return null;
			}
		});
	}

	@Override
	public boolean contains(final Object entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Boolean>() {
			@Override
			public Boolean doInHibernate(Session session) {
				return session.contains(entity);
			}
		});
	}

	@Override
	public void evict(final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.evict(entity);
				return null;
			}
		});
	}

	@Override
	public void initialize(Object proxy) throws DataAccessException {
		try {
			Hibernate.initialize(proxy);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}

	@Override
	public Filter enableFilter(String filterName) throws IllegalStateException {
		Session session = SessionFactoryUtils.getSession(getSessionFactory(), false);
		Filter filter = session.getEnabledFilter(filterName);
		if (filter == null) {
			filter = session.enableFilter(filterName);
		}
		return filter;
	}


	//-------------------------------------------------------------------------
	// Convenience methods for storing individual objects
	//-------------------------------------------------------------------------

	@Override
	public void lock(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.lock(entity, lockMode);
				return null;
			}
		});
	}

	@Override
	public void lock(final String entityName, final Object entity, final LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.lock(entityName, entity, lockMode);
				return null;
			}
		});
	}

	@Override
	public Serializable save(final Object entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Serializable>() {
			@Override
			public Serializable doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return session.save(entity);
			}
		});
	}

	@Override
	public Serializable save(final String entityName, final Object entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Serializable>() {
			@Override
			public Serializable doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return session.save(entityName, entity);
			}
		});
	}

	@Override
	public void update(Object entity) throws DataAccessException {
		update(entity, null);
	}

	@Override
	public void update(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.update(entity);
				if (lockMode != null) {
					session.lock(entity, lockMode);
				}
				return null;
			}
		});
	}

	@Override
	public void update(String entityName, Object entity) throws DataAccessException {
		update(entityName, entity, null);
	}

	@Override
	public void update(final String entityName, final Object entity, final LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.update(entityName, entity);
				if (lockMode != null) {
					session.lock(entity, lockMode);
				}
				return null;
			}
		});
	}

	@Override
	public void saveOrUpdate(final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.saveOrUpdate(entity);
				return null;
			}
		});
	}

	@Override
	public void saveOrUpdate(final String entityName, final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.saveOrUpdate(entityName, entity);
				return null;
			}
		});
	}

	@Override
	public void saveOrUpdateAll(final Collection entities) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				for (Object entity : entities) {
					session.saveOrUpdate(entity);
				}
				return null;
			}
		});
	}

	@Override
	public void replicate(final Object entity, final ReplicationMode replicationMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.replicate(entity, replicationMode);
				return null;
			}
		});
	}

	@Override
	public void replicate(final String entityName, final Object entity, final ReplicationMode replicationMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.replicate(entityName, entity, replicationMode);
				return null;
			}
		});
	}

	@Override
	public void persist(final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.persist(entity);
				return null;
			}
		});
	}

	@Override
	public void persist(final String entityName, final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.persist(entityName, entity);
				return null;
			}
		});
	}

	@Override
	public <T> T merge(final T entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return (T) session.merge(entity);
			}
		});
	}

	@Override
	public <T> T merge(final String entityName, final T entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return (T) session.merge(entityName, entity);
			}
		});
	}

	@Override
	public void delete(Object entity) throws DataAccessException {
		delete(entity, null);
	}

	@Override
	public void delete(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				if (lockMode != null) {
					session.lock(entity, lockMode);
				}
				session.delete(entity);
				return null;
			}
		});
	}

	@Override
	public void delete(String entityName, Object entity) throws DataAccessException {
		delete(entityName, entity, null);
	}

	@Override
	public void delete(final String entityName, final Object entity, final LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				if (lockMode != null) {
					session.lock(entityName, entity, lockMode);
				}
				session.delete(entityName, entity);
				return null;
			}
		});
	}

	@Override
	public void deleteAll(final Collection entities) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				for (Object entity : entities) {
					session.delete(entity);
				}
				return null;
			}
		});
	}

	@Override
	public void flush() throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.flush();
				return null;
			}
		});
	}

	@Override
	public void clear() throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) {
				session.clear();
				return null;
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for HQL strings
	//-------------------------------------------------------------------------

	@Override
	public List find(String queryString) throws DataAccessException {
		return find(queryString, (Object[]) null);
	}

	@Override
	public List find(String queryString, Object value) throws DataAccessException {
		return find(queryString, new Object[] {value});
	}

	@Override
	public List find(final String queryString, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryString);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List findByNamedParam(String queryString, String paramName, Object value)
			throws DataAccessException {

		return findByNamedParam(queryString, new String[] {paramName}, new Object[] {value});
	}

	@Override
	public List findByNamedParam(final String queryString, final String[] paramNames, final Object[] values)
			throws DataAccessException {

		if (paramNames.length != values.length) {
			throw new IllegalArgumentException("Length of paramNames array must match length of values array");
		}
		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryString);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
					}
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List findByValueBean(final String queryString, final Object valueBean)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryString);
				prepareQuery(queryObject);
				queryObject.setProperties(valueBean);
				return queryObject.list();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for named queries
	//-------------------------------------------------------------------------

	@Override
	public List findByNamedQuery(String queryName) throws DataAccessException {
		return findByNamedQuery(queryName, (Object[]) null);
	}

	@Override
	public List findByNamedQuery(String queryName, Object value) throws DataAccessException {
		return findByNamedQuery(queryName, new Object[] {value});
	}

	@Override
	public List findByNamedQuery(final String queryName, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.getNamedQuery(queryName);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
			throws DataAccessException {

		return findByNamedQueryAndNamedParam(queryName, new String[] {paramName}, new Object[] {value});
	}

	@Override
	public List findByNamedQueryAndNamedParam(
			final String queryName, final String[] paramNames, final Object[] values)
			throws DataAccessException {

		if (paramNames != null && values != null && paramNames.length != values.length) {
			throw new IllegalArgumentException("Length of paramNames array must match length of values array");
		}
		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.getNamedQuery(queryName);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
					}
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List findByNamedQueryAndValueBean(final String queryName, final Object valueBean)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.getNamedQuery(queryName);
				prepareQuery(queryObject);
				queryObject.setProperties(valueBean);
				return queryObject.list();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for detached criteria
	//-------------------------------------------------------------------------

	@Override
	public List findByCriteria(DetachedCriteria criteria) throws DataAccessException {
		return findByCriteria(criteria, -1, -1);
	}

	@Override
	public List findByCriteria(final DetachedCriteria criteria, final int firstResult, final int maxResults)
			throws DataAccessException {

		Assert.notNull(criteria, "DetachedCriteria must not be null");
		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Criteria executableCriteria = criteria.getExecutableCriteria(session);
				prepareCriteria(executableCriteria);
				if (firstResult >= 0) {
					executableCriteria.setFirstResult(firstResult);
				}
				if (maxResults > 0) {
					executableCriteria.setMaxResults(maxResults);
				}
				return executableCriteria.list();
			}
		});
	}

	@Override
	public List findByExample(Object exampleEntity) throws DataAccessException {
		return findByExample(null, exampleEntity, -1, -1);
	}

	@Override
	public List findByExample(String entityName, Object exampleEntity) throws DataAccessException {
		return findByExample(entityName, exampleEntity, -1, -1);
	}

	@Override
	public List findByExample(Object exampleEntity, int firstResult, int maxResults) throws DataAccessException {
		return findByExample(null, exampleEntity, firstResult, maxResults);
	}

	@Override
	public List findByExample(
			final String entityName, final Object exampleEntity, final int firstResult, final int maxResults)
			throws DataAccessException {

		Assert.notNull(exampleEntity, "Example entity must not be null");
		return executeWithNativeSession(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException {
				Criteria executableCriteria = (entityName != null ?
						session.createCriteria(entityName) : session.createCriteria(exampleEntity.getClass()));
				executableCriteria.add(Example.create(exampleEntity));
				prepareCriteria(executableCriteria);
				if (firstResult >= 0) {
					executableCriteria.setFirstResult(firstResult);
				}
				if (maxResults > 0) {
					executableCriteria.setMaxResults(maxResults);
				}
				return executableCriteria.list();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience query methods for iteration and bulk updates/deletes
	//-------------------------------------------------------------------------

	@Override
	public Iterator iterate(String queryString) throws DataAccessException {
		return iterate(queryString, (Object[]) null);
	}

	@Override
	public Iterator iterate(String queryString, Object value) throws DataAccessException {
		return iterate(queryString, new Object[] {value});
	}

	@Override
	public Iterator iterate(final String queryString, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Iterator>() {
			@Override
			public Iterator doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryString);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.iterate();
			}
		});
	}

	@Override
	public void closeIterator(Iterator it) throws DataAccessException {
		try {
			Hibernate.close(it);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}

	@Override
	public int bulkUpdate(String queryString) throws DataAccessException {
		return bulkUpdate(queryString, (Object[]) null);
	}

	@Override
	public int bulkUpdate(String queryString, Object value) throws DataAccessException {
		return bulkUpdate(queryString, new Object[] {value});
	}

	@Override
	public int bulkUpdate(final String queryString, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryString);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.executeUpdate();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Helper methods used by the operations above
	//-------------------------------------------------------------------------

	/**
	 * Check whether write operations are allowed on the given Session.
	 * <p>Default implementation throws an InvalidDataAccessApiUsageException in
	 * case of {@code FlushMode.MANUAL}. Can be overridden in subclasses.
	 * @param session current Hibernate Session
	 * @throws InvalidDataAccessApiUsageException if write operations are not allowed
	 * @see #setCheckWriteOperations
	 * @see #getFlushMode()
	 * @see #FLUSH_EAGER
	 * @see org.hibernate.Session#getFlushMode()
	 * @see org.hibernate.FlushMode#MANUAL
	 */
	protected void checkWriteOperationAllowed(Session session) throws InvalidDataAccessApiUsageException {
		if (isCheckWriteOperations() && getFlushMode() != FLUSH_EAGER &&
				session.getFlushMode().lessThan(FlushMode.COMMIT)) {
			throw new InvalidDataAccessApiUsageException(
					"Write operations are not allowed in read-only mode (FlushMode.MANUAL): "+
					"Turn your Session into FlushMode.COMMIT/AUTO or remove 'readOnly' marker from transaction definition.");
		}
	}

	/**
	 * Prepare the given Query object, applying cache settings and/or
	 * a transaction timeout.
	 * @param queryObject the Query object to prepare
	 * @see #setCacheQueries
	 * @see #setQueryCacheRegion
	 * @see SessionFactoryUtils#applyTransactionTimeout
	 */
	protected void prepareQuery(Query queryObject) {
		if (isCacheQueries()) {
			queryObject.setCacheable(true);
			if (getQueryCacheRegion() != null) {
				queryObject.setCacheRegion(getQueryCacheRegion());
			}
		}
		if (getFetchSize() > 0) {
			queryObject.setFetchSize(getFetchSize());
		}
		if (getMaxResults() > 0) {
			queryObject.setMaxResults(getMaxResults());
		}
		SessionFactoryUtils.applyTransactionTimeout(queryObject, getSessionFactory());
	}

	/**
	 * Prepare the given Criteria object, applying cache settings and/or
	 * a transaction timeout.
	 * @param criteria the Criteria object to prepare
	 * @see #setCacheQueries
	 * @see #setQueryCacheRegion
	 * @see SessionFactoryUtils#applyTransactionTimeout
	 */
	protected void prepareCriteria(Criteria criteria) {
		if (isCacheQueries()) {
			criteria.setCacheable(true);
			if (getQueryCacheRegion() != null) {
				criteria.setCacheRegion(getQueryCacheRegion());
			}
		}
		if (getFetchSize() > 0) {
			criteria.setFetchSize(getFetchSize());
		}
		if (getMaxResults() > 0) {
			criteria.setMaxResults(getMaxResults());
		}
		SessionFactoryUtils.applyTransactionTimeout(criteria, getSessionFactory());
	}

	/**
	 * Apply the given name parameter to the given Query object.
	 * @param queryObject the Query object
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @throws HibernateException if thrown by the Query object
	 */
	protected void applyNamedParameterToQuery(Query queryObject, String paramName, Object value)
			throws HibernateException {

		if (value instanceof Collection) {
			queryObject.setParameterList(paramName, (Collection) value);
		}
		else if (value instanceof Object[]) {
			queryObject.setParameterList(paramName, (Object[]) value);
		}
		else {
			queryObject.setParameter(paramName, value);
		}
	}


	/**
	 * Invocation handler that suppresses close calls on Hibernate Sessions.
	 * Also prepares returned Query and Criteria objects.
	 * @see org.hibernate.Session#close
	 */
	private class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Session target;

		public CloseSuppressingInvocationHandler(Session target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Session interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of Session proxy.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("close")) {
				// Handle close method: suppress, not valid.
				return null;
			}

			// Invoke method on target Session.
			try {
				Object retVal = method.invoke(this.target, args);

				// If return value is a Query or Criteria, apply transaction timeout.
				// Applies to createQuery, getNamedQuery, createCriteria.
				if (retVal instanceof Query) {
					prepareQuery(((Query) retVal));
				}
				if (retVal instanceof Criteria) {
					prepareCriteria(((Criteria) retVal));
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
