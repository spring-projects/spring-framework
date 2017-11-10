/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.orm.jpa.vendor;

import java.util.Collections;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaVendorAdapter;

/**
 * Abstract {@link JpaVendorAdapter} implementation that defines common properties,
 * to be translated into vendor-specific JPA properties by concrete subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 */
public abstract class AbstractJpaVendorAdapter implements JpaVendorAdapter {

	private Database database = Database.DEFAULT;

	@Nullable
	private String databasePlatform;

	private boolean generateDdl = false;

	private boolean showSql = false;


	/**
	 * Specify the target database to operate on, as a value of the {@code Database} enum:
	 * DB2, DERBY, H2, HSQL, INFORMIX, MYSQL, ORACLE, POSTGRESQL, SQL_SERVER, SYBASE
	 */
	public void setDatabase(Database database) {
		this.database = database;
	}

	/**
	 * Return the target database to operate on.
	 */
	protected Database getDatabase() {
		return this.database;
	}

	/**
	 * Specify the name of the target database to operate on.
	 * The supported values are vendor-dependent platform identifiers.
	 */
	public void setDatabasePlatform(@Nullable String databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	/**
	 * Return the name of the target database to operate on.
	 */
	@Nullable
	protected String getDatabasePlatform() {
		return this.databasePlatform;
	}

	/**
	 * Set whether to generate DDL after the EntityManagerFactory has been initialized,
	 * creating/updating all relevant tables.
	 * <p>Note that the exact semantics of this flag depend on the underlying
	 * persistence provider. For any more advanced needs, specify the appropriate
	 * vendor-specific settings as "jpaProperties".
	 * <p><b>NOTE: Do not set this flag to 'true' while also setting JPA 2.1's
	 * {@code javax.persistence.schema-generation.database.action} property.</b>
	 * These two schema generation mechanisms - standard JPA versus provider-native -
	 * are mutually exclusive, e.g. with Hibernate 5.
	 * @see org.springframework.orm.jpa.AbstractEntityManagerFactoryBean#setJpaProperties
	 */
	public void setGenerateDdl(boolean generateDdl) {
		this.generateDdl = generateDdl;
	}

	/**
	 * Return whether to generate DDL after the EntityManagerFactory has been initialized
	 * creating/updating all relevant tables.
	 */
	protected boolean isGenerateDdl() {
		return this.generateDdl;
	}

	/**
	 * Set whether to show SQL in the log (or in the console).
	 * <p>For more specific logging configuration, specify the appropriate
	 * vendor-specific settings as "jpaProperties".
	 * @see org.springframework.orm.jpa.AbstractEntityManagerFactoryBean#setJpaProperties
	 */
	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	/**
	 * Return whether to show SQL in the log (or in the console).
	 */
	protected boolean isShowSql() {
		return this.showSql;
	}


	@Override
	@Nullable
	public String getPersistenceProviderRootPackage() {
		return null;
	}

	@Override
	public Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return getJpaPropertyMap();
	}

	@Override
	public Map<String, ?> getJpaPropertyMap() {
		return Collections.emptyMap();
	}

	@Override
	@Nullable
	public JpaDialect getJpaDialect() {
		return null;
	}

	@Override
	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return EntityManagerFactory.class;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return EntityManager.class;
	}

	@Override
	public void postProcessEntityManagerFactory(EntityManagerFactory emf) {
	}

}
