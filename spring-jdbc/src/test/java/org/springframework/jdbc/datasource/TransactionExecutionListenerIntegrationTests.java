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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.Mockito.mock;

class TransactionExecutionListenerIntegrationTests {

	private final EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
			.generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();

	private final List<Connection> connections = new ArrayList<>();

	private final DelegatingDataSource dataSource = new DelegatingDataSource(this.database) {
		@Override
		public Connection getConnection() throws SQLException {
			Connection connection = super.getConnection();
			connections.add(connection);
			return connection;
		}
	};

	private final JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);

	private final DataSourceTransactionManager manager = new DataSourceTransactionManager(this.dataSource);

	private final TransactionTemplate transaction = new TransactionTemplate(this.manager);


	@BeforeEach
	void createTable() {
		this.jdbc.execute("create table entries (id integer)");
	}

	@AfterEach
	void verifyCleanup() throws SQLException {
		try {
			assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
			for (Connection connection : this.connections) {
				assertThat(connection.isClosed()).isTrue();
			}
		}
		finally {
			TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
			TransactionSynchronizationManager.clear();
			for (Connection connection : this.connections) {
				if (!connection.isClosed()) {
					connection.rollback();
					connection.close();
				}
			}
			this.database.shutdown();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"beforeBegin", "afterBegin", "beforeCommit", "afterCommit"})
	void commitWithFailingListener(String callback) {
		this.manager.addListener(failingListener(callback, false));

		this.transaction.executeWithoutResult(status -> this.jdbc.update("insert into entries values (1)"));

		assertThat(this.jdbc.queryForList("select id from entries", Integer.class)).containsExactly(1);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void rollbackWithFailingListenerDoesNotCommitChanges(boolean rollbackOnly) {
		this.manager.addListener(failingListener("beforeRollback", false));
		RuntimeException businessFailure = new IllegalStateException("Business failure");

		if (rollbackOnly) {
			this.transaction.executeWithoutResult(status -> {
				this.jdbc.update("insert into entries values (1)");
				status.setRollbackOnly();
			});
		}
		else {
			assertThatRuntimeException().isThrownBy(() -> this.transaction.executeWithoutResult(status -> {
				this.jdbc.update("insert into entries values (1)");
				throw businessFailure;
			})).isSameAs(businessFailure);
		}

		assertThat(this.jdbc.queryForList("select id from entries", Integer.class)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"beforeBegin", "afterBegin", "beforeRollback", "afterRollback"})
	void nestedRollbackWithFailingListenerPreservesOuterTransaction(String callback) {
		this.manager.addListener(failingListener(callback, true));
		TransactionTemplate nested = nestedTransaction(TransactionDefinition.PROPAGATION_NESTED);
		RuntimeException businessFailure = new IllegalStateException("Business failure");

		this.transaction.executeWithoutResult(outer -> {
			Object resource = TransactionSynchronizationManager.getResource(this.dataSource);
			this.jdbc.update("insert into entries values (1)");
			assertThatRuntimeException().isThrownBy(() -> nested.executeWithoutResult(inner -> {
				this.jdbc.update("insert into entries values (2)");
				throw businessFailure;
			})).isSameAs(businessFailure);
			assertThat(TransactionSynchronizationManager.getResource(this.dataSource)).isSameAs(resource);
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			this.jdbc.update("insert into entries values (3)");
		});

		assertThat(this.jdbc.queryForList("select id from entries order by id", Integer.class)).containsExactly(1, 3);
	}

	@ParameterizedTest
	@ValueSource(strings = {"beforeCommit", "afterCommit"})
	void nestedCommitWithFailingListenerPreservesOuterTransaction(String callback) {
		this.manager.addListener(failingListener(callback, true));
		TransactionTemplate nested = nestedTransaction(TransactionDefinition.PROPAGATION_NESTED);

		this.transaction.executeWithoutResult(outer -> {
			this.jdbc.update("insert into entries values (1)");
			nested.executeWithoutResult(inner -> this.jdbc.update("insert into entries values (2)"));
		});

		assertThat(this.jdbc.queryForList("select id from entries order by id", Integer.class)).containsExactly(1, 2);
	}

	@ParameterizedTest
	@ValueSource(strings = {"beforeBegin", "afterBegin", "beforeCommit", "afterCommit"})
	void requiresNewWithFailingListenerRestoresOuterTransaction(String callback) {
		this.manager.addListener(failingListener(callback, true));
		TransactionTemplate inner = nestedTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		this.transaction.executeWithoutResult(outer -> {
			Object resource = TransactionSynchronizationManager.getResource(this.dataSource);
			this.jdbc.update("insert into entries values (1)");
			inner.executeWithoutResult(status -> {
				assertThat(TransactionSynchronizationManager.getResource(this.dataSource)).isNotSameAs(resource);
				this.jdbc.update("insert into entries values (2)");
			});
			assertThat(TransactionSynchronizationManager.getResource(this.dataSource)).isSameAs(resource);
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			this.jdbc.update("insert into entries values (3)");
		});

		assertThat(this.jdbc.queryForList("select id from entries order by id", Integer.class)).containsExactly(1, 2, 3);
	}

	private TransactionTemplate nestedTransaction(int propagation) {
		TransactionTemplate nested = new TransactionTemplate(this.manager);
		nested.setName("inner");
		nested.setPropagationBehavior(propagation);
		return nested;
	}

	private static TransactionExecutionListener failingListener(String callback, boolean innerOnly) {
		return mock(TransactionExecutionListener.class, invocation -> {
			if (invocation.getMethod().getName().equals(callback)) {
				TransactionExecution transaction = invocation.getArgument(0);
				if (!innerOnly || "inner".equals(transaction.getTransactionName())) {
					throw new IllegalStateException("Listener failure");
				}
			}
			return null;
		});
	}

}
