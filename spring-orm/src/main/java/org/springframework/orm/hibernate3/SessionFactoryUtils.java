/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.JDBCException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PropertyValueException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;

import org.springframework.core.NamedThreadLocal;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.transaction.jta.SpringJtaSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class featuring methods for Hibernate Session handling,
 * allowing for reuse of Hibernate Session instances within transactions.
 * Also provides support for exception translation.
 *
 * <p>Supports synchronization with both Spring-managed JTA transactions
 * (see {@link org.springframework.transaction.jta.JtaTransactionManager})
 * and non-Spring JTA transactions (i.e. plain JTA or EJB CMT),
 * transparently providing transaction-scoped Hibernate Sessions.
 * Note that for non-Spring JTA transactions, a JTA TransactionManagerLookup
 * has to be specified in the Hibernate configuration.
 *
 * <p>Used internally by {@link HibernateTemplate}, {@link HibernateInterceptor}
 * and {@link HibernateTransactionManager}. Can also be used directly in
 * application code.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getSession
 * @see #releaseSession
 * @see HibernateTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public abstract class SessionFactoryUtils {

	/**
	 * Order value for TransactionSynchronization objects that clean up Hibernate Sessions.
	 * Returns {@code DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100}
	 * to execute Session cleanup before JDBC Connection cleanup, if any.
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
	 */
	public static final int SESSION_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);

	private static final ThreadLocal<Map<SessionFactory, Set<Session>>> deferredCloseHolder =
			new NamedThreadLocal<Map<SessionFactory, Set<Session>>>("Hibernate Sessions registered for deferred close");


	/**
	 * Determine the DataSource of the given SessionFactory.
	 * @param sessionFactory the SessionFactory to check
	 * @return the DataSource, or {@code null} if none found
	 * @see org.hibernate.engine.SessionFactoryImplementor#getConnectionProvider
	 * @see LocalDataSourceConnectionProvider
	 */
	public static DataSource getDataSource(SessionFactory sessionFactory) {
		if (sessionFactory instanceof SessionFactoryImplementor) {
			ConnectionProvider cp = ((SessionFactoryImplementor) sessionFactory).getConnectionProvider();
			if (cp instanceof LocalDataSourceConnectionProvider) {
				return ((LocalDataSourceConnectionProvider) cp).getDataSource();
			}
		}
		return null;
	}

	/**
	 * Create an appropriate SQLExceptionTranslator for the given SessionFactory.
	 * If a DataSource is found, a SQLErrorCodeSQLExceptionTranslator for the DataSource
	 * is created; else, a SQLStateSQLExceptionTranslator as fallback.
	 * @param sessionFactory the SessionFactory to create the translator for
	 * @return the SQLExceptionTranslator
	 * @see #getDataSource
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 */
	public static SQLExceptionTranslator newJdbcExceptionTranslator(SessionFactory sessionFactory) {
		DataSource ds = getDataSource(sessionFactory);
		if (ds != null) {
			return new SQLErrorCodeSQLExceptionTranslator(ds);
		}
		return new SQLStateSQLExceptionTranslator();
	}

	/**
	 * Try to retrieve the JTA TransactionManager from the given SessionFactory
	 * and/or Session. Check the passed-in SessionFactory for implementing
	 * SessionFactoryImplementor (the usual case), falling back to the
	 * SessionFactory reference that the Session itself carries.
	 * @param sessionFactory Hibernate SessionFactory
	 * @param session Hibernate Session (can also be {@code null})
	 * @return the JTA TransactionManager, if any
	 * @see javax.transaction.TransactionManager
	 * @see SessionFactoryImplementor#getTransactionManager
	 * @see Session#getSessionFactory
	 * @see org.hibernate.impl.SessionFactoryImpl
	 */
	public static TransactionManager getJtaTransactionManager(SessionFactory sessionFactory, Session session) {
		SessionFactoryImplementor sessionFactoryImpl = null;
		if (sessionFactory instanceof SessionFactoryImplementor) {
			sessionFactoryImpl = ((SessionFactoryImplementor) sessionFactory);
		}
		else if (session != null) {
			SessionFactory internalFactory = session.getSessionFactory();
			if (internalFactory instanceof SessionFactoryImplementor) {
				sessionFactoryImpl = (SessionFactoryImplementor) internalFactory;
			}
		}
		return (sessionFactoryImpl != null ? sessionFactoryImpl.getTransactionManager() : null);
	}


	/**
	 * Get a Hibernate Session for the given SessionFactory. Is aware of and will
	 * return any existing corresponding Session bound to the current thread, for
	 * example when using {@link HibernateTransactionManager}. Will create a new
	 * Session otherwise, if "allowCreate" is {@code true}.
	 * <p>This is the {@code getSession} method used by typical data access code,
	 * in combination with {@code releaseSession} called when done with
	 * the Session. Note that HibernateTemplate allows to write data access code
	 * without caring about such resource handling.
	 * @param sessionFactory Hibernate SessionFactory to create the session with
	 * @param allowCreate whether a non-transactional Session should be created
	 * when no transactional Session can be found for the current thread
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and
	 * "allowCreate" is {@code false}
	 * @see #getSession(SessionFactory, Interceptor, SQLExceptionTranslator)
	 * @see #releaseSession
	 * @see HibernateTemplate
	 */
	public static Session getSession(SessionFactory sessionFactory, boolean allowCreate)
			throws DataAccessResourceFailureException, IllegalStateException {

		try {
			return doGetSession(sessionFactory, null, null, allowCreate);
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	/**
	 * Get a Hibernate Session for the given SessionFactory. Is aware of and will
	 * return any existing corresponding Session bound to the current thread, for
	 * example when using {@link HibernateTransactionManager}. Will always create
	 * a new Session otherwise.
	 * <p>Supports setting a Session-level Hibernate entity interceptor that allows
	 * to inspect and change property values before writing to and reading from the
	 * database. Such an interceptor can also be set at the SessionFactory level
	 * (i.e. on LocalSessionFactoryBean), on HibernateTransactionManager, etc.
	 * @param sessionFactory Hibernate SessionFactory to create the session with
	 * @param entityInterceptor Hibernate entity interceptor, or {@code null} if none
	 * @param jdbcExceptionTranslator SQLExcepionTranslator to use for flushing the
	 * Session on transaction synchronization (may be {@code null}; only used
	 * when actually registering a transaction synchronization)
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @see LocalSessionFactoryBean#setEntityInterceptor
	 * @see HibernateTemplate#setEntityInterceptor
	 */
	public static Session getSession(
			SessionFactory sessionFactory, Interceptor entityInterceptor,
			SQLExceptionTranslator jdbcExceptionTranslator) throws DataAccessResourceFailureException {

		try {
			return doGetSession(sessionFactory, entityInterceptor, jdbcExceptionTranslator, true);
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	/**
	 * Get a Hibernate Session for the given SessionFactory. Is aware of and will
	 * return any existing corresponding Session bound to the current thread, for
	 * example when using {@link HibernateTransactionManager}. Will create a new
	 * Session otherwise, if "allowCreate" is {@code true}.
	 * <p>Throws the original HibernateException, in contrast to {@link #getSession}.
	 * @param sessionFactory Hibernate SessionFactory to create the session with
	 * @param allowCreate whether a non-transactional Session should be created
	 * when no transactional Session can be found for the current thread
	 * @return the Hibernate Session
	 * @throws HibernateException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate false
	 */
	public static Session doGetSession(SessionFactory sessionFactory, boolean allowCreate)
			throws HibernateException, IllegalStateException {

		return doGetSession(sessionFactory, null, null, allowCreate);
	}

	/**
	 * Get a Hibernate Session for the given SessionFactory. Is aware of and will
	 * return any existing corresponding Session bound to the current thread, for
	 * example when using {@link HibernateTransactionManager}. Will create a new
	 * Session otherwise, if "allowCreate" is {@code true}.
	 * <p>Same as {@link #getSession}, but throwing the original HibernateException.
	 * @param sessionFactory Hibernate SessionFactory to create the session with
	 * @param entityInterceptor Hibernate entity interceptor, or {@code null} if none
	 * @param jdbcExceptionTranslator SQLExcepionTranslator to use for flushing the
	 * Session on transaction synchronization (may be {@code null})
	 * @param allowCreate whether a non-transactional Session should be created
	 * when no transactional Session can be found for the current thread
	 * @return the Hibernate Session
	 * @throws HibernateException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and
	 * "allowCreate" is {@code false}
	 */
	private static Session doGetSession(
			SessionFactory sessionFactory, Interceptor entityInterceptor,
			SQLExceptionTranslator jdbcExceptionTranslator, boolean allowCreate)
			throws HibernateException, IllegalStateException {

		Assert.notNull(sessionFactory, "No SessionFactory specified");

		Object resource = TransactionSynchronizationManager.getResource(sessionFactory);
		if (resource instanceof Session) {
			return (Session) resource;
		}
		SessionHolder sessionHolder = (SessionHolder) resource;
		if (sessionHolder != null && !sessionHolder.isEmpty()) {
			// pre-bound Hibernate Session
			Session session = null;
			if (TransactionSynchronizationManager.isSynchronizationActive() &&
					sessionHolder.doesNotHoldNonDefaultSession()) {
				// Spring transaction management is active ->
				// register pre-bound Session with it for transactional flushing.
				session = sessionHolder.getValidatedSession();
				if (session != null && !sessionHolder.isSynchronizedWithTransaction()) {
					logger.debug("Registering Spring transaction synchronization for existing Hibernate Session");
					TransactionSynchronizationManager.registerSynchronization(
							new SpringSessionSynchronization(sessionHolder, sessionFactory, jdbcExceptionTranslator, false));
					sessionHolder.setSynchronizedWithTransaction(true);
					// Switch to FlushMode.AUTO, as we have to assume a thread-bound Session
					// with FlushMode.MANUAL, which needs to allow flushing within the transaction.
					FlushMode flushMode = session.getFlushMode();
					if (flushMode.lessThan(FlushMode.COMMIT) &&
							!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
						session.setFlushMode(FlushMode.AUTO);
						sessionHolder.setPreviousFlushMode(flushMode);
					}
				}
			}
			else {
				// No Spring transaction management active -> try JTA transaction synchronization.
				session = getJtaSynchronizedSession(sessionHolder, sessionFactory, jdbcExceptionTranslator);
			}
			if (session != null) {
				return session;
			}
		}

		logger.debug("Opening Hibernate Session");
		Session session = (entityInterceptor != null ?
				sessionFactory.openSession(entityInterceptor) : sessionFactory.openSession());

		// Use same Session for further Hibernate actions within the transaction.
		// Thread object will get removed by synchronization at transaction completion.
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			// We're within a Spring-managed transaction, possibly from JtaTransactionManager.
			logger.debug("Registering Spring transaction synchronization for new Hibernate Session");
			SessionHolder holderToUse = sessionHolder;
			if (holderToUse == null) {
				holderToUse = new SessionHolder(session);
			}
			else {
				holderToUse.addSession(session);
			}
			if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
				session.setFlushMode(FlushMode.MANUAL);
			}
			TransactionSynchronizationManager.registerSynchronization(
					new SpringSessionSynchronization(holderToUse, sessionFactory, jdbcExceptionTranslator, true));
			holderToUse.setSynchronizedWithTransaction(true);
			if (holderToUse != sessionHolder) {
				TransactionSynchronizationManager.bindResource(sessionFactory, holderToUse);
			}
		}
		else {
			// No Spring transaction management active -> try JTA transaction synchronization.
			registerJtaSynchronization(session, sessionFactory, jdbcExceptionTranslator, sessionHolder);
		}

		// Check whether we are allowed to return the Session.
		if (!allowCreate && !isSessionTransactional(session, sessionFactory)) {
			closeSession(session);
			throw new IllegalStateException("No Hibernate Session bound to thread, " +
				"and configuration does not allow creation of non-transactional one here");
		}

		return session;
	}

	/**
	 * Retrieve a Session from the given SessionHolder, potentially from a
	 * JTA transaction synchronization.
	 * @param sessionHolder the SessionHolder to check
	 * @param sessionFactory the SessionFactory to get the JTA TransactionManager from
	 * @param jdbcExceptionTranslator SQLExcepionTranslator to use for flushing the
	 * Session on transaction synchronization (may be {@code null})
	 * @return the associated Session, if any
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 */
	private static Session getJtaSynchronizedSession(
			SessionHolder sessionHolder, SessionFactory sessionFactory,
			SQLExceptionTranslator jdbcExceptionTranslator) throws DataAccessResourceFailureException {

		// JTA synchronization is only possible with a javax.transaction.TransactionManager.
		// We'll check the Hibernate SessionFactory: If a TransactionManagerLookup is specified
		// in Hibernate configuration, it will contain a TransactionManager reference.
		TransactionManager jtaTm = getJtaTransactionManager(sessionFactory, sessionHolder.getAnySession());
		if (jtaTm != null) {
			// Check whether JTA transaction management is active ->
			// fetch pre-bound Session for the current JTA transaction, if any.
			// (just necessary for JTA transaction suspension, with an individual
			// Hibernate Session per currently active/suspended transaction)
			try {
				// Look for transaction-specific Session.
				Transaction jtaTx = jtaTm.getTransaction();
				if (jtaTx != null) {
					int jtaStatus = jtaTx.getStatus();
					if (jtaStatus == Status.STATUS_ACTIVE || jtaStatus == Status.STATUS_MARKED_ROLLBACK) {
						Session session = sessionHolder.getValidatedSession(jtaTx);
						if (session == null && !sessionHolder.isSynchronizedWithTransaction()) {
							// No transaction-specific Session found: If not already marked as
							// synchronized with transaction, register the default thread-bound
							// Session as JTA-transactional. If there is no default Session,
							// we're a new inner JTA transaction with an outer one being suspended:
							// In that case, we'll return null to trigger opening of a new Session.
							session = sessionHolder.getValidatedSession();
							if (session != null) {
								logger.debug("Registering JTA transaction synchronization for existing Hibernate Session");
								sessionHolder.addSession(jtaTx, session);
								jtaTx.registerSynchronization(
										new SpringJtaSynchronizationAdapter(
												new SpringSessionSynchronization(sessionHolder, sessionFactory, jdbcExceptionTranslator, false),
												jtaTm));
								sessionHolder.setSynchronizedWithTransaction(true);
								// Switch to FlushMode.AUTO, as we have to assume a thread-bound Session
								// with FlushMode.NEVER, which needs to allow flushing within the transaction.
								FlushMode flushMode = session.getFlushMode();
								if (flushMode.lessThan(FlushMode.COMMIT)) {
									session.setFlushMode(FlushMode.AUTO);
									sessionHolder.setPreviousFlushMode(flushMode);
								}
							}
						}
						return session;
					}
				}
				// No transaction active -> simply return default thread-bound Session, if any
				// (possibly from OpenSessionInViewFilter/Interceptor).
				return sessionHolder.getValidatedSession();
			}
			catch (Throwable ex) {
				throw new DataAccessResourceFailureException("Could not check JTA transaction", ex);
			}
		}
		else {
			// No JTA TransactionManager -> simply return default thread-bound Session, if any
			// (possibly from OpenSessionInViewFilter/Interceptor).
			return sessionHolder.getValidatedSession();
		}
	}

	/**
	 * Register a JTA synchronization for the given Session, if any.
	 * @param sessionHolder the existing thread-bound SessionHolder, if any
	 * @param session the Session to register
	 * @param sessionFactory the SessionFactory that the Session was created with
	 * @param jdbcExceptionTranslator SQLExcepionTranslator to use for flushing the
	 * Session on transaction synchronization (may be {@code null})
	 */
	private static void registerJtaSynchronization(Session session, SessionFactory sessionFactory,
			SQLExceptionTranslator jdbcExceptionTranslator, SessionHolder sessionHolder) {

		// JTA synchronization is only possible with a javax.transaction.TransactionManager.
		// We'll check the Hibernate SessionFactory: If a TransactionManagerLookup is specified
		// in Hibernate configuration, it will contain a TransactionManager reference.
		TransactionManager jtaTm = getJtaTransactionManager(sessionFactory, session);
		if (jtaTm != null) {
			try {
				Transaction jtaTx = jtaTm.getTransaction();
				if (jtaTx != null) {
					int jtaStatus = jtaTx.getStatus();
					if (jtaStatus == Status.STATUS_ACTIVE || jtaStatus == Status.STATUS_MARKED_ROLLBACK) {
						logger.debug("Registering JTA transaction synchronization for new Hibernate Session");
						SessionHolder holderToUse = sessionHolder;
						// Register JTA Transaction with existing SessionHolder.
						// Create a new SessionHolder if none existed before.
						if (holderToUse == null) {
							holderToUse = new SessionHolder(jtaTx, session);
						}
						else {
							holderToUse.addSession(jtaTx, session);
						}
						jtaTx.registerSynchronization(
								new SpringJtaSynchronizationAdapter(
										new SpringSessionSynchronization(holderToUse, sessionFactory, jdbcExceptionTranslator, true),
										jtaTm));
						holderToUse.setSynchronizedWithTransaction(true);
						if (holderToUse != sessionHolder) {
							TransactionSynchronizationManager.bindResource(sessionFactory, holderToUse);
						}
					}
				}
			}
			catch (Throwable ex) {
				throw new DataAccessResourceFailureException(
						"Could not register synchronization with JTA TransactionManager", ex);
			}
		}
	}


	/**
	 * Get a new Hibernate Session from the given SessionFactory.
	 * Will return a new Session even if there already is a pre-bound
	 * Session for the given SessionFactory.
	 * <p>Within a transaction, this method will create a new Session
	 * that shares the transaction's JDBC Connection. More specifically,
	 * it will use the same JDBC Connection as the pre-bound Hibernate Session.
	 * @param sessionFactory Hibernate SessionFactory to create the session with
	 * @return the new Session
	 */
	public static Session getNewSession(SessionFactory sessionFactory) {
		return getNewSession(sessionFactory, null);
	}

	/**
	 * Get a new Hibernate Session from the given SessionFactory.
	 * Will return a new Session even if there already is a pre-bound
	 * Session for the given SessionFactory.
	 * <p>Within a transaction, this method will create a new Session
	 * that shares the transaction's JDBC Connection. More specifically,
	 * it will use the same JDBC Connection as the pre-bound Hibernate Session.
	 * @param sessionFactory Hibernate SessionFactory to create the session with
	 * @param entityInterceptor Hibernate entity interceptor, or {@code null} if none
	 * @return the new Session
	 */
	public static Session getNewSession(SessionFactory sessionFactory, Interceptor entityInterceptor) {
		Assert.notNull(sessionFactory, "No SessionFactory specified");

		try {
			SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
			if (sessionHolder != null && !sessionHolder.isEmpty()) {
				if (entityInterceptor != null) {
					return sessionFactory.openSession(sessionHolder.getAnySession().connection(), entityInterceptor);
				}
				else {
					return sessionFactory.openSession(sessionHolder.getAnySession().connection());
				}
			}
			else {
				if (entityInterceptor != null) {
					return sessionFactory.openSession(entityInterceptor);
				}
				else {
					return sessionFactory.openSession();
				}
			}
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}


	/**
	 * Stringify the given Session for debug logging.
	 * Returns output equivalent to {@code Object.toString()}:
	 * the fully qualified class name + "@" + the identity hash code.
	 * <p>The sole reason why this is necessary is because Hibernate3's
	 * {@code Session.toString()} implementation is broken (and won't be fixed):
	 * it logs the toString representation of all persistent objects in the Session,
	 * which might lead to ConcurrentModificationExceptions if the persistent objects
	 * in turn refer to the Session (for example, for lazy loading).
	 * @param session the Hibernate Session to stringify
	 * @return the String representation of the given Session
	 */
	public static String toString(Session session) {
		return session.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(session));
	}

	/**
	 * Return whether there is a transactional Hibernate Session for the current thread,
	 * that is, a Session bound to the current thread by Spring's transaction facilities.
	 * @param sessionFactory Hibernate SessionFactory to check (may be {@code null})
	 * @return whether there is a transactional Session for current thread
	 */
	public static boolean hasTransactionalSession(SessionFactory sessionFactory) {
		if (sessionFactory == null) {
			return false;
		}
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		return (sessionHolder != null && !sessionHolder.isEmpty());
	}

	/**
	 * Return whether the given Hibernate Session is transactional, that is,
	 * bound to the current thread by Spring's transaction facilities.
	 * @param session the Hibernate Session to check
	 * @param sessionFactory Hibernate SessionFactory that the Session was created with
	 * (may be {@code null})
	 * @return whether the Session is transactional
	 */
	public static boolean isSessionTransactional(Session session, SessionFactory sessionFactory) {
		if (sessionFactory == null) {
			return false;
		}
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		return (sessionHolder != null && sessionHolder.containsSession(session));
	}

	/**
	 * Apply the current transaction timeout, if any, to the given
	 * Hibernate Query object.
	 * @param query the Hibernate Query object
	 * @param sessionFactory Hibernate SessionFactory that the Query was created for
	 * (may be {@code null})
	 * @see org.hibernate.Query#setTimeout
	 */
	public static void applyTransactionTimeout(Query query, SessionFactory sessionFactory) {
		Assert.notNull(query, "No Query object specified");
		if (sessionFactory != null) {
			SessionHolder sessionHolder =
					(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
			if (sessionHolder != null && sessionHolder.hasTimeout()) {
				query.setTimeout(sessionHolder.getTimeToLiveInSeconds());
			}
		}
	}

	/**
	 * Apply the current transaction timeout, if any, to the given
	 * Hibernate Criteria object.
	 * @param criteria the Hibernate Criteria object
	 * @param sessionFactory Hibernate SessionFactory that the Criteria was created for
	 * @see org.hibernate.Criteria#setTimeout
	 */
	public static void applyTransactionTimeout(Criteria criteria, SessionFactory sessionFactory) {
		Assert.notNull(criteria, "No Criteria object specified");
		if (sessionFactory != null) {
			SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
			if (sessionHolder != null && sessionHolder.hasTimeout()) {
				criteria.setTimeout(sessionHolder.getTimeToLiveInSeconds());
			}
		}
	}

	/**
	 * Convert the given HibernateException to an appropriate exception
	 * from the {@code org.springframework.dao} hierarchy.
	 * @param ex HibernateException that occurred
	 * @return the corresponding DataAccessException instance
	 * @see HibernateAccessor#convertHibernateAccessException
	 * @see HibernateTransactionManager#convertHibernateAccessException
	 */
	public static DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (ex instanceof JDBCConnectionException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof SQLGrammarException) {
			SQLGrammarException jdbcEx = (SQLGrammarException) ex;
			return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof LockAcquisitionException) {
			LockAcquisitionException jdbcEx = (LockAcquisitionException) ex;
			return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof ConstraintViolationException) {
			ConstraintViolationException jdbcEx = (ConstraintViolationException) ex;
			return new DataIntegrityViolationException(ex.getMessage()  + "; SQL [" + jdbcEx.getSQL() +
					"]; constraint [" + jdbcEx.getConstraintName() + "]", ex);
		}
		if (ex instanceof DataException) {
			DataException jdbcEx = (DataException) ex;
			return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof JDBCException) {
			return new HibernateJdbcException((JDBCException) ex);
		}
		// end of JDBCException (subclass) handling

		if (ex instanceof QueryException) {
			return new HibernateQueryException((QueryException) ex);
		}
		if (ex instanceof NonUniqueResultException) {
			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof NonUniqueObjectException) {
			return new DuplicateKeyException(ex.getMessage(), ex);
		}
		if (ex instanceof PropertyValueException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		if (ex instanceof PersistentObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof TransientObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof ObjectDeletedException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof UnresolvableObjectException) {
			return new HibernateObjectRetrievalFailureException((UnresolvableObjectException) ex);
		}
		if (ex instanceof WrongClassException) {
			return new HibernateObjectRetrievalFailureException((WrongClassException) ex);
		}
		if (ex instanceof StaleObjectStateException) {
			return new HibernateOptimisticLockingFailureException((StaleObjectStateException) ex);
		}
		if (ex instanceof StaleStateException) {
			return new HibernateOptimisticLockingFailureException((StaleStateException) ex);
		}

		// fallback
		return new HibernateSystemException(ex);
	}


	/**
	 * Determine whether deferred close is active for the current thread
	 * and the given SessionFactory.
	 * @param sessionFactory the Hibernate SessionFactory to check
	 * @return whether deferred close is active
	 */
	public static boolean isDeferredCloseActive(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "No SessionFactory specified");
		Map<SessionFactory, Set<Session>> holderMap = deferredCloseHolder.get();
		return (holderMap != null && holderMap.containsKey(sessionFactory));
	}

	/**
	 * Initialize deferred close for the current thread and the given SessionFactory.
	 * Sessions will not be actually closed on close calls then, but rather at a
	 * {@link #processDeferredClose} call at a finishing point (like request completion).
	 * <p>Used by {@link org.springframework.orm.hibernate3.support.OpenSessionInViewFilter}
	 * and {@link org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor}
	 * when not configured for a single session.
	 * @param sessionFactory the Hibernate SessionFactory to initialize deferred close for
	 * @see #processDeferredClose
	 * @see #releaseSession
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewFilter#setSingleSession
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor#setSingleSession
	 */
	public static void initDeferredClose(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "No SessionFactory specified");
		logger.debug("Initializing deferred close of Hibernate Sessions");
		Map<SessionFactory, Set<Session>> holderMap = deferredCloseHolder.get();
		if (holderMap == null) {
			holderMap = new HashMap<SessionFactory, Set<Session>>();
			deferredCloseHolder.set(holderMap);
		}
		holderMap.put(sessionFactory, new LinkedHashSet<Session>(4));
	}

	/**
	 * Process all Hibernate Sessions that have been registered for deferred close
	 * for the given SessionFactory.
	 * @param sessionFactory the Hibernate SessionFactory to process deferred close for
	 * @see #initDeferredClose
	 * @see #releaseSession
	 */
	public static void processDeferredClose(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "No SessionFactory specified");
		Map<SessionFactory, Set<Session>> holderMap = deferredCloseHolder.get();
		if (holderMap == null || !holderMap.containsKey(sessionFactory)) {
			throw new IllegalStateException("Deferred close not active for SessionFactory [" + sessionFactory + "]");
		}
		logger.debug("Processing deferred close of Hibernate Sessions");
		Set<Session> sessions = holderMap.remove(sessionFactory);
		for (Session session : sessions) {
			closeSession(session);
		}
		if (holderMap.isEmpty()) {
			deferredCloseHolder.remove();
		}
	}

	/**
	 * Close the given Session, created via the given factory,
	 * if it is not managed externally (i.e. not bound to the thread).
	 * @param session the Hibernate Session to close (may be {@code null})
	 * @param sessionFactory Hibernate SessionFactory that the Session was created with
	 * (may be {@code null})
	 */
	public static void releaseSession(Session session, SessionFactory sessionFactory) {
		if (session == null) {
			return;
		}
		// Only close non-transactional Sessions.
		if (!isSessionTransactional(session, sessionFactory)) {
			closeSessionOrRegisterDeferredClose(session, sessionFactory);
		}
	}

	/**
	 * Close the given Session or register it for deferred close.
	 * @param session the Hibernate Session to close
	 * @param sessionFactory Hibernate SessionFactory that the Session was created with
	 * (may be {@code null})
	 * @see #initDeferredClose
	 * @see #processDeferredClose
	 */
	static void closeSessionOrRegisterDeferredClose(Session session, SessionFactory sessionFactory) {
		Map<SessionFactory, Set<Session>> holderMap = deferredCloseHolder.get();
		if (holderMap != null && sessionFactory != null && holderMap.containsKey(sessionFactory)) {
			logger.debug("Registering Hibernate Session for deferred close");
			// Switch Session to FlushMode.MANUAL for remaining lifetime.
			session.setFlushMode(FlushMode.MANUAL);
			Set<Session> sessions = holderMap.get(sessionFactory);
			sessions.add(session);
		}
		else {
			closeSession(session);
		}
	}

	/**
	 * Perform actual closing of the Hibernate Session,
	 * catching and logging any cleanup exceptions thrown.
	 * @param session the Hibernate Session to close (may be {@code null})
	 * @see org.hibernate.Session#close()
	 */
	public static void closeSession(Session session) {
		if (session != null) {
			logger.debug("Closing Hibernate Session");
			try {
				session.close();
			}
			catch (HibernateException ex) {
				logger.debug("Could not close Hibernate Session", ex);
			}
			catch (Throwable ex) {
				logger.debug("Unexpected exception on closing Hibernate Session", ex);
			}
		}
	}

}
