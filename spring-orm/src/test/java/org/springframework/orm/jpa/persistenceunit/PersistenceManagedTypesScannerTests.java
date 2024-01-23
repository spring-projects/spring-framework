/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.List;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link PersistenceManagedTypesScanner}.
 *
 * @author Stephane Nicoll
 */
class PersistenceManagedTypesScannerTests {

	public static final DefaultResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

	private final PersistenceManagedTypesScanner scanner = new PersistenceManagedTypesScanner(RESOURCE_LOADER);

	@Test
	void scanPackageWithOnlyEntities() {
		PersistenceManagedTypes managedTypes = this.scanner.scan("org.springframework.orm.jpa.domain");
		assertThat(managedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
				Person.class.getName(), DriversLicense.class.getName(), Employee.class.getName(),
				EmployeeLocationConverter.class.getName());
		assertThat(managedTypes.getManagedPackages()).isEmpty();
	}

	@Test
	void scanPackageInvokesManagedClassNamesFilter() {
		ManagedClassNameFilter filter = mock(ManagedClassNameFilter.class);
		given(filter.matches(anyString())).willReturn(true);
		new PersistenceManagedTypesScanner(RESOURCE_LOADER, filter)
				.scan("org.springframework.orm.jpa.domain");
		verify(filter).matches(Person.class.getName());
		verify(filter).matches(DriversLicense.class.getName());
		verify(filter).matches(Employee.class.getName());
		verify(filter).matches(EmployeeLocationConverter.class.getName());
		verifyNoMoreInteractions(filter);
	}

	@Test
	void scanPackageWithUseManagedClassNamesFilter() {
		List<String> candidates = List.of(Person.class.getName(), DriversLicense.class.getName());
		PersistenceManagedTypes managedTypes = new PersistenceManagedTypesScanner(
				RESOURCE_LOADER, candidates::contains).scan("org.springframework.orm.jpa.domain");
		assertThat(managedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
				Person.class.getName(), DriversLicense.class.getName());
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

	@Test
	void scanPackageUsesIndexAndClassNameFilterIfPresent() {
		List<String> candidates = List.of("com.example.domain.Address");
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
						new ClassPathResource("test-spring.components", getClass())));
		PersistenceManagedTypes managedTypes = new PersistenceManagedTypesScanner(
				resourceLoader, candidates::contains).scan("com.example");
		assertThat(managedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
				"com.example.domain.Address");
		assertThat(managedTypes.getManagedPackages()).containsExactlyInAnyOrder(
				"com.example.domain");
	}

}
