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

package org.springframework.jdbc.datasource.init;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceDatabasePopulator}.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see AbstractDatabasePopulatorTests
 */
class ResourceDatabasePopulatorTests {

	private static final Resource script1 = mock();
	private static final Resource script2 = mock();
	private static final Resource script3 = mock();


	@Test
	void constructWithNullResource() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ResourceDatabasePopulator((Resource) null));
	}

	@Test
	void constructWithNullResourceArray() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ResourceDatabasePopulator((Resource[]) null));
	}

	@Test
	void constructWithResource() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(script1);
		assertThat(databasePopulator.scripts).hasSize(1);
	}

	@Test
	void constructWithMultipleResources() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(script1, script2);
		assertThat(databasePopulator.scripts).hasSize(2);
	}

	@Test
	void constructWithMultipleResourcesAndThenAddScript() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(script1, script2);
		assertThat(databasePopulator.scripts).hasSize(2);

		databasePopulator.addScript(script3);
		assertThat(databasePopulator.scripts).hasSize(3);
	}

	@Test
	void addScriptsWithNullResource() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		assertThatIllegalArgumentException().isThrownBy(() ->
				databasePopulator.addScripts((Resource) null));
	}

	@Test
	void addScriptsWithNullResourceArray() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		assertThatIllegalArgumentException().isThrownBy(() ->
				databasePopulator.addScripts((Resource[]) null));
	}

	@Test
	void setScriptsWithNullResource() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		assertThatIllegalArgumentException().isThrownBy(() ->
				databasePopulator.setScripts((Resource) null));
	}

	@Test
	void setScriptsWithNullResourceArray() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		assertThatIllegalArgumentException().isThrownBy(() ->
				databasePopulator.setScripts((Resource[]) null));
	}

	@Test
	void setScriptsAndThenAddScript() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		assertThat(databasePopulator.scripts).isEmpty();

		databasePopulator.setScripts(script1, script2);
		assertThat(databasePopulator.scripts).hasSize(2);

		databasePopulator.addScript(script3);
		assertThat(databasePopulator.scripts).hasSize(3);
	}

}
