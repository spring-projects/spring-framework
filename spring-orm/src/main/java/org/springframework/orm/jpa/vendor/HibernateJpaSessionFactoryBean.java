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

package org.springframework.orm.jpa.vendor;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.util.Assert;

/**
 * Simple <code>FactoryBean</code> that exposes the underlying {@link SessionFactory}
 * behind a Hibernate-backed JPA {@link EntityManagerFactory}.
 *
 * <p>Primarily available for resolving a SessionFactory by JPA persistence unit name
 * via the {@link #setPersistenceUnitName "persistenceUnitName"} bean property.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setPersistenceUnitName
 * @see #setEntityManagerFactory
 */
public class HibernateJpaSessionFactoryBean extends EntityManagerFactoryAccessor implements FactoryBean<SessionFactory> {

	public SessionFactory getObject() {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.isInstanceOf(HibernateEntityManagerFactory.class, emf);
		return ((HibernateEntityManagerFactory) emf).getSessionFactory();
	}

	public Class<?> getObjectType() {
		return SessionFactory.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
