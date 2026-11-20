/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceContextSetter;
import org.springframework.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceUnitSetterNamedPerson;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class PersistenceInjectionIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {

	@Autowired
	private DefaultPublicPersistenceContextSetter defaultSetterInjected;

	@Autowired
	private DefaultPublicPersistenceUnitSetterNamedPerson namedSetterInjected;


	@Test
	void defaultPersistenceContextSetterInjection() {
		assertThat(defaultSetterInjected.getEntityManager()).isNotNull();
	}

	@Test
	void setterInjectionOfNamedPersistenceContext() {
		assertThat(namedSetterInjected.getEntityManagerFactory()).isNotNull();
	}

}
