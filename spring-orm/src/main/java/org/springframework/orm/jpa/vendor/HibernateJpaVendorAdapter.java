/*
 * Copyright 2002-2016 the original author or authors.
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
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.SQLServerDialect;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for Hibernate
 * EntityManager. Developed and tested against Hibernate 5.0, 5.1 and 5.2;
 * backwards-compatible with Hibernate 4.3 as well.
 *
 * <p>Exposes Hibernate's persistence provider and EntityManager extension interface,
 * and adapts {@link AbstractJpaVendorAdapter}'s common configuration settings.
 * Also supports the detection of annotated packages (through
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}),
 * e.g. containing Hibernate {@link org.hibernate.annotations.FilterDef} annotations,
 * along with Spring-driven entity scanning which requires no {@code persistence.xml}
 * ({@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPackagesToScan}).
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see HibernateJpaDialect
 */
public class HibernateJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final HibernateJpaDialect jpaDialect = new HibernateJpaDialect();

	private final PersistenceProvider persistenceProvider;

	private final Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	private final Class<? extends EntityManager> entityManagerInterface;


	@SuppressWarnings("deprecation")
	public HibernateJpaVendorAdapter() {
		this.persistenceProvider = new SpringHibernateJpaPersistenceProvider();
		this.entityManagerFactoryInterface = org.hibernate.jpa.HibernateEntityManagerFactory.class;
		this.entityManagerInterface = org.hibernate.jpa.HibernateEntityManager.class;
	}


	/**
	 * Set whether to prepare the underlying JDBC Connection of a transactional
	 * Hibernate Session, that is, whether to apply a transaction-specific
	 * isolation level and/or the transaction's read-only flag to the underlying
	 * JDBC Connection.
	 * <p>See {@link HibernateJpaDialect#setPrepareConnection(boolean)} for details.
	 * This is just a convenience flag passed through to {@code HibernateJpaDialect}.
	 * <p>On Hibernate 5.2, this flag remains {@code true} by default like against
	 * previous Hibernate versions. The vendor adapter manually enforces Hibernate's
	 * new connection handling mode {@code DELAYED_ACQUISITION_AND_HOLD} in that case
	 * unless a user-specified connection handling mode property indicates otherwise;
	 * switch this flag to {@code false} to avoid that interference.
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
		Map<String, Object> jpaProperties = new HashMap<>();

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
			// Hibernate 5.2: manually enforce connection release mode ON_CLOSE (the former default)
			jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
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
			case DERBY: return org.hibernate.dialect.DerbyDialect.class;
			case H2: return H2Dialect.class;
			case HSQL: return HSQLDialect.class;
			case INFORMIX: return InformixDialect.class;
			case MYSQL: return MySQLDialect.class;
			case ORACLE: return Oracle9iDialect.class;
			case POSTGRESQL: return org.hibernate.dialect.PostgreSQLDialect.class;
			case SQL_SERVER: return SQLServerDialect.class;
			case SYBASE: return org.hibernate.dialect.SybaseDialect.class;
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
