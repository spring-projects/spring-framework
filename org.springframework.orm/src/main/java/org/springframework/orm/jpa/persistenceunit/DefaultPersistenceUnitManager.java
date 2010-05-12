/*
 * Copyright 2002-2010 the original author or authors.
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of the {@link PersistenceUnitManager} interface.
 * Used as internal default by
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}.
 *
 * <p>Supports standard JPA scanning for <code>persistence.xml</code> files,
 * with configurable file locations, JDBC DataSource lookup and load-time weaving.
 *
 * <p>The default XML file location is <code>classpath*:META-INF/persistence.xml</code>,
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
	 * Default location of the <code>persistence.xml</code> file:
	 * "classpath*:META-INF/persistence.xml".
	 */
	public final static String DEFAULT_PERSISTENCE_XML_LOCATION = "classpath*:META-INF/persistence.xml";

	/**
	 * Default location for the persistence unit root URL:
	 * "classpath:", indicating the root of the class path.
	 */
	public final static String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION = "classpath:";


	private static final boolean jpa2ApiPresent = ClassUtils.hasMethod(PersistenceUnitInfo.class, "getSharedCacheMode");

	private String[] persistenceXmlLocations = new String[] {DEFAULT_PERSISTENCE_XML_LOCATION};

	private String defaultPersistenceUnitRootLocation = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION;

	private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	private DataSource defaultDataSource;

	private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;

	private LoadTimeWeaver loadTimeWeaver;

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private final Set<String> persistenceUnitInfoNames = new HashSet<String>();

	private final Map<String, PersistenceUnitInfo> persistenceUnitInfos = new HashMap<String, PersistenceUnitInfo>();


	/**
	 * Specify the location of the <code>persistence.xml</code> files to load.
	 * These can be specified as Spring resource locations and/or location patterns.
	 * <p>Default is "classpath*:META-INF/persistence.xml".
	 */
	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
		this.persistenceXmlLocations = new String[] {persistenceXmlLocation};
	}

	/**
	 * Specify multiple locations of <code>persistence.xml</code> files to load.
	 * These can be specified as Spring resource locations and/or location patterns.
	 * <p>Default is "classpath*:META-INF/persistence.xml".
	 * @param persistenceXmlLocations an array of Spring resource Strings
	 * identifying the location of the <code>persistence.xml</code> files to read
	 */
	public void setPersistenceXmlLocations(String[] persistenceXmlLocations) {
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
	 * Specify the JDBC DataSources that the JPA persistence provider is supposed
	 * to use for accessing the database, resolving data source names in
	 * <code>persistence.xml</code> against Spring-managed DataSources.
	 * <p>The specified Map needs to define data source names for specific DataSource
	 * objects, matching the data source names used in <code>persistence.xml</code>.
	 * If not specified, data source names will be resolved as JNDI names instead
	 * (as defined by standard JPA).
	 * @see org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
	 */
	public void setDataSources(Map<String, DataSource> dataSources) {
		this.dataSourceLookup = new MapDataSourceLookup(dataSources);
	}

	/**
	 * Specify the JDBC DataSourceLookup that provides DataSources for the
	 * persistence provider, resolving data source names in <code>persistence.xml</code>
	 * against Spring-managed DataSource instances.
	 * <p>Default is JndiDataSourceLookup, which resolves DataSource names as
	 * JNDI names (as defined by standard JPA). Specify a BeanFactoryDataSourceLookup
	 * instance if you want DataSource names to be resolved against Spring bean names.
	 * <p>Alternatively, consider passing in a map from names to DataSource instances
	 * via the "dataSources" property. If the <code>persistence.xml</code> file
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
	 * persistence provider, resolving data source names in <code>persistence.xml</code>
	 * against Spring-managed DataSource instances.
	 */
	public DataSourceLookup getDataSourceLookup() {
		return this.dataSourceLookup;
	}

	/**
	 * Specify the JDBC DataSource that the JPA persistence provider is supposed
	 * to use for accessing the database if none has been specified in
	 * <code>persistence.xml</code>.
	 * <p>In JPA speak, a DataSource passed in here will be uses as "nonJtaDataSource"
	 * on the PersistenceUnitInfo passed to the PersistenceProvider, provided that
	 * none has been registered before.
	 * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
	 */
	public void setDefaultDataSource(DataSource defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	/**
	 * Return the JDBC DataSource that the JPA persistence provider is supposed
	 * to use for accessing the database if none has been specified in
	 * <code>persistence.xml</code>.
	 */
	public DataSource getDefaultDataSource() {
		return this.defaultDataSource;
	}

	/**
	 * Set the PersistenceUnitPostProcessors to be applied to each
	 * PersistenceUnitInfo that has been parsed by this manager.
	 * <p>Such post-processors can, for example, register further entity
	 * classes and jar files, in addition to the metadata read in from
	 * <code>persistence.xml</code>.
	 */
	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor[] postProcessors) {
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
	 * Consider using the <code>context:load-time-weaver</code> XML tag for creating
	 * such a shared LoadTimeWeaver (autodetecting the environment by default).
	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
	 * @see org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader
	 */
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

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	public void afterPropertiesSet() {
		if (this.loadTimeWeaver == null && InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(this.resourcePatternResolver.getClassLoader());
		}
		preparePersistenceUnitInfos();
	}

	/**
	 * Prepare the PersistenceUnitInfos according to the configuration
	 * of this manager: scanning for <code>persistence.xml</code> files,
	 * parsing all matching files, configurating and post-processing them.
	 * <p>PersistenceUnitInfos cannot be obtained before this preparation
	 * method has been invoked.
	 * @see #obtainDefaultPersistenceUnitInfo()
	 * @see #obtainPersistenceUnitInfo(String)
	 */
	public void preparePersistenceUnitInfos() {
		this.persistenceUnitInfoNames.clear();
		this.persistenceUnitInfos.clear();
		SpringPersistenceUnitInfo[] puis = readPersistenceUnitInfos();
		for (SpringPersistenceUnitInfo pui : puis) {
			if (pui.getPersistenceUnitRootUrl() == null) {
				pui.setPersistenceUnitRootUrl(determineDefaultPersistenceUnitRootUrl());
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
			this.persistenceUnitInfoNames.add(name);
			PersistenceUnitInfo puiToStore = pui;
			if (jpa2ApiPresent) {
				puiToStore = (PersistenceUnitInfo) Proxy.newProxyInstance(SmartPersistenceUnitInfo.class.getClassLoader(),
						new Class[] {SmartPersistenceUnitInfo.class}, new Jpa2PersistenceUnitInfoDecorator(pui));
			}
			this.persistenceUnitInfos.put(name, puiToStore);
		}
	}

	/**
	 * Read all persistence unit infos from <code>persistence.xml</code>,
	 * as defined in the JPA specification.
	 */
	private SpringPersistenceUnitInfo[] readPersistenceUnitInfos() {
		PersistenceUnitReader reader = new PersistenceUnitReader(this.resourcePatternResolver, this.dataSourceLookup);
		return reader.readPersistenceUnitInfos(this.persistenceXmlLocations);
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
	 * @return the PersistenceUnitInfo in mutable form, or <code>null</code> if not available
	 */
	protected final MutablePersistenceUnitInfo getPersistenceUnitInfo(String persistenceUnitName) {
		PersistenceUnitInfo pui = this.persistenceUnitInfos.get(persistenceUnitName);
		if (pui != null && Proxy.isProxyClass(pui.getClass())) {
			// JPA 2.0 PersistenceUnitInfo decorator with a SpringPersistenceUnitInfo as target
			Jpa2PersistenceUnitInfoDecorator dec = (Jpa2PersistenceUnitInfoDecorator) Proxy.getInvocationHandler(pui);
			return dec.getTarget();
		}
		else {
			// Must be a raw JPA 1.0 SpringPersistenceUnitInfo instance
			return (MutablePersistenceUnitInfo) pui;
		}
	}

	/**
	 * Hook method allowing subclasses to customize each PersistenceUnitInfo.
	 * <p>Default implementation delegates to all registered PersistenceUnitPostProcessors.
	 * It is usually preferable to register further entity classes, jar files etc there
	 * rather than in a subclass of this manager, to be able to reuse the post-processors.
	 * @param pui the chosen PersistenceUnitInfo, as read from <code>persistence.xml</code>.
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
			throw new IllegalStateException("No single default persistence unit defined in " +
					ObjectUtils.nullSafeToString(this.persistenceXmlLocations));
		}
		PersistenceUnitInfo pui = this.persistenceUnitInfos.values().iterator().next();
		this.persistenceUnitInfos.clear();
		return pui;
	}

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


	/**
	 * Decorator that exposes a JPA 2.0 compliant PersistenceUnitInfo interface for a
	 * JPA 1.0 based SpringPersistenceUnitInfo object, adapting the <code>getSharedCacheMode</code>
	 * and <code>getValidationMode</code> methods from String names to enum return values.
	 */
	private static class Jpa2PersistenceUnitInfoDecorator implements InvocationHandler {

		private final SpringPersistenceUnitInfo target;

		private final Class<? extends Enum> sharedCacheModeEnum;

		private final Class<? extends Enum> validationModeEnum;

		@SuppressWarnings("unchecked")
		public Jpa2PersistenceUnitInfoDecorator(SpringPersistenceUnitInfo target) {
			this.target = target;
			try {
				this.sharedCacheModeEnum = (Class<? extends Enum>)
						ClassUtils.forName("javax.persistence.SharedCacheMode", PersistenceUnitInfo.class.getClassLoader());
				this.validationModeEnum = (Class<? extends Enum>)
						ClassUtils.forName("javax.persistence.ValidationMode", PersistenceUnitInfo.class.getClassLoader());
			}
			catch (Exception ex) {
				throw new IllegalStateException("JPA 2.0 API enum types not present", ex);
			}
		}

		public final SpringPersistenceUnitInfo getTarget() {
			return this.target;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("getSharedCacheMode")) {
				return Enum.valueOf(this.sharedCacheModeEnum, this.target.getSharedCacheModeName());
			}
			else if (method.getName().equals("getValidationMode")) {
				return Enum.valueOf(this.validationModeEnum, this.target.getValidationModeName());
			}
			else {
				return method.invoke(this.target, args);
			}
		}
	}

}
