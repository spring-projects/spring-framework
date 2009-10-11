package org.springframework.samples.petclinic;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.samples.petclinic.hibernate.HibernateClinicTests;
import org.springframework.samples.petclinic.jdbc.SimpleJdbcClinicTests;
import org.springframework.samples.petclinic.jpa.EntityManagerClinicTests;
import org.springframework.samples.petclinic.jpa.HibernateEntityManagerClinicTests;
import org.springframework.samples.petclinic.jpa.OpenJpaEntityManagerClinicTests;
import org.springframework.samples.petclinic.web.VisitsAtomViewTest;

/**
 * JUnit 4 based test suite for all PetClinic tests.
 *
 * @author Sam Brannen
 */
@RunWith(Suite.class)
@SuiteClasses({
	OwnerTests.class,
	SimpleJdbcClinicTests.class,
	HibernateClinicTests.class,
	EntityManagerClinicTests.class,
	HibernateEntityManagerClinicTests.class,
	OpenJpaEntityManagerClinicTests.class,
	VisitsAtomViewTest.class
})
public class PetClinicTestSuite {
}
