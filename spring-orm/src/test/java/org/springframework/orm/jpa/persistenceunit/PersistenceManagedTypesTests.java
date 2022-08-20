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
