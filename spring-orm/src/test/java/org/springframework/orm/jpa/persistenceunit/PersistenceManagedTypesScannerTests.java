/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.index.CandidateComponentsTestClassLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.orm.jpa.domain.Car;
import org.springframework.orm.jpa.domain.DriversLicense;
import org.springframework.orm.jpa.domain.Employee;
import org.springframework.orm.jpa.domain.EmployeeCategoryConverter;
import org.springframework.orm.jpa.domain.EmployeeKindConverter;
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

	private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

	private final PersistenceManagedTypesScanner scanner = new PersistenceManagedTypesScanner(resourceLoader);


	@Test
	void scanPackageWithOnlyEntities() {
		PersistenceManagedTypes managedTypes = this.scanner.scan("org.springframework.orm.jpa.domain");
		assertThat(managedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
				Person.class.getName(), DriversLicense.class.getName(), Employee.class.getName(),
				EmployeeCategoryConverter.class.getName(), EmployeeKindConverter.class.getName(),
				EmployeeLocationConverter.class.getName(), Car.class.getName());
		assertThat(managedTypes.getManagedPackages()).isEmpty();
	}

	@Test
	void scanPackageInvokesManagedClassNamesFilter() {
		ManagedClassNameFilter filter = mock(ManagedClassNameFilter.class);
		given(filter.matches(anyString())).willReturn(true);
		new PersistenceManagedTypesScanner(resourceLoader, filter)
				.scan("org.springframework.orm.jpa.domain");
		verify(filter).matches(Person.class.getName());
		verify(filter).matches(DriversLicense.class.getName());
		verify(filter).matches(Employee.class.getName());
		verify(filter).matches(EmployeeCategoryConverter.class.getName());
		verify(filter).matches(EmployeeKindConverter.class.getName());
		verify(filter).matches(EmployeeLocationConverter.class.getName());
		verify(filter).matches(Car.class.getName());
		verifyNoMoreInteractions(filter);
	}

	@Test
	void scanPackageWithUseManagedClassNamesFilter() {
		List<String> candidates = List.of(Person.class.getName(), DriversLicense.class.getName());
		PersistenceManagedTypes managedTypes = new PersistenceManagedTypesScanner(
				resourceLoader, candidates::contains).scan("org.springframework.orm.jpa.domain");
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
