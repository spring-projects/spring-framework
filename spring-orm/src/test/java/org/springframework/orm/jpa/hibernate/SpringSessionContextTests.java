/*
 * Copyright 2002-present the original author or authors.
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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.engine.creation.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SpringSessionContext}.
 */
class SpringSessionContextTests {

	@AfterEach
	void clearTransactionState() {
		for (Object resource : new ArrayList<>(TransactionSynchronizationManager.getResourceMap().keySet())) {
			TransactionSynchronizationManager.unbindResource(resource);
		}
		TransactionSynchronizationManager.clear();
	}


	@Test
	void nonTransactionalScopeDoesNotBorrowDataSourceConnection() {
		SessionImplementor session = mock(SessionImplementor.class);
		SessionFactoryImplementor sessionFactory = sessionFactory(session);

		TransactionSynchronizationManager.initSynchronization();
		Session current = new SpringSessionContext(sessionFactory).currentSession();

		assertThat(current).isSameAs(session);
		verify(sessionFactory).openSession();
		verify(sessionFactory, never()).withOptions();
	}

	@Test
	void actualTransactionUsesSharedDataSourceConnection() throws Exception {
		SessionImplementor session = mock(SessionImplementor.class);
		SessionFactoryImplementor sessionFactory = sessionFactory(session);
		DataSource dataSource = (DataSource) sessionFactory.getProperties()
				.get("jakarta.persistence.nonJtaDataSource");
		Connection connection = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(connection);

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		Session current = new SpringSessionContext(sessionFactory).currentSession();

		assertThat(current).isSameAs(session);
		verify(sessionFactory).withOptions();
		verify(sessionFactory, never()).openSession();
	}

	private static SessionFactoryImplementor sessionFactory(SessionImplementor session) {
		SessionFactoryImplementor sessionFactory = mock(SessionFactoryImplementor.class);
		ServiceRegistryImplementor services = mock(ServiceRegistryImplementor.class);
		JtaPlatform jtaPlatform = mock(JtaPlatform.class);
		when(sessionFactory.getServiceRegistry()).thenReturn(services);
		when(services.requireService(JtaPlatform.class)).thenReturn(jtaPlatform);
		when(jtaPlatform.retrieveTransactionManager()).thenReturn(null);

		DataSource dataSource = mock(DataSource.class);
		when(sessionFactory.getProperties()).thenReturn(Map.of("jakarta.persistence.nonJtaDataSource", dataSource));
		SessionBuilderImplementor options = mock(SessionBuilderImplementor.class);
		when(sessionFactory.withOptions()).thenReturn(options);
		when(options.connection(any())).thenReturn(options);
		when(options.openSession()).thenReturn(session);
		when(sessionFactory.openSession()).thenReturn(session);
		return sessionFactory;
	}

}
