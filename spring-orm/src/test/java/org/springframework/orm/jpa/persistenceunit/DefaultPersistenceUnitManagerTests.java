/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import jakarta.persistence.spi.PersistenceUnitInfo;
import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.index.CandidateComponentsTestClassLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.orm.jpa.domain.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultPersistenceUnitManager}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class DefaultPersistenceUnitManagerTests {

	private final DefaultPersistenceUnitManager manager = new DefaultPersistenceUnitManager();


	@Test
	void defaultDomainWithScan() {
		this.manager.setPackagesToScan("org.springframework.orm.jpa.domain");
		this.manager.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testDefaultDomain();
	}

	@Test
	void defaultDomainWithIndex() {
		this.manager.setPackagesToScan("org.springframework.orm.jpa.domain");
		this.manager.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
						new ClassPathResource("spring.components", Person.class))));
		testDefaultDomain();
	}

	private void testDefaultDomain() {
		this.manager.preparePersistenceUnitInfos();
		PersistenceUnitInfo pui = this.manager.obtainDefaultPersistenceUnitInfo();
		assertThat(pui.getManagedClassNames()).contains(
				"org.springframework.orm.jpa.domain.Person",
				"org.springframework.orm.jpa.domain.DriversLicense");
	}

}
