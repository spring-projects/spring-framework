/*
 * Copyright 2002-2016 the original author or authors.
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

import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

/**
 * Hibernate-specific JPA tests.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 */
@SuppressWarnings("deprecation")
public class HibernateEntityManagerFactoryIntegrationTests extends
		AbstractContainerEntityManagerFactoryIntegrationTests {

	@Override
	protected String[] getConfigPaths() {
		return HIBERNATE_CONFIG_LOCATIONS;
	}

	public void testCanCastNativeEntityManagerFactoryToHibernateEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertTrue(emfi.getNativeEntityManagerFactory() instanceof HibernateEntityManagerFactory);
	}

	public void testCanCastSharedEntityManagerProxyToHibernateEntityManager() {
		assertTrue(sharedEntityManager instanceof HibernateEntityManager);
		HibernateEntityManager hibernateEntityManager = (HibernateEntityManager) sharedEntityManager;
		assertNotNull(hibernateEntityManager.getSession());
	}

	public void testCanUnwrapAopProxy() {
		EntityManager em = entityManagerFactory.createEntityManager();
		EntityManager proxy = ProxyFactory.getProxy(EntityManager.class, new SingletonTargetSource(em));
		assertTrue(em instanceof HibernateEntityManager);
		assertFalse(proxy instanceof HibernateEntityManager);
		assertTrue(proxy.unwrap(HibernateEntityManager.class) instanceof HibernateEntityManager);
		assertSame(em, proxy.unwrap(HibernateEntityManager.class));
		assertSame(em.getDelegate(), proxy.getDelegate());
	}

}
