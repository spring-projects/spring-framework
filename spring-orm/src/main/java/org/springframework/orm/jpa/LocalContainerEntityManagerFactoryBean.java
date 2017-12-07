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

package org.springframework.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.lookup.SingleDataSourceLookup;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a JPA
 * {@link javax.persistence.EntityManagerFactory} according to JPA's standard
 * <i>container</i> bootstrap contract. This is the most powerful way to set
 * up a shared JPA EntityManagerFactory in a Spring application context;
 * the EntityManagerFactory can then be passed to JPA-based DAOs via
 * dependency injection. Note that switching to a JNDI lookup or to a
 * {@link LocalEntityManagerFactoryBean} definition is just a matter of
 * configuration!
 *
 * <p>As with {@link LocalEntityManagerFactoryBean}, configuration settings
 * are usually read in from a {@code META-INF/persistence.xml} config file,
 * residing in the class path, according to the general JPA configuration contract.
 * However, this FactoryBean is more flexible in that you can override the location
 * of the {@code persistence.xml} file, specify the JDBC DataSources to link to,
 * etc. Furthermore, it allows for pluggable class instrumentation through Spring's
 * {@link org.springframework.instrument.classloading.LoadTimeWeaver} abstraction,
 * instead of being tied to a special VM agent specified on JVM startup.
 *
 * <p>Internally, this FactoryBean parses the {@code persistence.xml} file
 * itself and creates a corresponding {@link javax.persistence.spi.PersistenceUnitInfo}
 * object (with further configuration merged in, such as JDBC DataSources and the
 * Spring LoadTimeWeaver), to be passed to the chosen JPA
 * {@link javax.persistence.spi.PersistenceProvider}. This corresponds to a
 * local JPA container with full support for the standard JPA container contract.
 *
 * <p>The exposed EntityManagerFactory object will implement all the interfaces of
 * the underlying native EntityManagerFactory returned by the PersistenceProvider,
 * plus the {@link EntityManagerFactoryInfo} interface which exposes additional
 * metadata as assembled by this FactoryBean.
 *
 * <p><b>NOTE: Spring's JPA support requires JPA 2.1 or higher, as of Spring 5.0.</b>
 * JPA 1.0/2.0 based applications are still supported; however, a JPA 2.1 compliant
 * persistence provider is needed at runtime.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see #setPersistenceXmlLocation
 * @see #setJpaProperties
 * @see #setJpaVendorAdapter
 * @see #setLoadTimeWeaver
 * @see #setDataSource
 * @see EntityManagerFactoryInfo
 * @see LocalEntityManagerFactoryBean
 * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory
 */
