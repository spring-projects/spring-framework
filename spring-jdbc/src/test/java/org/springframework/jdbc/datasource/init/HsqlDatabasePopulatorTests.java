/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * @author Sam Brannen
 * @since 4.0.3
 */
class HsqlDatabasePopulatorTests extends AbstractDatabasePopulatorTests {

	@Override
	protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
		return EmbeddedDatabaseType.HSQL;
	}

}
