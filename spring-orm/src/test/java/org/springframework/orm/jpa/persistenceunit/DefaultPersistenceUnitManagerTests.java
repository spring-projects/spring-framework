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

package org.springframework.orm.jpa.persistenceunit;

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
		SpringPersistenceUnitInfo puInfo = buildDefaultPersistenceUnitInfo();
		assertThat(puInfo.getManagedClassNames()).contains(
				"org.springframework.orm.jpa.domain.Person",
				"org.springframework.orm.jpa.domain.DriversLicense");
	}

	private SpringPersistenceUnitInfo buildDefaultPersistenceUnitInfo() {
		this.manager.preparePersistenceUnitInfos();
		return (SpringPersistenceUnitInfo) this.manager.obtainDefaultPersistenceUnitInfo();
	}

}