@SuppressWarnings("serial")
public class LocalContainerEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean
		implements ResourceLoaderAware, LoadTimeWeaverAware {

	@Nullable
	private PersistenceUnitManager persistenceUnitManager;

	private final DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

	@Nullable
	private PersistenceUnitInfo persistenceUnitInfo;


	/**
	 * Set the PersistenceUnitManager to use for obtaining the JPA persistence unit
	 * that this FactoryBean is supposed to build an EntityManagerFactory for.
	 * <p>The default is to rely on the local settings specified on this FactoryBean,
	 * such as "persistenceXmlLocation", "dataSource" and "loadTimeWeaver".
	 * <p>For reuse of existing persistence unit configuration or more advanced forms
	 * of custom persistence unit handling, consider defining a separate
	 * PersistenceUnitManager bean (typically a DefaultPersistenceUnitManager instance)
	 * and linking it in here. {@code persistence.xml} location, DataSource
	 * configuration and LoadTimeWeaver will be defined on that separate
	 * DefaultPersistenceUnitManager bean in such a scenario.
	 * @see #setPersistenceXmlLocation
	 * @see #setDataSource
	 * @see #setLoadTimeWeaver
	 * @see org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager
	 */
	public void setPersistenceUnitManager(PersistenceUnitManager persistenceUnitManager) {
		this.persistenceUnitManager = persistenceUnitManager;
	}

	/**
	 * Set the location of the {@code persistence.xml} file
	 * we want to use. This is a Spring resource location.
	 * <p>Default is "classpath:META-INF/persistence.xml".
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @param persistenceXmlLocation a Spring resource String
	 * identifying the location of the {@code persistence.xml} file
	 * that this LocalContainerEntityManagerFactoryBean should parse
	 * @see #setPersistenceUnitManager
	 */
	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
		this.internalPersistenceUnitManager.setPersistenceXmlLocation(persistenceXmlLocation);
	}

	/**
	 * Uses the specified persistence unit name as the name of the default
	 * persistence unit, if applicable.
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @see DefaultPersistenceUnitManager#setDefaultPersistenceUnitName
	 */
	@Override
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		super.setPersistenceUnitName(persistenceUnitName);
		if (persistenceUnitName != null) {
			this.internalPersistenceUnitManager.setDefaultPersistenceUnitName(persistenceUnitName);
		}
	}

	/**
	 * Set a persistence unit root location for the default persistence unit.
	 * <p>Default is "classpath:", that is, the root of the current classpath
	 * (nearest root directory). To be overridden if unit-specific resolution
	 * does not work and the classpath root is not appropriate either.
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @since 4.3.3
	 * @see DefaultPersistenceUnitManager#setDefaultPersistenceUnitRootLocation
	 */
	public void setPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
		this.internalPersistenceUnitManager.setDefaultPersistenceUnitRootLocation(defaultPersistenceUnitRootLocation);
	}

	/**
	 * Set whether to use Spring-based scanning for entity classes in the classpath
	 * instead of using JPA's standard scanning of jar files with {@code persistence.xml}
	 * markers in them. In case of Spring-based scanning, no {@code persistence.xml}
	 * is necessary; all you need to do is to specify base packages to search here.
	 * <p>Default is none. Specify packages to search for autodetection of your entity
	 * classes in the classpath. This is analogous to Spring's component-scan feature
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 * <p><p>Note: There may be limitations in comparison to regular JPA scanning.</b>
	 * In particular, JPA providers may pick up annotated packages for provider-specific
	 * annotations only when driven by {@code persistence.xml}. As of 4.1, Spring's
	 * scan can detect annotated packages as well if supported by the given
	 * {@link JpaVendorAdapter} (e.g. for Hibernate).
	 * <p>If no explicit {@link #setMappingResources mapping resources} have been
	 * specified in addition to these packages, Spring's setup looks for a default
	 * {@code META-INF/orm.xml} file in the classpath, registering it as a mapping
	 * resource for the default unit if the mapping file is not co-located with a
	 * {@code persistence.xml} file (in which case we assume it is only meant to be
	 * used with the persistence units defined there, like in standard JPA).
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @param packagesToScan one or more base packages to search, analogous to
	 * Spring's component-scan configuration for regular Spring components
	 * @see #setPersistenceUnitManager
	 * @see DefaultPersistenceUnitManager#setPackagesToScan
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.internalPersistenceUnitManager.setPackagesToScan(packagesToScan);
	}

	/**
	 * Specify one or more mapping resources (equivalent to {@code <mapping-file>}
	 * entries in {@code persistence.xml}) for the default persistence unit.
	 * Can be used on its own or in combination with entity scanning in the classpath,
	 * in both cases avoiding {@code persistence.xml}.
	 * <p>Note that mapping resources must be relative to the classpath root,
	 * e.g. "META-INF/mappings.xml" or "com/mycompany/repository/mappings.xml",
	 * so that they can be loaded through {@code ClassLoader.getResource}.
	 * <p>If no explicit mapping resources have been specified next to
	 * {@link #setPackagesToScan packages to scan}, Spring's setup looks for a default
	 * {@code META-INF/orm.xml} file in the classpath, registering it as a mapping
	 * resource for the default unit if the mapping file is not co-located with a
	 * {@code persistence.xml} file (in which case we assume it is only meant to be
	 * used with the persistence units defined there, like in standard JPA).
	 * <p>Note that specifying an empty array/list here suppresses the default
	 * {@code META-INF/orm.xml} check. On the other hand, explicitly specifying
	 * {@code META-INF/orm.xml} here will register that file even if it happens
	 * to be co-located with a {@code persistence.xml} file.
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @see #setPersistenceUnitManager
	 * @see DefaultPersistenceUnitManager#setMappingResources
	 */
	public void setMappingResources(String... mappingResources) {
		this.internalPersistenceUnitManager.setMappingResources(mappingResources);
	}

	/**
	 * Specify the JPA 2.0 shared cache mode for this persistence unit,
	 * overriding a value in {@code persistence.xml} if set.
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @since 4.0
	 * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
	 * @see #setPersistenceUnitManager
	 */
	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.internalPersistenceUnitManager.setSharedCacheMode(sharedCacheMode);
	}

	/**
	 * Specify the JPA 2.0 validation mode for this persistence unit,
	 * overriding a value in {@code persistence.xml} if set.
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @since 4.0
	 * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
	 * @see #setPersistenceUnitManager
	 */
	public void setValidationMode(ValidationMode validationMode) {
		this.internalPersistenceUnitManager.setValidationMode(validationMode);
	}

	/**
	 * Specify the JDBC DataSource that the JPA persistence provider is supposed
	 * to use for accessing the database. This is an alternative to keeping the
	 * JDBC configuration in {@code persistence.xml}, passing in a Spring-managed
	 * DataSource instead.
	 * <p>In JPA speak, a DataSource passed in here will be used as "nonJtaDataSource"
	 * on the PersistenceUnitInfo passed to the PersistenceProvider, as well as
	 * overriding data source configuration in {@code persistence.xml} (if any).
	 * Note that this variant typically works for JTA transaction management as well;
	 * if it does not, consider using the explicit {@link #setJtaDataSource} instead.
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
	 * @see #setPersistenceUnitManager
	 */
	public void setDataSource(DataSource dataSource) {
		this.internalPersistenceUnitManager.setDataSourceLookup(new SingleDataSourceLookup(dataSource));
		this.internalPersistenceUnitManager.setDefaultDataSource(dataSource);
	}

	/**
	 * Specify the JDBC DataSource that the JPA persistence provider is supposed
	 * to use for accessing the database. This is an alternative to keeping the
	 * JDBC configuration in {@code persistence.xml}, passing in a Spring-managed
	 * DataSource instead.
	 * <p>In JPA speak, a DataSource passed in here will be used as "jtaDataSource"
	 * on the PersistenceUnitInfo passed to the PersistenceProvider, as well as
	 * overriding data source configuration in {@code persistence.xml} (if any).
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
	 * @see #setPersistenceUnitManager
	 */
	public void setJtaDataSource(DataSource jtaDataSource) {
		this.internalPersistenceUnitManager.setDataSourceLookup(new SingleDataSourceLookup(jtaDataSource));
		this.internalPersistenceUnitManager.setDefaultJtaDataSource(jtaDataSource);
	}

	/**
	 * Set the PersistenceUnitPostProcessors to be applied to the
	 * PersistenceUnitInfo used for creating this EntityManagerFactory.
	 * <p>Such post-processors can, for example, register further entity
	 * classes and jar files, in addition to the metadata read from
	 * {@code persistence.xml}.
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * @see #setPersistenceUnitManager
	 */
	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... postProcessors) {
		this.internalPersistenceUnitManager.setPersistenceUnitPostProcessors(postProcessors);
	}

	/**
	 * Specify the Spring LoadTimeWeaver to use for class instrumentation according
	 * to the JPA class transformer contract.
	 * <p>It is a not required to specify a LoadTimeWeaver: Most providers will be
	 * able to provide a subset of their functionality without class instrumentation
	 * as well, or operate with their VM agent specified on JVM startup.
	 * <p>In terms of Spring-provided weaving options, the most important ones are
	 * InstrumentationLoadTimeWeaver, which requires a Spring-specific (but very general)
	 * VM agent specified on JVM startup, and ReflectiveLoadTimeWeaver, which interacts
	 * with an underlying ClassLoader based on specific extended methods being available
	 * on it.
	 * <p><b>NOTE:</b> As of Spring 2.5, the context's default LoadTimeWeaver (defined
	 * as bean with name "loadTimeWeaver") will be picked up automatically, if available,
	 * removing the need for LoadTimeWeaver configuration on each affected target bean.</b>
	 * Consider using the {@code context:load-time-weaver} XML tag for creating
	 * such a shared LoadTimeWeaver (autodetecting the environment by default).
	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
	 * Otherwise, the external {@link #setPersistenceUnitManager PersistenceUnitManager}
	 * is responsible for the weaving configuration.
	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
	 */
	@Override
	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		this.internalPersistenceUnitManager.setLoadTimeWeaver(loadTimeWeaver);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.internalPersistenceUnitManager.setResourceLoader(resourceLoader);
	}


	@Override
	public void afterPropertiesSet() throws PersistenceException {
		PersistenceUnitManager managerToUse = this.persistenceUnitManager;
		if (this.persistenceUnitManager == null) {
			this.internalPersistenceUnitManager.afterPropertiesSet();
			managerToUse = this.internalPersistenceUnitManager;
		}

		this.persistenceUnitInfo = determinePersistenceUnitInfo(managerToUse);
		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null && this.persistenceUnitInfo instanceof SmartPersistenceUnitInfo) {
			String rootPackage = jpaVendorAdapter.getPersistenceProviderRootPackage();
			if (rootPackage != null) {
				((SmartPersistenceUnitInfo) this.persistenceUnitInfo).setPersistenceProviderPackageName(rootPackage);
			}
		}

		super.afterPropertiesSet();
	}

	@Override
	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
		Assert.state(this.persistenceUnitInfo != null, "PersistenceUnitInfo not initialized");

		PersistenceProvider provider = getPersistenceProvider();
		if (provider == null) {
			String providerClassName = this.persistenceUnitInfo.getPersistenceProviderClassName();
			if (providerClassName == null) {
				throw new IllegalArgumentException(
						"No PersistenceProvider specified in EntityManagerFactory configuration, " +
						"and chosen PersistenceUnitInfo does not specify a provider class name either");
			}
			Class<?> providerClass = ClassUtils.resolveClassName(providerClassName, getBeanClassLoader());
			provider = (PersistenceProvider) BeanUtils.instantiateClass(providerClass);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Building JPA container EntityManagerFactory for persistence unit '" +
					this.persistenceUnitInfo.getPersistenceUnitName() + "'");
		}
		EntityManagerFactory emf =
				provider.createContainerEntityManagerFactory(this.persistenceUnitInfo, getJpaPropertyMap());
		postProcessEntityManagerFactory(emf, this.persistenceUnitInfo);

		return emf;
	}


	/**
	 * Determine the PersistenceUnitInfo to use for the EntityManagerFactory
	 * created by this bean.
	 * <p>The default implementation reads in all persistence unit infos from
	 * {@code persistence.xml}, as defined in the JPA specification.
	 * If no entity manager name was specified, it takes the first info in the
	 * array as returned by the reader. Otherwise, it checks for a matching name.
	 * @param persistenceUnitManager the PersistenceUnitManager to obtain from
	 * @return the chosen PersistenceUnitInfo
	 */
	protected PersistenceUnitInfo determinePersistenceUnitInfo(PersistenceUnitManager persistenceUnitManager) {
		if (getPersistenceUnitName() != null) {
			return persistenceUnitManager.obtainPersistenceUnitInfo(getPersistenceUnitName());
		}
		else {
			return persistenceUnitManager.obtainDefaultPersistenceUnitInfo();
		}
	}

	/**
	 * Hook method allowing subclasses to customize the EntityManagerFactory
	 * after its creation via the PersistenceProvider.
	 * <p>The default implementation is empty.
	 * @param emf the newly created EntityManagerFactory we are working with
	 * @param pui the PersistenceUnitInfo used to configure the EntityManagerFactory
	 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory
	 */
	protected void postProcessEntityManagerFactory(EntityManagerFactory emf, PersistenceUnitInfo pui) {
	}


	@Override
	@Nullable
	public PersistenceUnitInfo getPersistenceUnitInfo() {
		return this.persistenceUnitInfo;
	}

	@Override
	@Nullable
	public String getPersistenceUnitName() {
		if (this.persistenceUnitInfo != null) {
			return this.persistenceUnitInfo.getPersistenceUnitName();
		}
		return super.getPersistenceUnitName();
	}

	@Override
	public DataSource getDataSource() {
		if (this.persistenceUnitInfo != null) {
			return (this.persistenceUnitInfo.getJtaDataSource() != null ?
					this.persistenceUnitInfo.getJtaDataSource() :
					this.persistenceUnitInfo.getNonJtaDataSource());
		}
		return (this.internalPersistenceUnitManager.getDefaultJtaDataSource() != null ?
				this.internalPersistenceUnitManager.getDefaultJtaDataSource() :
				this.internalPersistenceUnitManager.getDefaultDataSource());
	}

}
