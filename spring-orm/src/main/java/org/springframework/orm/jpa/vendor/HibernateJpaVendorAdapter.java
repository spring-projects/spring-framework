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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.jspecify.annotations.Nullable;

/**
 * {@link org.springframework.orm.jpa.JpaVendorAdapter} implementation for Hibernate.
 * Compatible with Hibernate ORM 7.0.
 *
 * <p>Exposes Hibernate's persistence provider and Hibernate's Session as extended
 * EntityManager interface, and adapts {@link AbstractJpaVendorAdapter}'s common
 * configuration settings. Also supports the detection of annotated packages (through
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}),
 * for example, containing Hibernate {@link org.hibernate.annotations.FilterDef} annotations,
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


	public HibernateJpaVendorAdapter() {
		this.persistenceProvider = new SpringHibernateJpaPersistenceProvider();
		this.entityManagerFactoryInterface = SessionFactory.class;  // as of Spring 5.3
		this.entityManagerInterface = Session.class;  // as of Spring 5.3
	}


	/**
	 * Set whether to prepare the underlying JDBC Connection of a transactional
	 * Hibernate Session, that is, whether to apply a transaction-specific
	 * isolation level and/or the transaction's read-only flag to the underlying
	 * JDBC Connection.
	 * <p>See {@link HibernateJpaDialect#setPrepareConnection(boolean)} for details.
	 * This is just a convenience flag passed through to {@code HibernateJpaDialect}.
	 * <p>On Hibernate 5.1+, this flag remains {@code true} by default like against
	 * previous Hibernate versions. The vendor adapter manually enforces Hibernate's
	 * new connection handling mode {@code DELAYED_ACQUISITION_AND_HOLD} in that case
	 * unless a user-specified connection handling mode property indicates otherwise;
	 * switch this flag to {@code false} to avoid that interference.
	 * <p><b>NOTE: For a persistence unit with transaction type JTA, for example, on WebLogic,
	 * the connection release mode will never be altered from its provider default,
	 * i.e. not be forced to {@code DELAYED_ACQUISITION_AND_HOLD} by this flag.</b>
	 * Alternatively, set Hibernate's "hibernate.connection.handling_mode"
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

	@SuppressWarnings("removal")
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
			else {
				String databaseDialectName = determineDatabaseDialectName(getDatabase());
				if (databaseDialectName != null) {
					jpaProperties.put(AvailableSettings.DIALECT, databaseDialectName);
				}
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		}
		if (isShowSql()) {
			jpaProperties.put(AvailableSettings.SHOW_SQL, "true");
		}

		if (connectionReleaseOnClose) {
			jpaProperties.put(AvailableSettings.CONNECTION_HANDLING,
					PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD);
		}

		// For SpringBeanContainer to be called on Hibernate 6.2
		jpaProperties.put("hibernate.cdi.extensions", "true");

		return jpaProperties;
	}

	/**
	 * Determine the Hibernate database dialect class for the given target database.
	 * <p>The default implementation covers the common built-in dialects.
	 * @param database the target database
	 * @return the Hibernate database dialect class, or {@code null} if none found
	 * @see #determineDatabaseDialectName
	 */
	protected @Nullable Class<?> determineDatabaseDialectClass(Database database) {
		return switch (database) {
			case DB2 -> DB2Dialect.class;
			case H2 -> H2Dialect.class;
			case HANA -> HANADialect.class;
			case HSQL -> HSQLDialect.class;
			case MYSQL -> MySQLDialect.class;
			case ORACLE -> OracleDialect.class;
			case POSTGRESQL -> PostgreSQLDialect.class;
			case SQL_SERVER -> SQLServerDialect.class;
			case SYBASE -> SybaseDialect.class;
			default -> null;
		};
	}

	/**
	 * Determine the Hibernate database dialect class name for the given target database.
	 * <p>The default implementation covers the common community dialect for Derby.
	 * @param database the target database
	 * @return the Hibernate database dialect class name, or {@code null} if none found
	 * @since 7.0
	 * @see #determineDatabaseDialectClass
	 */
	protected @Nullable String determineDatabaseDialectName(Database database) {
		return switch (database) {
			case DERBY -> "org.hibernate.community.dialect.DerbyDialect";
			default -> null;
		};
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
