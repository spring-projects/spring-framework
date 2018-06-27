/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.orm.hibernate5;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.springframework.core.InfrastructureProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
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
 * <p>Compatible with Hibernate 5.0/5.1 as well as 5.2, as of Spring 4.3.
 *
 * @author Juergen Hoeller
 * @since 4.2
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

	private static final TypeFilter CONVERTER_TYPE_FILTER = new AnnotationTypeFilter(Converter.class, false);


	private final ResourcePatternResolver resourcePatternResolver;

	@Nullable
	private TypeFilter[] entityTypeFilters = DEFAULT_ENTITY_TYPE_FILTERS;


	/**
	 * Create a new LocalSessionFactoryBuilder for the given DataSource.
	 * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
	 * (may be {@code null})
	 */
	public LocalSessionFactoryBuilder(@Nullable DataSource dataSource) {
		this(dataSource, new PathMatchingResourcePatternResolver());
	}

	/**
	 * Create a new LocalSessionFactoryBuilder for the given DataSource.
	 * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
	 * (may be {@code null})
	 * @param classLoader the ClassLoader to load application classes from
	 */
	public LocalSessionFactoryBuilder(@Nullable DataSource dataSource, ClassLoader classLoader) {
		this(dataSource, new PathMatchingResourcePatternResolver(classLoader));
	}

	/**
	 * Create a new LocalSessionFactoryBuilder for the given DataSource.
	 * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
	 * (may be {@code null})
	 * @param resourceLoader the ResourceLoader to load application classes from
	 */
	public LocalSessionFactoryBuilder(@Nullable DataSource dataSource, ResourceLoader resourceLoader) {
		this(dataSource, resourceLoader, new MetadataSources(
				new BootstrapServiceRegistryBuilder().applyClassLoader(resourceLoader.getClassLoader()).build()));
	}

	/**
	 * Create a new LocalSessionFactoryBuilder for the given DataSource.
	 * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
	 * (may be {@code null})
	 * @param resourceLoader the ResourceLoader to load application classes from
	 * @param metadataSources the Hibernate MetadataSources service to use (e.g. reusing an existing one)
	 * @since 4.3
	 */
	public LocalSessionFactoryBuilder(@Nullable DataSource dataSource, ResourceLoader resourceLoader, MetadataSources metadataSources) {
		super(metadataSources);

		getProperties().put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
		if (dataSource != null) {
			getProperties().put(AvailableSettings.DATASOURCE, dataSource);
		}

		// Hibernate 5.1/5.2: manually enforce connection release mode ON_CLOSE (the former default)
		try {
			// Try Hibernate 5.2
			AvailableSettings.class.getField("CONNECTION_HANDLING");
			getProperties().put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
		}
		catch (NoSuchFieldException ex) {
			// Try Hibernate 5.1
			try {
				AvailableSettings.class.getField("ACQUIRE_CONNECTIONS");
				getProperties().put("hibernate.connection.release_mode", "ON_CLOSE");
			}
			catch (NoSuchFieldException ex2) {
				// on Hibernate 5.0.x or lower - no need to change the default there
			}
		}

		getProperties().put(AvailableSettings.CLASSLOADERS, Collections.singleton(resourceLoader.getClassLoader()));
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	/**
	 * Set the Spring {@link JtaTransactionManager} or the JTA {@link TransactionManager}
	 * to be used with Hibernate, if any. Allows for using a Spring-managed transaction
	 * manager for Hibernate 5's session and cache synchronization, with the
	 * "hibernate.transaction.jta.platform" automatically set to it.
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
						"org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform");
			}
			else {
				JtaTransactionManager jtaTm = (JtaTransactionManager) jtaTransactionManager;
				if (jtaTm.getTransactionManager() == null) {
					throw new IllegalArgumentException(
							"Can only apply JtaTransactionManager which has a TransactionManager reference set");
				}
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						new ConfigurableJtaPlatform(jtaTm.getTransactionManager(), jtaTm.getUserTransaction(),
								jtaTm.getTransactionSynchronizationRegistry()));
			}
		}
		else if (jtaTransactionManager instanceof TransactionManager) {
			getProperties().put(AvailableSettings.JTA_PLATFORM,
					new ConfigurableJtaPlatform((TransactionManager) jtaTransactionManager, null, null));
		}
		else {
			throw new IllegalArgumentException(
					"Unknown transaction manager type: " + jtaTransactionManager.getClass().getName());
		}

		// Hibernate 5.1/5.2: manually enforce connection release mode AFTER_STATEMENT (the JTA default)
		try {
			// Try Hibernate 5.2
			AvailableSettings.class.getField("CONNECTION_HANDLING");
			getProperties().put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT");
		}
		catch (NoSuchFieldException ex) {
			// Try Hibernate 5.1
			try {
				AvailableSettings.class.getField("ACQUIRE_CONNECTIONS");
				getProperties().put("hibernate.connection.release_mode", "AFTER_STATEMENT");
			}
			catch (NoSuchFieldException ex2) {
				// on Hibernate 5.0.x or lower - no need to change the default there
			}
		}

		return this;
	}

	/**
	 * Set a {@link MultiTenantConnectionProvider} to be passed on to the SessionFactory.
	 * @since 4.3
	 * @see AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
	 */
	public LocalSessionFactoryBuilder setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
		getProperties().put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
		return this;
	}

	/**
	 * Overridden to reliably pass a {@link CurrentTenantIdentifierResolver} to the SessionFactory.
	 * @since 4.3.2
	 * @see AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
	 */
	@Override
	public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		getProperties().put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
		super.setCurrentTenantIdentifierResolver(currentTenantIdentifierResolver);
	}

	/**
	 * Specify custom type filters for Spring-based scanning for entity classes.
	 * <p>Default is to search all specified packages for classes annotated with
	 * {@code @javax.persistence.Entity}, {@code @javax.persistence.Embeddable}
	 * or {@code @javax.persistence.MappedSuperclass}.
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
	@SuppressWarnings("unchecked")
	public LocalSessionFactoryBuilder scanPackages(String... packagesToScan) throws HibernateException {
		Set<String> entityClassNames = new TreeSet<>();
		Set<String> converterClassNames = new TreeSet<>();
		Set<String> packageNames = new TreeSet<>();
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
						else if (CONVERTER_TYPE_FILTER.match(reader, readerFactory)) {
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
				addAnnotatedClass(ClassUtils.forName(className, cl));
			}
			for (String className : converterClassNames) {
				addAttributeConverter((Class<? extends AttributeConverter<?, ?>>) ClassUtils.forName(className, cl));
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

	/**
	 * Build the Hibernate {@code SessionFactory} through background bootstrapping,
	 * using the given executor for a parallel initialization phase
	 * (e.g. a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}).
	 * <p>{@code SessionFactory} initialization will then switch into background
	 * bootstrap mode, with a {@code SessionFactory} proxy immediately returned for
	 * injection purposes instead of waiting for Hibernate's bootstrapping to complete.
	 * However, note that the first actual call to a {@code SessionFactory} method will
	 * then block until Hibernate's bootstrapping completed, if not ready by then.
	 * For maximum benefit, make sure to avoid early {@code SessionFactory} calls
	 * in init methods of related beans, even for metadata introspection purposes.
	 * @since 4.3
	 * @see #buildSessionFactory()
	 */
	public SessionFactory buildSessionFactory(AsyncTaskExecutor bootstrapExecutor) {
		Assert.notNull(bootstrapExecutor, "AsyncTaskExecutor must not be null");
		return (SessionFactory) Proxy.newProxyInstance(this.resourcePatternResolver.getClassLoader(),
				new Class<?>[] {SessionFactoryImplementor.class, InfrastructureProxy.class},
				new BootstrapSessionFactoryInvocationHandler(bootstrapExecutor));
	}


	/**
	 * Proxy invocation handler for background bootstrapping, only enforcing
	 * a fully initialized target {@code SessionFactory} when actually needed.
	 * @since 4.3
	 */
	private class BootstrapSessionFactoryInvocationHandler implements InvocationHandler {

		private final Future<SessionFactory> sessionFactoryFuture;

		public BootstrapSessionFactoryInvocationHandler(AsyncTaskExecutor bootstrapExecutor) {
			this.sessionFactoryFuture = bootstrapExecutor.submit(
					(Callable<SessionFactory>) LocalSessionFactoryBuilder.this::buildSessionFactory);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().equals("equals")) {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				else if (method.getName().equals("hashCode")) {
					// Use hashCode of EntityManagerFactory proxy.
					return System.identityHashCode(proxy);
				}
				else if (method.getName().equals("getProperties")) {
					return getProperties();
				}
				else if (method.getName().equals("getWrappedObject")) {
					// Call coming in through InfrastructureProxy interface...
					return getSessionFactory();
				}
				// Regular delegation to the target SessionFactory,
				// enforcing its full initialization...
				return method.invoke(getSessionFactory(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private SessionFactory getSessionFactory() {
			try {
				return this.sessionFactoryFuture.get();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted during initialization of Hibernate SessionFactory", ex);
			}
			catch (ExecutionException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof HibernateException) {
					// Rethrow a provider configuration exception (possibly with a nested cause) directly
					throw (HibernateException) cause;
				}
				throw new IllegalStateException("Failed to asynchronously initialize Hibernate SessionFactory: " +
						ex.getMessage(), cause);
			}
		}
	}

}
