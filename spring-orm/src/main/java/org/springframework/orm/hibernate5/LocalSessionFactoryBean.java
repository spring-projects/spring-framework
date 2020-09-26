/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.orm.hibernate5;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.ServiceRegistry;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link FactoryBean} that creates a Hibernate {@link SessionFactory}. This is the usual
 * way to set up a shared Hibernate SessionFactory in a Spring application context; the
 * SessionFactory can then be passed to data access objects via dependency injection.
 *
 * <p>Compatible with Hibernate 5.2/5.3/5.4, as of Spring 5.3.
 * This Hibernate-specific {@code LocalSessionFactoryBean} can be an immediate alternative
 * to {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean} for common
 * JPA purposes: In particular with Hibernate 5.3/5.4, the Hibernate {@code SessionFactory}
 * will natively expose the JPA {@code EntityManagerFactory} interface as well, and
 * Hibernate {@code BeanContainer} integration will be registered out of the box.
 * In combination with {@link HibernateTransactionManager}, this naturally allows for
 * mixing JPA access code with native Hibernate access code within the same transaction.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setDataSource
 * @see #setPackagesToScan
 * @see HibernateTransactionManager
 * @see LocalSessionFactoryBuilder
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
 */
public class LocalSessionFactoryBean extends HibernateExceptionTranslator
		implements FactoryBean<SessionFactory>, ResourceLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

	@Nullable
	private DataSource dataSource;

	@Nullable
	private Resource[] configLocations;

	@Nullable
	private String[] mappingResources;

	@Nullable
	private Resource[] mappingLocations;

	@Nullable
	private Resource[] cacheableMappingLocations;

	@Nullable
	private Resource[] mappingJarLocations;

	@Nullable
	private Resource[] mappingDirectoryLocations;

	@Nullable
	private Interceptor entityInterceptor;

	@Nullable
	private ImplicitNamingStrategy implicitNamingStrategy;

	@Nullable
	private PhysicalNamingStrategy physicalNamingStrategy;

	@Nullable
	private Object jtaTransactionManager;

	@Nullable
	private RegionFactory cacheRegionFactory;

	@Nullable
	private MultiTenantConnectionProvider multiTenantConnectionProvider;

	@Nullable
	private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

	@Nullable
	private Properties hibernateProperties;

	@Nullable
	private TypeFilter[] entityTypeFilters;

	@Nullable
	private Class<?>[] annotatedClasses;

	@Nullable
	private String[] annotatedPackages;

	@Nullable
	private String[] packagesToScan;

	@Nullable
	private AsyncTaskExecutor bootstrapExecutor;

	@Nullable
	private Integrator[] hibernateIntegrators;

	private boolean metadataSourcesAccessed = false;

	@Nullable
	private MetadataSources metadataSources;

	@Nullable
	private ResourcePatternResolver resourcePatternResolver;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	@Nullable
	private Configuration configuration;

	@Nullable
	private SessionFactory sessionFactory;


	/**
	 * Set the DataSource to be used by the SessionFactory.
	 * If set, this will override corresponding settings in Hibernate properties.
	 * <p>If this is set, the Hibernate settings should not define
	 * a connection provider to avoid meaningless double configuration.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Set the location of a single Hibernate XML config file, for example as
	 * classpath resource "classpath:hibernate.cfg.xml".
	 * <p>Note: Can be omitted when all necessary properties and mapping
	 * resources are specified locally via this bean.
	 * @see Configuration#configure(java.net.URL)
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocations = new Resource[] {configLocation};
	}

	/**
	 * Set the locations of multiple Hibernate XML config files, for example as
	 * classpath resources "classpath:hibernate.cfg.xml,classpath:extension.cfg.xml".
	 * <p>Note: Can be omitted when all necessary properties and mapping
	 * resources are specified locally via this bean.
	 * @see Configuration#configure(java.net.URL)
	 */
	public void setConfigLocations(Resource... configLocations) {
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
	 * @see Configuration#addResource
	 */
	public void setMappingResources(String... mappingResources) {
		this.mappingResources = mappingResources;
	}

	/**
	 * Set locations of Hibernate mapping files, for example as classpath
	 * resource "classpath:example.hbm.xml". Supports any resource location
	 * via Spring's resource abstraction, for example relative paths like
	 * "WEB-INF/mappings/example.hbm.xml" when running in an application context.
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see Configuration#addInputStream
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * Set locations of cacheable Hibernate mapping files, for example as web app
	 * resource "/WEB-INF/mapping/example.hbm.xml". Supports any resource location
	 * via Spring's resource abstraction, as long as the resource can be resolved
	 * in the file system.
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see Configuration#addCacheableFile(File)
	 */
	public void setCacheableMappingLocations(Resource... cacheableMappingLocations) {
		this.cacheableMappingLocations = cacheableMappingLocations;
	}

	/**
	 * Set locations of jar files that contain Hibernate mapping resources,
	 * like "WEB-INF/lib/example.hbm.jar".
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see Configuration#addJar(File)
	 */
	public void setMappingJarLocations(Resource... mappingJarLocations) {
		this.mappingJarLocations = mappingJarLocations;
	}

	/**
	 * Set locations of directories that contain Hibernate mapping resources,
	 * like "WEB-INF/mappings".
	 * <p>Can be used to add to mappings from a Hibernate XML config file,
	 * or to specify all mappings locally.
	 * @see Configuration#addDirectory(File)
	 */
	public void setMappingDirectoryLocations(Resource... mappingDirectoryLocations) {
		this.mappingDirectoryLocations = mappingDirectoryLocations;
	}

	/**
	 * Set a Hibernate entity interceptor that allows to inspect and change
	 * property values before writing to and reading from the database.
	 * Will get applied to any new Session created by this factory.
	 * @see Configuration#setInterceptor
	 */
	public void setEntityInterceptor(Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * Set a Hibernate 5 {@link ImplicitNamingStrategy} for the SessionFactory.
	 * @see Configuration#setImplicitNamingStrategy
	 */
	public void setImplicitNamingStrategy(ImplicitNamingStrategy implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
	}

	/**
	 * Set a Hibernate 5 {@link PhysicalNamingStrategy} for the SessionFactory.
	 * @see Configuration#setPhysicalNamingStrategy
	 */
	public void setPhysicalNamingStrategy(PhysicalNamingStrategy physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
	}

	/**
	 * Set the Spring {@link org.springframework.transaction.jta.JtaTransactionManager}
	 * or the JTA {@link javax.transaction.TransactionManager} to be used with Hibernate,
	 * if any. Implicitly sets up {@code JtaPlatform}.
	 * @see LocalSessionFactoryBuilder#setJtaTransactionManager
	 */
	public void setJtaTransactionManager(Object jtaTransactionManager) {
		this.jtaTransactionManager = jtaTransactionManager;
	}

	/**
	 * Set the Hibernate {@link RegionFactory} to use for the SessionFactory.
	 * Allows for using a Spring-managed {@code RegionFactory} instance.
	 * <p>Note: If this is set, the Hibernate settings should not define a
	 * cache provider to avoid meaningless double configuration.
	 * @since 5.1
	 * @see LocalSessionFactoryBuilder#setCacheRegionFactory
	 */
	public void setCacheRegionFactory(RegionFactory cacheRegionFactory) {
		this.cacheRegionFactory = cacheRegionFactory;
	}

	/**
	 * Set a {@link MultiTenantConnectionProvider} to be passed on to the SessionFactory.
	 * @since 4.3
	 * @see LocalSessionFactoryBuilder#setMultiTenantConnectionProvider
	 */
	public void setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
		this.multiTenantConnectionProvider = multiTenantConnectionProvider;
	}

	/**
	 * Set a {@link CurrentTenantIdentifierResolver} to be passed on to the SessionFactory.
	 * @see LocalSessionFactoryBuilder#setCurrentTenantIdentifierResolver
	 */
	public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		this.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
	}

	/**
	 * Set Hibernate properties, such as "hibernate.dialect".
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
	 * Specify custom type filters for Spring-based scanning for entity classes.
	 * <p>Default is to search all specified packages for classes annotated with
	 * {@code @javax.persistence.Entity}, {@code @javax.persistence.Embeddable}
	 * or {@code @javax.persistence.MappedSuperclass}.
	 * @see #setPackagesToScan
	 */
	public void setEntityTypeFilters(TypeFilter... entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
	}

	/**
	 * Specify annotated entity classes to register with this Hibernate SessionFactory.
	 * @see Configuration#addAnnotatedClass(Class)
	 */
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	/**
	 * Specify the names of annotated packages, for which package-level
	 * annotation metadata will be read.
	 * @see Configuration#addPackage(String)
	 */
	public void setAnnotatedPackages(String... annotatedPackages) {
		this.annotatedPackages = annotatedPackages;
	}

	/**
	 * Specify packages to search for autodetection of your entity classes in the
	 * classpath. This is analogous to Spring's component-scan feature
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * Specify an asynchronous executor for background bootstrapping,
	 * e.g. a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 * <p>{@code SessionFactory} initialization will then switch into background
	 * bootstrap mode, with a {@code SessionFactory} proxy immediately returned for
	 * injection purposes instead of waiting for Hibernate's bootstrapping to complete.
	 * However, note that the first actual call to a {@code SessionFactory} method will
	 * then block until Hibernate's bootstrapping completed, if not ready by then.
	 * For maximum benefit, make sure to avoid early {@code SessionFactory} calls
	 * in init methods of related beans, even for metadata introspection purposes.
	 * @since 4.3
	 * @see LocalSessionFactoryBuilder#buildSessionFactory(AsyncTaskExecutor)
	 */
	public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * Specify one or more Hibernate {@link Integrator} implementations to apply.
	 * <p>This will only be applied for an internally built {@link MetadataSources}
	 * instance. {@link #setMetadataSources} effectively overrides such settings,
	 * with integrators to be applied to the externally built {@link MetadataSources}.
	 * @since 5.1
	 * @see #setMetadataSources
	 * @see BootstrapServiceRegistryBuilder#applyIntegrator
	 */
	public void setHibernateIntegrators(Integrator... hibernateIntegrators) {
		this.hibernateIntegrators = hibernateIntegrators;
	}

	/**
	 * Specify a Hibernate {@link MetadataSources} service to use (e.g. reusing an
	 * existing one), potentially populated with a custom Hibernate bootstrap
	 * {@link org.hibernate.service.ServiceRegistry} as well.
	 * @since 4.3
	 * @see MetadataSources#MetadataSources(ServiceRegistry)
	 * @see BootstrapServiceRegistryBuilder#build()
	 */
	public void setMetadataSources(MetadataSources metadataSources) {
		this.metadataSourcesAccessed = true;
		this.metadataSources = metadataSources;
	}

	/**
	 * Determine the Hibernate {@link MetadataSources} to use.
	 * <p>Can also be externally called to initialize and pre-populate a {@link MetadataSources}
	 * instance which is then going to be used for {@link SessionFactory} building.
	 * @return the MetadataSources to use (never {@code null})
	 * @since 4.3
	 * @see LocalSessionFactoryBuilder#LocalSessionFactoryBuilder(DataSource, ResourceLoader, MetadataSources)
	 */
	public MetadataSources getMetadataSources() {
		this.metadataSourcesAccessed = true;
		if (this.metadataSources == null) {
			BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
			if (this.resourcePatternResolver != null) {
				builder = builder.applyClassLoader(this.resourcePatternResolver.getClassLoader());
			}
			if (this.hibernateIntegrators != null) {
				for (Integrator integrator : this.hibernateIntegrators) {
					builder = builder.applyIntegrator(integrator);
				}
			}
			this.metadataSources = new MetadataSources(builder.build());
		}
		return this.metadataSources;
	}

	/**
	 * Specify a Spring {@link ResourceLoader} to use for Hibernate metadata.
	 * @param resourceLoader the ResourceLoader to use (never {@code null})
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}

	/**
	 * Determine the Spring {@link ResourceLoader} to use for Hibernate metadata.
	 * @return the ResourceLoader to use (never {@code null})
	 * @since 4.3
	 */
	public ResourceLoader getResourceLoader() {
		if (this.resourcePatternResolver == null) {
			this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
		}
		return this.resourcePatternResolver;
	}

	/**
	 * Accept the containing {@link BeanFactory}, registering corresponding Hibernate
	 * {@link org.hibernate.resource.beans.container.spi.BeanContainer} integration for
	 * it if possible. This requires a Spring {@link ConfigurableListableBeanFactory}
	 * and Hibernate 5.3 or higher on the classpath.
	 * @since 5.1
	 * @see SpringBeanContainer
	 * @see LocalSessionFactoryBuilder#setBeanContainer
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory &&
				ClassUtils.isPresent("org.hibernate.resource.beans.container.spi.BeanContainer",
						getClass().getClassLoader())) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
	}


	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.metadataSources != null && !this.metadataSourcesAccessed) {
			// Repeated initialization with no user-customized MetadataSources -> clear it.
			this.metadataSources = null;
		}

		LocalSessionFactoryBuilder sfb = new LocalSessionFactoryBuilder(
				this.dataSource, getResourceLoader(), getMetadataSources());

		if (this.configLocations != null) {
			for (Resource resource : this.configLocations) {
				// Load Hibernate configuration from given location.
				sfb.configure(resource.getURL());
			}
		}

		if (this.mappingResources != null) {
			// Register given Hibernate mapping definitions, contained in resource files.
			for (String mapping : this.mappingResources) {
				Resource mr = new ClassPathResource(mapping.trim(), getResourceLoader().getClassLoader());
				sfb.addInputStream(mr.getInputStream());
			}
		}

		if (this.mappingLocations != null) {
			// Register given Hibernate mapping definitions, contained in resource files.
			for (Resource resource : this.mappingLocations) {
				sfb.addInputStream(resource.getInputStream());
			}
		}

		if (this.cacheableMappingLocations != null) {
			// Register given cacheable Hibernate mapping definitions, read from the file system.
			for (Resource resource : this.cacheableMappingLocations) {
				sfb.addCacheableFile(resource.getFile());
			}
		}

		if (this.mappingJarLocations != null) {
			// Register given Hibernate mapping definitions, contained in jar files.
			for (Resource resource : this.mappingJarLocations) {
				sfb.addJar(resource.getFile());
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
				sfb.addDirectory(file);
			}
		}

		if (this.entityInterceptor != null) {
			sfb.setInterceptor(this.entityInterceptor);
		}

		if (this.implicitNamingStrategy != null) {
			sfb.setImplicitNamingStrategy(this.implicitNamingStrategy);
		}

		if (this.physicalNamingStrategy != null) {
			sfb.setPhysicalNamingStrategy(this.physicalNamingStrategy);
		}

		if (this.jtaTransactionManager != null) {
			sfb.setJtaTransactionManager(this.jtaTransactionManager);
		}

		if (this.beanFactory != null) {
			sfb.setBeanContainer(this.beanFactory);
		}

		if (this.cacheRegionFactory != null) {
			sfb.setCacheRegionFactory(this.cacheRegionFactory);
		}

		if (this.multiTenantConnectionProvider != null) {
			sfb.setMultiTenantConnectionProvider(this.multiTenantConnectionProvider);
		}

		if (this.currentTenantIdentifierResolver != null) {
			sfb.setCurrentTenantIdentifierResolver(this.currentTenantIdentifierResolver);
		}

		if (this.hibernateProperties != null) {
			sfb.addProperties(this.hibernateProperties);
		}

		if (this.entityTypeFilters != null) {
			sfb.setEntityTypeFilters(this.entityTypeFilters);
		}

		if (this.annotatedClasses != null) {
			sfb.addAnnotatedClasses(this.annotatedClasses);
		}

		if (this.annotatedPackages != null) {
			sfb.addPackages(this.annotatedPackages);
		}

		if (this.packagesToScan != null) {
			sfb.scanPackages(this.packagesToScan);
		}

		// Build SessionFactory instance.
		this.configuration = sfb;
		this.sessionFactory = buildSessionFactory(sfb);
	}

	/**
	 * Subclasses can override this method to perform custom initialization
	 * of the SessionFactory instance, creating it via the given Configuration
	 * object that got prepared by this LocalSessionFactoryBean.
	 * <p>The default implementation invokes LocalSessionFactoryBuilder's buildSessionFactory.
	 * A custom implementation could prepare the instance in a specific way (e.g. applying
	 * a custom ServiceRegistry) or use a custom SessionFactoryImpl subclass.
	 * @param sfb a LocalSessionFactoryBuilder prepared by this LocalSessionFactoryBean
	 * @return the SessionFactory instance
	 * @see LocalSessionFactoryBuilder#buildSessionFactory
	 */
	protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
		return (this.bootstrapExecutor != null ? sfb.buildSessionFactory(this.bootstrapExecutor) :
				sfb.buildSessionFactory());
	}

	/**
	 * Return the Hibernate Configuration object used to build the SessionFactory.
	 * Allows for access to configuration metadata stored there (rarely needed).
	 * @throws IllegalStateException if the Configuration object has not been initialized yet
	 */
	public final Configuration getConfiguration() {
		if (this.configuration == null) {
			throw new IllegalStateException("Configuration not initialized yet");
		}
		return this.configuration;
	}


	@Override
	@Nullable
	public SessionFactory getObject() {
		return this.sessionFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.sessionFactory != null) {
			this.sessionFactory.close();
		}
	}

}
