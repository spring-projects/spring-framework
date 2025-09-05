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

package org.springframework.orm.jpa.hibernate;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.service.UnknownServiceException;
import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementation of Hibernate's {@link CurrentSessionContext} interface
 * that provides a Spring-managed current {@link Session}.
 *
 * <p>This CurrentSessionContext implementation can also be specified in custom
 * SessionFactory setup through the "hibernate.current_session_context_class"
 * property, with the fully qualified name of this class as value.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
@SuppressWarnings("serial")
public class SpringSessionContext implements CurrentSessionContext {

	private final SessionFactoryImplementor sessionFactory;

	private @Nullable TransactionManager transactionManager;

	private @Nullable CurrentSessionContext jtaSessionContext;


	/**
	 * Create a new SpringSessionContext for the given Hibernate SessionFactory.
	 * @param sessionFactory the SessionFactory to provide current Sessions for
	 */
	public SpringSessionContext(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		try {
			JtaPlatform jtaPlatform = sessionFactory.getServiceRegistry().requireService(JtaPlatform.class);
			this.transactionManager = jtaPlatform.retrieveTransactionManager();
			if (this.transactionManager != null) {
				this.jtaSessionContext = new SpringJtaSessionContext(sessionFactory);
			}
		}
		catch (Exception ex) {
			LogFactory.getLog(SpringSessionContext.class).warn(
					"Could not introspect Hibernate JtaPlatform for SpringJtaSessionContext", ex);
		}
	}


	/**
	 * Retrieve the Spring-managed Session for the current thread, if any.
	 */
	@Override
	public Session currentSession() throws HibernateException {
		Object value = TransactionSynchronizationManager.getResource(this.sessionFactory);
		SessionHolder holder = null;
		if (value instanceof Session session) {
			return session;
		}
		else if (value instanceof SessionHolder sessionHolder) {
			// HibernateTransactionManager
			if (sessionHolder.hasSession()) {
				Session session = sessionHolder.getSession();
				if (!sessionHolder.isSynchronizedWithTransaction() &&
						TransactionSynchronizationManager.isSynchronizationActive()) {
					TransactionSynchronizationManager.registerSynchronization(
							new SpringSessionSynchronization(sessionHolder, this.sessionFactory, false));
					sessionHolder.setSynchronizedWithTransaction(true);
					// Switch to FlushMode.AUTO, as we have to assume a thread-bound Session
					// with FlushMode.MANUAL, which needs to allow flushing within the transaction.
					FlushMode flushMode = session.getHibernateFlushMode();
					if (flushMode.equals(FlushMode.MANUAL) &&
							!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
						session.setHibernateFlushMode(FlushMode.AUTO);
						sessionHolder.setPreviousFlushMode(flushMode);
					}
				}
				return session;
			}
			holder = sessionHolder;
		}
		else if (value instanceof EntityManagerHolder entityManagerHolder) {
			// JpaTransactionManager
			return entityManagerHolder.getEntityManager().unwrap(Session.class);
		}

		if (this.transactionManager != null && this.jtaSessionContext != null) {
			try {
				if (this.transactionManager.getStatus() == Status.STATUS_ACTIVE) {
					Session session = this.jtaSessionContext.currentSession();
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.registerSynchronization(
								new SpringFlushSynchronization(session));
					}
					return session;
				}
			}
			catch (SystemException ex) {
				throw new HibernateException("JTA TransactionManager found but status check failed", ex);
			}
		}

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			Session session;
			DataSource dataSource = determineDataSource(this.sessionFactory);
			if (dataSource != null) {
				session = this.sessionFactory.withOptions()
						.connection(DataSourceUtils.getConnection(dataSource))
						.openSession();
			}
			else {
				session = this.sessionFactory.openSession();
			}
			if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
				session.setHibernateFlushMode(FlushMode.MANUAL);
			}
			if (holder != null) {
				holder.setSession(session);
			}
			else {
				bindSessionHolder(this.sessionFactory, new SessionHolder(session));
			}
			return session;
		}
		else {
			throw new HibernateException("Could not obtain transaction-synchronized Session for current thread");
		}
	}


	/**
	 * Obtain a {@link StatelessSession} for the current transaction.
	 * @param sessionFactory the target SessionFactory
	 * @return the current StatelessSession
	 */
	public static StatelessSession currentStatelessSession(SessionFactory sessionFactory) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new HibernateException("Could not obtain transaction-synchronized Session for current thread");
		}
		Object value = TransactionSynchronizationManager.getResource(sessionFactory);
		if (value instanceof StatelessSession statelessSession) {
			return statelessSession;
		}
		SessionHolder holder = null;
		if (value instanceof SessionHolder sessionHolder) {
			if (sessionHolder.hasStatelessSession()) {
				return sessionHolder.getStatelessSession();
			}
			holder = sessionHolder;
		}
		StatelessSession session = sessionFactory.openStatelessSession(determineConnection(sessionFactory, holder));
		if (holder != null) {
			holder.setStatelessSession(session);
		}
		else {
			bindSessionHolder(sessionFactory, new SessionHolder(session));
		}
		return session;
	}

	private static void bindSessionHolder(SessionFactory sessionFactory, SessionHolder holder) {
		TransactionSynchronizationManager.registerSynchronization(
				new SpringSessionSynchronization(holder, sessionFactory, true));
		TransactionSynchronizationManager.bindResource(sessionFactory, holder);
		holder.setSynchronizedWithTransaction(true);
	}

	private static Connection determineConnection(SessionFactory sessionFactory, @Nullable SessionHolder holder) {
		if (holder != null && holder.getSession() instanceof SessionImplementor session) {
			return session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
		}
		DataSource dataSource = determineDataSource(sessionFactory);
		if (dataSource != null) {
			return DataSourceUtils.getConnection(dataSource);
		}
		throw new IllegalStateException(
				"Cannot determine JDBC DataSource for Hibernate SessionFactory: " + sessionFactory);
	}

	/**
	 * Determine the DataSource of the given SessionFactory.
	 * @return the DataSource, or {@code null} if none found
	 * @see ConnectionProvider
	 */
	static @Nullable DataSource determineDataSource(SessionFactory sessionFactory) {
		Map<String, Object> props = sessionFactory.getProperties();
		if (props != null) {
			Object dataSourceValue = props.get(Environment.JAKARTA_NON_JTA_DATASOURCE);
			if (dataSourceValue instanceof DataSource dataSourceToUse) {
				return dataSourceToUse;
			}
		}
		if (sessionFactory instanceof SessionFactoryImplementor sfi) {
			try {
				ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
				if (cp != null) {
					return cp.unwrap(DataSource.class);
				}
			}
			catch (UnknownServiceException ex) {
				// Ignore - cannot determine
			}
		}
		return null;
	}

}
