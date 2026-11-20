/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PersistenceManagedTypes}.
 *
 * @author Stephane Nicoll
 */
class PersistenceManagedTypesTests {

	@Test
	void createWithManagedClassNames() {
		PersistenceManagedTypes managedTypes = PersistenceManagedTypes.of(
				"com.example.One", "com.example.Two");
		assertThat(managedTypes.getManagedClassNames()).containsExactly(
				"com.example.One", "com.example.Two");
		assertThat(managedTypes.getManagedPackages()).isEmpty();
		assertThat(managedTypes.getPersistenceUnitRootUrl()).isNull();
	}

	@Test
	void createWithNullManagedClasses() {
		assertThatIllegalArgumentException().isThrownBy(() -> PersistenceManagedTypes.of((String[]) null));
	}

	@Test
	void createWithManagedClassNamesAndPackages() {
		PersistenceManagedTypes managedTypes = PersistenceManagedTypes.of(
				List.of("com.example.One", "com.example.Two"), List.of("com.example"));
		assertThat(managedTypes.getManagedClassNames()).containsExactly(
				"com.example.One", "com.example.Two");
		assertThat(managedTypes.getManagedPackages()).containsExactly("com.example");
		assertThat(managedTypes.getPersistenceUnitRootUrl()).isNull();
	}

}
