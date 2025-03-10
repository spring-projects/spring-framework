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

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Ilia Sazonov
 */
class JpaTransactionManagerConnectionReleaseTests {

	private EntityManagerFactory factory = mock();

	private EntityManager manager = mock();

	private EntityTransaction tx = mock();

	private JpaTransactionManager tm = new JpaTransactionManager(factory);

	private TransactionTemplate tt = new TransactionTemplate(tm);

	private DataSource ds = mock();

	private ConnectionHandle connHandle = mock();

	private JpaDialect jpaDialect = spy(new DefaultJpaDialect());


	@BeforeEach
	void setup() throws SQLException {
		given(factory.createEntityManager()).willReturn(manager);
		given(manager.getTransaction()).willReturn(tx);
		given(manager.isOpen()).willReturn(true);
		given(jpaDialect.getJdbcConnection(same(manager), anyBoolean())).willReturn(connHandle);
		tm.setJpaDialect(jpaDialect);
		tm.setDataSource(ds);
	}

	@AfterEach
	void verifyTransactionSynchronizationManagerState() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

	@Test
	void testConnectionIsReleasedAfterTransactionCleanup() throws SQLException {
		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
				return l;
			});
			assertThat(result).isSameAs(l);

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(tx).begin();
		verify(tx).commit();

		InOrder cleanupBeforeRelease = inOrder(jpaDialect);
		cleanupBeforeRelease.verify(jpaDialect).cleanupTransaction(any());
		cleanupBeforeRelease.verify(jpaDialect).releaseJdbcConnection(same(connHandle), same(manager));
	}

	@Test
	void testConnectionIsNotReleasedIfSesionIsClosing() throws SQLException {
		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		Object result = tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
			return l;
		});
		assertThat(result).isSameAs(l);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).begin();
		verify(tx).commit();

		verify(jpaDialect).cleanupTransaction(any());
		verify(jpaDialect, never()).releaseJdbcConnection(any(), any());
	}
}
