/*
 * Copyright 2002-2011 the original author or authors.
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
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.EventListeners;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.transaction.JTATransactionFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for Spring's Hibernate {@code SessionFactory} {@code
 * *Builder} and {@code *FactoryBean} types. Provides functionality suitable for
 * setting up a shared Hibernate {@code SessionFactory} in a Spring application
 * context; the {@code SessionFactory} can then be passed to Hibernate-based
 * Repositories/DAOs via dependency injection.
 *
 * <p>Configuration settings can either be read from a
 * <a href="http://bit.ly/g6Lwqv">Hibernate XML file</a>, specified by the
 * {@linkplain #setConfigLocation configLocation} property, or completely via this
 * class. A typical local configuration consists of one or more {@linkplain
 * #setMappingResources mappingResources}, various {@linkplain
 * #setHibernateProperties hibernateProperties} (not strictly necessary), and a
 * {@linkplain #setDataSource dataSource} that the {@code SessionFactory} should
 * use. The latter can also be specified via Hibernate properties, but {@link
 * #setDataSource} supports any Spring-configured {@code DataSource}, instead of
 * relying on Hibernate's own connection providers.
 *
 * <p>This {@code SessionFactory} handling strategy is appropriate for most types
 * of applications, from Hibernate-only single database apps to ones that need
 * distributed transactions. Either a {@link HibernateTransactionManager} or {@link
 * org.springframework.transaction.jta.JtaTransactionManager JtaTransactionManager}
 * can be used for transaction demarcation, with the latter only necessary for
 * transactions which span multiple databases.
 *
 * <p>By default a transaction-aware {@code SessionFactory} proxy will be exposed,
 * letting data access code work with the plain Hibernate {@code SessionFactory}
 * and its {@code getCurrentSession()} method, while still being able to
 * participate in current Spring-managed transactions: with any transaction
 * management strategy, either local or JTA / EJB CMT, and any transaction
 * synchronization mechanism, either Spring or JTA. Furthermore,
 * {@code getCurrentSession()} will also seamlessly work with a request-scoped
 * {@code Session} managed by
 * {@link org.springframework.orm.hibernate3.support.OpenSessionInViewFilter} or
 * {@link org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor}.
 *
 * <p><b>Requires Hibernate 3.2 or later; tested against 3.2, 3.3, 3.5 and 3.6.</b>
 * Note that this factory will use "on_close" as default Hibernate connection
 * release mode, unless in the case of a {@linkplain #setJtaTransactionManager
 * jtaTransactionManager} being specified, as this is appropriate for most
 * Spring-based applications (in particular when using Spring's
 * {@code HibernateTransactionManager}).
 *
 * <p>Concrete {@code *Builder} subclasses are designed for use within Spring
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * classes, while {@code *FactoryBean} subclasses are designed for use when
 * configuring the container via Spring XML. See individual subclass documentation
 * for complete details.
 *
 * <p>It is common to use the subclasses mentioned above in conjunction with
 * Spring's {@link HibernateExceptionTranslator} and a
 * {@code PersistenceExceptionTranslationPostProcessor} in order to convert native
 * {@code HibernateException} types to Spring's {@link DataAccessException}
 * hierarchy. {@code *FactoryBean} automatically perform translation through a
 * built-in {@code HibernateExceptionTranslator}, but {@code @Configuration}
 * class {@code @Bean} methods that use {@code *Builder} subclasses should
 * manually register a {@code HibernateExceptionTranslator} {@code @Bean}.
 * When using either type of subclass you must manually register a
 * {@code PersistenceExceptionTranslationPostProcessor} bean.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1
 * @see SessionFactoryBuilder
 * @see LocalSessionFactoryBean
 * @see org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBuilder
 * @see org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @see org.springframework.orm.hibernate3.HibernateExceptionTranslator
 * @see HibernateTemplate#setSessionFactory
 * @see HibernateTransactionManager#setSessionFactory
 * @see #setExposeTransactionAwareSessionFactory
 * @see #setJtaTransactionManager
 * @see org.hibernate.SessionFactory#getCurrentSession
 * @see HibernateTransactionManager
 */
public abstract class SessionFactoryBuilderSupport<This extends SessionFactoryBuilderSupport<This>> {

	static final ThreadLocal<DataSource> configTimeDataSourceHolder =
			new ThreadLocal<DataSource>();

	private static final ThreadLocal<TransactionManager> configTimeTransactionManagerHolder =
			new ThreadLocal<TransactionManager>();

	private static final ThreadLocal<Object> configTimeRegionFactoryHolder =
			new ThreadLocal<Object>();

	private static final ThreadLocal<LobHandler> configTimeLobHandlerHolder =
			new ThreadLocal<LobHandler>();

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	@SuppressWarnings("unchecked")
	private final This instance = (This) this;

	private Class<? extends Configuration> configurationClass = getDefaultConfigurationClass();

	/** Lazily initialized during calls to {@link #getConfiguration()} */
	private Configuration configuration;

	private DataSource dataSource;

	private SessionFactory sessionFactory;

	private boolean schemaUpdate = false;

	private boolean exposeTransactionAwareSessionFactory = true;

	private boolean useTransactionAwareDataSource = false;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Resource[] mappingLocations;

	private Resource[] cacheableMappingLocations;

	private Resource[] mappingJarLocations;

	private Resource[] mappingDirectoryLocations;

	private TransactionManager jtaTransactionManager;

	private Object cacheRegionFactory;

	private LobHandler lobHandler;

	private Interceptor entityInterceptor;

	private NamingStrategy namingStrategy;

