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
	void testDefaultPersistenceContextSetterInjection() {
		assertThat(defaultSetterInjected.getEntityManager()).isNotNull();
	}

	@Test
	void testSetterInjectionOfNamedPersistenceContext() {
		assertThat(namedSetterInjected.getEntityManagerFactory()).isNotNull();
	}

}
