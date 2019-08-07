/*
 * Copyright 2002-2018 the original author or authors.
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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.SybaseDialect;

import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for Hibernate
 * EntityManager. Developed and tested against Hibernate 5.0, 5.1 and 5.2;
 * backwards-compatible with Hibernate 4.3 at runtime on a best-effort basis.
 *
 * <p>Exposes Hibernate's persistence provider and EntityManager extension interface,
 * and adapts {@link AbstractJpaVendorAdapter}'s common configuration settings.
 * Also supports the detection of annotated packages (through
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}),
 * e.g. containing Hibernate {@link org.hibernate.annotations.FilterDef} annotations,
 * along with Spring-driven entity scanning which requires no {@code persistence.xml}
 * ({@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPackagesToScan}).
 *
 * <p><b>A note about {@code HibernateJpaVendorAdapter} vs native Hibernate settings:</b>
 * Some settings on this adapter may conflict with native Hibernate configuration rules
 * or custom Hibernate properties. For example, specify either {@link #setDatabase} or
 * Hibernate's "hibernate.dialect_resolvers" property, not both. Also, be careful about
 * Hibernate's connection release mode: This adapter prefers {@code ON_CLOSE} behavior,
 * aligned with {@link HibernateJpaDialect#setPrepareConnection}, at least for non-JTA
 * scenarios; you may override this through corresponding native Hibernate properties.
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
	 * <p>On Hibernate 5.1/5.2, this flag remains {@code true} by default like against
	 * previous Hibernate versions. The vendor adapter manually enforces Hibernate's
	 * new connection handling mode {@code DELAYED_ACQUISITION_AND_HOLD} in that case
	 * unless a user-specified connection handling mode property indicates otherwise;
	 * switch this flag to {@code false} to avoid that interference.
	 * <p><b>NOTE: For a persistence unit with transaction type JTA e.g. on WebLogic,
	 * the connection release mode will never be altered from its provider default,
	 * i.e. not be forced to {@code DELAYED_ACQUISITION_AND_HOLD} by this flag.</b>
	 * Alternatively, set Hibernate 5.2's "hibernate.connection.handling_mode"
	 * property to "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION" or even
	 * "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT" in such a scenario.
	 * @since 4.3.1
	 * @see PersistenceUnitInfo#getTransactionType()
	 * @see #getJpaPropertyMap(PersistenceUnitInfo)
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
	public Map<String, Object> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return buildJpaPropertyMap(this.jpaDialect.prepareConnection &&
				pui.getTransactionType() != PersistenceUnitTransactionType.JTA);
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		return buildJpaPropertyMap(this.jpaDialect.prepareConnection);
	}

	private Map<String, Object> buildJpaPropertyMap(boolean connectionReleaseOnClose) {
		Map<String, Object> jpaProperties = new HashMap<>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(AvailableSettings.DIALECT, getDatabasePlatform());
		}
		else {
			Class<?> databaseDialectClass = determineDatabaseDialectClass(getDatabase());
			if (databaseDialectClass != null) {
				jpaProperties.put(AvailableSettings.DIALECT, databaseDialectClass.getName());
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		}
		if (isShowSql()) {
			jpaProperties.put(AvailableSettings.SHOW_SQL, "true");
		}

		if (connectionReleaseOnClose) {
			// Hibernate 5.1/5.2: manually enforce connection release mode ON_CLOSE (the former default)
			try {
				// Try Hibernate 5.2
				AvailableSettings.class.getField("CONNECTION_HANDLING");
				jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
			}
			catch (NoSuchFieldException ex) {
				// Try Hibernate 5.1
				try {
					AvailableSettings.class.getField("ACQUIRE_CONNECTIONS");
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
	@Nullable
	protected Class<?> determineDatabaseDialectClass(Database database) {
		switch (database) {
			case DB2: return DB2Dialect.class;
			case DERBY: return DerbyTenSevenDialect.class;
			case H2: return H2Dialect.class;
			case HSQL: return HSQLDialect.class;
			case INFORMIX: return InformixDialect.class;
			case MYSQL: return MySQL5Dialect.class;
			case ORACLE: return Oracle12cDialect.class;
			case POSTGRESQL: return PostgreSQL95Dialect.class;
			case SQL_SERVER: return SQLServer2012Dialect.class;
			case SYBASE: return SybaseDialect.class;
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
