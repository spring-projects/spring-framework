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
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServer2008Dialect;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for Hibernate
 * EntityManager. Developed and tested against Hibernate 3.6, 4.2/4.3 as well as 5.x.
 * <b>Hibernate 4.2+ is strongly recommended for use with Spring 4.0+.</b>
 *
 * <p>Exposes Hibernate's persistence provider and EntityManager extension interface,
 * and adapts {@link AbstractJpaVendorAdapter}'s common configuration settings.
 * Also supports the detection of annotated packages (through
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}),
 * e.g. containing Hibernate {@link org.hibernate.annotations.FilterDef} annotations,
 * along with Spring-driven entity scanning which requires no {@code persistence.xml}
 * ({@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPackagesToScan}).
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
 * @see HibernateJpaDialect
 * @see org.hibernate.ejb.HibernatePersistence
 * @see org.hibernate.ejb.HibernateEntityManager
 */
public class HibernateJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final HibernateJpaDialect jpaDialect = new HibernateJpaDialect();

	private final PersistenceProvider persistenceProvider;

	private final Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	private final Class<? extends EntityManager> entityManagerInterface;


	@SuppressWarnings("unchecked")
	public HibernateJpaVendorAdapter() {
		ClassLoader cl = HibernateJpaVendorAdapter.class.getClassLoader();
		Class<? extends EntityManagerFactory> emfIfcToUse;
		Class<? extends EntityManager> emIfcToUse;
		Class<?> providerClass;
		PersistenceProvider providerToUse;
		try {
			try {
				// Try Hibernate 4.3/5.0's org.hibernate.jpa package in order to avoid deprecation warnings
				emfIfcToUse = (Class<? extends EntityManagerFactory>) cl.loadClass("org.hibernate.jpa.HibernateEntityManagerFactory");
				emIfcToUse = (Class<? extends EntityManager>) cl.loadClass("org.hibernate.jpa.HibernateEntityManager");
				providerClass = cl.loadClass("org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider");
			}
			catch (ClassNotFoundException ex) {
				// Fall back to Hibernate 3.6-4.2 org.hibernate.ejb package
				emfIfcToUse = (Class<? extends EntityManagerFactory>) cl.loadClass("org.hibernate.ejb.HibernateEntityManagerFactory");
				emIfcToUse = (Class<? extends EntityManager>) cl.loadClass("org.hibernate.ejb.HibernateEntityManager");
				providerClass = cl.loadClass("org.springframework.orm.jpa.vendor.SpringHibernateEjbPersistenceProvider");
			}
			providerToUse = (PersistenceProvider) providerClass.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to determine Hibernate PersistenceProvider", ex);
		}
		this.persistenceProvider = providerToUse;
		this.entityManagerFactoryInterface = emfIfcToUse;
		this.entityManagerInterface = emIfcToUse;
	}


	/**
	 * Set whether to prepare the underlying JDBC Connection of a transactional
	 * Hibernate Session, that is, whether to apply a transaction-specific
	 * isolation level and/or the transaction's read-only flag to the underlying
	 * JDBC Connection.
	 * <p>See {@link HibernateJpaDialect#setPrepareConnection(boolean)} for details.
	 * This is just a convenience flag passed through to {@code HibernateJpaDialect}.
	 * <p>On Hibernate 5.1/5.2, this flag remains {@code true} by default like against
	 * previous Hibernate versions. The vendor adapter manually enforces Hibernate's
	 * new connection handling mode {@code DELAYED_ACQUISITION_AND_HOLD} in that case
	 * unless a user-specified connection handling mode property indicates otherwise;
	 * switch this flag to {@code false} to avoid that interference.
	 * <p><b>NOTE: Per the explanation above, you may have to turn this flag off
	 * when using Hibernate in a JTA environment, e.g. on WebLogic.</b> Alternatively,
	 * set Hibernate 5.2's "hibernate.connection.handling_mode" property to
	 * "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION" or even
	 * "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT" in such a scenario.
	 * @since 4.3.1
	 * @see #getJpaPropertyMap()
	 * @see HibernateJpaDialect#beginTransaction
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.jpaDialect.setPrepareConnection(prepareConnection);
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

		if (this.jpaDialect.prepareConnection) {
			// Hibernate 5.1/5.2: manually enforce connection release mode ON_CLOSE (the former default)
			try {
				// Try Hibernate 5.2
				Environment.class.getField("CONNECTION_HANDLING");
				jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
			}
			catch (NoSuchFieldException ex) {
				// Try Hibernate 5.1
				try {
					Environment.class.getField("ACQUIRE_CONNECTIONS");
					jpaProperties.put("hibernate.connection.release_mode", "ON_CLOSE");
				}
				catch (NoSuchFieldException ex2) {
					// on Hibernate 5.0.x or lower - no need to change the default there
				}
			}
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
			case MYSQL: return MySQL5Dialect.class;
			case ORACLE: return Oracle9iDialect.class;
			case POSTGRESQL: return PostgreSQLDialect.class;  // PostgreSQLDialect deprecated in 4.x
			case SQL_SERVER: return SQLServer2008Dialect.class;
			case SYBASE: return org.hibernate.dialect.SybaseDialect.class;  // SybaseDialect deprecated in 3.6 but not 4.x
			default: return null;
		}
	}

	@Override
	public HibernateJpaDialect getJpaDialect() {
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
