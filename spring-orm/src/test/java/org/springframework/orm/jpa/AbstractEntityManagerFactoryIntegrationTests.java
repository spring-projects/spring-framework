/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractEntityManagerFactoryIntegrationTests {

	protected static final String[] ECLIPSELINK_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/eclipselink/eclipselink-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};

	protected static final String[] HIBERNATE_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/hibernate/hibernate-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};


	private static ConfigurableApplicationContext applicationContext;

	protected EntityManagerFactory entityManagerFactory;

	protected EntityManager sharedEntityManager;

	protected PlatformTransactionManager transactionManager;

	protected TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

	protected TransactionStatus transactionStatus;

	private boolean complete = false;

	protected JdbcTemplate jdbcTemplate;

	private boolean zappedTables = false;


	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
		this.sharedEntityManager = SharedEntityManagerCreator.createSharedEntityManager(this.entityManagerFactory);
	}

	@Autowired
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@Before
	public void setup() {
		if (applicationContext == null) {
			applicationContext = new ClassPathXmlApplicationContext(getConfigLocations());
		}
		applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

		if (this.transactionManager != null && this.transactionDefinition != null) {
			startNewTransaction();
		}
	}

	protected String[] getConfigLocations() {
		return ECLIPSELINK_CONFIG_LOCATIONS;
	}

	@After
	public void cleanup() {
		if (this.transactionStatus != null && !this.transactionStatus.isCompleted()) {
			endTransaction();
		}

		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@AfterClass
	public static void closeContext() {
		if (applicationContext != null) {
			applicationContext.close();
			applicationContext = null;
		}
	}


	protected EntityManager createContainerManagedEntityManager() {
		return ExtendedEntityManagerCreator.createContainerManagedEntityManager(this.entityManagerFactory);
	}

	protected void setComplete() {
		if (this.transactionManager == null) {
			throw new IllegalStateException("No transaction manager set");
		}
		if (this.zappedTables) {
			throw new IllegalStateException("Cannot set complete after deleting tables");
		}
		this.complete = true;
	}

	protected void endTransaction() {
		final boolean commit = this.complete;
		if (this.transactionStatus != null) {
			try {
				if (commit) {
					this.transactionManager.commit(this.transactionStatus);
				}
				else {
					this.transactionManager.rollback(this.transactionStatus);
				}
			}
			finally {
				this.transactionStatus = null;
			}
		}
	}

	protected void startNewTransaction() throws TransactionException {
		this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
	}

	protected void deleteFromTables(String... tableNames) {
		for (String tableName : tableNames) {
			this.jdbcTemplate.update("DELETE FROM " + tableName);
		}
		this.zappedTables = true;
	}

	protected int countRowsInTable(EntityManager em, String tableName) {
		Query query = em.createNativeQuery("SELECT COUNT(0) FROM " + tableName);
		return ((Number) query.getSingleResult()).intValue();
	}

	protected int countRowsInTable(String tableName) {
		return this.jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
	}

	protected void executeSqlScript(String sqlResourcePath) throws DataAccessException {
		Resource resource = applicationContext.getResource(sqlResourcePath);
		new ResourceDatabasePopulator(resource).execute(this.jdbcTemplate.getDataSource());
	}

}
