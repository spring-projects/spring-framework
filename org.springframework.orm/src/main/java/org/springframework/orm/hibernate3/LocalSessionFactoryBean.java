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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Subclass of {@link SessionFactoryBuilder} adhering to Spring's
 * {@link org.springframework.beans.factory.FactoryBean FactoryBean} contract,
 * making it suitable for use in XML configuration.
 *
 * <p>A typical {@code LocalSessionFactoryBean} bean definition:
 *
 * <pre class="code">
 * {@code
 * <bean id="sessionFactory" class="org.springframework.orm.hibernate3.LocalSessionFactoryBean">
 *   <property name="dataSource" ref="dataSource"/>
 *   <property name="mappingLocations" value="classpath:com/foo/*.hbm.xml"/>
 * </bean>}</pre>
 *
 * <p>Implements the
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator
 * PersistenceExceptionTranslator} interface, as autodetected by Spring's {@link
 * org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * PersistenceExceptionTranslationPostProcessor}, for AOP-based translation of
 * native Hibernate exceptions to Spring's {@link DataAccessException} hierarchy.
 * Hence, the presence of an {@code LocalSessionFactoryBean} automatically
 * enables a {@code PersistenceExceptionTranslationPostProcessor} to translate
 * Hibernate exceptions.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.2
 * @see SessionFactoryBuilderSupport
 * @see SessionFactoryBeanOperations
 * @see org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean
 */
public class LocalSessionFactoryBean extends SessionFactoryBuilder implements SessionFactoryBeanOperations {

	private final SessionFactoryBeanDelegate delegate = new SessionFactoryBeanDelegate(this);

	@Deprecated
	public void setCacheProvider(org.hibernate.cache.CacheProvider cacheProvider) {
		delegate.setCacheProvider(cacheProvider);
	}

	@Override
	protected final void preBuildSessionFactory() {
		delegate.preBuildSessionFactory();
	}

	@Override
	protected final void postBuildSessionFactory() {
		delegate.postBuildSessionFactory();
	}

	public void destroy() throws HibernateException {
		delegate.destroy();
	}

	public SessionFactory getObject() throws Exception {
		return delegate.getSessionFactory();
	}

	public Class<? extends SessionFactory> getObjectType() {
		return delegate.getObjectType();
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		delegate.setBeanClassLoader(classLoader);
	}

	public boolean isSingleton() {
		return delegate.isSingleton();
	}

	public void afterPropertiesSet() throws Exception {
		delegate.afterPropertiesSet();
	}

	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return delegate.translateExceptionIfPossible(ex);
	}

	public void setJdbcExceptionTranslator(
			SQLExceptionTranslator jdbcExceptionTranslator) {
		delegate.setJdbcExceptionTranslator(jdbcExceptionTranslator);
	}

	public void setPersistenceExceptionTranslator(
			HibernateExceptionTranslator hibernateExceptionTranslator) {
		delegate.setPersistenceExceptionTranslator(hibernateExceptionTranslator);
	}

	/**
	 * @deprecated as of Spring 3.1 in favor of {@link #newSessionFactory()} which
	 * can access the internal {@code Configuration} instance via {@link #getConfiguration()}.
	 */
	@Deprecated
	protected SessionFactory newSessionFactory(org.hibernate.cfg.Configuration config) throws HibernateException {
		return this.newSessionFactory();
	}

	/**
	 * @deprecated as of Spring 3.1 in favor of {@link #postProcessMappings()} which
	 * can access the internal {@code Configuration} instance via {@link #getConfiguration()}.
	 */
	@Deprecated
	protected void postProcessMappings(org.hibernate.cfg.Configuration config) throws HibernateException {
		this.postProcessMappings();
	}

	/**
	 * @deprecated as of Spring 3.1 in favor of {@link #postProcessConfiguration()} which
	 * can access the internal {@code Configuration} instance via {@link #getConfiguration()}.
	 */
	@Deprecated
	protected void postProcessConfiguration(org.hibernate.cfg.Configuration config) throws HibernateException {
		this.postProcessConfiguration();
	}

}
