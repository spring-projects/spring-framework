/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.EntityManagerProxy;

import static org.junit.Assert.*;

/**
 * Hibernate-specific JPA tests.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 */
@SuppressWarnings("deprecation")
public class HibernateEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager.xml",
				"/org/springframework/orm/jpa/memdb.xml", "/org/springframework/orm/jpa/inject.xml"};
	}


	@Test
	public void testCanCastNativeEntityManagerFactoryToHibernateEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertTrue(emfi.getNativeEntityManagerFactory() instanceof org.hibernate.jpa.HibernateEntityManagerFactory);
		assertTrue(emfi.getNativeEntityManagerFactory() instanceof SessionFactory);  // as of Hibernate 5.2
	}

	@Test
	public void testCanCastSharedEntityManagerProxyToHibernateEntityManager() {
		assertTrue(sharedEntityManager instanceof org.hibernate.jpa.HibernateEntityManager);
		assertTrue(((EntityManagerProxy) sharedEntityManager).getTargetEntityManager() instanceof Session);  // as of Hibernate 5.2
	}

	@Test
	public void testCanUnwrapAopProxy() {
		EntityManager em = entityManagerFactory.createEntityManager();
		EntityManager proxy = ProxyFactory.getProxy(EntityManager.class, new SingletonTargetSource(em));
		assertTrue(em instanceof org.hibernate.jpa.HibernateEntityManager);
		assertFalse(proxy instanceof org.hibernate.jpa.HibernateEntityManager);
		assertTrue(proxy.unwrap(org.hibernate.jpa.HibernateEntityManager.class) != null);
		assertSame(em, proxy.unwrap(org.hibernate.jpa.HibernateEntityManager.class));
		assertSame(em.getDelegate(), proxy.getDelegate());
	}

	@Test  // SPR-16956
	public void testReadOnly() {
		assertSame(FlushMode.AUTO, sharedEntityManager.unwrap(Session.class).getHibernateFlushMode());
		assertFalse(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly());
		endTransaction();

		this.transactionDefinition.setReadOnly(true);
		startNewTransaction();
		assertSame(FlushMode.MANUAL, sharedEntityManager.unwrap(Session.class).getHibernateFlushMode());
		assertTrue(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly());
	}

}
