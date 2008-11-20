
package org.springframework.samples.petclinic.jpa;

/**
 * <p>
 * Tests for the DAO variant based on the shared EntityManager approach, using
 * Apache OpenJPA for testing instead of the reference implementation.
 * </p>
 * <p>
 * Specifically tests usage of an <code>orm.xml</code> file, loaded by the
 * persistence provider through the Spring-provided persistence unit root URL.
 * </p>
 *
 * @author Juergen Hoeller
 */
public class OpenJpaEntityManagerClinicTests extends EntityManagerClinicTests {

	@Override
	protected String[] getConfigPaths() {
		return new String[] {
			"applicationContext-jpaCommon.xml",
			"applicationContext-openJpaAdapter.xml",
			"applicationContext-entityManager.xml"
		};
	}

}
