/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.junit4.orm;

import javax.persistence.PersistenceException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.orm.domain.DriversLicense;
import org.springframework.test.context.junit4.orm.domain.Person;
import org.springframework.test.context.junit4.orm.service.PersonService;
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
 * @see org.springframework.test.context.junit.jupiter.orm.JpaEntityListenerTests
 */
@ContextConfiguration
public class HibernateSessionFlushingTests extends AbstractTransactionalJUnit4SpringContextTests {

	private static final String SAM = "Sam";
	private static final String JUERGEN = "Juergen";

	@Autowired
	private PersonService personService;

	@Autowired
	private SessionFactory sessionFactory;


	@Before
	public void setup() {
		assertThatTransaction().isActive();
		assertThat(personService).as("PersonService should have been autowired.").isNotNull();
		assertThat(sessionFactory).as("SessionFactory should have been autowired.").isNotNull();
	}


	@Test
	public void findSam() {
		Person sam = personService.findByName(SAM);
		assertThat(sam).as("Should be able to find Sam").isNotNull();
		DriversLicense driversLicense = sam.getDriversLicense();
		assertThat(driversLicense).as("Sam's driver's license should not be null").isNotNull();
		assertThat(driversLicense.getNumber()).as("Verifying Sam's driver's license number").isEqualTo(Long.valueOf(1234));
	}

	@Test  // SPR-16956
	@Transactional(readOnly = true)
	public void findSamWithReadOnlySession() {
		Person sam = personService.findByName(SAM);
		sam.setName("Vlad");
		// By setting setDefaultReadOnly(true), the user can no longer modify any entity...
		Session session = sessionFactory.getCurrentSession();
		session.flush();
		session.refresh(sam);
		assertThat(sam.getName()).isEqualTo("Sam");
	}

	@Test
	public void saveJuergenWithDriversLicense() {
		DriversLicense driversLicense = new DriversLicense(2L, 2222L);
		Person juergen = new Person(JUERGEN, driversLicense);
		int numRows = countRowsInTable("person");
		personService.save(juergen);
		assertThat(countRowsInTable("person")).as("Verifying number of rows in the 'person' table.").isEqualTo((numRows + 1));
		assertThat(personService.findByName(JUERGEN)).as("Should be able to save and retrieve Juergen").isNotNull();
		assertThat(juergen.getId()).as("Juergen's ID should have been set").isNotNull();
	}

	@Test
	public void saveJuergenWithNullDriversLicense() {
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() ->
				personService.save(new Person(JUERGEN)));
	}

	@Test
	// no expected exception!
	public void updateSamWithNullDriversLicenseWithoutSessionFlush() {
		updateSamWithNullDriversLicense();
		// False positive, since an exception will be thrown once the session is
		// finally flushed (i.e., in production code)
	}

	@Test
	public void updateSamWithNullDriversLicenseWithSessionFlush() throws Throwable {
		updateSamWithNullDriversLicense();
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> {
			// Manual flush is required to avoid false positive in test
			try {
				sessionFactory.getCurrentSession().flush();
			}
			catch (PersistenceException ex) {
				// Wrapped in Hibernate 5.2, with the constraint violation as cause
				throw ex.getCause();
			}
		});
	}

	private void updateSamWithNullDriversLicense() {
		Person sam = personService.findByName(SAM);
		assertThat(sam).as("Should be able to find Sam").isNotNull();
		sam.setDriversLicense(null);
		personService.save(sam);
	}

}
