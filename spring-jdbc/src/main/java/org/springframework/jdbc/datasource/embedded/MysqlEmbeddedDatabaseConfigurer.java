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

import java.nio.file.Paths;
import java.sql.Driver;
import javax.sql.DataSource;

import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.EmbeddedMysql.Builder;
import com.wix.mysql.config.DownloadConfig;
import com.wix.mysql.config.MysqldConfig;
import com.wix.mysql.distribution.Version;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.config.DownloadConfig.aDownloadConfig;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;

/**
 * {@link EmbeddedDatabaseConfigurer} for Mysql embedded database instance.
 *
 * <p> Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Juliano Alves
 * @since 5.2
 */
public final class MysqlEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private EmbeddedMysql mysql;
	private final Builder builder;

	@Nullable
	private static MysqlEmbeddedDatabaseConfigurer instance;

	private final Class<? extends Driver> driverClass;

	public MysqlEmbeddedDatabaseConfigurer(final Class<? extends Driver> driverClass) {
		this.driverClass = driverClass;
		DownloadConfig downloadConfig = aDownloadConfig().withCacheDir(Paths.get(System.getProperty("user.home"), ".springframework", "embedded_mysql").toString()).build();
		MysqldConfig mysqldConfig = aMysqldConfig(Version.v5_7_latest).withPort(33060).build();
		this.builder = anEmbeddedMysql(mysqldConfig, downloadConfig).addSchema("testdb");
	}

	@SuppressWarnings("unchecked")
	public static MysqlEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (instance == null) {
			instance = new MysqlEmbeddedDatabaseConfigurer((Class<? extends Driver>)
					ClassUtils.forName("com.mysql.cj.jdbc.Driver", MysqlEmbeddedDatabaseConfigurer.class.getClassLoader()));
		}
		return instance;
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		this.mysql = this.builder.start();

		properties.setUrl("jdbc:mysql://localhost:33060/" + databaseName);
		properties.setUsername(MysqldConfig.SystemDefaults.USERNAME);
		properties.setPassword("");
		properties.setDriverClass(this.driverClass);
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		this.mysql.stop();
	}
}
