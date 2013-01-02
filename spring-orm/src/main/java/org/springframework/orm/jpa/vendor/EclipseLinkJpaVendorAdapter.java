/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceProvider;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.eclipse.persistence.jpa.JpaEntityManager;

import org.springframework.orm.jpa.JpaDialect;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for Eclipse
 * Persistence Services (EclipseLink). Developed and tested against EclipseLink
 * 1.0 as well as 2.0-2.3.
 *
 * <p>Exposes EclipseLink's persistence provider and EntityManager extension interface,
 * and supports {@link AbstractJpaVendorAdapter}'s common configuration settings.
 *
 * <p>This class is very analogous to {@link TopLinkJpaVendorAdapter}, since
 * EclipseLink is effectively the next generation of the TopLink product.
 * Thanks to Mike Keith for the original EclipseLink support prototype!
 *
 * <p>NOTE: No need to filter out classes from the JPA providers package for
 * EclipseLink (see SPR-6040)
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 2.5.2
 * @see org.eclipse.persistence.jpa.PersistenceProvider
 * @see org.eclipse.persistence.jpa.JpaEntityManager
 */
public class EclipseLinkJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final PersistenceProvider persistenceProvider = new org.eclipse.persistence.jpa.PersistenceProvider();

	private final JpaDialect jpaDialect = new EclipseLinkJpaDialect();


	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		Map<String, Object> jpaProperties = new HashMap<String, Object>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(PersistenceUnitProperties.TARGET_DATABASE, getDatabasePlatform());
		}
		else if (getDatabase() != null) {
			String targetDatabase = determineTargetDatabaseName(getDatabase());
			if (targetDatabase != null) {
				jpaProperties.put(PersistenceUnitProperties.TARGET_DATABASE, targetDatabase);
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION,
					PersistenceUnitProperties.CREATE_ONLY);
			jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION_MODE,
					PersistenceUnitProperties.DDL_DATABASE_GENERATION);
		}
		if (isShowSql()) {
			jpaProperties.put(PersistenceUnitProperties.LOGGING_LEVEL, Level.FINE.toString());
		}

		return jpaProperties;
	}

	/**
	 * Determine the EclipseLink target database name for the given database.
	 * @param database the specified database
	 * @return the EclipseLink target database name, or {@code null} if none found
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
		return JpaEntityManager.class;
	}

}
