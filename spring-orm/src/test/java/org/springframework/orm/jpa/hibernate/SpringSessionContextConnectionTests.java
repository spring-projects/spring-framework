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

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.sql.DataSource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * A Session opened by {@link SpringSessionContext} outside a real transaction
 * must not keep its pooled connection while that scope is suspended.
 */
class SpringSessionContextConnectionTests {

	private BoundedDataSource dataSource;

	private SessionFactory sessionFactory;

	private PlatformTransactionManager transactionManager;


	@AfterEach
	void tearDown() {
		if (this.sessionFactory != null) {
			this.sessionFactory.close();
		}
	}

	@Test
	void neverScopeReleasesConnectionForNestedTransaction() {
		setUp(10);
		runOuterThenNested(TransactionDefinition.PROPAGATION_NEVER);
		assertThat(this.dataSource.maxBorrowed()).isEqualTo(1);
	}

	@Test
	void supportsScopeReleasesConnectionForNestedTransaction() {
		setUp(10);
		runOuterThenNested(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(this.dataSource.maxBorrowed()).isEqualTo(1);
	}

	@Test
	void singleConnectionPoolSurvivesNestedTransaction() {
		setUp(1);
		assertThatNoException().isThrownBy(() -> runOuterThenNested(TransactionDefinition.PROPAGATION_NEVER));
		setUp(1);
		assertThatNoException().isThrownBy(() -> runOuterThenNested(TransactionDefinition.PROPAGATION_SUPPORTS));
	}

	@Test
	void supportsJoinsTheConnectionOfAnExistingTransaction() {
		setUp(2);
		TransactionTemplate required = new TransactionTemplate(this.transactionManager);
		TransactionTemplate supports = new TransactionTemplate(this.transactionManager);
		supports.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		required.executeWithoutResult(status -> {
			this.sessionFactory.getCurrentSession().persist(new Person("Ada"));
			supports.executeWithoutResult(ignored -> {
				Long count = this.sessionFactory.getCurrentSession()
						.createQuery("select count(p) from Person p", Long.class)
						.getSingleResult();
				assertThat(count).isEqualTo(1L);
			});
		});

		assertThat(this.dataSource.maxBorrowed()).isEqualTo(1);
	}

	private void setUp(int poolSize) {
		if (this.sessionFactory != null) {
			this.sessionFactory.close();
		}
		JDBCDataSource hsqldb = new JDBCDataSource();
		hsqldb.setDatabase("jdbc:hsqldb:mem:" + UUID.randomUUID() + ";sql.syntax_ora=false");
		hsqldb.setUser("sa");
		this.dataSource = new BoundedDataSource(hsqldb, poolSize);

		Properties properties = new Properties();
		properties.put("hibernate.hbm2ddl.auto", "create-drop");
		LocalSessionFactoryBuilder builder = new LocalSessionFactoryBuilder(this.dataSource);
		builder.addAnnotatedClass(Person.class);
		builder.addProperties(properties);
		this.sessionFactory = builder.buildSessionFactory();
		HibernateTransactionManager transactionManager = new HibernateTransactionManager(this.sessionFactory);
		transactionManager.setDataSource(this.dataSource);
		this.transactionManager = transactionManager;
		this.dataSource.resetStatistics();
	}

	private void runOuterThenNested(int outerPropagation) {
		TransactionTemplate outer = new TransactionTemplate(this.transactionManager);
		outer.setPropagationBehavior(outerPropagation);
		outer.setReadOnly(true);
		TransactionTemplate nested = new TransactionTemplate(this.transactionManager);
		nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		outer.executeWithoutResult(outerStatus -> {
			this.sessionFactory.getCurrentSession().createQuery("from Person", Person.class).getResultList();
			nested.executeWithoutResult(nestedStatus ->
					this.sessionFactory.getCurrentSession().persist(new Person("John")));
			Long count = this.sessionFactory.getCurrentSession()
					.createQuery("select count(p) from Person p", Long.class)
					.getSingleResult();
			assertThat(count).isEqualTo(1L);
		});
	}


	@Entity(name = "Person")
	@Table(name = "person")
	static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		Person() {
		}

		Person(String name) {
			this.name = name;
		}

	}


	/**
	 * Counts borrowed connections and fails once {@code maxSize} are in use.
	 */
	static final class BoundedDataSource implements DataSource {

		private final DataSource target;

		private final int maxSize;

		private final Semaphore permits;

		private final AtomicInteger borrowed = new AtomicInteger();

		private final AtomicInteger maxBorrowed = new AtomicInteger();

		BoundedDataSource(DataSource target, int maxSize) {
			this.target = target;
			this.maxSize = maxSize;
			this.permits = new Semaphore(maxSize);
		}

		int maxBorrowed() {
			return this.maxBorrowed.get();
		}

		void resetStatistics() {
			this.maxBorrowed.set(this.borrowed.get());
		}

		@Override
		public Connection getConnection() throws SQLException {
			try {
				if (!this.permits.tryAcquire(500, TimeUnit.MILLISECONDS)) {
					throw new SQLException("Pool exhausted: all " + this.maxSize + " connections are in use");
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new SQLException(ex);
			}
			Connection connection;
			try {
				connection = this.target.getConnection();
			}
			catch (SQLException | RuntimeException ex) {
				this.permits.release();
				throw ex;
			}
			this.maxBorrowed.accumulateAndGet(this.borrowed.incrementAndGet(), Math::max);
			return wrap(connection);
		}

		private Connection wrap(Connection connection) {
			AtomicBoolean closed = new AtomicBoolean();
			return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
					(proxy, method, args) -> {
						if ("close".equals(method.getName()) && closed.compareAndSet(false, true)) {
							this.borrowed.decrementAndGet();
							this.permits.release();
						}
						try {
							return method.invoke(connection, args);
						}
						catch (InvocationTargetException ex) {
							throw ex.getTargetException();
						}
					});
		}

		@Override
		public Connection getConnection(String username, String password) {
			throw new UnsupportedOperationException();
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return this.target.getLogWriter();
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.target.setLogWriter(out);
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			this.target.setLoginTimeout(seconds);
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return this.target.getLoginTimeout();
		}

		@Override
		public Logger getParentLogger() {
			return Logger.getLogger(BoundedDataSource.class.getName());
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			if (iface.isInstance(this)) {
				return iface.cast(this);
			}
			throw new SQLException("Not a wrapper for " + iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) {
			return iface.isInstance(this);
		}

	}

}
