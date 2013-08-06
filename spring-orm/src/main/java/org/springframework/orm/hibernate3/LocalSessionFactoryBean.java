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

package org.springframework.orm.hibernate3;

import java.io.File;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.EventListeners;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.transaction.JTATransactionFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a
 * Hibernate {@link org.hibernate.SessionFactory}. This is the usual way to
 * set up a shared Hibernate SessionFactory in a Spring application context;
 * the SessionFactory can then be passed to Hibernate-based DAOs via
 * dependency injection.
 *
 * <p>Configuration settings can either be read from a Hibernate XML file,
 * specified as "configLocation", or completely via this class. A typical
 * local configuration consists of one or more "mappingResources", various
 * "hibernateProperties" (not strictly necessary), and a "dataSource" that the
 * SessionFactory should use. The latter can also be specified via Hibernate
 * properties, but "dataSource" supports any Spring-configured DataSource,
 * instead of relying on Hibernate's own connection providers.
 *
 * <p>This SessionFactory handling strategy is appropriate for most types of
 * applications, from Hibernate-only single database apps to ones that need
 * distributed transactions. Either {@link HibernateTransactionManager} or
 * {@link org.springframework.transaction.jta.JtaTransactionManager} can be
 * used for transaction demarcation, with the latter only necessary for
 * transactions which span multiple databases.
 *
 * <p>This factory bean will by default expose a transaction-aware SessionFactory
 * proxy, letting data access code work with the plain Hibernate SessionFactory
 * and its {@code getCurrentSession()} method, while still being able to
 * participate in current Spring-managed transactions: with any transaction
 * management strategy, either local or JTA / EJB CMT, and any transaction
 * synchronization mechanism, either Spring or JTA. Furthermore,
 * {@code getCurrentSession()} will also seamlessly work with
 * a request-scoped Session managed by
 * {@link org.springframework.orm.hibernate3.support.OpenSessionInViewFilter} /
 * {@link org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor}.
 *
 * <p><b>Requires Hibernate 3.6 or later.</b>
 * Note that this factory will use "on_close" as default Hibernate connection
 * release mode, unless in the case of a "jtaTransactionManager" specified,
 * for the reason that this is appropriate for most Spring-based applications
 * (in particular when using Spring's HibernateTransactionManager).
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see HibernateTemplate#setSessionFactory
 * @see HibernateTransactionManager#setSessionFactory
 * @see #setExposeTransactionAwareSessionFactory
 * @see #setJtaTransactionManager
 * @see org.hibernate.SessionFactory#getCurrentSession()
 * @see HibernateTransactionManager
 */
public class LocalSessionFactoryBean extends AbstractSessionFactoryBean implements BeanClassLoaderAware {

	private static final ThreadLocal<DataSource> configTimeDataSourceHolder =
			new ThreadLocal<DataSource>();

	private static final ThreadLocal<TransactionManager> configTimeTransactionManagerHolder =
			new ThreadLocal<TransactionManager>();

	private static final ThreadLocal<Object> configTimeRegionFactoryHolder =
			new ThreadLocal<Object>();

	@SuppressWarnings("deprecation")
	private static final ThreadLocal<org.hibernate.cache.CacheProvider> configTimeCacheProviderHolder =
			new ThreadLocal<org.hibernate.cache.CacheProvider>();

	private static final ThreadLocal<LobHandler> configTimeLobHandlerHolder =
			new ThreadLocal<LobHandler>();

	/**
	 * Return the DataSource for the currently configured Hibernate SessionFactory,
	 * to be used by LocalDataSourceConnectionProvoder.
	 * <p>This instance will be set before initialization of the corresponding
	 * SessionFactory, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setDataSource
	 * @see LocalDataSourceConnectionProvider
	 */
	public static DataSource getConfigTimeDataSource() {
		return configTimeDataSourceHolder.get();
	}

	/**
	 * Return the JTA TransactionManager for the currently configured Hibernate
	 * SessionFactory, to be used by LocalTransactionManagerLookup.
	 * <p>This instance will be set before initialization of the corresponding
	 * SessionFactory, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setJtaTransactionManager
	 * @see LocalTransactionManagerLookup
	 */
	public static TransactionManager getConfigTimeTransactionManager() {
		return configTimeTransactionManagerHolder.get();
	}

	/**
	 * Return the RegionFactory for the currently configured Hibernate SessionFactory,
	 * to be used by LocalRegionFactoryProxy.
	 * <p>This instance will be set before initialization of the corresponding
	 * SessionFactory, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setCacheRegionFactory
	 */
	static Object getConfigTimeRegionFactory() {
		return configTimeRegionFactoryHolder.get();
	}

	/**
	 * Return the LobHandler for the currently configured Hibernate SessionFactory,
	 * to be used by UserType implementations like ClobStringType.
	 * <p>This instance will be set before initialization of the corresponding
	 * SessionFactory, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setLobHandler
	 * @see org.springframework.orm.hibernate3.support.ClobStringType
	 * @see org.springframework.orm.hibernate3.support.BlobByteArrayType
	 * @see org.springframework.orm.hibernate3.support.BlobSerializableType
	 */
	public static LobHandler getConfigTimeLobHandler() {
		return configTimeLobHandlerHolder.get();
	}


	private Class<? extends Configuration> configurationClass = Configuration.class;

	private Resource[] configLocations;

	private String[] mappingResources;

	private Resource[] mappingLocations;

	private Resource[] cacheableMappingLocations;

	private Resource[] mappingJarLocations;

	private Resource[] mappingDirectoryLocations;

	private Properties hibernateProperties;

	private TransactionManager jtaTransactionManager;

	private RegionFactory cacheRegionFactory;

	private LobHandler lobHandler;

	private Interceptor entityInterceptor;

	private NamingStrategy namingStrategy;

	private TypeDefinitionBean[] typeDefinitions;

	private FilterDefinition[] filterDefinitions;

	private Properties entityCacheStrategies;

	private Properties collectionCacheStrategies;

	private Map<String, Object> eventListeners;

	private boolean schemaUpdate = false;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Configuration configuration;


	/**
	 * Specify the Hibernate Configuration class to use.
	 * Default is "org.hibernate.cfg.Configuration"; any subclass of
	 * this default Hibernate Configuration class can be specified.
	 * <p>Can be set to "org.hibernate.cfg.AnnotationConfiguration" for
	 * using Hibernate3 annotation support (initially only available as
	 * alpha download separate from the main Hibernate3 distribution).
	 * <p>Annotated packages and annotated classes can be specified via the
	 * corresponding tags in "hibernate.cfg.xml" then, so this will usually
	 * be combined with a "configLocation" property that points at such a
	 * standard Hibernate configuration file.
	 * @see #setConfigLocation
	 * @see org.hibernate.cfg.Configuration
	 * @see org.hibernate.cfg.AnnotationConfiguration
	 */
	@SuppressWarnings("unchecked")
	public void setConfigurationClass(Class<?> configurationClass) {
		if (configurationClass == null || !Configuration.class.isAssignableFrom(configurationClass)) {
			throw new IllegalArgumentException(
					"'configurationClass' must be assignable to [org.hibernate.cfg.Configuration]");
		}
		this.configurationClass = (Class<? extends Configuration>) configurationClass;
	}

	/**
	 * Set the location of a single Hibernate XML config file, for example as
	 * classpath resource "classpath:hibernate.cfg.xml".
	 * <p>Note: Can be omitted when all necessary properties and mapping
	 * resources are specified locally via this bean.
	 * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocations = new Resource[] {configLocation};
	}

	/**
	 * Set the locations of multiple Hibernate XML config files, for example as
	 * classpath resources "classpath:hibernate.cfg.xml,classpath:extension.cfg.xml".
	 * <p>Note: Can be omitted when all necessary properties and mapping
	 * resources are specified locally via this bean.
	 * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
	 */
	public void setConfigLocations(Resource[] configLocations) {
		this.configLocations = configLocations;
	}

	/**
	 * Set Hibernate mapping resources to be found in the class path,
	 * like "example.hbm.xml" or "mypackage/example.hbm.xml".
	 * Analogous to mapping entries in a Hibernate XML config file.
	 * Alternative to the more generic setMappingLocations method.
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see #setMappingLocations
	 * @see org.hibernate.cfg.Configuration#addResource
	 */
	public void setMappingResources(String[] mappingResources) {
		this.mappingResources = mappingResources;
	}

	/**
	 * Set locations of Hibernate mapping files, for example as classpath
	 * resource "classpath:example.hbm.xml". Supports any resource location
	 * via Spring's resource abstraction, for example relative paths like
	 * "WEB-INF/mappings/example.hbm.xml" when running in an application context.
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see org.hibernate.cfg.Configuration#addInputStream
	 */
	public void setMappingLocations(Resource[] mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * Set locations of cacheable Hibernate mapping files, for example as web app
	 * resource "/WEB-INF/mapping/example.hbm.xml". Supports any resource location
	 * via Spring's resource abstraction, as long as the resource can be resolved
	 * in the file system.
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see org.hibernate.cfg.Configuration#addCacheableFile(java.io.File)
	 */
	public void setCacheableMappingLocations(Resource[] cacheableMappingLocations) {
		this.cacheableMappingLocations = cacheableMappingLocations;
	}

	/**
	 * Set locations of jar files that contain Hibernate mapping resources,
	 * like "WEB-INF/lib/example.hbm.jar".
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see org.hibernate.cfg.Configuration#addJar(java.io.File)
	 */
	public void setMappingJarLocations(Resource[] mappingJarLocations) {
		this.mappingJarLocations = mappingJarLocations;
	}

	/**
	 * Set locations of directories that contain Hibernate mapping resources,
	 * like "WEB-INF/mappings".
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see org.hibernate.cfg.Configuration#addDirectory(java.io.File)
	 */
	public void setMappingDirectoryLocations(Resource[] mappingDirectoryLocations) {
		this.mappingDirectoryLocations = mappingDirectoryLocations;
	}

	/**
	 * Set Hibernate properties, such as "hibernate.dialect".
	 * <p>Can be used to override values in a Hibernate XML config file,
	 * or to specify all necessary properties locally.
	 * <p>Note: Do not specify a transaction provider here when using
	 * Spring-driven transactions. It is also advisable to omit connection
	 * provider settings and use a Spring-set DataSource instead.
	 * @see #setDataSource
	 */
	public void setHibernateProperties(Properties hibernateProperties) {
		this.hibernateProperties = hibernateProperties;
	}

	/**
	 * Return the Hibernate properties, if any. Mainly available for
	 * configuration through property paths that specify individual keys.
	 */
	public Properties getHibernateProperties() {
		if (this.hibernateProperties == null) {
			this.hibernateProperties = new Properties();
		}
		return this.hibernateProperties;
	}

	/**
	 * Set the JTA TransactionManager to be used for Hibernate's
	 * TransactionManagerLookup. Allows for using a Spring-managed
	 * JTA TransactionManager for Hibernate's cache synchronization.
	 * <p>Note: If this is set, the Hibernate settings should not define a
	 * transaction manager lookup to avoid meaningless double configuration.
	 * @see LocalTransactionManagerLookup
	 */
	public void setJtaTransactionManager(TransactionManager jtaTransactionManager) {
		this.jtaTransactionManager = jtaTransactionManager;
	}

	/**
	 * Set the Hibernate RegionFactory to use for the SessionFactory.
	 * Allows for using a Spring-managed RegionFactory instance.
	 * <p>Note: If this is set, the Hibernate settings should not define a
	 * cache provider to avoid meaningless double configuration.
	 * @see org.hibernate.cache.RegionFactory
	 */
	public void setCacheRegionFactory(RegionFactory cacheRegionFactory) {
		this.cacheRegionFactory = cacheRegionFactory;
	}

	/**
	 * Set the LobHandler to be used by the SessionFactory.
	 * Will be exposed at config time for UserType implementations.
	 * @see #getConfigTimeLobHandler
	 * @see org.hibernate.usertype.UserType
	 * @see org.springframework.orm.hibernate3.support.ClobStringType
	 * @see org.springframework.orm.hibernate3.support.BlobByteArrayType
	 * @see org.springframework.orm.hibernate3.support.BlobSerializableType
	 */
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	/**
	 * Set a Hibernate entity interceptor that allows to inspect and change
	 * property values before writing to and reading from the database.
	 * Will get applied to any new Session created by this factory.
	 * <p>Such an interceptor can either be set at the SessionFactory level, i.e. on
	 * LocalSessionFactoryBean, or at the Session level, i.e. on HibernateTemplate,
	 * HibernateInterceptor, and HibernateTransactionManager. It's preferable to set
	 * it on LocalSessionFactoryBean or HibernateTransactionManager to avoid repeated
	 * configuration and guarantee consistent behavior in transactions.
	 * @see HibernateTemplate#setEntityInterceptor
	 * @see HibernateInterceptor#setEntityInterceptor
	 * @see HibernateTransactionManager#setEntityInterceptor
	 * @see org.hibernate.cfg.Configuration#setInterceptor
	 */
	public void setEntityInterceptor(Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * Set a Hibernate NamingStrategy for the SessionFactory, determining the
	 * physical column and table names given the info in the mapping document.
	 * @see org.hibernate.cfg.Configuration#setNamingStrategy
	 */
	public void setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * Specify the Hibernate type definitions to register with the SessionFactory,
	 * as Spring TypeDefinitionBean instances. This is an alternative to specifying
	 * <&lt;typedef&gt; elements in Hibernate mapping files.
	 * <p>Unfortunately, Hibernate itself does not define a complete object that
	 * represents a type definition, hence the need for Spring's TypeDefinitionBean.
	 * @see TypeDefinitionBean
	 * @see org.hibernate.cfg.Mappings#addTypeDef(String, String, java.util.Properties)
	 */
	public void setTypeDefinitions(TypeDefinitionBean[] typeDefinitions) {
		this.typeDefinitions = typeDefinitions;
	}

	/**
	 * Specify the Hibernate FilterDefinitions to register with the SessionFactory.
	 * This is an alternative to specifying <&lt;filter-def&gt; elements in
	 * Hibernate mapping files.
	 * <p>Typically, the passed-in FilterDefinition objects will have been defined
	 * as Spring FilterDefinitionFactoryBeans, probably as inner beans within the
	 * LocalSessionFactoryBean definition.
	 * @see FilterDefinitionFactoryBean
	 * @see org.hibernate.cfg.Configuration#addFilterDefinition
	 */
	public void setFilterDefinitions(FilterDefinition[] filterDefinitions) {
		this.filterDefinitions = filterDefinitions;
	}

	/**
	 * Specify the cache strategies for entities (persistent classes or named entities).
	 * This configuration setting corresponds to the &lt;class-cache&gt; entry
	 * in the "hibernate.cfg.xml" configuration format.
	 * <p>For example:
	 * <pre class="code">
	 * &lt;property name="entityCacheStrategies"&gt;
	 *   &lt;props&gt;
	 *     &lt;prop key="com.mycompany.Customer"&gt;read-write&lt;/prop&gt;
	 *     &lt;prop key="com.mycompany.Product"&gt;read-only,myRegion&lt;/prop&gt;
	 *   &lt;/props&gt;
	 * &lt;/property&gt;</pre>
	 * @param entityCacheStrategies properties that define entity cache strategies,
	 * with class names as keys and cache concurrency strategies as values
	 * @see org.hibernate.cfg.Configuration#setCacheConcurrencyStrategy(String, String)
	 */
	public void setEntityCacheStrategies(Properties entityCacheStrategies) {
		this.entityCacheStrategies = entityCacheStrategies;
	}

	/**
	 * Specify the cache strategies for persistent collections (with specific roles).
	 * This configuration setting corresponds to the &lt;collection-cache&gt; entry
	 * in the "hibernate.cfg.xml" configuration format.
	 * <p>For example:
	 * <pre class="code">
	 * &lt;property name="collectionCacheStrategies"&gt;
	 *   &lt;props&gt;
	 *     &lt;prop key="com.mycompany.Order.items">read-write&lt;/prop&gt;
	 *     &lt;prop key="com.mycompany.Product.categories"&gt;read-only,myRegion&lt;/prop&gt;
	 *   &lt;/props&gt;
	 * &lt;/property&gt;</pre>
	 * @param collectionCacheStrategies properties that define collection cache strategies,
	 * with collection roles as keys and cache concurrency strategies as values
	 * @see org.hibernate.cfg.Configuration#setCollectionCacheConcurrencyStrategy(String, String)
	 */
	public void setCollectionCacheStrategies(Properties collectionCacheStrategies) {
		this.collectionCacheStrategies = collectionCacheStrategies;
	}

	/**
	 * Specify the Hibernate event listeners to register, with listener types
	 * as keys and listener objects as values. Instead of a single listener object,
	 * you can also pass in a list or set of listeners objects as value.
	 * <p>See the Hibernate documentation for further details on listener types
	 * and associated listener interfaces.
	 * <p>See {@code org.hibernate.cfg.Configuration#setListener(String, Object)}
	 * @param eventListeners Map with listener type Strings as keys and
	 * listener objects as values
	 */
	public void setEventListeners(Map<String, Object> eventListeners) {
		this.eventListeners = eventListeners;
	}

	/**
	 * Set whether to execute a schema update after SessionFactory initialization.
	 * <p>For details on how to make schema update scripts work, see the Hibernate
	 * documentation, as this class leverages the same schema update script support
	 * in org.hibernate.cfg.Configuration as Hibernate's own SchemaUpdate tool.
	 * @see org.hibernate.cfg.Configuration#generateSchemaUpdateScript
	 * @see org.hibernate.tool.hbm2ddl.SchemaUpdate
	 */
	public void setSchemaUpdate(boolean schemaUpdate) {
		this.schemaUpdate = schemaUpdate;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	@SuppressWarnings("unchecked")
	protected SessionFactory buildSessionFactory() throws Exception {
		// Create Configuration instance.
		Configuration config = newConfiguration();

		DataSource dataSource = getDataSource();
		if (dataSource != null) {
			// Make given DataSource available for SessionFactory configuration.
			configTimeDataSourceHolder.set(dataSource);
		}
		if (this.jtaTransactionManager != null) {
			// Make Spring-provided JTA TransactionManager available.
			configTimeTransactionManagerHolder.set(this.jtaTransactionManager);
		}
		if (this.cacheRegionFactory != null) {
			// Make Spring-provided Hibernate RegionFactory available.
			configTimeRegionFactoryHolder.set(this.cacheRegionFactory);
		}
		if (this.lobHandler != null) {
			// Make given LobHandler available for SessionFactory configuration.
			// Do early because because mapping resource might refer to custom types.
			configTimeLobHandlerHolder.set(this.lobHandler);
		}

		// Analogous to Hibernate EntityManager's Ejb3Configuration:
		// Hibernate doesn't allow setting the bean ClassLoader explicitly,
		// so we need to expose it as thread context ClassLoader accordingly.
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader =
				(this.beanClassLoader != null && !this.beanClassLoader.equals(threadContextClassLoader));
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.beanClassLoader);
		}

		try {
			if (isExposeTransactionAwareSessionFactory()) {
				// Set Hibernate 3.1+ CurrentSessionContext implementation,
				// providing the Spring-managed Session as current Session.
				// Can be overridden by a custom value for the corresponding Hibernate property.
				config.setProperty(
						Environment.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
			}

			if (this.jtaTransactionManager != null) {
				// Set Spring-provided JTA TransactionManager as Hibernate property.
				config.setProperty(
						Environment.TRANSACTION_STRATEGY, JTATransactionFactory.class.getName());
				config.setProperty(
						Environment.TRANSACTION_MANAGER_STRATEGY, LocalTransactionManagerLookup.class.getName());
			}
			else {
				// Makes the Hibernate Session aware of the presence of a Spring-managed transaction.
				// Also sets connection release mode to ON_CLOSE by default.
				config.setProperty(
						Environment.TRANSACTION_STRATEGY, SpringTransactionFactory.class.getName());
			}

			if (this.entityInterceptor != null) {
				// Set given entity interceptor at SessionFactory level.
				config.setInterceptor(this.entityInterceptor);
			}

			if (this.namingStrategy != null) {
				// Pass given naming strategy to Hibernate Configuration.
				config.setNamingStrategy(this.namingStrategy);
			}

			if (this.typeDefinitions != null) {
				// Register specified Hibernate type definitions.
				Mappings mappings = config.createMappings();
				for (TypeDefinitionBean typeDef : this.typeDefinitions) {
					mappings.addTypeDef(typeDef.getTypeName(), typeDef.getTypeClass(), typeDef.getParameters());
				}
			}

			if (this.filterDefinitions != null) {
				// Register specified Hibernate FilterDefinitions.
				for (FilterDefinition filterDef : this.filterDefinitions) {
					config.addFilterDefinition(filterDef);
				}
			}

			if (this.configLocations != null) {
				for (Resource resource : this.configLocations) {
					// Load Hibernate configuration from given location.
					config.configure(resource.getURL());
				}
			}

			if (this.hibernateProperties != null) {
				// Add given Hibernate properties to Configuration.
				config.addProperties(this.hibernateProperties);
			}

			if (dataSource != null) {
				Class<?> providerClass = LocalDataSourceConnectionProvider.class;
				if (isUseTransactionAwareDataSource() || dataSource instanceof TransactionAwareDataSourceProxy) {
					providerClass = TransactionAwareDataSourceConnectionProvider.class;
				}
				else if (config.getProperty(Environment.TRANSACTION_MANAGER_STRATEGY) != null) {
					providerClass = LocalJtaDataSourceConnectionProvider.class;
				}
				// Set Spring-provided DataSource as Hibernate ConnectionProvider.
				config.setProperty(Environment.CONNECTION_PROVIDER, providerClass.getName());
			}

			if (this.cacheRegionFactory != null) {
				// Expose Spring-provided Hibernate RegionFactory.
				config.setProperty(Environment.CACHE_REGION_FACTORY, LocalRegionFactoryProxy.class.getName());
			}

			if (this.mappingResources != null) {
				// Register given Hibernate mapping definitions, contained in resource files.
				for (String mapping : this.mappingResources) {
					Resource resource = new ClassPathResource(mapping.trim(), this.beanClassLoader);
					config.addInputStream(resource.getInputStream());
				}
			}

			if (this.mappingLocations != null) {
				// Register given Hibernate mapping definitions, contained in resource files.
				for (Resource resource : this.mappingLocations) {
					config.addInputStream(resource.getInputStream());
				}
			}

			if (this.cacheableMappingLocations != null) {
				// Register given cacheable Hibernate mapping definitions, read from the file system.
				for (Resource resource : this.cacheableMappingLocations) {
					config.addCacheableFile(resource.getFile());
				}
			}

			if (this.mappingJarLocations != null) {
				// Register given Hibernate mapping definitions, contained in jar files.
				for (Resource resource : this.mappingJarLocations) {
					config.addJar(resource.getFile());
				}
			}

			if (this.mappingDirectoryLocations != null) {
				// Register all Hibernate mapping definitions in the given directories.
				for (Resource resource : this.mappingDirectoryLocations) {
					File file = resource.getFile();
					if (!file.isDirectory()) {
						throw new IllegalArgumentException(
								"Mapping directory location [" + resource + "] does not denote a directory");
					}
					config.addDirectory(file);
				}
			}

			// Tell Hibernate to eagerly compile the mappings that we registered,
			// for availability of the mapping information in further processing.
			postProcessMappings(config);
			config.buildMappings();

			if (this.entityCacheStrategies != null) {
				// Register cache strategies for mapped entities.
				for (Enumeration<?> classNames = this.entityCacheStrategies.propertyNames(); classNames.hasMoreElements();) {
					String className = (String) classNames.nextElement();
					String[] strategyAndRegion =
							StringUtils.commaDelimitedListToStringArray(this.entityCacheStrategies.getProperty(className));
					if (strategyAndRegion.length > 1) {
						config.setCacheConcurrencyStrategy(className, strategyAndRegion[0], strategyAndRegion[1]);
					}
					else if (strategyAndRegion.length > 0) {
						config.setCacheConcurrencyStrategy(className, strategyAndRegion[0]);
					}
				}
			}

			if (this.collectionCacheStrategies != null) {
				// Register cache strategies for mapped collections.
				for (Enumeration<?> collRoles = this.collectionCacheStrategies.propertyNames(); collRoles.hasMoreElements();) {
					String collRole = (String) collRoles.nextElement();
					String[] strategyAndRegion =
							StringUtils.commaDelimitedListToStringArray(this.collectionCacheStrategies.getProperty(collRole));
					if (strategyAndRegion.length > 1) {
						config.setCollectionCacheConcurrencyStrategy(collRole, strategyAndRegion[0], strategyAndRegion[1]);
					}
					else if (strategyAndRegion.length > 0) {
						config.setCollectionCacheConcurrencyStrategy(collRole, strategyAndRegion[0]);
					}
				}
			}

			if (this.eventListeners != null) {
				// Register specified Hibernate event listeners.
				for (Map.Entry<String, Object> entry : this.eventListeners.entrySet()) {
					String listenerType = entry.getKey();
					Object listenerObject = entry.getValue();
					if (listenerObject instanceof Collection) {
						Collection<Object> listeners = (Collection<Object>) listenerObject;
						EventListeners listenerRegistry = config.getEventListeners();
						Object[] listenerArray =
								(Object[]) Array.newInstance(listenerRegistry.getListenerClassFor(listenerType), listeners.size());
						listenerArray = listeners.toArray(listenerArray);
						config.setListeners(listenerType, listenerArray);
					}
					else {
						config.setListener(listenerType, listenerObject);
					}
				}
			}

			// Perform custom post-processing in subclasses.
			postProcessConfiguration(config);

			// Build SessionFactory instance.
			logger.info("Building new Hibernate SessionFactory");
			this.configuration = config;
			return newSessionFactory(config);
		}

		finally {
			if (dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
			if (this.jtaTransactionManager != null) {
				configTimeTransactionManagerHolder.remove();
			}
			if (this.cacheRegionFactory != null) {
				configTimeRegionFactoryHolder.remove();
			}
			if (this.lobHandler != null) {
				configTimeLobHandlerHolder.remove();
			}
			if (overrideClassLoader) {
				// Reset original thread context ClassLoader.
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

	/**
	 * Subclasses can override this method to perform custom initialization
	 * of the Configuration instance used for SessionFactory creation.
	 * The properties of this LocalSessionFactoryBean will be applied to
	 * the Configuration object that gets returned here.
	 * <p>The default implementation creates a new Configuration instance.
	 * A custom implementation could prepare the instance in a specific way,
	 * or use a custom Configuration subclass.
	 * @return the Configuration instance
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#Configuration()
	 */
	protected Configuration newConfiguration() throws HibernateException {
		return BeanUtils.instantiateClass(this.configurationClass);
	}

	/**
	 * To be implemented by subclasses that want to to register further mappings
	 * on the Configuration object after this FactoryBean registered its specified
	 * mappings.
	 * <p>Invoked <i>before</i> the {@code Configuration.buildMappings()} call,
	 * so that it can still extend and modify the mapping information.
	 * @param config the current Configuration object
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#buildMappings()
	 */
	protected void postProcessMappings(Configuration config) throws HibernateException {
	}

	/**
	 * To be implemented by subclasses that want to to perform custom
	 * post-processing of the Configuration object after this FactoryBean
	 * performed its default initialization.
	 * <p>Invoked <i>after</i> the {@code Configuration.buildMappings()} call,
	 * so that it can operate on the completed and fully parsed mapping information.
	 * @param config the current Configuration object
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#buildMappings()
	 */
	protected void postProcessConfiguration(Configuration config) throws HibernateException {
	}

	/**
	 * Subclasses can override this method to perform custom initialization
	 * of the SessionFactory instance, creating it via the given Configuration
	 * object that got prepared by this LocalSessionFactoryBean.
	 * <p>The default implementation invokes Configuration's buildSessionFactory.
	 * A custom implementation could prepare the instance in a specific way,
	 * or use a custom SessionFactoryImpl subclass.
	 * @param config Configuration prepared by this LocalSessionFactoryBean
	 * @return the SessionFactory instance
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#buildSessionFactory
	 */
	protected SessionFactory newSessionFactory(Configuration config) throws HibernateException {
		return config.buildSessionFactory();
	}

	/**
	 * Return the Configuration object used to build the SessionFactory.
	 * Allows for access to configuration metadata stored there (rarely needed).
	 * @throws IllegalStateException if the Configuration object has not been initialized yet
	 */
	public final Configuration getConfiguration() {
		if (this.configuration == null) {
			throw new IllegalStateException("Configuration not initialized yet");
		}
		return this.configuration;
	}

	/**
	 * Executes schema update if requested.
	 * @see #setSchemaUpdate
	 * @see #updateDatabaseSchema()
	 */
	@Override
	protected void afterSessionFactoryCreation() throws Exception {
		if (this.schemaUpdate) {
			updateDatabaseSchema();
		}
	}

	/**
	 * Allows for schema export on shutdown.
	 */
	@Override
	public void destroy() throws HibernateException {
		DataSource dataSource = getDataSource();
		if (dataSource != null) {
			// Make given DataSource available for potential SchemaExport,
			// which unfortunately reinstantiates a ConnectionProvider.
			configTimeDataSourceHolder.set(dataSource);
		}
		try {
			super.destroy();
		}
		finally {
			if (dataSource != null) {
				// Reset DataSource holder.
				configTimeDataSourceHolder.remove();
			}
		}
	}


	/**
	 * Execute schema update script, determined by the Configuration object
	 * used for creating the SessionFactory. A replacement for Hibernate's
	 * SchemaUpdate class, for automatically executing schema update scripts
	 * on application startup. Can also be invoked manually.
	 * <p>Fetch the LocalSessionFactoryBean itself rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * {@code LocalSessionFactoryBean lsfb = (LocalSessionFactoryBean) ctx.getBean("&mySessionFactory");}.
	 * <p>Uses the SessionFactory that this bean generates for accessing a
	 * JDBC connection to perform the script.
	 * @throws DataAccessException in case of script execution errors
	 * @see #setSchemaUpdate
	 * @see org.hibernate.cfg.Configuration#generateSchemaUpdateScript
	 * @see org.hibernate.tool.hbm2ddl.SchemaUpdate
	 */
	public void updateDatabaseSchema() throws DataAccessException {
		logger.info("Updating database schema for Hibernate SessionFactory");
		DataSource dataSource = getDataSource();
		if (dataSource != null) {
			// Make given DataSource available for the schema update.
			configTimeDataSourceHolder.set(dataSource);
		}
		try {
			SessionFactory sessionFactory = getSessionFactory();
			final Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
			HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
			hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_NEVER);
			hibernateTemplate.execute(
				new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						@SuppressWarnings("deprecation")
						Connection con = session.connection();
						DatabaseMetadata metadata = new DatabaseMetadata(con, dialect);
						String[] sql = getConfiguration().generateSchemaUpdateScript(dialect, metadata);
						executeSchemaScript(con, sql);
						return null;
					}
				}
			);
		}
		finally {
			if (dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
		}
	}

	/**
	 * Execute schema creation script, determined by the Configuration object
	 * used for creating the SessionFactory. A replacement for Hibernate's
	 * SchemaValidator class, to be invoked after application startup.
	 * <p>Fetch the LocalSessionFactoryBean itself rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * {@code LocalSessionFactoryBean lsfb = (LocalSessionFactoryBean) ctx.getBean("&mySessionFactory");}.
	 * <p>Uses the SessionFactory that this bean generates for accessing a
	 * JDBC connection to perform the script.
	 * @throws DataAccessException in case of script execution errors
	 * @see org.hibernate.cfg.Configuration#validateSchema
	 * @see org.hibernate.tool.hbm2ddl.SchemaValidator
	 */
	public void validateDatabaseSchema() throws DataAccessException {
		logger.info("Validating database schema for Hibernate SessionFactory");
		DataSource dataSource = getDataSource();
		if (dataSource != null) {
			// Make given DataSource available for the schema update.
			configTimeDataSourceHolder.set(dataSource);
		}
		try {
			SessionFactory sessionFactory = getSessionFactory();
			final Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
			HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
			hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_NEVER);
			hibernateTemplate.execute(
				new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						@SuppressWarnings("deprecation")
						Connection con = session.connection();
						DatabaseMetadata metadata = new DatabaseMetadata(con, dialect, false);
						getConfiguration().validateSchema(dialect, metadata);
						return null;
					}
				}
			);
		}
		finally {
			if (dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
		}
	}

	/**
	 * Execute schema drop script, determined by the Configuration object
	 * used for creating the SessionFactory. A replacement for Hibernate's
	 * SchemaExport class, to be invoked on application setup.
	 * <p>Fetch the LocalSessionFactoryBean itself rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * {@code LocalSessionFactoryBean lsfb = (LocalSessionFactoryBean) ctx.getBean("&mySessionFactory");}.
	 * <p>Uses the SessionFactory that this bean generates for accessing a
	 * JDBC connection to perform the script.
	 * @throws org.springframework.dao.DataAccessException in case of script execution errors
	 * @see org.hibernate.cfg.Configuration#generateDropSchemaScript
	 * @see org.hibernate.tool.hbm2ddl.SchemaExport#drop
	 */
	public void dropDatabaseSchema() throws DataAccessException {
		logger.info("Dropping database schema for Hibernate SessionFactory");
		SessionFactory sessionFactory = getSessionFactory();
		final Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
		HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
		hibernateTemplate.execute(
			new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					@SuppressWarnings("deprecation")
					Connection con = session.connection();
					String[] sql = getConfiguration().generateDropSchemaScript(dialect);
					executeSchemaScript(con, sql);
					return null;
				}
			}
		);
	}

	/**
	 * Execute schema creation script, determined by the Configuration object
	 * used for creating the SessionFactory. A replacement for Hibernate's
	 * SchemaExport class, to be invoked on application setup.
	 * <p>Fetch the LocalSessionFactoryBean itself rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * {@code LocalSessionFactoryBean lsfb = (LocalSessionFactoryBean) ctx.getBean("&mySessionFactory");}.
	 * <p>Uses the SessionFactory that this bean generates for accessing a
	 * JDBC connection to perform the script.
	 * @throws DataAccessException in case of script execution errors
	 * @see org.hibernate.cfg.Configuration#generateSchemaCreationScript
	 * @see org.hibernate.tool.hbm2ddl.SchemaExport#create
	 */
	public void createDatabaseSchema() throws DataAccessException {
		logger.info("Creating database schema for Hibernate SessionFactory");
		DataSource dataSource = getDataSource();
		if (dataSource != null) {
			// Make given DataSource available for the schema update.
			configTimeDataSourceHolder.set(dataSource);
		}
		try {
			SessionFactory sessionFactory = getSessionFactory();
			final Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
			HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
			hibernateTemplate.execute(
				new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						@SuppressWarnings("deprecation")
						Connection con = session.connection();
						String[] sql = getConfiguration().generateSchemaCreationScript(dialect);
						executeSchemaScript(con, sql);
						return null;
					}
				}
			);
		}
		finally {
			if (dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
		}
	}

	/**
	 * Execute the given schema script on the given JDBC Connection.
	 * <p>Note that the default implementation will log unsuccessful statements
	 * and continue to execute. Override the {@code executeSchemaStatement}
	 * method to treat failures differently.
	 * @param con the JDBC Connection to execute the script on
	 * @param sql the SQL statements to execute
	 * @throws SQLException if thrown by JDBC methods
	 * @see #executeSchemaStatement
	 */
	protected void executeSchemaScript(Connection con, String[] sql) throws SQLException {
		if (sql != null && sql.length > 0) {
			boolean oldAutoCommit = con.getAutoCommit();
			if (!oldAutoCommit) {
				con.setAutoCommit(true);
			}
			try {
				Statement stmt = con.createStatement();
				try {
					for (String sqlStmt : sql) {
						executeSchemaStatement(stmt, sqlStmt);
					}
				}
				finally {
					JdbcUtils.closeStatement(stmt);
				}
			}
			finally {
				if (!oldAutoCommit) {
					con.setAutoCommit(false);
				}
			}
		}
	}

	/**
	 * Execute the given schema SQL on the given JDBC Statement.
	 * <p>Note that the default implementation will log unsuccessful statements
	 * and continue to execute. Override this method to treat failures differently.
	 * @param stmt the JDBC Statement to execute the SQL on
	 * @param sql the SQL statement to execute
	 * @throws SQLException if thrown by JDBC methods (and considered fatal)
	 */
	protected void executeSchemaStatement(Statement stmt, String sql) throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing schema statement: " + sql);
		}
		try {
			stmt.executeUpdate(sql);
		}
		catch (SQLException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unsuccessful schema statement: " + sql, ex);
			}
		}
	}

}
