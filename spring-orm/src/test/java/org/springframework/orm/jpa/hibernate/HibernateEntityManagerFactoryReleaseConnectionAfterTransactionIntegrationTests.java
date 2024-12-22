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

package org.springframework.orm.jpa.hibernate;

import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link org.springframework.orm.jpa.vendor.HibernateJpaDialect#releaseConnectionAfterTransaction}
 *
 * @author Ilia Sazonov
 */
class HibernateEntityManagerFactoryReleaseConnectionAfterTransactionIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager-release-after-transaction.xml",
				"/org/springframework/orm/jpa/memdb.xml", "/org/springframework/orm/jpa/inject.xml"};
	}

	@Test
	public void testReleaseConnectionAfterTransaction() {
		endTransaction();

		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			EntityManagerHolder emHolder = new EntityManagerHolder(em);
			TransactionSynchronizationManager.bindResource(entityManagerFactory, emHolder);

			startNewTransaction();
			endTransaction();

			assertThat(em.isOpen()).isTrue();
			final LogicalConnectionImplementor logicalConnection = em.unwrap(SessionImplementor.class)
					.getJdbcCoordinator().getLogicalConnection();
			assertThat(logicalConnection.isPhysicallyConnected()).isFalse();

		} finally {
			TransactionSynchronizationManager.unbindResource(entityManagerFactory);
		}
	}
}
