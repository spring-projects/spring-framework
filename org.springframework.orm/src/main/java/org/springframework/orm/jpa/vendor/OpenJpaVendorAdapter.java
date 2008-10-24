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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.PersistenceProviderImpl;

import org.springframework.orm.jpa.JpaDialect;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for
 * Apache OpenJPA. Developed and tested against OpenJPA 1.0.0.
 *
 * <p>Exposes OpenJPA's persistence provider and EntityManager extension interface,
 * and supports {@link AbstractJpaVendorAdapter}'s common configuration settings.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.apache.openjpa.persistence.PersistenceProviderImpl
 * @see org.apache.openjpa.persistence.OpenJPAEntityManager
 */
public class OpenJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final PersistenceProvider persistenceProvider = new PersistenceProviderImpl();

	private final OpenJpaDialect jpaDialect = new OpenJpaDialect();


	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	public String getPersistenceProviderRootPackage() {
		return "org.apache.openjpa";
	}

	public Map getJpaPropertyMap() {
		Properties jpaProperties = new Properties();

		if (getDatabasePlatform() != null) {
			jpaProperties.setProperty("openjpa.jdbc.DBDictionary", getDatabasePlatform());
		}
		else if (getDatabase() != null) {
			String databaseDictonary = determineDatabaseDictionary(getDatabase());
			if (databaseDictonary != null) {
				jpaProperties.setProperty("openjpa.jdbc.DBDictionary", databaseDictonary);
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
		}

		if (isShowSql()) {
			// Taken from the OpenJPA 0.9.6 docs ("Standard OpenJPA Log Configuration + All SQL Statements")
			jpaProperties.setProperty("openjpa.Log", "DefaultLevel=WARN, Runtime=INFO, Tool=INFO, SQL=TRACE");
		}

		return jpaProperties;
	}

	/**
	 * Determine the OpenJPA database dictionary name for the given database.
	 * @param database the specified database
	 * @return the OpenJPA database dictionary name, or <code>null<code> if none found
	 */
	protected String determineDatabaseDictionary(Database database) {
		switch (database) {
			case DB2: return "db2";
			case DERBY: return "derby";
			case HSQL: return "hsql(SimulateLocking=true)";
			case INFORMIX: return "informix";
			case MYSQL: return "mysql";
			case ORACLE: return "oracle";
			case POSTGRESQL: return "postgres";
			case SQL_SERVER: return "sqlserver";
			case SYBASE: return "sybase";
			default: return null;
		}
	}

	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return OpenJPAEntityManagerFactorySPI.class;
	}

	public Class<? extends EntityManager> getEntityManagerInterface() {
		return OpenJPAEntityManagerSPI.class;
	}

}
