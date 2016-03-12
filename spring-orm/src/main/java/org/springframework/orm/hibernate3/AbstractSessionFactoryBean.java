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

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Abstract {@link org.springframework.beans.factory.FactoryBean} that creates
 * a Hibernate {@link org.hibernate.SessionFactory} within a Spring application
 * context, providing general infrastructure not related to Hibernate's
 * specific configuration API.
 *
 * <p>This class implements the
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator}
 * interface, as autodetected by Spring's
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor},
 * for AOP-based translation of native exceptions to Spring DataAccessExceptions.
 * Hence, the presence of e.g. LocalSessionFactoryBean automatically enables
 * a PersistenceExceptionTranslationPostProcessor to translate Hibernate exceptions.
 *
 * <p>This class mainly serves as common base class for {@link LocalSessionFactoryBean}.
 * For details on typical SessionFactory setup, see the LocalSessionFactoryBean javadoc.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setExposeTransactionAwareSessionFactory
 * @see org.hibernate.SessionFactory#getCurrentSession()
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public abstract class AbstractSessionFactoryBean extends HibernateExceptionTranslator
		implements FactoryBean<SessionFactory>, InitializingBean, DisposableBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private DataSource dataSource;

	private boolean useTransactionAwareDataSource = false;

	private boolean exposeTransactionAwareSessionFactory = true;

	private SessionFactory sessionFactory;


	/**
	 * Set the DataSource to be used by the SessionFactory.
	 * If set, this will override corresponding settings in Hibernate properties.
	 * <p>If this is set, the Hibernate settings should not define
	 * a connection provider to avoid meaningless double configuration.
	 * <p>If using HibernateTransactionManager as transaction strategy, consider
	 * proxying your target DataSource with a LazyConnectionDataSourceProxy.
	 * This defers fetching of an actual JDBC Connection until the first JDBC
	 * Statement gets executed, even within JDBC transactions (as performed by
	 * HibernateTransactionManager). Such lazy fetching is particularly beneficial
	 * for read-only operations, in particular if the chances of resolving the
	 * result in the second-level cache are high.
	 * <p>As JTA and transactional JNDI DataSources already provide lazy enlistment
	 * of JDBC Connections, LazyConnectionDataSourceProxy does not add value with
	 * JTA (i.e. Spring's JtaTransactionManager) as transaction strategy.
	 * @see #setUseTransactionAwareDataSource
	 * @see HibernateTransactionManager
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Return the DataSource to be used by the SessionFactory.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * Set whether to use a transaction-aware DataSource for the SessionFactory,
	 * i.e. whether to automatically wrap the passed-in DataSource with Spring's
	 * TransactionAwareDataSourceProxy.
	 * <p>Default is "false": LocalSessionFactoryBean is usually used with Spring's
	 * HibernateTransactionManager or JtaTransactionManager, both of which work nicely
	 * on a plain JDBC DataSource. Hibernate Sessions and their JDBC Connections are
	 * fully managed by the Hibernate/JTA transaction infrastructure in such a scenario.
	 * <p>If you switch this flag to "true", Spring's Hibernate access will be able to
	 * <i>participate in JDBC-based transactions managed outside of Hibernate</i>
	 * (for example, by Spring's DataSourceTransactionManager). This can be convenient
	 * if you need a different local transaction strategy for another O/R mapping tool,
	 * for example, but still want Hibernate access to join into those transactions.
	 * <p>A further benefit of this option is that <i>plain Sessions opened directly
	 * via the SessionFactory</i>, outside of Spring's Hibernate support, will still
	 * participate in active Spring-managed transactions. However, consider using
	 * Hibernate's {@code getCurrentSession()} method instead (see javadoc of
	 * "exposeTransactionAwareSessionFactory" property).
	 * <p><b>WARNING:</b> When using a transaction-aware JDBC DataSource in combination
	 * with OpenSessionInViewFilter/Interceptor, whether participating in JTA or
	 * external JDBC-based transactions, it is strongly recommended to set Hibernate's
	 * Connection release mode to "after_transaction" or "after_statement", which
	 * guarantees proper Connection handling in such a scenario. In contrast to that,
	 * HibernateTransactionManager generally requires release mode "on_close".
	 * <p>Note: If you want to use Hibernate's Connection release mode "after_statement"
	 * with a DataSource specified on this LocalSessionFactoryBean (for example, a
	 * JTA-aware DataSource fetched from JNDI), switch this setting to "true".
	 * Otherwise, the ConnectionProvider used underneath will vote against aggressive
	 * release and thus silently switch to release mode "after_transaction".
	 * @see #setDataSource
	 * @see #setExposeTransactionAwareSessionFactory
	 * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewFilter
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
	 * @see HibernateTransactionManager
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	public void setUseTransactionAwareDataSource(boolean useTransactionAwareDataSource) {
		this.useTransactionAwareDataSource = useTransactionAwareDataSource;
	}

	/**
	 * Return whether to use a transaction-aware DataSource for the SessionFactory.
	 */
	protected boolean isUseTransactionAwareDataSource() {
		return this.useTransactionAwareDataSource;
	}

	/**
	 * Set whether to expose a transaction-aware current Session from the
	 * SessionFactory's {@code getCurrentSession()} method, returning the
	 * Session that's associated with the current Spring-managed transaction, if any.
	 * <p>Default is "true", letting data access code work with the plain
	 * Hibernate SessionFactory and its {@code getCurrentSession()} method,
	 * while still being able to participate in current Spring-managed transactions:
	 * with any transaction management strategy, either local or JTA / EJB CMT,
	 * and any transaction synchronization mechanism, either Spring or JTA.
	 * Furthermore, {@code getCurrentSession()} will also seamlessly work with
	 * a request-scoped Session managed by OpenSessionInViewFilter/Interceptor.
	 * <p>Turn this flag off to expose the plain Hibernate SessionFactory with
	 * Hibernate's default {@code getCurrentSession()} behavior, supporting
	 * plain JTA synchronization only. Alternatively, simply override the
	 * corresponding Hibernate property "hibernate.current_session_context_class".
	 * @see SpringSessionContext
	 * @see org.hibernate.SessionFactory#getCurrentSession()
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 * @see HibernateTransactionManager
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewFilter
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
	 */
	public void setExposeTransactionAwareSessionFactory(boolean exposeTransactionAwareSessionFactory) {
		this.exposeTransactionAwareSessionFactory = exposeTransactionAwareSessionFactory;
	}

	/**
	 * Return whether to expose a transaction-aware proxy for the SessionFactory.
	 */
	protected boolean isExposeTransactionAwareSessionFactory() {
		return this.exposeTransactionAwareSessionFactory;
	}


	/**
	 * Build and expose the SessionFactory.
	 * @see #buildSessionFactory()
	 * @see #wrapSessionFactoryIfNecessary
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		SessionFactory rawSf = buildSessionFactory();
		this.sessionFactory = wrapSessionFactoryIfNecessary(rawSf);
		afterSessionFactoryCreation();
	}

	/**
	 * Wrap the given SessionFactory with a proxy, if demanded.
	 * <p>The default implementation simply returns the given SessionFactory as-is.
	 * Subclasses may override this to implement transaction awareness through
	 * a SessionFactory proxy, for example.
	 * @param rawSf the raw SessionFactory as built by {@link #buildSessionFactory()}
	 * @return the SessionFactory reference to expose
	 * @see #buildSessionFactory()
	 */
	protected SessionFactory wrapSessionFactoryIfNecessary(SessionFactory rawSf) {
		return rawSf;
	}

	/**
	 * Return the exposed SessionFactory.
	 * Will throw an exception if not initialized yet.
	 * @return the SessionFactory (never {@code null})
	 * @throws IllegalStateException if the SessionFactory has not been initialized yet
	 */
	protected final SessionFactory getSessionFactory() {
		if (this.sessionFactory == null) {
			throw new IllegalStateException("SessionFactory not initialized yet");
		}
		return this.sessionFactory;
	}

	/**
	 * Close the SessionFactory on bean factory shutdown.
	 */
	@Override
	public void destroy() throws HibernateException {
		logger.info("Closing Hibernate SessionFactory");
		try {
			beforeSessionFactoryDestruction();
		}
		finally {
			this.sessionFactory.close();
		}
	}


	/**
	 * Return the singleton SessionFactory.
	 */
	@Override
	public SessionFactory getObject() {
		return this.sessionFactory;
	}

	@Override
	public Class<? extends SessionFactory> getObjectType() {
		return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Build the underlying Hibernate SessionFactory.
	 * @return the raw SessionFactory (potentially to be wrapped with a
	 * transaction-aware proxy before it is exposed to the application)
	 * @throws Exception in case of initialization failure
	 */
	protected abstract SessionFactory buildSessionFactory() throws Exception;

	/**
	 * Hook that allows post-processing after the SessionFactory has been
	 * successfully created. The SessionFactory is already available through
	 * {@code getSessionFactory()} at this point.
	 * <p>This implementation is empty.
	 * @throws Exception in case of initialization failure
	 * @see #getSessionFactory()
	 */
	protected void afterSessionFactoryCreation() throws Exception {
	}

	/**
	 * Hook that allows shutdown processing before the SessionFactory
	 * will be closed. The SessionFactory is still available through
	 * {@code getSessionFactory()} at this point.
	 * <p>This implementation is empty.
	 * @see #getSessionFactory()
	 */
	protected void beforeSessionFactoryDestruction() {
	}

}
