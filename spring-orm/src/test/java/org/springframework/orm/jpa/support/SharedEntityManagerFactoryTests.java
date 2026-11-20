/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
class SharedEntityManagerFactoryTests {

	@Test
	void validUsage() {
		Object o = new Object();

		EntityManager mockEm = mock();

		EntityManagerFactory mockEmf = mock();
		given(mockEmf.createEntityManager()).willReturn(mockEm);

		SharedEntityManagerBean proxyFactoryBean = new SharedEntityManagerBean();
		proxyFactoryBean.setEntityManagerFactory(mockEmf);
		proxyFactoryBean.afterPropertiesSet();

		assertThat(EntityManager.class.isAssignableFrom(proxyFactoryBean.getObjectType())).isTrue();
		assertThat(proxyFactoryBean.isSingleton()).isTrue();

		EntityManager proxy = proxyFactoryBean.getObject();
		assertThat(proxyFactoryBean.getObject()).isSameAs(proxy);
		assertThat(proxy.contains(o)).isFalse();

		assertThat(proxy).isInstanceOf(EntityManagerProxy.class);
		EntityManagerProxy emProxy = (EntityManagerProxy) proxy;
		assertThatIllegalStateException().as("outside of transaction").isThrownBy(
				emProxy::getTargetEntityManager);

		TransactionSynchronizationManager.bindResource(mockEmf, new EntityManagerHolder(mockEm));
		try {
			assertThat(emProxy.getTargetEntityManager()).isSameAs(mockEm);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(mockEmf);
		}

		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		verify(mockEm).contains(o);
		verify(mockEm).close();
	}

}
