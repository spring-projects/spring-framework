/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.junit4.spr9051;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.Employee;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Concrete implementation of {@link AbstractTransactionalAnnotatedConfigClassTests}
 * that does <b>not</b> use a true {@link Configuration @Configuration class} but
 * rather a <em>lite mode</em> configuration class (see the Javadoc for {@link Bean @Bean}
 * for details).
 * 
 * @author Sam Brannen
 * @since 3.2
 * @see Bean
 * @see TransactionalAnnotatedConfigClassWithAtConfigurationTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TransactionalAnnotatedConfigClassesWithoutAtConfigurationTests.AnnotatedFactoryBeans.class)
public class TransactionalAnnotatedConfigClassesWithoutAtConfigurationTests extends
		AbstractTransactionalAnnotatedConfigClassTests {

	/**
	 * This is intentionally <b>not</b> annotated with {@code @Configuration}.
	 * 
	 * <p>Consequently, this class contains <i>annotated factory bean methods</i>
	 * instead of standard singleton bean methods.
	 */
	// @Configuration
	static class AnnotatedFactoryBeans {

		@Bean
		public Employee employee() {
			Employee employee = new Employee();
			employee.setName("John Smith");
			employee.setAge(42);
			employee.setCompany("Acme Widgets, Inc.");
			return employee;
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		/**
		 * Since this method does not reside in a true {@code @Configuration class},
		 * it acts as a factory method instead of a singleton bean. The result is
		 * that this method will be called at least twice:
		 * 
		 * <ul>
		 * <li>once <em>indirectly</em> by the {@link TransactionalTestExecutionListener}
		 * when it retrieves the {@link PlatformTransactionManager} from the
		 * application context</li>
		 * <li>and again when the {@link DataSource} is injected into the test
		 * instance in {@link AbstractTransactionalAnnotatedConfigClassTests#setDataSource(DataSource)}.</li>
		 *</ul>
		 *
		 * Consequently, the {@link JdbcTemplate} used by this test instance and
		 * the {@link PlatformTransactionManager} used by the Spring TestContext
		 * Framework will operate on two different {@code DataSource} instances,
		 * which is most certainly not the desired or intended behavior.
		 */
		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
			.addScript("classpath:/org/springframework/test/context/junit4/spr9051/schema.sql")//
			.build();
		}

	}


	@Before
	public void compareDataSources() throws Exception {
		// NOTE: the two DataSource instances are NOT the same!
		assertNotSame(dataSourceFromTxManager, dataSourceViaInjection);
	}

	/**
	 * Overrides {@code afterTransaction()} in order to assert a different result. 
	 * 
	 * <p>See in-line comments for details.
	 *
	 * @see AbstractTransactionalAnnotatedConfigClassTests#afterTransaction()
	 * @see AbstractTransactionalAnnotatedConfigClassTests#modifyTestDataWithinTransaction()
	 */
	@AfterTransaction
	@Override
	public void afterTransaction() {
		assertEquals("Deleting yoda", 1, deletePerson(YODA));

		// NOTE: We would actually expect that there are now ZERO entries in the
		// person table, since the transaction is rolled back by the framework;
		// however, since our JdbcTemplate and the transaction manager used by
		// the Spring TestContext Framework use two different DataSource
		// instances, our insert statements were executed in transactions that
		// are not controlled by the test framework. Consequently, there was no
		// rollback for the two insert statements in
		// modifyTestDataWithinTransaction().
		//
		assertNumRowsInPersonTable(2, "after a transactional test method");
	}

}
