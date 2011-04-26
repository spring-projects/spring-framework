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
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

/**
 * Operations common to {@link LocalSessionFactoryBean} and
 * {@link AnnotationSessionFactoryBean} types but not available via
 * their respective {@code *Builder} supertypes.
 *
 * @author Chris Beams
 * @since 3.1
 * @see LocalSessionFactoryBean
 * @see AnnotationSessionFactoryBean
 * @see SessionFactoryBeanDelegate
 */
public interface SessionFactoryBeanOperations
		extends BeanClassLoaderAware, FactoryBean<SessionFactory>,
		InitializingBean, DisposableBean, PersistenceExceptionTranslator {

	/**
	 * <b>Deprecated.</b> <i>as of Spring 3.0 in favor of {@code setCacheRegionFactory}
	 * following Hibernate 3.3's deprecation of the {@code CacheProvider} SPI.</i>
	 * <p>Set the Hibernate {@code CacheProvider} to use for the {@code SessionFactory}.
	 * Allows for using a Spring-managed {@code CacheProvider} instance.
	 * <p>Note: If this is set, the Hibernate settings should not define a
	 * cache provider to avoid meaningless double configuration.
	 * of the {@code CacheProvider} SPI in favor of {@link RegionFactory} SPI.
	 */
	@Deprecated
	void setCacheProvider(org.hibernate.cache.CacheProvider cacheProvider);

	/**
	 * Customize the {@code HibernateExceptionTranslator} to be used when translating native
	 * {@code HibernateException} types to Spring's {@code DataAccessException} hierarchy.
	 */
	void setPersistenceExceptionTranslator(HibernateExceptionTranslator hibernateExceptionTranslator);

	/**
	 * Exists for compatibility with {@link BeanClassLoaderAware} but
	 * simply delegates to
	 * {@link SessionFactoryBuilderSupport#setClassLoader setClassLoader}.
	 */
	void setBeanClassLoader(ClassLoader beanClassLoader);

	/**
	 * Return the singleton SessionFactory.
	 */
	SessionFactory getObject() throws Exception;

	/**
	 * Return the SessionFactory class used.
	 */
	Class<? extends SessionFactory> getObjectType();

	/**
	 * Return {@code true}.
	 */
	boolean isSingleton();

	/**
	 * Build and expose the SessionFactory.
	 * @see SessionFactoryBuilderSupport#buildSessionFactory
	 * @see SessionFactoryBuilderSupport#doBuildSessionFactory
	 * @see SessionFactoryBuilderSupport#wrapSessionFactoryIfNecessary
	 * @see SessionFactoryBuilderSupport#afterSessionFactoryCreation
	 */
	void afterPropertiesSet() throws Exception;

	/**
	 * Close the SessionFactory on bean factory shutdown.
	 */
	void destroy() throws HibernateException;

	/**
	 * Set the JDBC exception translator for the SessionFactory
	 * on this instance's underlying {@link #setPersistenceExceptionTranslator
	 * HibernateExceptionTranslator}.
	 * <p>Applied to any SQLException root cause of a Hibernate JDBCException,
	 * overriding Hibernate's default SQLException translation (which is
	 * based on Hibernate's Dialect for a specific target database).
	 * @param jdbcExceptionTranslator the exception translator
	 * @see #setPersistenceExceptionTranslator(HibernateExceptionTranslator)
	 * @see HibernateExceptionTranslator#setJdbcExceptionTranslator
	 * @see java.sql.SQLException
	 * @see org.hibernate.JDBCException
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 * @see org.springframework.dao.support.PersistenceExceptionTranslator
	 */
	void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator);

	/**
	 * Implementation of the PersistenceExceptionTranslator interface,
	 * as autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
	 * <p>Converts the exception if it is a HibernateException;
	 * else returns <code>null</code> to indicate an unknown exception.
	 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
	 * @see #convertHibernateAccessException
	 */
	DataAccessException translateExceptionIfPossible(RuntimeException ex);

	/**
	 * Override the default {@link DisposableBean} proxying behavior in
	 * {@link SessionFactoryBuilderSupport#wrapSessionFactoryIfNecessary(SessionFactory)}
	 * and return the raw {@code SessionFactory} instance, as {@link SessionFactory#close()}
	 * will be called during this FactoryBean's normal {@linkplain #destroy() destruction lifecycle}.
	 */
	SessionFactory wrapSessionFactoryIfNecessary(SessionFactory rawSf);

}
