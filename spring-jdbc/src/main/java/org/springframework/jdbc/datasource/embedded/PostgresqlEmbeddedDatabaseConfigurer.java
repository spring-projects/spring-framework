/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Driver;
import java.util.List;
import javax.sql.DataSource;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.distribution.Version.Main;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.cachedRuntimeConfig;

/**
 * {@link EmbeddedDatabaseConfigurer} for PostgreSQL embedded database instance.
 *
 * <p>
 * Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Juliano Alves
 * @since 5.2
 */
public final class PostgresqlEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private static final List<String> DEFAULT_ADD_PARAMS = asList(
			"-E",
			"SQL_ASCII",
			"--locale=C",
			"--lc-collate=C",
			"--lc-ctype=C");

	private final EmbeddedPostgres postgres;
	private final IRuntimeConfig cached;

	@Nullable
	private static PostgresqlEmbeddedDatabaseConfigurer instance;

	private final Class<? extends Driver> driverClass;

	private PostgresqlEmbeddedDatabaseConfigurer(final Class<? extends Driver> driverClass) {
		this.driverClass = driverClass;
		this.postgres = new EmbeddedPostgres(Main.V9_6);
		this.cached = cachedRuntimeConfig(Paths.get(System.getProperty("user.home"), ".springframework", "embedded_postgres"));
	}

	/**
	 * Get the singleton {@link PostgresqlEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer instance
	 */
	@SuppressWarnings("unchecked")
	public static synchronized PostgresqlEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (instance == null) {
			instance = new PostgresqlEmbeddedDatabaseConfigurer((Class<? extends Driver>)
					ClassUtils.forName("org.postgresql.Driver", PostgresqlEmbeddedDatabaseConfigurer.class.getClassLoader()));
		}
		return instance;
	}

	@Override
	public void configureConnectionProperties(final ConnectionProperties properties, final String databaseName) {
		String url;
		try {
			url = this.postgres.start(this.cached, "localhost", 63585, databaseName, "sa", "sa", DEFAULT_ADD_PARAMS);
		}
		catch (final IOException ex) {
			throw new RuntimeException(ex);
		}

		properties.setDriverClass(this.driverClass);
		properties.setUrl(url);
	}

	@Override
	public void shutdown(final DataSource dataSource, final String databaseName) {
		this.postgres.stop();
	}
}
