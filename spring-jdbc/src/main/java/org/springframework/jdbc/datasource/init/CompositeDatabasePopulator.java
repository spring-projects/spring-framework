/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link DatabasePopulator} implementation that delegates to a list of other
 * {@code DatabasePopulator} implementations, executing all scripts.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public class CompositeDatabasePopulator implements DatabasePopulator {

	private List<DatabasePopulator> populators = new ArrayList<DatabasePopulator>();


	/**
	 * Specify a list of populators to delegate to.
	 */
	public void setPopulators(DatabasePopulator... populators) {
		this.populators.clear();
		this.populators.addAll(Arrays.asList(populators));
	}

	/**
	 * Add one or more populators to the list of delegates.
	 */
	public void addPopulators(DatabasePopulator... populators) {
		this.populators.addAll(Arrays.asList(populators));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void populate(Connection connection) throws SQLException, ScriptException {
		for (DatabasePopulator populator : this.populators) {
			populator.populate(connection);
		}
	}

}
