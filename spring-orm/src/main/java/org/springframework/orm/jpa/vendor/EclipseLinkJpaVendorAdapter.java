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

package org.springframework.orm.jpa.vendor;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import jakarta.persistence.EntityManager;
import jakarta.persistence.spi.PersistenceProvider;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.jspecify.annotations.Nullable;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for Eclipse
 * Persistence Services (EclipseLink). Compatible with EclipseLink 3.0/4.0.
 *
 * <p>Exposes EclipseLink's persistence provider and EntityManager extension interface,
 * and adapts {@link AbstractJpaVendorAdapter}'s common configuration settings.
 * No support for the detection of annotated packages (through
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()})
 * since EclipseLink doesn't use package-level metadata.
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 2.5.2
 * @see EclipseLinkJpaDialect
 * @see org.eclipse.persistence.jpa.PersistenceProvider
 * @see org.eclipse.persistence.jpa.JpaEntityManager
 */
public class EclipseLinkJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final PersistenceProvider persistenceProvider = new org.eclipse.persistence.jpa.PersistenceProvider();

	private final EclipseLinkJpaDialect jpaDialect = new EclipseLinkJpaDialect();


	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		Map<String, Object> jpaProperties = new HashMap<>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(PersistenceUnitProperties.TARGET_DATABASE, getDatabasePlatform());
		}
		else {
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
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +
					org.eclipse.persistence.logging.SessionLog.SQL, Level.FINE.toString());
			jpaProperties.put(PersistenceUnitProperties.LOGGING_PARAMETERS, Boolean.TRUE.toString());
		}

		return jpaProperties;
	}

	/**
	 * Determine the EclipseLink target database name for the given database.
	 * @param database the specified database
	 * @return the EclipseLink target database name, or {@code null} if none found
	 */
	protected @Nullable String determineTargetDatabaseName(Database database) {
		return switch (database) {
			case DB2 -> TargetDatabase.DB2;
			case DERBY -> TargetDatabase.Derby;
			case HANA -> TargetDatabase.HANA;
			case HSQL -> TargetDatabase.HSQL;
			case INFORMIX -> TargetDatabase.Informix;
			case MYSQL -> TargetDatabase.MySQL;
			case ORACLE -> TargetDatabase.Oracle;
			case POSTGRESQL -> TargetDatabase.PostgreSQL;
			case SQL_SERVER -> TargetDatabase.SQLServer;
			case SYBASE -> TargetDatabase.Sybase;
			default -> null;
		};
	}

	@Override
	public EclipseLinkJpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return JpaEntityManager.class;
	}

}
