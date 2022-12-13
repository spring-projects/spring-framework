/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

import org.springframework.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an H2 embedded database instance
 * with auto-commit disabled.
 *
 * @author Sam Brannen
 * @since 5.3.11
 */
public class AutoCommitDisabledH2EmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	private final Class<? extends Driver> driverClass;


	@SuppressWarnings("unchecked")
	public AutoCommitDisabledH2EmbeddedDatabaseConfigurer() throws Exception {
		this.driverClass = (Class<? extends Driver>) ClassUtils.forName("org.h2.Driver",
			AutoCommitDisabledH2EmbeddedDatabaseConfigurer.class.getClassLoader());
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		String url = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;AUTOCOMMIT=false", databaseName);

		properties.setDriverClass(this.driverClass);
		properties.setUrl(url);
		properties.setUsername("sa");
		properties.setPassword("");
	}

}
