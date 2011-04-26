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

package org.springframework.orm.hibernate3;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cfg.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Encapsulates common {@link SessionFactoryBeanOperations} behavior in order
 * to avoid multiple-inheritance issues with {@code SessionFactoryBeanOperations}
 * implementations that need to extend {@code *SessionFactoryBuilder} types.
 *
 * <p>Maintainer's note: Unless otherwise documented, JavaDoc for all methods is
 * inherited from {@link SessionFactoryBeanOperations}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class SessionFactoryBeanDelegate implements SessionFactoryBeanOperations {

	private SessionFactory sessionFactory;

	private HibernateExceptionTranslator hibernateExceptionTranslator = new HibernateExceptionTranslator();

	private final SessionFactoryBuilderSupport<?> builder;

	private final ThreadContextClassLoaderHelper threadContextClassLoader = new ThreadContextClassLoaderHelper();

	@Deprecated
	private org.hibernate.cache.CacheProvider cacheProvider;

	@Deprecated
	private static final ThreadLocal<org.hibernate.cache.CacheProvider> configTimeCacheProviderHolder =
			new ThreadLocal<org.hibernate.cache.CacheProvider>();

	/**
	 * Create a new {@code SessionFactoryBeanDelegate}
	 * @param builder the {@code *SessionFactoryBuilder} that delegates to this
	 * instance.
	 */
	public SessionFactoryBeanDelegate(SessionFactoryBuilderSupport<?> builder) {
		this.builder = builder;
	}

	/**
	 * Return the {@code SessionFactory} maintained by this delegate.
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * Return the CacheProvider for the currently configured Hibernate SessionFactory,
	 * to be used by LocalCacheProviderProxy.
	 * <p>This instance will be set before initialization of the corresponding
	 * SessionFactory, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setCacheProvider
	 * @deprecated as of Spring 3.1 in favor of Hibernate's {@link RegionFactory} SPI
	 */
	@Deprecated
	public static org.hibernate.cache.CacheProvider getConfigTimeCacheProvider() {
		return configTimeCacheProviderHolder.get();
	}

	@Deprecated
	public void setCacheProvider(org.hibernate.cache.CacheProvider cacheProvider) {
		this.cacheProvider = cacheProvider;
	}

	public void setPersistenceExceptionTranslator(HibernateExceptionTranslator hibernateExceptionTranslator) {
		this.hibernateExceptionTranslator = hibernateExceptionTranslator;
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		builder.setClassLoader(beanClassLoader);
	}

	public SessionFactory getObject() {
		return this.sessionFactory;
	}

	public Class<? extends SessionFactory> getObjectType() {
		return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws Exception {
		SessionFactory rawSf = builder.doBuildSessionFactory();
		this.sessionFactory = builder.wrapSessionFactoryIfNecessary(rawSf);
		builder.afterSessionFactoryCreation();
	}

	public void destroy() throws HibernateException {
		SessionFactoryBuilderSupport.closeHibernateSessionFactory(this.builder, this.sessionFactory);
	}

	public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
		this.hibernateExceptionTranslator.setJdbcExceptionTranslator(jdbcExceptionTranslator);
	}

	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return hibernateExceptionTranslator.translateExceptionIfPossible(ex);
	}

	public SessionFactory wrapSessionFactoryIfNecessary(SessionFactory rawSf) {
		return rawSf;
	}


	/**
	 * @see SessionFactoryBuilderSupport#preBuildSessionFactory()
	 */
	@SuppressWarnings("deprecation")
	public void preBuildSessionFactory() {
		if (this.cacheProvider != null) {
			// Make Spring-provided Hibernate CacheProvider available.
			configTimeCacheProviderHolder.set(this.cacheProvider);
		}
		if (builder.getCacheRegionFactory() == null && this.cacheProvider != null) {
			// Expose Spring-provided Hibernate CacheProvider.
			builder.getConfiguration().setProperty(Environment.CACHE_PROVIDER, LocalCacheProviderProxy.class.getName());
		}

		// Analogous to Hibernate EntityManager's Ejb3Configuration:
		// Hibernate doesn't allow setting the bean ClassLoader explicitly,
		// so we need to expose it as thread context ClassLoader accordingly.
		threadContextClassLoader.overrideIfNecessary();
	}

	/**
	 * @see SessionFactoryBuilderSupport#postBuildSessionFactory
	 */
	public void postBuildSessionFactory() {
		if (this.cacheProvider != null) {
			configTimeCacheProviderHolder.remove();
		}
		threadContextClassLoader.resetIfNecessary();
	}


	private class ThreadContextClassLoaderHelper {
		private Thread currentThread = Thread.currentThread();
		private boolean shouldOverride = false;
		private ClassLoader originalLoader;

		void overrideIfNecessary() {
			this.originalLoader = currentThread.getContextClassLoader();
			this.shouldOverride =
					(builder.getBeanClassLoader() != null && !builder.getBeanClassLoader().equals(this.originalLoader));
			if (shouldOverride) {
				this.currentThread.setContextClassLoader(builder.getBeanClassLoader());
			}
		}

		void resetIfNecessary() {
			if (this.shouldOverride) {
				// Reset original thread context ClassLoader.
				this.currentThread.setContextClassLoader(this.originalLoader);
			}
		}
	}

}
