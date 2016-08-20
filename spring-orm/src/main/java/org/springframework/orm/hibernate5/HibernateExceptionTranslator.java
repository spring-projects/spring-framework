/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.orm.hibernate5;

import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * {@link PersistenceExceptionTranslator} capable of translating {@link HibernateException}
 * instances to Spring's {@link DataAccessException} hierarchy. As of Spring 4.3.2 and
 * Hibernate 5.2, it also converts standard JPA {@link PersistenceException} instances.
 *
 * <p>Extended by {@link LocalSessionFactoryBean}, so there is no need to declare this
 * translator in addition to a {@code LocalSessionFactoryBean}.
 *
 * <p>When configuring the container with {@code @Configuration} classes, a {@code @Bean}
 * of this type must be registered manually.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @see SessionFactoryUtils#convertHibernateAccessException(HibernateException)
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible(RuntimeException)
 */
public class HibernateExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex);
		}
		if (ex instanceof PersistenceException && ex.getCause() instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex.getCause());
		}
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}

	/**
	 * Convert the given HibernateException to an appropriate exception from the
	 * {@code org.springframework.dao} hierarchy.
	 * @param ex HibernateException that occurred
	 * @return a corresponding DataAccessException
	 * @see SessionFactoryUtils#convertHibernateAccessException
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}

}
