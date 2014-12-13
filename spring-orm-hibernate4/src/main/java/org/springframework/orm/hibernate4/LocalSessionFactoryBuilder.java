/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.orm.hibernate4;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.AttributeConverter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.service.ServiceRegistry;

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
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A Spring-provided extension of the standard Hibernate {@link Configuration} class,
 * adding {@link SpringSessionContext} as a default and providing convenient ways
 * to specify a DataSource and an application class loader.
 *
 * <p>This is designed for programmatic use, e.g. in {@code @Bean} factory methods.
 * Consider using {@link LocalSessionFactoryBean} for XML bean definition files.
 *
 * <p><b>Requires Hibernate 4.0 or higher.</b> As of Spring 4.0, it is compatible with
 * (the quite refactored) Hibernate 4.3 as well. We recommend using the latest
 * Hibernate 4.2.x or 4.3.x version, depending on whether you need to remain JPA 2.0
 * compatible at runtime (Hibernate 4.2) or can upgrade to JPA 2.1 (Hibernate 4.3).
 *
 * <p><b>NOTE:</b> To set up Hibernate 4 for Spring-driven JTA transactions, make
 * sure to either use the {@link #setJtaTransactionManager} method or to set the
 * "hibernate.transaction.factory_class" property to {@link CMTTransactionFactory}.
 * Otherwise, Hibernate's smart flushing mechanism won't work properly.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see LocalSessionFactoryBean
 */
@SuppressWarnings("serial")
public class LocalSessionFactoryBuilder extends Configuration {

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";

	private static final TypeFilter[] DEFAULT_ENTITY_TYPE_FILTERS = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false)};


	private static TypeFilter converterTypeFilter;

	static {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> converterAnnotation = (Class<? extends Annotation>)
					ClassUtils.forName("javax.persistence.Converter", LocalSessionFactoryBuilder.class.getClassLoader());
			converterTypeFilter = new AnnotationTypeFilter(converterAnnotation, false);
		}
		catch (ClassNotFoundException ex) {
			// JPA 2.1 API not available - Hibernate <4.3
		}
	}


	private final ResourcePatternResolver resourcePatternResolver;

	private RegionFactory cacheRegionFactory;

	private TypeFilter[] entityTypeFilters = DEFAULT_ENTITY_TYPE_FILTERS;


	/**
	 * Create a new LocalSessionFactoryBuilder for the given DataSource.
	 * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
	 * (may be {@code null})
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource) {
		this(dataSource, new PathMatchingResourcePatternResolver());
	}

	/**
	 * Create a new LocalSessionFactoryBuilder for the given DataSource.
	 * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
	 * (may be {@code null})
	 * @param classLoader the ClassLoader to load application classes from
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource, ClassLoader classLoader) {
		this(dataSource, new PathMatchingResourcePatternResolver(classLoader));
	}

	/**
	 * Create a new LocalSessionFactoryBuilder for the given DataSource.
	 * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
	 * (may be {@code null})
	 * @param resourceLoader the ResourceLoader to load application classes from
	 */
	@SuppressWarnings("deprecation")  // to be able to build against Hibernate 4.3
	public LocalSessionFactoryBuilder(DataSource dataSource, ResourceLoader resourceLoader) {
		getProperties().put(Environment.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
		if (dataSource != null) {
			getProperties().put(Environment.DATASOURCE, dataSource);
		}
		// APP_CLASSLOADER is deprecated as of Hibernate 4.3 but we need to remain compatible with 4.0+
		getProperties().put(AvailableSettings.APP_CLASSLOADER, resourceLoader.getClassLoader());
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	/**
	 * Set the Spring {@link JtaTransactionManager} or the JTA {@link TransactionManager}
	 * to be used with Hibernate, if any. Allows for using a Spring-managed transaction
	 * manager for Hibernate 4's session and cache synchronization, with the
	 * "hibernate.transaction.jta.platform" automatically set to it. Also sets
	 * "hibernate.transaction.factory_class" to {@link CMTTransactionFactory},
	 * instructing Hibernate to interact with externally managed transactions.
	 * <p>A passed-in Spring {@link JtaTransactionManager} needs to contain a JTA
	 * {@link TransactionManager} reference to be usable here, except for the WebSphere
	 * case where we'll automatically set {@code WebSphereExtendedJtaPlatform} accordingly.
	 * <p>Note: If this is set, the Hibernate settings should not contain a JTA platform
	 * setting to avoid meaningless double configuration.
	 */
	public LocalSessionFactoryBuilder setJtaTransactionManager(Object jtaTransactionManager) {
		Assert.notNull(jtaTransactionManager, "Transaction manager reference must not be null");
		if (jtaTransactionManager instanceof JtaTransactionManager) {
			boolean webspherePresent = ClassUtils.isPresent("com.ibm.wsspi.uow.UOWManager", getClass().getClassLoader());
			if (webspherePresent) {
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						ConfigurableJtaPlatform.getJtaPlatformBasePackage() + "internal.WebSphereExtendedJtaPlatform");
			}
			else {
				JtaTransactionManager jtaTm = (JtaTransactionManager) jtaTransactionManager;
				if (jtaTm.getTransactionManager() == null) {
					throw new IllegalArgumentException(
							"Can only apply JtaTransactionManager which has a TransactionManager reference set");
				}
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						new ConfigurableJtaPlatform(jtaTm.getTransactionManager(), jtaTm.getUserTransaction(),
								jtaTm.getTransactionSynchronizationRegistry()).getJtaPlatformProxy());
			}
		}
		else if (jtaTransactionManager instanceof TransactionManager) {
			getProperties().put(AvailableSettings.JTA_PLATFORM,
					new ConfigurableJtaPlatform((TransactionManager) jtaTransactionManager, null, null).getJtaPlatformProxy());
		}
		else {
			throw new IllegalArgumentException(
					"Unknown transaction manager type: " + jtaTransactionManager.getClass().getName());
		}
		getProperties().put(AvailableSettings.TRANSACTION_STRATEGY, new CMTTransactionFactory());
		return this;
	}

	/**
	 * Set a Hibernate 4.1/4.2/4.3 {@code MultiTenantConnectionProvider} to be passed
	 * on to the SessionFactory: as an instance, a Class, or a String class name.
	 * <p>Note that the package location of the {@code MultiTenantConnectionProvider}
	 * interface changed between Hibernate 4.2 and 4.3. This method accepts both variants.
	 * @since 4.0
	 * @see AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
	 */
	public LocalSessionFactoryBuilder setMultiTenantConnectionProvider(Object multiTenantConnectionProvider) {
		getProperties().put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
		return this;
	}

	/**
	 * Set a Hibernate 4.1/4.2/4.3 {@code CurrentTenantIdentifierResolver} to be passed
	 * on to the SessionFactory: as an instance, a Class, or a String class name.
	 * @since 4.0
	 * @see AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
	 */
	public LocalSessionFactoryBuilder setCurrentTenantIdentifierResolver(Object currentTenantIdentifierResolver) {
		getProperties().put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
		return this;
	}

	/**
	 * Set the Hibernate RegionFactory to use for the SessionFactory.
	 * Allows for using a Spring-managed RegionFactory instance.
	 * <p>Note: If this is set, the Hibernate settings should not define a
	 * cache provider to avoid meaningless double configuration.
	 * @since 4.0
	 * @see org.hibernate.cache.spi.RegionFactory
	 */
	public LocalSessionFactoryBuilder setCacheRegionFactory(RegionFactory cacheRegionFactory) {
		this.cacheRegionFactory = cacheRegionFactory;
		return this;
	}

	/**
	 * Specify custom type filters for Spring-based scanning for entity classes.
	 * <p>Default is to search all specified packages for classes annotated with
	 * {@code @javax.persistence.Entity}, {@code @javax.persistence.Embeddable}
	 * or {@code @javax.persistence.MappedSuperclass}.
	 * @since 4.1
	 * @see #scanPackages
	 */
	public LocalSessionFactoryBuilder setEntityTypeFilters(TypeFilter... entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
		return this;
	}

	/**
	 * Add the given annotated classes in a batch.
	 * @see #addAnnotatedClass
	 * @see #scanPackages
	 */
	public LocalSessionFactoryBuilder addAnnotatedClasses(Class<?>... annotatedClasses) {
		for (Class<?> annotatedClass : annotatedClasses) {
			addAnnotatedClass(annotatedClass);
		}
		return this;
	}

	/**
	 * Add the given annotated packages in a batch.
	 * @see #addPackage
	 * @see #scanPackages
	 */
	public LocalSessionFactoryBuilder addPackages(String... annotatedPackages) {
		for (String annotatedPackage : annotatedPackages) {
			addPackage(annotatedPackage);
		}
		return this;
	}

	/**
	 * Perform Spring-based scanning for entity classes, registering them
	 * as annotated classes with this {@code Configuration}.
	 * @param packagesToScan one or more Java package names
	 * @throws HibernateException if scanning fails for any reason
	 */
	public LocalSessionFactoryBuilder scanPackages(String... packagesToScan) throws HibernateException {
		Set<String> entityClassNames = new TreeSet<String>();
		Set<String> converterClassNames = new TreeSet<String>();
		Set<String> packageNames = new TreeSet<String>();
		try {
			for (String pkg : packagesToScan) {
				String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
						ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;
				Resource[] resources = this.resourcePatternResolver.getResources(pattern);
				MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
				for (Resource resource : resources) {
					if (resource.isReadable()) {
						MetadataReader reader = readerFactory.getMetadataReader(resource);
						String className = reader.getClassMetadata().getClassName();
						if (matchesEntityTypeFilter(reader, readerFactory)) {
							entityClassNames.add(className);
						}
						else if (converterTypeFilter != null && converterTypeFilter.match(reader, readerFactory)) {
							converterClassNames.add(className);
						}
						else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
							packageNames.add(className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
						}
					}
				}
			}
		}
		catch (IOException ex) {
			throw new MappingException("Failed to scan classpath for unlisted classes", ex);
		}
		try {
			ClassLoader cl = this.resourcePatternResolver.getClassLoader();
			for (String className : entityClassNames) {
				addAnnotatedClass(cl.loadClass(className));
			}
			for (String className : converterClassNames) {
				ConverterRegistrationDelegate.registerConverter(this, cl.loadClass(className));
			}
			for (String packageName : packageNames) {
				addPackage(packageName);
			}
		}
		catch (ClassNotFoundException ex) {
			throw new MappingException("Failed to load annotated classes from classpath", ex);
		}
		return this;
	}

	/**
	 * Check whether any of the configured entity type filters matches
	 * the current class descriptor contained in the metadata reader.
	 */
	private boolean matchesEntityTypeFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		if (this.entityTypeFilters != null) {
			for (TypeFilter filter : this.entityTypeFilters) {
				if (filter.match(reader, readerFactory)) {
					return true;
				}
			}
		}
		return false;
	}


	// Overridden methods from Hibernate's Configuration class

	@Override
	public Settings buildSettings(Properties props, ServiceRegistry serviceRegistry) throws HibernateException {
		Settings settings = super.buildSettings(props, serviceRegistry);
		if (this.cacheRegionFactory != null) {
			try {
				Method setRegionFactory = Settings.class.getDeclaredMethod("setRegionFactory", RegionFactory.class);
				setRegionFactory.setAccessible(true);
				setRegionFactory.invoke(settings, this.cacheRegionFactory);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to invoke Hibernate's setRegionFactory method", ex);
			}
		}
		return settings;
	}

	/**
	 * Build the {@code SessionFactory}.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public SessionFactory buildSessionFactory() throws HibernateException {
		ClassLoader appClassLoader = (ClassLoader) getProperties().get(AvailableSettings.APP_CLASSLOADER);
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader =
				(appClassLoader != null && !appClassLoader.equals(threadContextClassLoader));
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(appClassLoader);
		}
		try {
			return super.buildSessionFactory();
		}
		finally {
			if (overrideClassLoader) {
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}


	/**
	 * Inner class to avoid hard dependency on JPA 2.1 / Hibernate 4.3.
	 */
	private static class ConverterRegistrationDelegate {

		@SuppressWarnings("unchecked")
		public static void registerConverter(Configuration config, Class<?> converterClass) {
			config.addAttributeConverter((Class<? extends AttributeConverter<?, ?>>) converterClass);
		}
	}

}
