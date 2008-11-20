package org.springframework.samples.petclinic;

import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.util.EntityUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

/**
 * <p>
 * Base class for {@link Clinic} integration tests.
 * </p>
 * <p>
 * &quot;AbstractClinicTests-context.xml&quot; declares a common
 * {@link javax.sql.DataSource DataSource}. Subclasses should specify
 * additional context locations which declare a
 * {@link org.springframework.transaction.PlatformTransactionManager PlatformTransactionManager}
 * and a concrete implementation of {@link Clinic}.
 * </p>
 * <p>
 * This class extends {@link AbstractTransactionalJUnit4SpringContextTests},
 * one of the valuable testing support classes provided by the
 * <em>Spring TestContext Framework</em> found in the
 * <code>org.springframework.test.context</code> package. The
 * annotation-driven configuration used here represents best practice for
 * integration tests with Spring. Note, however, that
 * AbstractTransactionalJUnit4SpringContextTests serves only as a convenience
 * for extension. For example, if you do not wish for your test classes to be
 * tied to a Spring-specific class hierarchy, you may configure your tests with
 * annotations such as {@link ContextConfiguration @ContextConfiguration},
 * {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners},
 * {@link org.springframework.transaction.annotation.Transactional @Transactional},
 * etc.
 * </p>
 * <p>
 * AbstractClinicTests and its subclasses benefit from the following services
 * provided by the Spring TestContext Framework:
 * </p>
 * <ul>
 * <li><strong>Spring IoC container caching</strong> which spares us
 * unnecessary set up time between test execution.</li>
 * <li><strong>Dependency Injection</strong> of test fixture instances,
 * meaning that we don't need to perform application context lookups. See the
 * use of {@link Autowired @Autowired} on the <code>clinic</code> instance
 * variable, which uses autowiring <em>by type</em>. As an alternative, we
 * could annotate <code>clinic</code> with
 * {@link javax.annotation.Resource @Resource} to achieve dependency injection
 * <em>by name</em>.
 * <em>(see: {@link ContextConfiguration @ContextConfiguration},
 * {@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener DependencyInjectionTestExecutionListener})</em></li>
 * <li><strong>Transaction management</strong>, meaning each test method is
 * executed in its own transaction, which is automatically rolled back by
 * default. Thus, even if tests insert or otherwise change database state, there
 * is no need for a teardown or cleanup script.
 * <em>(see: {@link org.springframework.test.context.transaction.TransactionConfiguration @TransactionConfiguration},
 * {@link org.springframework.transaction.annotation.Transactional @Transactional},
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener TransactionalTestExecutionListener})</em></li>
 * <li><strong>Useful inherited protected fields</strong>, such as a
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcTemplate SimpleJdbcTemplate}
 * that can be used to verify database state after test operations or to verify
 * the results of queries performed by application code. An
 * {@link org.springframework.context.ApplicationContext ApplicationContext} is
 * also inherited and can be used for explicit bean lookup if necessary.
 * <em>(see: {@link org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests AbstractJUnit4SpringContextTests},
 * {@link AbstractTransactionalJUnit4SpringContextTests})</em></li>
 * </ul>
 * <p>
 * The Spring TestContext Framework and related unit and integration testing
 * support classes are shipped in <code>spring-test.jar</code>.
 * </p>
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@ContextConfiguration
public abstract class AbstractClinicTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	protected Clinic clinic;


	@Test
	public void getVets() {
		Collection<Vet> vets = this.clinic.getVets();
		// Use the inherited countRowsInTable() convenience method (from
		// AbstractTransactionalJUnit4SpringContextTests) to verify the results.
		assertEquals("JDBC query must show the same number of vets", super.countRowsInTable("VETS"), vets.size());
		Vet v1 = EntityUtils.getById(vets, Vet.class, 2);
		assertEquals("Leary", v1.getLastName());
		assertEquals(1, v1.getNrOfSpecialties());
		assertEquals("radiology", (v1.getSpecialties().get(0)).getName());
		Vet v2 = EntityUtils.getById(vets, Vet.class, 3);
		assertEquals("Douglas", v2.getLastName());
		assertEquals(2, v2.getNrOfSpecialties());
		assertEquals("dentistry", (v2.getSpecialties().get(0)).getName());
		assertEquals("surgery", (v2.getSpecialties().get(1)).getName());
	}

	@Test
	public void getPetTypes() {
		Collection<PetType> petTypes = this.clinic.getPetTypes();
		assertEquals("JDBC query must show the same number of pet types", super.countRowsInTable("TYPES"),
				petTypes.size());
		PetType t1 = EntityUtils.getById(petTypes, PetType.class, 1);
		assertEquals("cat", t1.getName());
		PetType t4 = EntityUtils.getById(petTypes, PetType.class, 4);
		assertEquals("snake", t4.getName());
	}

	@Test
	public void findOwners() {
		Collection<Owner> owners = this.clinic.findOwners("Davis");
		assertEquals(2, owners.size());
		owners = this.clinic.findOwners("Daviss");
		assertEquals(0, owners.size());
	}

	@Test
	public void loadOwner() {
		Owner o1 = this.clinic.loadOwner(1);
		assertTrue(o1.getLastName().startsWith("Franklin"));
		Owner o10 = this.clinic.loadOwner(10);
		assertEquals("Carlos", o10.getFirstName());

		// XXX: Add programmatic support for ending transactions with the
		// TestContext Framework.

		// Check lazy loading, by ending the transaction:
		// endTransaction();

		// Now Owners are "disconnected" from the data store.
		// We might need to touch this collection if we switched to lazy loading
		// in mapping files, but this test would pick this up.
		o1.getPets();
	}

	@Test
	public void insertOwner() {
		Collection<Owner> owners = this.clinic.findOwners("Schultz");
		int found = owners.size();
		Owner owner = new Owner();
		owner.setLastName("Schultz");
		this.clinic.storeOwner(owner);
		// assertTrue(!owner.isNew()); -- NOT TRUE FOR TOPLINK (before commit)
		owners = this.clinic.findOwners("Schultz");
		assertEquals("Verifying number of owners after inserting a new one.", found + 1, owners.size());
	}

	@Test
	public void updateOwner() throws Exception {
		Owner o1 = this.clinic.loadOwner(1);
		String old = o1.getLastName();
		o1.setLastName(old + "X");
		this.clinic.storeOwner(o1);
		o1 = this.clinic.loadOwner(1);
		assertEquals(old + "X", o1.getLastName());
	}

	@Test
	public void loadPet() {
		Collection<PetType> types = this.clinic.getPetTypes();
		Pet p7 = this.clinic.loadPet(7);
		assertTrue(p7.getName().startsWith("Samantha"));
		assertEquals(EntityUtils.getById(types, PetType.class, 1).getId(), p7.getType().getId());
		assertEquals("Jean", p7.getOwner().getFirstName());
		Pet p6 = this.clinic.loadPet(6);
		assertEquals("George", p6.getName());
		assertEquals(EntityUtils.getById(types, PetType.class, 4).getId(), p6.getType().getId());
		assertEquals("Peter", p6.getOwner().getFirstName());
	}

	@Test
	public void insertPet() {
		Owner o6 = this.clinic.loadOwner(6);
		int found = o6.getPets().size();
		Pet pet = new Pet();
		pet.setName("bowser");
		Collection<PetType> types = this.clinic.getPetTypes();
		pet.setType(EntityUtils.getById(types, PetType.class, 2));
		pet.setBirthDate(new Date());
		o6.addPet(pet);
		assertEquals(found + 1, o6.getPets().size());
		// both storePet and storeOwner are necessary to cover all ORM tools
		this.clinic.storePet(pet);
		this.clinic.storeOwner(o6);
		// assertTrue(!pet.isNew()); -- NOT TRUE FOR TOPLINK (before commit)
		o6 = this.clinic.loadOwner(6);
		assertEquals(found + 1, o6.getPets().size());
	}

	@Test
	public void updatePet() throws Exception {
		Pet p7 = this.clinic.loadPet(7);
		String old = p7.getName();
		p7.setName(old + "X");
		this.clinic.storePet(p7);
		p7 = this.clinic.loadPet(7);
		assertEquals(old + "X", p7.getName());
	}

	@Test
	public void insertVisit() {
		Pet p7 = this.clinic.loadPet(7);
		int found = p7.getVisits().size();
		Visit visit = new Visit();
		p7.addVisit(visit);
		visit.setDescription("test");
		// both storeVisit and storePet are necessary to cover all ORM tools
		this.clinic.storeVisit(visit);
		this.clinic.storePet(p7);
		// assertTrue(!visit.isNew()); -- NOT TRUE FOR TOPLINK (before commit)
		p7 = this.clinic.loadPet(7);
		assertEquals(found + 1, p7.getVisits().size());
	}

}
