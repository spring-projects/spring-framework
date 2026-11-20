/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
