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

package org.springframework.orm.hibernate4;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.sql.DataSource;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
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
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a
 * Hibernate {@link org.hibernate.SessionFactory}. This is the usual way to
 * set up a shared Hibernate SessionFactory in a Spring application context;
 * the SessionFactory can then be passed to Hibernate-based DAOs via
 * dependency injection.
 *
 * <p><b>NOTE:</b> This variant of LocalSessionFactoryBean requires Hibernate 4.0
 * or higher. It is similar in role to the same-named class in the <code>orm.hibernate3</code>
 * package. However, in practice, it is closer to <code>AnnotationSessionFactoryBean</code>
 * since its core purpose is to bootstrap a <code>SessionFactory</code> from annotation scanning.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setDataSource
 * @see #setPackagesToScan
 */
public class LocalSessionFactoryBean implements FactoryBean<SessionFactory>, ResourceLoaderAware,
		InitializingBean, DisposableBean {

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private static final Method addAnnotatedClassMethod =
			ClassUtils.getMethodIfAvailable(Configuration.class, "addAnnotatedClass", Class.class);


	private DataSource dataSource;

	private Properties hibernateProperties;

	private String[] packagesToScan;

	private Class<?>[] annotatedClasses;

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private TypeFilter[] entityTypeFilters = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false)};

	private SessionFactory sessionFactory;


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
	 * @see HibernateTransactionManager
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
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
	 * Specify packages to search for autodetection of your entity classes in the
	 * classpath. This is analogous to Spring's component-scan feature
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * Specify annotated entity classes to register with this Hibernate SessionFactory.
	 */
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	public void afterPropertiesSet() {
		Configuration config = new Configuration();
		config.getProperties().put(Environment.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
		config.getProperties().put(Environment.DATASOURCE, this.dataSource);
		config.getProperties().put("hibernate.classLoader.application", this.resourcePatternResolver.getClassLoader());
		if (this.hibernateProperties != null) {
			config.addProperties(this.hibernateProperties);
		}
		scanPackages(config);
		for (Class<?> annotatedClass : this.annotatedClasses) {
			ReflectionUtils.invokeMethod(addAnnotatedClassMethod, config, annotatedClass);
		}
		this.sessionFactory = config.buildSessionFactory();
	}

	/**
	 * Perform Spring-based scanning for entity classes.
	 * @see #setPackagesToScan
	 */
	private void scanPackages(Configuration config) {
		if (this.packagesToScan != null) {
			try {
				for (String pkg : this.packagesToScan) {
					String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
							ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;
					Resource[] resources = this.resourcePatternResolver.getResources(pattern);
					MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
					for (Resource resource : resources) {
						if (resource.isReadable()) {
							MetadataReader reader = readerFactory.getMetadataReader(resource);
							String className = reader.getClassMetadata().getClassName();
							if (matchesFilter(reader, readerFactory)) {
								Class<?> annotatedClass = this.resourcePatternResolver.getClassLoader().loadClass(className);
								ReflectionUtils.invokeMethod(addAnnotatedClassMethod, config, annotatedClass);
							}
						}
					}
				}
			}
			catch (IOException ex) {
				throw new MappingException("Failed to scan classpath for unlisted classes", ex);
			}
			catch (ClassNotFoundException ex) {
				throw new MappingException("Failed to load annotated classes from classpath", ex);
			}
		}
	}

	/**
	 * Check whether any of the configured entity type filters matches
	 * the current class descriptor contained in the metadata reader.
	 */
	private boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		if (this.entityTypeFilters != null) {
			for (TypeFilter filter : this.entityTypeFilters) {
				if (filter.match(reader, readerFactory)) {
					return true;
				}
			}
		}
		return false;
	}


	public SessionFactory getObject() {
		return this.sessionFactory;
	}

	public Class<?> getObjectType() {
		return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
	}

	public boolean isSingleton() {
		return true;
	}


	public void destroy() {
		this.sessionFactory.close();
	}

}
