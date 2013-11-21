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

package org.springframework.test.context.junit4.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.transaction.TransactionTestUtils.assertInTransaction;

import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.orm.domain.DriversLicense;
import org.springframework.test.context.junit4.orm.domain.Person;
import org.springframework.test.context.junit4.orm.service.PersonService;

/**
 * Transactional integration tests regarding <i>manual</i> session flushing with
 * Hibernate.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@ContextConfiguration
public class HibernateSessionFlushingTests extends AbstractTransactionalJUnit4SpringContextTests {

	private static final String SAM = "Sam";
	private static final String JUERGEN = "Juergen";

	@Autowired
	private PersonService personService;

	@Autowired
	private SessionFactory sessionFactory;


	protected int countRowsInPersonTable() {
		return countRowsInTable("person");
	}

	protected void assertPersonCount(int expectedCount) {
		assertEquals("Verifying number of rows in the 'person' table.", expectedCount, countRowsInPersonTable());
	}

	@Before
	public void setUp() {
		assertInTransaction(true);
		assertNotNull("PersonService should have been autowired.", personService);
		assertNotNull("SessionFactory should have been autowired.", sessionFactory);
	}

	@Test
	public void findSam() {
		Person sam = personService.findByName(SAM);
		assertNotNull("Should be able to find Sam", sam);
		DriversLicense driversLicense = sam.getDriversLicense();
		assertNotNull("Sam's driver's license should not be null", driversLicense);
		assertEquals("Verifying Sam's driver's license number", new Long(1234), driversLicense.getNumber());
	}

	@Test
	public void saveJuergenWithDriversLicense() {
		DriversLicense driversLicense = new DriversLicense(2L, 2222L);
		Person juergen = new Person(JUERGEN, driversLicense);
		int numRows = countRowsInPersonTable();
		personService.save(juergen);
		assertPersonCount(numRows + 1);
		assertNotNull("Should be able to save and retrieve Juergen", personService.findByName(JUERGEN));
		assertNotNull("Juergen's ID should have been set", juergen.getId());
	}

	@Test(expected = ConstraintViolationException.class)
	public void saveJuergenWithNullDriversLicense() {
		personService.save(new Person(JUERGEN));
	}

	private void updateSamWithNullDriversLicense() {
		Person sam = personService.findByName(SAM);
		assertNotNull("Should be able to find Sam", sam);
		sam.setDriversLicense(null);
		personService.save(sam);
	}

	@Test
	// no expected exception!
	public void updateSamWithNullDriversLicenseWithoutSessionFlush() {
		updateSamWithNullDriversLicense();
		// False positive, since an exception will be thrown once the session is
		// finally flushed (i.e., in production code)
	}

	@Test(expected = ConstraintViolationException.class)
	public void updateSamWithNullDriversLicenseWithSessionFlush() {
		updateSamWithNullDriversLicense();
		// Manual flush is required to avoid false positive in test
		sessionFactory.getCurrentSession().flush();
	}

}
