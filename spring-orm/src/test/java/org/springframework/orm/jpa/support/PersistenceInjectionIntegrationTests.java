/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceContextSetter;
import org.springframework.orm.jpa.support.PersistenceInjectionTests.DefaultPublicPersistenceUnitSetterNamedPerson;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class PersistenceInjectionIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {

	// Enable support for @Autowired
	{
		setAutowireMode(AUTOWIRE_NO);
		setDependencyCheck(false);
	}

	@Autowired
	private DefaultPublicPersistenceContextSetter defaultSetterInjected;

	@Autowired
	private DefaultPublicPersistenceUnitSetterNamedPerson namedSetterInjected;


	public void testDefaultPersistenceContextSetterInjection() {
		assertNotNull(defaultSetterInjected.getEntityManager());
	}

	public void testSetterInjectionOfNamedPersistenceContext() {
		assertNotNull(namedSetterInjected.getEntityManagerFactory());
	}

}
