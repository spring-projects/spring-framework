/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceProvider;

import oracle.toplink.essentials.config.TargetDatabase;
import oracle.toplink.essentials.config.TopLinkProperties;
import oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider;

import org.springframework.orm.jpa.JpaDialect;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for
 * Oracle TopLink Essentials. Developed and tested against TopLink Essentials v2.
 *
 * <p>Exposes TopLink's persistence provider and EntityManager extension interface,
 * and supports {@link AbstractJpaVendorAdapter}'s common configuration settings.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider
 * @see oracle.toplink.essentials.ejb.cmp3.EntityManager
 */
public class TopLinkJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final PersistenceProvider persistenceProvider = new EntityManagerFactoryProvider();

	private final JpaDialect jpaDialect = new TopLinkJpaDialect();


	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public String getPersistenceProviderRootPackage() {
		return "oracle.toplink.essentials";
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		Map<String, Object> jpaProperties = new HashMap<String, Object>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(TopLinkProperties.TARGET_DATABASE, getDatabasePlatform());
		}
		else if (getDatabase() != null) {
			String targetDatabase = determineTargetDatabaseName(getDatabase());
			if (targetDatabase != null) {
				jpaProperties.put(TopLinkProperties.TARGET_DATABASE, targetDatabase);
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(EntityManagerFactoryProvider.DDL_GENERATION,
					EntityManagerFactoryProvider.CREATE_ONLY);
			jpaProperties.put(EntityManagerFactoryProvider.DDL_GENERATION_MODE,
					EntityManagerFactoryProvider.DDL_DATABASE_GENERATION);
		}
		if (isShowSql()) {
			jpaProperties.put(TopLinkProperties.LOGGING_LEVEL, Level.FINE.toString());
		}

		return jpaProperties;
	}

	/**
	 * Determine the TopLink target database name for the given database.
	 * @param database the specified database
	 * @return the TopLink target database name, or <code>null<code> if none found
	 */
	protected String determineTargetDatabaseName(Database database) {
		switch (database) {
			case DB2: return TargetDatabase.DB2;
			case DERBY: return TargetDatabase.Derby;
			case HSQL: return TargetDatabase.HSQL;
			case INFORMIX: return TargetDatabase.Informix;
			case MYSQL: return TargetDatabase.MySQL4;
			case ORACLE: return TargetDatabase.Oracle;
			case POSTGRESQL: return TargetDatabase.PostgreSQL;
			case SQL_SERVER: return TargetDatabase.SQLServer;
			case SYBASE: return TargetDatabase.Sybase;
			default: return null;
		}
	}

	@Override
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return oracle.toplink.essentials.ejb.cmp3.EntityManager.class;
	}

}
