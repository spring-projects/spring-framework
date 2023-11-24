/*
 * Copyright 2002-2022 the original author or authors.
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
import org.springframework.orm.jpa.domain.DriversLicense;
import org.springframework.orm.jpa.domain.Employee;
import org.springframework.orm.jpa.domain.EmployeeLocationConverter;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.orm.jpa.domain2.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PersistenceManagedTypesScanner}.
 *
 * @author Stephane Nicoll
 */
class PersistenceManagedTypesScannerTests {

	private final PersistenceManagedTypesScanner scanner = new PersistenceManagedTypesScanner(new DefaultResourceLoader());

	@Test
	void scanPackageWithOnlyEntities() {
		PersistenceManagedTypes managedTypes = this.scanner.scan("org.springframework.orm.jpa.domain");
		assertThat(managedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
				Person.class.getName(), DriversLicense.class.getName(), Employee.class.getName(),
				EmployeeLocationConverter.class.getName());
		assertThat(managedTypes.getManagedPackages()).isEmpty();
	}

	@Test
	void scanPackageWithEntitiesAndManagedPackages() {
		PersistenceManagedTypes managedTypes = this.scanner.scan("org.springframework.orm.jpa.domain2");
		assertThat(managedTypes.getManagedClassNames()).containsExactlyInAnyOrder(User.class.getName());
		assertThat(managedTypes.getManagedPackages()).containsExactlyInAnyOrder(
				"org.springframework.orm.jpa.domain2");
	}

	@Test
	void scanPackageUsesIndexIfPresent() {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
						new ClassPathResource("test-spring.components", getClass())));
		PersistenceManagedTypes managedTypes = new PersistenceManagedTypesScanner(resourceLoader).scan("com.example");
		assertThat(managedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
				"com.example.domain.Person", "com.example.domain.Address");
		assertThat(managedTypes.getManagedPackages()).containsExactlyInAnyOrder(
				"com.example.domain");

	}

}
