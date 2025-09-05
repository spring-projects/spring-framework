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

package org.springframework.test.context.orm.hibernate;

import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.orm.hibernate.domain.DriversLicense;
import org.springframework.test.context.orm.hibernate.domain.Person;
import org.springframework.test.context.orm.hibernate.service.PersonService;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Transactional integration tests regarding <i>manual</i> session flushing with
 * Hibernate.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Vlad Mihalcea
 * @since 3.0
 * @see org.springframework.test.context.orm.jpa.JpaEntityListenerTests
 */
@SpringJUnitConfig
@Transactional
class HibernateSessionFlushingTests {

	private static final String SAM = "Sam";
	private static final String JUERGEN = "Juergen";

	JdbcTemplate jdbcTemplate;

	@Autowired
	SessionFactory sessionFactory;

	@Autowired
	PersonService personService;


	@Autowired
	void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@BeforeEach
	void setup() {
		assertThatTransaction().isActive();
		assertThat(personService).as("PersonService should have been autowired.").isNotNull();
		assertThat(sessionFactory).as("SessionFactory should have been autowired.").isNotNull();
	}


	@Test
	void findSam() {
		Person sam = personService.findByName(SAM);
		assertThat(sam).as("Should be able to find Sam").isNotNull();
		DriversLicense driversLicense = sam.getDriversLicense();
		assertThat(driversLicense).as("Sam's driver's license should not be null").isNotNull();
		assertThat(driversLicense.getNumber()).as("Verifying Sam's driver's license number").isEqualTo(Long.valueOf(1234));
	}

	@Test  // SPR-16956
	@Transactional(readOnly = true)
	void findSamWithReadOnlySession() {
		Person sam = personService.findByName(SAM);
		sam.setName("Vlad");
		// By setting setDefaultReadOnly(true), the user can no longer modify any entity...
		Session session = sessionFactory.getCurrentSession();
		session.flush();
		session.refresh(sam);
		assertThat(sam.getName()).isEqualTo("Sam");
	}

	@Test
	void saveJuergenWithDriversLicense() {
		DriversLicense driversLicense = new DriversLicense(2L, 2222L);
		Person juergen = new Person(JUERGEN, driversLicense);
		int numRows = countRowsInTable("person");
		personService.save(juergen);
		assertThat(countRowsInTable("person")).as("Verifying number of rows in the 'person' table.").isEqualTo((numRows + 1));
		assertThat(personService.findByName(JUERGEN)).as("Should be able to save and retrieve Juergen").isNotNull();
		assertThat(juergen.getId()).as("Juergen's ID should have been set").isNotNull();
	}

	@Test
	void saveJuergenWithNullDriversLicense() {
		assertThatExceptionOfType(ConstraintViolationException.class)
				.isThrownBy(() -> personService.save(new Person(JUERGEN)));
	}

	@Test
	// no expected exception!
	void updateSamWithNullDriversLicenseWithoutSessionFlush() {
		updateSamWithNullDriversLicense();
		// False positive, since an exception will be thrown once the session is
		// finally flushed (i.e., in production code)
	}

	@Test
	void updateSamWithNullDriversLicenseWithSessionFlush() {
		updateSamWithNullDriversLicense();
		// Manual flush is required to avoid false positive in test
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(sessionFactory.getCurrentSession()::flush);
	}

	private void updateSamWithNullDriversLicense() {
		Person sam = personService.findByName(SAM);
		assertThat(sam).as("Should be able to find Sam").isNotNull();
		sam.setDriversLicense(null);
		personService.save(sam);
	}

	private int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

}
