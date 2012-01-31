/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.orm.jpa.support;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceContextSetter;
import org.springframework.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceUnitSetterNamedPerson;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class PersistenceInjectionIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {

	@Autowired
	private DefaultPublicPersistenceContextSetter defaultSetterInjected;

	private DefaultPublicPersistenceUnitSetterNamedPerson namedSetterInjected;


	public PersistenceInjectionIntegrationTests() {
		setAutowireMode(AUTOWIRE_NO);
		setDependencyCheck(false);
	}

	@Autowired
	private void init(DefaultPublicPersistenceUnitSetterNamedPerson namedSetterInjected) {
		this.namedSetterInjected = namedSetterInjected;
	}


	public void testDefaultSetterInjection() {
		EntityManager injectedEm = defaultSetterInjected.getEntityManager();
		assertNotNull("Default PersistenceContext Setter was injected", injectedEm);
	}

	public void testInjectedEntityManagerImplmentsPortableEntityManagerPlus() {
		EntityManager injectedEm = defaultSetterInjected.getEntityManager();
		assertNotNull("Default PersistenceContext Setter was injected", injectedEm);
	}

	public void testSetterInjectionOfNamedPersistenceContext() {
		assertNotNull("Named PersistenceContext Setter was injected", namedSetterInjected.getEntityManagerFactory());
	}

}
