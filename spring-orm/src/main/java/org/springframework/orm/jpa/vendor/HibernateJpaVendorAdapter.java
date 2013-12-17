/*
 * Copyright 2002-2013 the original author or authors.
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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.ejb.HibernatePersistence;

import org.springframework.orm.jpa.JpaDialect;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for
 * Hibernate EntityManager. Developed and tested against Hibernate 3.6 and 4.2/4.3.
 *
 * <p>Exposes Hibernate's persistence provider and EntityManager extension interface,
 * and supports {@link AbstractJpaVendorAdapter}'s common configuration settings.
 *
 * <p>Note that the package location of Hibernate's JPA support changed from 4.2 to 4.3:
 * from {@code org.hibernate.ejb.HibernateEntityManager(Factory)} to
 * {@code org.hibernate.jpa.HibernateEntityManager(Factory)}. As of Spring 4.0,
 * we're exposing the correct, non-deprecated variant depending on the Hibernate
 * version encountered at runtime, in order to avoid deprecation log entries.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 */
public class HibernateJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final PersistenceProvider persistenceProvider;

	private final JpaDialect jpaDialect = new HibernateJpaDialect();

	private final Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	private final Class<? extends EntityManager> entityManagerInterface;


	@SuppressWarnings({"deprecation", "unchecked"})
	public HibernateJpaVendorAdapter() {
		PersistenceProvider providerToUse;
		Class<? extends EntityManagerFactory> emfIfcToUse;
		Class<? extends EntityManager> emIfcToUse;
		try {
			// Try Hibernate 4.3's org.hibernate.jpa package in order to avoid deprecation warnings
			ClassLoader cl = HibernateJpaVendorAdapter.class.getClassLoader();
			Class<?> hibernatePersistenceProviderClass = cl.loadClass("org.hibernate.jpa.HibernatePersistenceProvider");
			providerToUse = (PersistenceProvider) hibernatePersistenceProviderClass.newInstance();
			emfIfcToUse = (Class<? extends EntityManagerFactory>) cl.loadClass("org.hibernate.jpa.HibernateEntityManagerFactory");
			emIfcToUse = (Class<? extends EntityManager>) cl.loadClass("org.hibernate.jpa.HibernateEntityManager");
		}
		catch (ClassNotFoundException ex) {
			// Fall back to Hibernate 3.6-4.2 org.hibernate.ejb package
			providerToUse = new HibernatePersistence();
			emfIfcToUse = HibernateEntityManagerFactory.class;
			emIfcToUse = HibernateEntityManager.class;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Found HibernatePersistenceProvider but could not instantiate it", ex);
		}
		this.persistenceProvider = providerToUse;
		this.entityManagerFactoryInterface = emfIfcToUse;
		this.entityManagerInterface = emIfcToUse;
	}


	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public String getPersistenceProviderRootPackage() {
		return "org.hibernate";
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		Map<String, Object> jpaProperties = new HashMap<String, Object>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(Environment.DIALECT, getDatabasePlatform());
		}
		else if (getDatabase() != null) {
			Class<?> databaseDialectClass = determineDatabaseDialectClass(getDatabase());
			if (databaseDialectClass != null) {
				jpaProperties.put(Environment.DIALECT, databaseDialectClass.getName());
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(Environment.HBM2DDL_AUTO, "update");
		}
		if (isShowSql()) {
			jpaProperties.put(Environment.SHOW_SQL, "true");
		}

		return jpaProperties;
	}

	/**
	 * Determine the Hibernate database dialect class for the given target database.
	 * @param database the target database
	 * @return the Hibernate database dialect class, or {@code null} if none found
	 */
	@SuppressWarnings("deprecation")
	protected Class<?> determineDatabaseDialectClass(Database database) {
		switch (database) {
			case DB2: return DB2Dialect.class;
			case DERBY: return DerbyDialect.class;  // DerbyDialect deprecated in 4.x
			case H2: return H2Dialect.class;
			case HSQL: return HSQLDialect.class;
			case INFORMIX: return InformixDialect.class;
			case MYSQL: return MySQLDialect.class;
			case ORACLE: return Oracle9iDialect.class;
			case POSTGRESQL: return PostgreSQLDialect.class;  // PostgreSQLDialect deprecated in 4.x
			case SQL_SERVER: return SQLServerDialect.class;
			case SYBASE: return org.hibernate.dialect.SybaseDialect.class;  // SybaseDialect deprecated in 3.6 but not 4.x
			default: return null;
		}
	}

	@Override
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	@Override
	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return this.entityManagerFactoryInterface;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return this.entityManagerInterface;
	}

}
