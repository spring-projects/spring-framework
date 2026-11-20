/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * JUnit based integration test which verifies support of Spring's
 * {@link Transactional @Transactional},
 * {@link TestExecutionListeners @TestExecutionListeners}, and
 * {@link ContextConfiguration @ContextConfiguration} annotations in conjunction
 * with the following {@link TestExecutionListener TestExecutionListeners}:
 *
 * <ul>
 * <li>{@link DependencyInjectionTestExecutionListener}</li>
 * <li>{@link DirtiesContextTestExecutionListener}</li>
 * <li>{@link TransactionalTestExecutionListener}</li>
 * </ul>
 *
 * <p>This class specifically tests usage of {@code @Transactional} defined
 * at the <strong>method level</strong>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ClassLevelTransactionalSpringTests
 */
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
	TransactionalTestExecutionListener.class })
@TestInstance(Lifecycle.PER_CLASS)
class MethodLevelTransactionalSpringTests extends AbstractTransactionalSpringTests {

	private JdbcTemplate jdbcTemplate;


	@Autowired
	@Qualifier("dataSource2")
	void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeEach
	void verifyInitialTestData() {
		clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
	}

	@Test
	@Transactional("transactionManager2")
	void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertThat(deletePerson(jdbcTemplate, BOB)).as("Deleting bob").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(2);
	}

	@Test
	void modifyTestDataWithoutTransaction() {
		assertThatTransaction().isNotActive();
		assertThat(addPerson(jdbcTemplate, LUKE)).as("Adding luke").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, LEIA)).as("Adding leia").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table without a transaction.").isEqualTo(4);
	}

	@AfterAll
	void verifyFinalTestData() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(4);
	}

}

