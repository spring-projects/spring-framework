/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate;

import jakarta.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
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
class HibernateEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager.xml",
				"/org/springframework/orm/jpa/memdb.xml", "/org/springframework/orm/jpa/inject.xml"};
	}


	@Test
	void advisedEntityManagerProxyFromSmartFactoryBean() {
		assertThat(AopUtils.isAopProxy(sharedEntityManager)).isTrue();
	}

	@Test
	void canCastNativeEntityManagerFactoryToHibernateEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertThat(emfi.getNativeEntityManagerFactory()).isInstanceOf(SessionFactory.class);
	}

	@Test
	void canCastSharedEntityManagerProxyToHibernateEntityManager() {
		assertThat(((EntityManagerProxy) sharedEntityManager).getTargetEntityManager()).isInstanceOf(Session.class);
	}

	@Test
	void canUnwrapAopProxy() {
		EntityManager em = entityManagerFactory.createEntityManager();
		EntityManager proxy = ProxyFactory.getProxy(EntityManager.class, new SingletonTargetSource(em));
		assertThat(proxy.unwrap(Session.class)).isSameAs(em);
		assertThat(proxy.getDelegate()).isSameAs(em.getDelegate());
	}

	@Test  // SPR-16956
	void readOnly() {
		assertThat(sharedEntityManager.unwrap(Session.class).getHibernateFlushMode()).isSameAs(FlushMode.AUTO);
		assertThat(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly()).isFalse();
		endTransaction();

		this.transactionDefinition.setReadOnly(true);
		startNewTransaction();
		assertThat(sharedEntityManager.unwrap(Session.class).getHibernateFlushMode()).isSameAs(FlushMode.MANUAL);
		assertThat(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly()).isTrue();
	}

}
