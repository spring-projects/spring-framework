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

package org.springframework.orm.jpa.persistenceunit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * Default implementation of the {@link PersistenceUnitManager} interface.
 * Used as internal default by
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}.
 *
 * <p>Supports standard JPA scanning for {@code persistence.xml} files,
 * with configurable file locations, JDBC DataSource lookup and load-time weaving.
 *
 * <p>The default XML file location is {@code classpath*:META-INF/persistence.xml},
 * scanning for all matching files in the class path (as defined in the JPA specification).
 * DataSource names are by default interpreted as JNDI names, and no load time weaving
 * is available (which requires weaving to be turned off in the persistence provider).
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setPersistenceXmlLocations
 * @see #setDataSourceLookup
 * @see #setLoadTimeWeaver
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitManager
 */
public class DefaultPersistenceUnitManager
		implements PersistenceUnitManager, ResourceLoaderAware, LoadTimeWeaverAware, InitializingBean {

	/**
	 * Default location of the {@code persistence.xml} file:
	 * "classpath*:META-INF/persistence.xml".
	 */
	public final static String DEFAULT_PERSISTENCE_XML_LOCATION = "classpath*:META-INF/persistence.xml";

	/**
	 * Default location for the persistence unit root URL:
	 * "classpath:", indicating the root of the class path.
	 */
	public final static String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION = "classpath:";

	public final static String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME = "default";

	private static final String ENTITY_CLASS_RESOURCE_PATTERN = "/**/*.class";


	private static final Set<TypeFilter> entityTypeFilters;

	static {
		entityTypeFilters = new LinkedHashSet<TypeFilter>(4);
		entityTypeFilters.add(new AnnotationTypeFilter(Entity.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(Embeddable.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(MappedSuperclass.class, false));
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> converterAnnotation = (Class<? extends Annotation>)
					DefaultPersistenceUnitManager.class.getClassLoader().loadClass("javax.persistence.Converter");
			entityTypeFilters.add(new AnnotationTypeFilter(converterAnnotation, false));
		}
		catch (ClassNotFoundException ex) {
			// JPA 2.1 API not available
		}
	}


	private String[] persistenceXmlLocations = new String[] {DEFAULT_PERSISTENCE_XML_LOCATION};

	private String defaultPersistenceUnitRootLocation = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION;

	private String defaultPersistenceUnitName = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME;

	private String[] packagesToScan;

	private String[] mappingResources;

	private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	private DataSource defaultDataSource;

	private DataSource defaultJtaDataSource;

	private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;

	private LoadTimeWeaver loadTimeWeaver;

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private final Set<String> persistenceUnitInfoNames = new HashSet<String>();

	private final Map<String, PersistenceUnitInfo> persistenceUnitInfos = new HashMap<String, PersistenceUnitInfo>();


	/**
	 * Specify the location of the {@code persistence.xml} files to load.
	 * These can be specified as Spring resource locations and/or location patterns.
	 * <p>Default is "classpath*:META-INF/persistence.xml".
	 */
	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
		this.persistenceXmlLocations = new String[] {persistenceXmlLocation};
	}

	/**
	 * Specify multiple locations of {@code persistence.xml} files to load.
	 * These can be specified as Spring resource locations and/or location patterns.
	 * <p>Default is "classpath*:META-INF/persistence.xml".
	 * @param persistenceXmlLocations an array of Spring resource Strings
	 * identifying the location of the {@code persistence.xml} files to read
	 */
	public void setPersistenceXmlLocations(String... persistenceXmlLocations) {
		this.persistenceXmlLocations = persistenceXmlLocations;
	}

	/**
	 * Set the default persistence unit root location, to be applied
	 * if no unit-specific persistence unit root could be determined.
	 * <p>Default is "classpath:", that is, the root of the current class path
	 * (nearest root directory). To be overridden if unit-specific resolution
	 * does not work and the class path root is not appropriate either.
	 */
	public void setDefaultPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
		this.defaultPersistenceUnitRootLocation = defaultPersistenceUnitRootLocation;
	}

	/**
	 * Specify the name of the default persistence unit, if any. Default is "default".
	 * <p>Primarily applied to a scanned persistence unit without {@code persistence.xml}.
	 * Also applicable to selecting a default unit from several persistence units available.
	 * @see #setPackagesToScan
	 * @see #obtainDefaultPersistenceUnitInfo
	 */
	public void setDefaultPersistenceUnitName(String defaultPersistenceUnitName) {
		this.defaultPersistenceUnitName = defaultPersistenceUnitName;
	}

	/**
	 * Set whether to use Spring-based scanning for entity classes in the classpath
	 * instead of using JPA's standard scanning of jar files with {@code persistence.xml}
	 * markers in them. In case of Spring-based scanning, no {@code persistence.xml}
	 * is necessary; all you need to do is to specify base packages to search here.
	 * <p>Default is none. Specify packages to search for autodetection of your entity
	 * classes in the classpath. This is analogous to Spring's component-scan feature
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * Specify one or more mapping resources (equivalent to {@code &lt;mapping-file&gt;}
	 * entries in {@code persistence.xml}) for the default persistence unit.
	 * Can be used on its own or in combination with entity scanning in the classpath,
	 * in both cases avoiding {@code persistence.xml}.
	 * <p>Note that mapping resources must be relative to the classpath root,
	 * e.g. "META-INF/mappings.xml" or "com/mycompany/repository/mappings.xml",
	 * so that they can be loaded through {@code ClassLoader.getResource}.
	 * @see #setPackagesToScan
	 */
	public void setMappingResources(String... mappingResources) {
		this.mappingResources = mappingResources;
	}

	/**
	 * Specify the JDBC DataSources that the JPA persistence provider is supposed
	 * to use for accessing the database, resolving data source names in
	 * {@code persistence.xml} against Spring-managed DataSources.
	 * <p>The specified Map needs to define data source names for specific DataSource
	 * objects, matching the data source names used in {@code persistence.xml}.
	 * If not specified, data source names will be resolved as JNDI names instead
	 * (as defined by standard JPA).
	 * @see org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
	 */
	public void setDataSources(Map<String, DataSource> dataSources) {
		this.dataSourceLookup = new MapDataSourceLookup(dataSources);
	}

	/**
	 * Specify the JDBC DataSourceLookup that provides DataSources for the
	 * persistence provider, resolving data source names in {@code persistence.xml}
	 * against Spring-managed DataSource instances.
	 * <p>Default is JndiDataSourceLookup, which resolves DataSource names as
	 * JNDI names (as defined by standard JPA). Specify a BeanFactoryDataSourceLookup
	 * instance if you want DataSource names to be resolved against Spring bean names.
	 * <p>Alternatively, consider passing in a map from names to DataSource instances
	 * via the "dataSources" property. If the {@code persistence.xml} file
	 * does not define DataSource names at all, specify a default DataSource
	 * via the "defaultDataSource" property.
	 * @see org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup
	 * @see org.springframework.jdbc.datasource.lookup.BeanFactoryDataSourceLookup
	 * @see #setDataSources
	 * @see #setDefaultDataSource
	 */
	public void setDataSourceLookup(DataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
	}

	/**
	 * Return the JDBC DataSourceLookup that provides DataSources for the
	 * persistence provider, resolving data source names in {@code persistence.xml}
	 * against Spring-managed DataSource instances.
	 */
	public DataSourceLookup getDataSourceLookup() {
		return this.dataSourceLookup;
	}

	/**
	 * Specify the JDBC DataSource that the JPA persistence provider is supposed to use
	 * for accessing the database if none has been specified in {@code persistence.xml}.
	 * This variant indicates no special transaction setup, i.e. typical resource-local.
	 * <p>In JPA speak, a DataSource passed in here will be uses as "nonJtaDataSource"
	 * on the PersistenceUnitInfo passed to the PersistenceProvider, provided that
	 * none has been registered before.
	 * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
	 */
	public void setDefaultDataSource(DataSource defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	/**
	 * Return the JDBC DataSource that the JPA persistence provider is supposed to use
	 * for accessing the database if none has been specified in {@code persistence.xml}.
	 */
	public DataSource getDefaultDataSource() {
		return this.defaultDataSource;
	}

	/**
	 * Specify the JDBC DataSource that the JPA persistence provider is supposed to use
	 * for accessing the database if none has been specified in {@code persistence.xml}.
	 * This variant indicates that JTA is supposed to be used as transaction type.
	 * <p>In JPA speak, a DataSource passed in here will be uses as "jtaDataSource"
	 * on the PersistenceUnitInfo passed to the PersistenceProvider, provided that
	 * none has been registered before.
	 * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
	 */
	public void setDefaultJtaDataSource(DataSource defaultJtaDataSource) {
		this.defaultJtaDataSource = defaultJtaDataSource;
	}

	/**
	 * Return the JTA-aware DataSource that the JPA persistence provider is supposed to use
	 * for accessing the database if none has been specified in {@code persistence.xml}.
	 */
	public DataSource getDefaultJtaDataSource() {
		return this.defaultJtaDataSource;
	}

	/**
	 * Set the PersistenceUnitPostProcessors to be applied to each
	 * PersistenceUnitInfo that has been parsed by this manager.
	 * <p>Such post-processors can, for example, register further entity classes and
	 * jar files, in addition to the metadata read from {@code persistence.xml}.
	 */
	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... postProcessors) {
		this.persistenceUnitPostProcessors = postProcessors;
	}

	/**
	 * Return the PersistenceUnitPostProcessors to be applied to each
	 * PersistenceUnitInfo that has been parsed by this manager.
	 */
	public PersistenceUnitPostProcessor[] getPersistenceUnitPostProcessors() {
		return this.persistenceUnitPostProcessors;
	}

	/**
	 * Specify the Spring LoadTimeWeaver to use for class instrumentation according
	 * to the JPA class transformer contract.
	 * <p>It is not required to specify a LoadTimeWeaver: Most providers will be
	 * able to provide a subset of their functionality without class instrumentation
	 * as well, or operate with their VM agent specified on JVM startup.
	 * <p>In terms of Spring-provided weaving options, the most important ones are
	 * InstrumentationLoadTimeWeaver, which requires a Spring-specific (but very general)
	 * VM agent specified on JVM startup, and ReflectiveLoadTimeWeaver, which interacts
	 * with an underlying ClassLoader based on specific extended methods being available
	 * on it (for example, interacting with Spring's TomcatInstrumentableClassLoader).
	 * <p><b>NOTE:</b> As of Spring 2.5, the context's default LoadTimeWeaver (defined
	 * as bean with name "loadTimeWeaver") will be picked up automatically, if available,
	 * removing the need for LoadTimeWeaver configuration on each affected target bean.</b>
	 * Consider using the {@code context:load-time-weaver} XML tag for creating
	 * such a shared LoadTimeWeaver (autodetecting the environment by default).
	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
	 * @see org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader
	 */
	@Override
	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	/**
	 * Return the Spring LoadTimeWeaver to use for class instrumentation according
	 * to the JPA class transformer contract.
	 */
	public LoadTimeWeaver getLoadTimeWeaver() {
		return this.loadTimeWeaver;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	@Override
	public void afterPropertiesSet() {
		if (this.loadTimeWeaver == null && InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(this.resourcePatternResolver.getClassLoader());
		}
		preparePersistenceUnitInfos();
	}

	/**
	 * Prepare the PersistenceUnitInfos according to the configuration
	 * of this manager: scanning for {@code persistence.xml} files,
	 * parsing all matching files, configuring and post-processing them.
	 * <p>PersistenceUnitInfos cannot be obtained before this preparation
	 * method has been invoked.
	 * @see #obtainDefaultPersistenceUnitInfo()
	 * @see #obtainPersistenceUnitInfo(String)
	 */
	public void preparePersistenceUnitInfos() {
		this.persistenceUnitInfoNames.clear();
		this.persistenceUnitInfos.clear();
		List<SpringPersistenceUnitInfo> puis = readPersistenceUnitInfos();
		for (SpringPersistenceUnitInfo pui : puis) {
			if (pui.getPersistenceUnitRootUrl() == null) {
				pui.setPersistenceUnitRootUrl(determineDefaultPersistenceUnitRootUrl());
			}
			if (pui.getJtaDataSource() == null) {
				pui.setJtaDataSource(this.defaultJtaDataSource);
			}
			if (pui.getNonJtaDataSource() == null) {
				pui.setNonJtaDataSource(this.defaultDataSource);
			}
			if (this.loadTimeWeaver != null) {
				pui.init(this.loadTimeWeaver);
			}
			else {
				pui.init(this.resourcePatternResolver.getClassLoader());
			}
			postProcessPersistenceUnitInfo(pui);
			String name = pui.getPersistenceUnitName();
			if (!this.persistenceUnitInfoNames.add(name) && !isPersistenceUnitOverrideAllowed()) {
				StringBuilder msg = new StringBuilder();
				msg.append("Conflicting persistence unit definitions for name '").append(name).append("': ");
				msg.append(pui.getPersistenceUnitRootUrl()).append(", ");
				msg.append(this.persistenceUnitInfos.get(name).getPersistenceUnitRootUrl());
				throw new IllegalStateException(msg.toString());
			}
			this.persistenceUnitInfos.put(name, pui);
		}
	}

	/**
	 * Read all persistence unit infos from {@code persistence.xml},
	 * as defined in the JPA specification.
	 */
	private List<SpringPersistenceUnitInfo> readPersistenceUnitInfos() {
		List<SpringPersistenceUnitInfo> infos = new LinkedList<SpringPersistenceUnitInfo>();
		boolean buildDefaultUnit = (this.packagesToScan != null || this.mappingResources != null);
		PersistenceUnitReader reader = new PersistenceUnitReader(this.resourcePatternResolver, this.dataSourceLookup);
		SpringPersistenceUnitInfo[] readInfos = reader.readPersistenceUnitInfos(this.persistenceXmlLocations);
		for (SpringPersistenceUnitInfo readInfo : readInfos) {
			infos.add(readInfo);
			if (this.defaultPersistenceUnitName != null &&
					this.defaultPersistenceUnitName.equals(readInfo.getPersistenceUnitName())) {
				buildDefaultUnit = false;
			}
		}
		if (buildDefaultUnit) {
			infos.add(buildDefaultPersistenceUnitInfo());
		}
		return infos;
	}

	/**
	 * Perform Spring-based scanning for entity classes.
	 * @see #setPackagesToScan
	 */
	private SpringPersistenceUnitInfo buildDefaultPersistenceUnitInfo() {
		SpringPersistenceUnitInfo scannedUnit = new SpringPersistenceUnitInfo();
		scannedUnit.setPersistenceUnitName(this.defaultPersistenceUnitName);
		scannedUnit.setExcludeUnlistedClasses(true);
		if (this.packagesToScan != null) {
			for (String pkg : this.packagesToScan) {
				try {
					String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
							ClassUtils.convertClassNameToResourcePath(pkg) + ENTITY_CLASS_RESOURCE_PATTERN;
					Resource[] resources = this.resourcePatternResolver.getResources(pattern);
					MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
					for (Resource resource : resources) {
						if (resource.isReadable()) {
							MetadataReader reader = readerFactory.getMetadataReader(resource);
							String className = reader.getClassMetadata().getClassName();
							if (matchesFilter(reader, readerFactory)) {
								scannedUnit.addManagedClassName(className);
								if (scannedUnit.getPersistenceUnitRootUrl() == null) {
									URL url = resource.getURL();
									if (ResourceUtils.isJarURL(url)) {
										scannedUnit.setPersistenceUnitRootUrl(ResourceUtils.extractJarFileURL(url));
									}
								}
							}
						}
					}
				}
				catch (IOException ex) {
					throw new PersistenceException("Failed to scan classpath for unlisted classes", ex);
				}
			}
		}
		if (this.mappingResources != null) {
			for (String mappingFileName : this.mappingResources) {
				scannedUnit.addMappingFileName(mappingFileName);
			}
		}
		return scannedUnit;
	}

	/**
	 * Check whether any of the configured entity type filters matches
	 * the current class descriptor contained in the metadata reader.
	 */
	private boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		for (TypeFilter filter : entityTypeFilters) {
			if (filter.match(reader, readerFactory)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Try to determine the persistence unit root URL based on the given
	 * "defaultPersistenceUnitRootLocation".
	 * @return the persistence unit root URL to pass to the JPA PersistenceProvider
	 * @see #setDefaultPersistenceUnitRootLocation
	 */
	private URL determineDefaultPersistenceUnitRootUrl() {
		if (this.defaultPersistenceUnitRootLocation == null) {
			return null;
		}
		try {
			Resource res = this.resourcePatternResolver.getResource(this.defaultPersistenceUnitRootLocation);
			return res.getURL();
		}
		catch (IOException ex) {
			throw new PersistenceException("Unable to resolve persistence unit root URL", ex);
		}
	}


	/**
	 * Return the specified PersistenceUnitInfo from this manager's cache
	 * of processed persistence units, keeping it in the cache (i.e. not
	 * 'obtaining' it for use but rather just accessing it for post-processing).
	 * <p>This can be used in {@link #postProcessPersistenceUnitInfo} implementations,
	 * detecting existing persistence units of the same name and potentially merging them.
	 * @param persistenceUnitName the name of the desired persistence unit
	 * @return the PersistenceUnitInfo in mutable form, or {@code null} if not available
	 */
	protected final MutablePersistenceUnitInfo getPersistenceUnitInfo(String persistenceUnitName) {
		PersistenceUnitInfo pui = this.persistenceUnitInfos.get(persistenceUnitName);
		return (MutablePersistenceUnitInfo) pui;
	}

	/**
	 * Hook method allowing subclasses to customize each PersistenceUnitInfo.
	 * <p>The default implementation delegates to all registered PersistenceUnitPostProcessors.
	 * It is usually preferable to register further entity classes, jar files etc there
	 * rather than in a subclass of this manager, to be able to reuse the post-processors.
	 * @param pui the chosen PersistenceUnitInfo, as read from {@code persistence.xml}.
	 * Passed in as MutablePersistenceUnitInfo.
	 * @see #setPersistenceUnitPostProcessors
	 */
	protected void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
		PersistenceUnitPostProcessor[] postProcessors = getPersistenceUnitPostProcessors();
		if (postProcessors != null) {
			for (PersistenceUnitPostProcessor postProcessor : postProcessors) {
				postProcessor.postProcessPersistenceUnitInfo(pui);
			}
		}
	}

	/**
	 * Return whether an override of a same-named persistence unit is allowed.
	 * <p>Default is {@code false}. May be overridden to return {@code true},
	 * for example if {@link #postProcessPersistenceUnitInfo} is able to handle that case.
	 */
	protected boolean isPersistenceUnitOverrideAllowed() {
		return false;
	}


	@Override
	public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() {
		if (this.persistenceUnitInfoNames.isEmpty()) {
			throw new IllegalStateException("No persistence units parsed from " +
					ObjectUtils.nullSafeToString(this.persistenceXmlLocations));
		}
		if (this.persistenceUnitInfos.isEmpty()) {
			throw new IllegalStateException("All persistence units from " +
					ObjectUtils.nullSafeToString(this.persistenceXmlLocations) + " already obtained");
		}
		if (this.persistenceUnitInfos.size() > 1) {
			return obtainPersistenceUnitInfo(this.defaultPersistenceUnitName);
		}
		PersistenceUnitInfo pui = this.persistenceUnitInfos.values().iterator().next();
		this.persistenceUnitInfos.clear();
		return pui;
	}

	@Override
	public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName) {
		PersistenceUnitInfo pui = this.persistenceUnitInfos.remove(persistenceUnitName);
		if (pui == null) {
			if (!this.persistenceUnitInfoNames.contains(persistenceUnitName)) {
				throw new IllegalArgumentException(
						"No persistence unit with name '" + persistenceUnitName + "' found");
			}
			else {
				throw new IllegalStateException(
						"Persistence unit with name '" + persistenceUnitName + "' already obtained");
			}
		}
		return pui;
	}

}
