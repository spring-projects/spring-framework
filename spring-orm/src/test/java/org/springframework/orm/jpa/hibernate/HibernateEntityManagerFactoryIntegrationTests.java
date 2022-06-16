/*
 * Copyright 2002-2019 the original author or authors.
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

import jakarta.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.EntityManagerProxy;

import static org.assertj.core.api.Assertions.assertThat;

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
		boolean condition1 = emfi.getNativeEntityManagerFactory() instanceof org.hibernate.jpa.HibernateEntityManagerFactory;
		assertThat(condition1).isTrue();
		// as of Hibernate 5.2
		boolean condition = emfi.getNativeEntityManagerFactory() instanceof SessionFactory;
		assertThat(condition).isTrue();
	}

	@Test
	public void testCanCastSharedEntityManagerProxyToHibernateEntityManager() {
		boolean condition1 = sharedEntityManager instanceof org.hibernate.jpa.HibernateEntityManager;
		assertThat(condition1).isTrue();
		// as of Hibernate 5.2
		boolean condition = ((EntityManagerProxy) sharedEntityManager).getTargetEntityManager() instanceof Session;
		assertThat(condition).isTrue();
	}

	@Test
	public void testCanUnwrapAopProxy() {
		EntityManager em = entityManagerFactory.createEntityManager();
		EntityManager proxy = ProxyFactory.getProxy(EntityManager.class, new SingletonTargetSource(em));
		boolean condition = em instanceof org.hibernate.jpa.HibernateEntityManager;
		assertThat(condition).isTrue();
		boolean condition1 = proxy instanceof org.hibernate.jpa.HibernateEntityManager;
		assertThat(condition1).isFalse();
		assertThat(proxy.unwrap(org.hibernate.jpa.HibernateEntityManager.class) != null).isTrue();
		assertThat(proxy.unwrap(org.hibernate.jpa.HibernateEntityManager.class)).isSameAs(em);
		assertThat(proxy.getDelegate()).isSameAs(em.getDelegate());
	}

	@Test  // SPR-16956
	public void testReadOnly() {
		assertThat(sharedEntityManager.unwrap(Session.class).getHibernateFlushMode()).isSameAs(FlushMode.AUTO);
		assertThat(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly()).isFalse();
		endTransaction();

		this.transactionDefinition.setReadOnly(true);
		startNewTransaction();
		assertThat(sharedEntityManager.unwrap(Session.class).getHibernateFlushMode()).isSameAs(FlushMode.MANUAL);
		assertThat(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly()).isTrue();
	}

}