	private TypeDefinitionBean[] typeDefinitions;

	private FilterDefinition[] filterDefinitions;

	private Resource[] configLocations;

	private Properties hibernateProperties;

	private Properties entityCacheStrategies;

	private Properties collectionCacheStrategies;

	private Map<String, Object> eventListeners;

	private String[] mappingResources;

	/**
	 * Constructor for use when DataSource is not available as a Spring bean
	 * instance, i.e.: when DataSource configuration should be read from
	 * {@code hibernate.cfg.xml}
	 * @see SessionFactoryBuilderSupport#AbstractSessionFactoryBuilder(DataSource)
	 */
	public SessionFactoryBuilderSupport() {
	}

	public SessionFactoryBuilderSupport(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Return the default Configuration type used by this instance.
	 * May be overridden with {@link #setConfigurationClass(Class)}.
	 * @see #setConfigurationClass(Class)
	 */
	protected abstract Class<? extends Configuration> getDefaultConfigurationClass();

	/**
	 * Build the underlying Hibernate SessionFactory.
	 * @return the raw SessionFactory (potentially to be wrapped with a
	 * transaction-aware proxy before it is exposed to the application)
	 * @throws Exception in case of initialization failure
	 */
	public SessionFactory buildSessionFactory() throws Exception {
		this.sessionFactory = wrapSessionFactoryIfNecessary(doBuildSessionFactory());
		afterSessionFactoryCreation();
		return this.sessionFactory;
	}

	/**
	 * Populate the underlying {@code Configuration} instance with the various
	 * properties of this builder. Customization may be performed through
	 * {@code pre*} and {@code post*} methods.
	 * @see #preBuildSessionFactory()
	 * @see #postProcessMappings()
	 * @see #postBuildSessionFactory()
	 * @see #postBuildSessionFactory()
	 */
	protected final SessionFactory doBuildSessionFactory() throws Exception {
		initializeConfigurationIfNecessary();

		if (this.dataSource != null) {
			// Make given DataSource available for SessionFactory configuration.
			configTimeDataSourceHolder.set(this.dataSource);
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

		try {
			if (isExposeTransactionAwareSessionFactory()) {
				// Set Hibernate 3.1+ CurrentSessionContext implementation,
				// providing the Spring-managed Session as current Session.
				// Can be overridden by a custom value for the corresponding Hibernate property.
				this.configuration.setProperty(
						Environment.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
			}

			if (this.jtaTransactionManager != null) {
				// Set Spring-provided JTA TransactionManager as Hibernate property.
				this.configuration.setProperty(
						Environment.TRANSACTION_STRATEGY, JTATransactionFactory.class.getName());
				this.configuration.setProperty(
						Environment.TRANSACTION_MANAGER_STRATEGY, LocalTransactionManagerLookup.class.getName());
			}
			else {
				// Makes the Hibernate Session aware of the presence of a Spring-managed transaction.
				// Also sets connection release mode to ON_CLOSE by default.
				this.configuration.setProperty(
						Environment.TRANSACTION_STRATEGY, SpringTransactionFactory.class.getName());
			}

			if (this.entityInterceptor != null) {
				// Set given entity interceptor at SessionFactory level.
				this.configuration.setInterceptor(this.entityInterceptor);
			}

			if (this.namingStrategy != null) {
				// Pass given naming strategy to Hibernate Configuration.
				this.configuration.setNamingStrategy(this.namingStrategy);
			}

			if (this.typeDefinitions != null) {
				// Register specified Hibernate type definitions.
				// Use reflection for compatibility with both Hibernate 3.3 and 3.5:
				// the returned Mappings object changed from a class to an interface.
				Method createMappings = Configuration.class.getMethod("createMappings");
				Method addTypeDef = createMappings.getReturnType().getMethod(
						"addTypeDef", String.class, String.class, Properties.class);
				Object mappings = ReflectionUtils.invokeMethod(createMappings, this.configuration);
				for (TypeDefinitionBean typeDef : this.typeDefinitions) {
					ReflectionUtils.invokeMethod(addTypeDef, mappings,
							typeDef.getTypeName(), typeDef.getTypeClass(), typeDef.getParameters());
				}
			}

			if (this.filterDefinitions != null) {
				// Register specified Hibernate FilterDefinitions.
				for (FilterDefinition filterDef : this.filterDefinitions) {
					this.configuration.addFilterDefinition(filterDef);
				}
			}

			if (this.configLocations != null) {
				for (Resource resource : this.configLocations) {
					// Load Hibernate configuration from given location.
					this.configuration.configure(resource.getURL());
				}
			}

			if (this.hibernateProperties != null) {
				// Add given Hibernate properties to Configuration.
				this.configuration.addProperties(this.hibernateProperties);
			}

			if (dataSource != null) {
				Class<?> providerClass = LocalDataSourceConnectionProvider.class;
				if (isUseTransactionAwareDataSource() || dataSource instanceof TransactionAwareDataSourceProxy) {
					providerClass = TransactionAwareDataSourceConnectionProvider.class;
				}
				else if (this.configuration.getProperty(Environment.TRANSACTION_MANAGER_STRATEGY) != null) {
					providerClass = LocalJtaDataSourceConnectionProvider.class;
				}
				// Set Spring-provided DataSource as Hibernate ConnectionProvider.
				this.configuration.setProperty(Environment.CONNECTION_PROVIDER, providerClass.getName());
			}

			if (this.cacheRegionFactory != null) {
				// Expose Spring-provided Hibernate RegionFactory.
				this.configuration.setProperty(Environment.CACHE_REGION_FACTORY,
						"org.springframework.orm.hibernate3.LocalRegionFactoryProxy");
			}

			if (this.mappingResources != null) {
				// Register given Hibernate mapping definitions, contained in resource files.
				for (String mapping : this.mappingResources) {
					Resource resource = new ClassPathResource(mapping.trim(), this.beanClassLoader);
					this.configuration.addInputStream(resource.getInputStream());
				}
			}

			if (this.mappingLocations != null) {
				// Register given Hibernate mapping definitions, contained in resource files.
				for (Resource resource : this.mappingLocations) {
					this.configuration.addInputStream(resource.getInputStream());
				}
			}

			if (this.cacheableMappingLocations != null) {
				// Register given cacheable Hibernate mapping definitions, read from the file system.
				for (Resource resource : this.cacheableMappingLocations) {
					this.configuration.addCacheableFile(resource.getFile());
				}
			}

			if (this.mappingJarLocations != null) {
				// Register given Hibernate mapping definitions, contained in jar files.
				for (Resource resource : this.mappingJarLocations) {
					this.configuration.addJar(resource.getFile());
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
					this.configuration.addDirectory(file);
				}
			}

			// Tell Hibernate to eagerly compile the mappings that we registered,
			// for availability of the mapping information in further processing.
			postProcessMappings();
			this.configuration.buildMappings();

			if (this.entityCacheStrategies != null) {
				// Register cache strategies for mapped entities.
				for (Enumeration<?> classNames = this.entityCacheStrategies.propertyNames(); classNames.hasMoreElements();) {
					String className = (String) classNames.nextElement();
					String[] strategyAndRegion =
							StringUtils.commaDelimitedListToStringArray(this.entityCacheStrategies.getProperty(className));
					if (strategyAndRegion.length > 1) {
						// method signature declares return type as Configuration on Hibernate 3.6
						// but as void on Hibernate 3.3 and 3.5
						Method setCacheConcurrencyStrategy = Configuration.class.getMethod(
								"setCacheConcurrencyStrategy", String.class, String.class, String.class);
						ReflectionUtils.invokeMethod(setCacheConcurrencyStrategy, this.configuration,
								className, strategyAndRegion[0], strategyAndRegion[1]);
					}
					else if (strategyAndRegion.length > 0) {
						this.configuration.setCacheConcurrencyStrategy(className, strategyAndRegion[0]);
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
						this.configuration.setCollectionCacheConcurrencyStrategy(collRole, strategyAndRegion[0], strategyAndRegion[1]);
					}
					else if (strategyAndRegion.length > 0) {
						this.configuration.setCollectionCacheConcurrencyStrategy(collRole, strategyAndRegion[0]);
					}
				}
			}

			if (this.eventListeners != null) {
				// Register specified Hibernate event listeners.
				for (Map.Entry<String, Object> entry : this.eventListeners.entrySet()) {
					String listenerType = entry.getKey();
					Object listenerObject = entry.getValue();
					if (listenerObject instanceof Collection) {
						Collection<?> listeners = (Collection<?>) listenerObject;
						EventListeners listenerRegistry = this.configuration.getEventListeners();
						Object[] listenerArray =
								(Object[]) Array.newInstance(listenerRegistry.getListenerClassFor(listenerType), listeners.size());
						listenerArray = listeners.toArray(listenerArray);
						this.configuration.setListeners(listenerType, listenerArray);
					}
					else {
						this.configuration.setListener(listenerType, listenerObject);
					}
				}
			}

			preBuildSessionFactory();

			// Perform custom post-processing in subclasses.
			postProcessConfiguration();

			// Build SessionFactory instance.
			logger.info("Building new Hibernate SessionFactory");

			return this.sessionFactory = newSessionFactory();
		}
		finally {
			if (dataSource != null) {
				SessionFactoryBuilderSupport.configTimeDataSourceHolder.remove();
			}
			if (this.jtaTransactionManager != null) {
				SessionFactoryBuilderSupport.configTimeTransactionManagerHolder.remove();
			}
			if (this.cacheRegionFactory != null) {
				SessionFactoryBuilderSupport.configTimeRegionFactoryHolder.remove();
			}
			if (this.lobHandler != null) {
				SessionFactoryBuilderSupport.configTimeLobHandlerHolder.remove();
			}

			postBuildSessionFactory();
		}
	}

	/**
	 * Return the Hibernate {@link Configuration} object used to build the {@link SessionFactory}.
	 * Allows access to configuration metadata stored there (rarely needed).
	 * <p>Favor use of {@link #doWithConfiguration(HibernateConfigurationCallback)}
	 * in order to customize the internal {@code Configuration} object.
	 * @see #doWithConfiguration(HibernateConfigurationCallback)
	 * @throws IllegalStateException if the Configuration object has not been initialized yet
	 */
	protected final Configuration getConfiguration() {
		initializeConfigurationIfNecessary();
		return this.configuration;
	}

	private void initializeConfigurationIfNecessary() {
		if (this.configuration == null) {
			this.configuration = newConfiguration();
		}
	}

	/**
	 * Subclasses can override this method to perform custom initialization
	 * of the Configuration instance used for SessionFactory creation.
	 * The properties of this Builder will be applied to
	 * the Configuration object that gets returned here.
	 * <p>The default implementation creates a new Configuration instance.
	 * A custom implementation could prepare the instance in a specific way,
	 * or use a custom Configuration subclass.
	 * @return the Configuration instance
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#Configuration()
	 */
	/**
	 * Instantiate and return an instance of the {@link Configuration} class
	 * type for this builder. Subclasses may override in order to customize
	 * instantiation logic, but clients should use {@link #doWithConfiguration}
	 * to directly customize the {@code Configuration} instance.
	 * @see #getDefaultConfigurationClass()
	 * @see #setConfigurationClass(Class)
	 * @see #getConfiguration()
	 * @see #doWithConfiguration(HibernateConfigurationCallback)
	 */
	protected Configuration newConfiguration() {
		return BeanUtils.instantiateClass(this.configurationClass);
	}

	/**
	 * Return the exposed SessionFactory.
	 * Will throw an exception if not initialized yet.
	 * @return the SessionFactory (never <code>null</code>)
	 * @throws IllegalStateException if the SessionFactory has not been initialized yet
	 */
	protected final SessionFactory getSessionFactory() {
		if (this.sessionFactory == null) {
			throw new IllegalStateException("SessionFactory not initialized yet. Have you called buildSessionFactory()?");
		}
		return this.sessionFactory;
	}

	/**
	 * Allow additional population of the underlying {@code Configuration}
	 * instance. Called during {@link #doBuildSessionFactory()}.
	 */
	protected void preBuildSessionFactory() {
	}

	/**
	 * Allow cleaning up resources, thread locals, etc after building the
	 * {@code SessionFactory}. Called during the {@code finally} block of
	 * {@link #doBuildSessionFactory()}.
	 */
	protected void postBuildSessionFactory() {
	}

	/**
	 * Wrap the given SessionFactory with a proxy, if demanded.
	 * <p>The default implementation simply returns the given SessionFactory as-is.
	 * Subclasses may override this to implement transaction awareness through
	 * a SessionFactory proxy, for example.
	 * @param rawSf the raw SessionFactory as built by {@link #buildSessionFactory()}
	 * @return the SessionFactory reference to expose
	 * @see #buildSessionFactory()
	 */
	protected SessionFactory wrapSessionFactoryIfNecessary(SessionFactory rawSf) {
		return rawSf;
	}

	/**
	 * Subclasses can override this method to perform custom initialization
	 * of the SessionFactory instance, creating it via the given Configuration
	 * object that got prepared by this Builder.
	 * <p>The default implementation invokes Configuration's buildSessionFactory.
	 * A custom implementation could prepare the instance in a specific way,
	 * or use a custom SessionFactoryImpl subclass.
	 * @param config Configuration prepared by this Builder
	 * @return the SessionFactory instance
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#buildSessionFactory
	 */
	protected SessionFactory newSessionFactory() throws HibernateException {
		return this.configuration.buildSessionFactory();
	}

	/**
	 * To be implemented by subclasses that want to to register further mappings
	 * on the Configuration object after this FactoryBean registered its specified
	 * mappings.
	 * <p>Invoked <i>before</i> the <code>Configuration.buildMappings()</code> call,
	 * so that it can still extend and modify the mapping information.
	 * @param config the current Configuration object
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#buildMappings()
	 */
	protected void postProcessMappings() throws HibernateException {
	}

	/**
	 * To be implemented by subclasses that want to to perform custom
	 * post-processing of the Configuration object after this FactoryBean
	 * performed its default initialization.
	 * <p>Invoked <i>after</i> the <code>Configuration.buildMappings()</code> call,
	 * so that it can operate on the completed and fully parsed mapping information.
	 * @throws HibernateException in case of Hibernate initialization errors
	 * @see org.hibernate.cfg.Configuration#buildMappings()
	 */
	protected void postProcessConfiguration() throws HibernateException {
	}

	/**
	 * Hook that allows post-processing after the SessionFactory has been
	 * successfully created. The SessionFactory is already available through
	 * <code>getSessionFactory()</code> at this point.
	 * <p>This implementation is empty.
	 * @throws Exception in case of initialization failure
	 * @see #getSessionFactory()
	 */
	/**
	 * Executes schema update if requested.
	 * @see #setSchemaUpdate
	 * @see #updateDatabaseSchema()
	 */
	protected void afterSessionFactoryCreation() throws Exception {
		if (this.schemaUpdate) {
			updateDatabaseSchema();
		}
	}

	/**
	 * Hook that allows shutdown processing before the SessionFactory
	 * will be closed. The SessionFactory is still available through
	 * <code>getSessionFactory()</code> at this point.
	 * <p>This implementation is empty.
	 * @see #getSessionFactory()
	 */
	protected void beforeSessionFactoryDestruction() {
	}

	/**
	 * Execute schema creation script, determined by the Configuration object
	 * used for creating the SessionFactory. A replacement for Hibernate's
	 * SchemaExport class, to be invoked on application setup.
	 * <p>Fetch the FactoryBean itself by rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * <pre class="code">
	 * LocalSessionFactoryBean sfb = ctx.getBean("&mySessionFactory", LocalSessionFactoryBean.class);
	 * </pre>
	 * or in the case of {@code @Configuration} class usage, register the
	 * SessionFactoryBuilder as a {@code @Bean} and fetch it by type:
	 * <pre class="code">
	 * SessionFactoryBuilder sfb = ctx.getBean(SessionFactoryBuilder.class);
	 * </pre>
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
			SessionFactoryBuilderSupport.configTimeDataSourceHolder.set(dataSource);
		}
		try {
			HibernateTemplate hibernateTemplate = new HibernateTemplate(getSessionFactory());
			hibernateTemplate.execute(
				new HibernateCallback<Object>() {
					@SuppressWarnings("deprecation")
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						Dialect dialect = ((SessionFactoryImplementor)session.getSessionFactory()).getDialect();
						String[] sql = configuration.generateSchemaCreationScript(dialect);
						executeSchemaScript(session.connection(), sql);
						return null;
					}
				}
			);
		}
		finally {
			if (dataSource != null) {
				SessionFactoryBuilderSupport.configTimeDataSourceHolder.remove();
			}
		}
	}

	/**
	 * Execute schema update script, determined by the Configuration object
	 * used for creating the SessionFactory. A replacement for Hibernate's
	 * SchemaUpdate class, for automatically executing schema update scripts
	 * on application startup. Can also be invoked manually.
	 * <p>Fetch the FactoryBean itself by rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * <pre class="code">
	 * LocalSessionFactoryBean sfb = ctx.getBean("&mySessionFactory", LocalSessionFactoryBean.class);
	 * </pre>
	 * or in the case of {@code @Configuration} class usage, register the
	 * SessionFactoryBuilder as a {@code @Bean} and fetch it by type:
	 * <pre class="code">
	 * SessionFactoryBuilder sfb = ctx.getBean(SessionFactoryBuilder.class);
	 * </pre>
	 * <p>Uses the SessionFactory that this bean generates for accessing a
	 * JDBC connection to perform the script.
	 * @throws DataAccessException in case of script execution errors
	 * @see #setSchemaUpdate
	 * @see org.hibernate.cfg.Configuration#generateSchemaUpdateScript
	 * @see org.hibernate.tool.hbm2ddl.SchemaUpdate
	 */
	public void updateDatabaseSchema() throws DataAccessException {
		logger.info("Updating database schema for Hibernate SessionFactory");
		if (this.dataSource != null) {
			// Make given DataSource available for the schema update.
			configTimeDataSourceHolder.set(this.dataSource);
		}
		try {
			HibernateTemplate hibernateTemplate = new HibernateTemplate(getSessionFactory());
			hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_NEVER);
			hibernateTemplate.execute(
				new HibernateCallback<Void>() {
					public Void doInHibernate(Session session) throws HibernateException, SQLException {
						@SuppressWarnings("deprecation")
						Connection conn = session.connection();
						Dialect dialect = ((SessionFactoryImplementor)session.getSessionFactory()).getDialect();
						DatabaseMetadata metadata = new DatabaseMetadata(conn, dialect);
						String[] sql = configuration.generateSchemaUpdateScript(dialect, metadata);
						executeSchemaScript(conn, sql);
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
	 * <p>Fetch the FactoryBean itself by rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * <pre class="code">
	 * LocalSessionFactoryBean sfb = ctx.getBean("&mySessionFactory", LocalSessionFactoryBean.class);
	 * </pre>
	 * or in the case of {@code @Configuration} class usage, register the
	 * SessionFactoryBuilder as a {@code @Bean} and fetch it by type:
	 * <pre class="code">
	 * SessionFactoryBuilder sfb = ctx.getBean(SessionFactoryBuilder.class);
	 * </pre>
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
			SessionFactoryBuilderSupport.configTimeDataSourceHolder.set(dataSource);
		}
		try {
			HibernateTemplate hibernateTemplate = new HibernateTemplate(getSessionFactory());
			hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_NEVER);
			hibernateTemplate.execute(
				new HibernateCallback<Object>() {
					@SuppressWarnings("deprecation")
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						Dialect dialect = ((SessionFactoryImplementor)session.getSessionFactory()).getDialect();
						DatabaseMetadata metadata = new DatabaseMetadata(session.connection(), dialect);
						configuration.validateSchema(dialect, metadata);
						return null;
					}
				}
			);
		}
		finally {
			if (dataSource != null) {
				SessionFactoryBuilderSupport.configTimeDataSourceHolder.remove();
			}
		}
	}

	/**
	 * Execute schema drop script, determined by the Configuration object
	 * used for creating the SessionFactory. A replacement for Hibernate's
	 * SchemaExport class, to be invoked on application setup.
	 * <p>Fetch the FactoryBean itself by rather than the exposed
	 * SessionFactory to be able to invoke this method, e.g. via
	 * <pre class="code">
	 * LocalSessionFactoryBean sfb = ctx.getBean("&mySessionFactory", LocalSessionFactoryBean.class);
	 * </pre>
	 * or in the case of {@code @Configuration} class usage, register the
	 * SessionFactoryBuilder as a {@code @Bean} and fetch it by type:
	 * <pre class="code">
	 * SessionFactoryBuilder sfb = ctx.getBean(SessionFactoryBuilder.class);
	 * </pre>
	 * <p>Uses the SessionFactory that this bean generates for accessing a
	 * JDBC connection to perform the script.
	 * @throws org.springframework.dao.DataAccessException in case of script execution errors
	 * @see org.hibernate.cfg.Configuration#generateDropSchemaScript
	 * @see org.hibernate.tool.hbm2ddl.SchemaExport#drop
	 */
	public void dropDatabaseSchema() throws DataAccessException {
		logger.info("Dropping database schema for Hibernate SessionFactory");
		HibernateTemplate hibernateTemplate = new HibernateTemplate(getSessionFactory());
		hibernateTemplate.execute(
			new HibernateCallback<Object>() {
				@SuppressWarnings("deprecation")
				public Void doInHibernate(Session session) throws HibernateException, SQLException {
					Dialect dialect = ((SessionFactoryImplementor)session.getSessionFactory()).getDialect();
					String[] sql = configuration.generateDropSchemaScript(dialect);
					executeSchemaScript(session.connection(), sql);
					return null;
				}
			}
		);
	}

	/**
	 * Execute the given schema script on the given JDBC Connection.
	 * <p>Note that the default implementation will log unsuccessful statements
	 * and continue to execute. Override the <code>executeSchemaStatement</code>
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

	/**
	 * Specify the Hibernate Configuration class to use.
	 * Default is {@code org.hibernate.cfg.Configuration}; any subclass of
	 * this default Hibernate {@code Configuration} class can be specified.
	 * <p>Can be set to {@code org.hibernate.cfg.AnnotationConfiguration} for
	 * using Hibernate3 annotation support (initially only available as
	 * alpha download separate from the main Hibernate3 distribution).
	 * <p>Annotated packages and annotated classes can be specified via the
	 * corresponding tags in "hibernate.cfg.xml" then, so this will usually
	 * be combined with a "configLocation" property that points at such a
	 * standard Hibernate configuration file.
	 * @see #setConfigLocation
	 * @see #doWithConfiguration
	 * @see org.hibernate.cfg.Configuration
	 * @see org.hibernate.cfg.AnnotationConfiguration
	 */
	public final This setConfigurationClass(Class<? extends Configuration> configurationClass) {
		Assert.notNull(configurationClass, "configurationClass must not be null");
		if (this.configuration != null) {
			throw new IllegalStateException(
					"setConfigurationClass() must be called before the internal " +
					"Configuration instance has been created. Have you perhaps called " +
					"getConfiguration() or doWithConfiguration() earlier than your call " +
					"to setConfiguration()?");
		}
		this.configurationClass = configurationClass;
		return this.instance;
	}

	/**
	 * Use the given {@link HibernateConfigurationCallback} instance to configure
	 * this {@code Builder}'s underlying Hibernate {@code Configuration} object.
	 * <p>The {@code HibernateConfigurationCallback} type may be parameterized to
	 * {@code org.hibernate.cfg.Configuration} (the default), or {@code
	 * org.hibernate.cfg.AnnotationConfiguration} if running a Hibernate version
	 * less than 3.6. Otherwise, may be parameterized to any custom {@code Configuration}
	 * class provided to {@link #setConfigurationClass}
	 * @throws Exception propagating any exception thrown during operations against
	 * the underlying {@code Configuration} object. {@code @Bean} methods should
	 * declare {@code throws Exception} when using this method, allowing the Spring
	 * container to deal with it and fail appropriately.
	 * @see #setConfigurationClass(Class)
	 */
	@SuppressWarnings("unchecked")
	public <C extends Configuration> This doWithConfiguration(HibernateConfigurationCallback<C> callback)
			throws Exception {
		callback.configure((C)this.getConfiguration());
		return this.instance;
	}

	/**
	 * Set the DataSource to be used by the SessionFactory.
	 * If set, this will override corresponding settings in Hibernate properties.
	 * <p>If this is set, the Hibernate settings should not define
	 * a connection provider to avoid meaningless double configuration.
	 * <p>If using HibernateTransactionManager as transaction strategy, consider
	 * proxying your target DataSource with a LazyConnectionDataSourceProxy.
	 * This defers fetching of an actual JDBC Connection until the first JDBC
	 * Statement gets executed, even within JDBC transactions (as performed by
	 * HibernateTransactionManager). Such lazy fetching is particularly beneficial
	 * for read-only operations, in particular if the chances of resolving the
	 * result in the second-level cache are high.
	 * <p>As JTA and transactional JNDI DataSources already provide lazy enlistment
	 * of JDBC Connections, LazyConnectionDataSourceProxy does not add value with
	 * JTA (i.e. Spring's JtaTransactionManager) as transaction strategy.
	 * @see #setUseTransactionAwareDataSource
	 * @see HibernateTransactionManager
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
	 */
	public This setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		return this.instance;
	}

	/**
	 * Return the DataSource to be used by the SessionFactory.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
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
	 * Set the Hibernate RegionFactory to use for the SessionFactory.
	 * Allows for using a Spring-managed RegionFactory instance.
	 * <p>As of Hibernate 3.3, this is the preferred mechanism for configuring
	 * caches, superseding the {@link #setCacheProvider CacheProvider SPI}.
	 * For Hibernate 3.2 compatibility purposes, the accepted reference is of type
	 * Object: the actual type is <code>org.hibernate.cache.RegionFactory</code>.
	 * <p>Note: If this is set, the Hibernate settings should not define a
	 * cache provider to avoid meaningless double configuration.
	 * @see org.hibernate.cache.RegionFactory
	 */
	public This setCacheRegionFactory(Object cacheRegionFactory) {
		this.cacheRegionFactory = cacheRegionFactory;
		return instance;
	}

	protected Object getCacheRegionFactory() {
		return this.cacheRegionFactory;
	}

	/**
	 * Set whether to expose a transaction-aware current Session from the
	 * SessionFactory's <code>getCurrentSession()</code> method, returning the
	 * Session that's associated with the current Spring-managed transaction, if any.
	 * <p>Default is "true", letting data access code work with the plain
	 * Hibernate SessionFactory and its <code>getCurrentSession()</code> method,
	 * while still being able to participate in current Spring-managed transactions:
	 * with any transaction management strategy, either local or JTA / EJB CMT,
	 * and any transaction synchronization mechanism, either Spring or JTA.
	 * Furthermore, <code>getCurrentSession()</code> will also seamlessly work with
	 * a request-scoped Session managed by OpenSessionInViewFilter/Interceptor.
	 * <p>Turn this flag off to expose the plain Hibernate SessionFactory with
	 * Hibernate's default <code>getCurrentSession()</code> behavior, supporting
	 * plain JTA synchronization only. Alternatively, simply override the
	 * corresponding Hibernate property "hibernate.current_session_context_class".
	 * @see SpringSessionContext
	 * @see org.hibernate.SessionFactory#getCurrentSession()
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 * @see HibernateTransactionManager
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewFilter
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
	 */
	public This setExposeTransactionAwareSessionFactory(boolean exposeTransactionAwareSessionFactory) {
		this.exposeTransactionAwareSessionFactory = exposeTransactionAwareSessionFactory;
		return this.instance;
	}

	/**
	 * Return whether to expose a transaction-aware proxy for the SessionFactory.
	 */
	protected boolean isExposeTransactionAwareSessionFactory() {
		return this.exposeTransactionAwareSessionFactory;
	}

	/**
	 * Set whether to use a transaction-aware DataSource for the SessionFactory,
	 * i.e. whether to automatically wrap the passed-in DataSource with Spring's
	 * TransactionAwareDataSourceProxy.
	 * <p>Default is "false": SessionFactoryBuilder types are usually used with Spring's
	 * HibernateTransactionManager or JtaTransactionManager, both of which work nicely
	 * on a plain JDBC DataSource. Hibernate Sessions and their JDBC Connections are
	 * fully managed by the Hibernate/JTA transaction infrastructure in such a scenario.
	 * <p>If you switch this flag to "true", Spring's Hibernate access will be able to
	 * <i>participate in JDBC-based transactions managed outside of Hibernate</i>
	 * (for example, by Spring's DataSourceTransactionManager). This can be convenient
	 * if you need a different local transaction strategy for another O/R mapping tool,
	 * for example, but still want Hibernate access to join into those transactions.
	 * <p>A further benefit of this option is that <i>plain Sessions opened directly
	 * via the SessionFactory</i>, outside of Spring's Hibernate support, will still
	 * participate in active Spring-managed transactions. However, consider using
	 * Hibernate's <code>getCurrentSession()</code> method instead (see javadoc of
	 * "exposeTransactionAwareSessionFactory" property).
	 * <p><b>WARNING:</b> When using a transaction-aware JDBC DataSource in combination
	 * with OpenSessionInViewFilter/Interceptor, whether participating in JTA or
	 * external JDBC-based transactions, it is strongly recommended to set Hibernate's
	 * Connection release mode to "after_transaction" or "after_statement", which
	 * guarantees proper Connection handling in such a scenario. In contrast to that,
	 * HibernateTransactionManager generally requires release mode "on_close".
	 * <p>Note: If you want to use Hibernate's Connection release mode "after_statement"
	 * with a DataSource specified on this SessionFactoryBuilder (for example, a
	 * JTA-aware DataSource fetched from JNDI), switch this setting to "true".
	 * Otherwise, the ConnectionProvider used underneath will vote against aggressive
	 * release and thus silently switch to release mode "after_transaction".
	 * @see #setDataSource
	 * @see #setExposeTransactionAwareSessionFactory
	 * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewFilter
	 * @see org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
	 * @see HibernateTransactionManager
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	public This setUseTransactionAwareDataSource(boolean useTransactionAwareDataSource) {
		this.useTransactionAwareDataSource = useTransactionAwareDataSource;
		return instance;
	}

	/**
	 * Return whether to use a transaction-aware DataSource for the SessionFactory.
	 */
	protected boolean isUseTransactionAwareDataSource() {
		return this.useTransactionAwareDataSource;
	}

	/**
	 * Set the JTA TransactionManager to be used for Hibernate's
	 * TransactionManagerLookup. Allows for using a Spring-managed
	 * JTA TransactionManager for Hibernate's cache synchronization.
	 * <p>Note: If this is set, the Hibernate settings should not define a
	 * transaction manager lookup to avoid meaningless double configuration.
	 * @see LocalTransactionManagerLookup
	 */
	public This setJtaTransactionManager(TransactionManager jtaTransactionManager) {
		this.jtaTransactionManager = jtaTransactionManager;
		return instance;
	}

	/**
	 * Set whether to execute a schema update after SessionFactory initialization.
	 * <p>For details on how to make schema update scripts work, see the Hibernate
	 * documentation, as this class leverages the same schema update script support
	 * in org.hibernate.cfg.Configuration as Hibernate's own SchemaUpdate tool.
	 * @see org.hibernate.cfg.Configuration#generateSchemaUpdateScript
	 * @see org.hibernate.tool.hbm2ddl.SchemaUpdate
	 */
	public This setSchemaUpdate(boolean schemaUpdate) {
		this.schemaUpdate = schemaUpdate;
		return this.instance;
	}

	/**
	 * Set the beanClassLoader for this instance. Not named {@code setBeanClassLoader}
	 * to allow subclasses to implement {@code BeanClassLoaderAware} without violating
	 * this method's signature. Any such implementation should simply delegate to this
	 * method.
	 * @see #getBeanClassLoader()
	 */
	public This setClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		return this.instance;
	}

	/**
	 * @see #setClassLoader(ClassLoader)
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
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
	public This setMappingResources(String[] mappingResources) {
		this.mappingResources = mappingResources;
		return instance;
	}

	/**
	 * Set locations of jar files that contain Hibernate mapping resources,
	 * like "WEB-INF/lib/example.hbm.jar".
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see org.hibernate.cfg.Configuration#addJar(java.io.File)
	 */
	public This setMappingJarLocations(Resource[] mappingJarLocations) {
		this.mappingJarLocations = mappingJarLocations;
		return instance;
	}

	/**
	 * Set locations of directories that contain Hibernate mapping resources,
	 * like "WEB-INF/mappings".
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see org.hibernate.cfg.Configuration#addDirectory(java.io.File)
	 */
	public This setMappingDirectoryLocations(Resource[] mappingDirectoryLocations) {
		this.mappingDirectoryLocations = mappingDirectoryLocations;
		return instance;
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
	public This setMappingLocations(Resource[] mappingLocations) {
		this.mappingLocations = mappingLocations;
		return instance;
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
	public This setHibernateProperties(Properties hibernateProperties) {
		this.hibernateProperties = hibernateProperties;
		return instance;
	}

	/**
	 * Set a Hibernate entity interceptor that allows to inspect and change
	 * property values before writing to and reading from the database.
	 * Will get applied to any new Session created by this factory.
	 * <p>Such an interceptor can either be set at the SessionFactory level, i.e. on
	 * SessionFactoryBuilder, or at the Session level, i.e. on HibernateTemplate,
	 * HibernateInterceptor, and HibernateTransactionManager. It's preferable to set
	 * it on SessionFactoryBuilder or HibernateTransactionManager to avoid repeated
	 * configuration and guarantee consistent behavior in transactions.
	 * @see HibernateTemplate#setEntityInterceptor
	 * @see HibernateInterceptor#setEntityInterceptor
	 * @see HibernateTransactionManager#setEntityInterceptor
	 * @see org.hibernate.cfg.Configuration#setInterceptor
	 */
	public This setEntityInterceptor(Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
		return instance;
	}

	/**
	 * Set a Hibernate NamingStrategy for the SessionFactory, determining the
	 * physical column and table names given the info in the mapping document.
	 * @see org.hibernate.cfg.Configuration#setNamingStrategy
	 */
	public This setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
		return instance;
	}

	/**
	 * Specify the cache strategies for entities (persistent classes or named entities).
	 * This configuration setting corresponds to the {@code <class-cache>} entry
	 * in the "hibernate.cfg.xml" configuration format.
	 * <p>For example:
	 * <pre class="code">
	 * {@code
	 * <property name="entityCacheStrategies">
	 *   <props>
	 *     <prop key="com.mycompany.Customer">read-write</prop>
	 *     <prop key="com.mycompany.Product">read-only,myRegion</prop>
	 *   </props>
	 * </property>}</pre>
	 * @param entityCacheStrategies properties that define entity cache strategies,
	 * with class names as keys and cache concurrency strategies as values
	 * @see org.hibernate.cfg.Configuration#setCacheConcurrencyStrategy(String, String)
	 */
	public This setEntityCacheStrategies(Properties entityCacheStrategies) {
		this.entityCacheStrategies = entityCacheStrategies;
		return instance;
	}

	/**
	 * Specify the cache strategies for persistent collections (with specific roles).
	 * This configuration setting corresponds to the {@code <collection-cache>} entry
	 * in the "hibernate.cfg.xml" configuration format.
	 * <p>For example:
	 * <pre class="code">
	 * {@code
	 * <property name="collectionCacheStrategies">
	 *   <props>
	 *     <prop key="com.mycompany.Order.items">read-write</prop>
	 *     <prop key="com.mycompany.Product.categories">read-only,myRegion</prop>
	 *   </props>
	 * </property>}</pre>
	 * @param collectionCacheStrategies properties that define collection cache strategies,
	 * with collection roles as keys and cache concurrency strategies as values
	 * @see org.hibernate.cfg.Configuration#setCollectionCacheConcurrencyStrategy(String, String)
	 */
	public This setCollectionCacheStrategies(Properties collectionCacheStrategies) {
		this.collectionCacheStrategies = collectionCacheStrategies;
		return instance;
	}

	/**
	 * Specify the Hibernate event listeners to register, with listener types
	 * as keys and listener objects as values. Instead of a single listener object,
	 * you can also pass in a list or set of listeners objects as value.
	 * <p>See the Hibernate documentation for further details on listener types
	 * and associated listener interfaces.
	 * @param eventListeners Map with listener type Strings as keys and
	 * listener objects as values
	 * @see org.hibernate.cfg.Configuration#setListener(String, Object)
	 */
	public This setEventListeners(Map<String, Object> eventListeners) {
		this.eventListeners = eventListeners;
		return this.instance;
	}

	/**
	 * Specify the Hibernate FilterDefinitions to register with the SessionFactory.
	 * This is an alternative to specifying {@code <filter-def>} elements in
	 * Hibernate mapping files.
	 * <p>Typically, the passed-in FilterDefinition objects will have been defined
	 * as Spring FilterDefinitionFactoryBeans, probably as inner beans within the
	 * LocalSessionFactoryBean definition.
	 * @see FilterDefinitionFactoryBean
	 * @see org.hibernate.cfg.Configuration#addFilterDefinition
	 */
	public This setFilterDefinitions(FilterDefinition[] filterDefinitions) {
		this.filterDefinitions = filterDefinitions;
		return this.instance;
	}

	/**
	 * Specify the Hibernate type definitions to register with the SessionFactory,
	 * as Spring TypeDefinitionBean instances. This is an alternative to specifying
	 * {@code <typedef>} elements in Hibernate mapping files.
	 * <p>Unfortunately, Hibernate itself does not define a complete object that
	 * represents a type definition, hence the need for Spring's TypeDefinitionBean.
	 * @see TypeDefinitionBean
	 * @see org.hibernate.cfg.Mappings#addTypeDef(String, String, java.util.Properties)
	 */
	public This setTypeDefinitions(TypeDefinitionBean[] typeDefinitions) {
		this.typeDefinitions = typeDefinitions;
		return this.instance;
	}

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

}
