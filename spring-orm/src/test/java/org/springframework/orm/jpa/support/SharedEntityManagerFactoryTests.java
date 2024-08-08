/*
 * Copyright 2002-2024 the original author or authors.
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
	void testValidUsage() {
		Object o = new Object();

		EntityManager mockEm = mock();
		given(mockEm.isOpen()).willReturn(true);

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

		boolean condition = proxy instanceof EntityManagerProxy;
		assertThat(condition).isTrue();
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
