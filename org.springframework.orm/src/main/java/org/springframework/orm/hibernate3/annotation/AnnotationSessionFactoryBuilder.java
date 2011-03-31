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

package org.springframework.orm.hibernate3.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.orm.hibernate3.SessionFactoryBuilderSupport;
import org.springframework.util.ClassUtils;

/**
 * Hibernate {@link AnnotationConfiguration} builder suitable for use within Spring
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * class {@link Bean @Bean} methods. For complete details on features, see the
 * JavaDoc for the {@link SessionFactoryBuilderSupport} superclass. For use in
 * Spring XML configuration, see the {@link AnnotationSessionFactoryBean} subclass.
 *
 * <p>As noted in {@code SessionFactoryBuilderSupport} JavaDoc, this class requires
 * Hibernate 3.2 or later; it additionally requires that the Java Persistence API
 * and Hibernate Annotations add-ons are present.
 *
 * <p>Setter methods return the builder instance in order to facilitate
 * a concise and convenient method-chaining style. For example:
 * <pre class="code">
 * {@code @Configuration}
 * public class DataConfig {
 *     {@code @Bean}
 *     public SessionFactory sessionFactory() {
 *         return new AnnotationSessionFactoryBuilder()
 *             .setDataSource(dataSource())
 *             .setPackagesToScan("com.myco"})
 *             .buildSessionFactory();
 *     }
 * }
 * </pre>
 *
 * <p>Most Hibernate configuration operations can be performed directly against
 * this API; however you may also access access and configure the underlying
 * {@code AnnotationConfiguration} object by using the {@link #doWithConfiguration}
 * method and providing a {@code HibernateConfigurationCallback} as follows:
 * <pre class="code">
 * SessionFactory sessionFactory =
 *     new AnnotationSessionFactoryBuilder()
 *         // ...
 *         .doWithConfiguration(new HibernateConfigurationCallback&lt;AnnotationConfiguration&gt;() {
 *             public void configure(AnnotationConfiguration cfg) {
 *                 cfg.addAnnotatedClass(Foo.class);
 *             }
 *          })
 *         .buildSessionFactory();
 * </pre>
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.orm.hibernate3.SessionFactoryBuilderSupport
 * @see org.springframework.orm.hibernate3.SessionFactoryBuilder
 * @see AnnotationSessionFactoryBean
 */
public class AnnotationSessionFactoryBuilder extends SessionFactoryBuilderSupport<AnnotationSessionFactoryBuilder> {

	/** Hibernate 3.6 consolidates Configuration and AnnotationConfiguration operations. */
	private static final boolean hibernate36Present = ClassUtils.hasMethod(Configuration.class, "addAnnotatedClass", Class.class);

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private Class<?>[] annotatedClasses;

	private String[] annotatedPackages;

	private String[] packagesToScan;

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private TypeFilter[] entityTypeFilters = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false),
			new AnnotationTypeFilter(org.hibernate.annotations.Entity.class, false)};


	/**
	 * Construct a new {@code AnnotationSessionFactoryBuilder}
	 */
	public AnnotationSessionFactoryBuilder() {
		super();
	}

	/**
	 * Construct a new {@code AnnotationSessionFactoryBuilder} with the given
	 * Spring-managed {@code DataSource} instance.
	 * @see #setDataSource
	 */
	public AnnotationSessionFactoryBuilder(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation returns {@link org.hibernate.cfg.Configuration} if
	 * Hibernate 3.6 or greater is available on the runtime classpath, otherwise
	 * {@link org.hibernate.cfg.AnnotationConfiguration}. This accommodates
	 * the consolidation of these two types and deprecation of the latter in
	 * Hibernate 3.6.
	 * @see #doWithConfiguration
	 */
	protected Class<? extends Configuration> getDefaultConfigurationClass() {
		return hibernate36Present ? Configuration.class : AnnotationConfiguration.class;
	}

	/**
	 * Set whether to use Spring-based scanning for entity classes in the classpath
	 * instead of listing annotated classes explicitly.
	 * <p>Default is none. Specify packages to search for autodetection of your entity
	 * classes in the classpath. This is analogous to Spring's component-scan feature
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 */
	public AnnotationSessionFactoryBuilder setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
		return this;
	}

	/**
	 * Specify annotated classes, for which mappings will be read from
	 * class-level JDK 1.5+ annotation metadata.
	 * @see org.hibernate.cfg.AnnotationConfiguration#addAnnotatedClass
	 */
	public AnnotationSessionFactoryBuilder setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
		return this;
	}

	/**
	 * Specify the names of annotated packages, for which package-level
	 * JDK 1.5+ annotation metadata will be read.
	 * @see org.hibernate.cfg.AnnotationConfiguration#addPackage
	 */
	public AnnotationSessionFactoryBuilder setAnnotatedPackages(String... annotatedPackages) {
		this.annotatedPackages = annotatedPackages;
		return this;
	}

	/**
	 * Specify custom type filters for Spring-based scanning for entity classes.
	 * <p>Default is to search all specified packages for classes annotated with
	 * <code>@javax.persistence.Entity</code>, <code>@javax.persistence.Embeddable</code>
	 * or <code>@javax.persistence.MappedSuperclass</code>, as well as for
	 * Hibernate's special <code>@org.hibernate.annotations.Entity</code>.
	 * @see #setPackagesToScan
	 */
	public AnnotationSessionFactoryBuilder setEntityTypeFilters(TypeFilter... entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
		return this;
	}

	public AnnotationSessionFactoryBuilder setResourcePatternResolver(ResourcePatternResolver resourcePatternResolver) {
		this.resourcePatternResolver = resourcePatternResolver;
		return this;
	}

	/**
	 * Perform Spring-based scanning for entity classes.
	 * @see #setPackagesToScan
	 */
	protected void scanPackages() {
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
								invokeConfigurationMethod("addAnnotatedClass", Class.class, annotatedClass);
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

	/**
	 * Reads metadata from annotated classes and packages into the
	 * AnnotationConfiguration instance.
	 */
	@Override
	protected void postProcessMappings() throws HibernateException {
		// org.hibernate.cfg.Configuration cfg = getConfiguration();
		if (this.annotatedClasses != null) {
			for (Class<?> annotatedClass : this.annotatedClasses) {
				invokeConfigurationMethod("addAnnotatedClass", Class.class, annotatedClass);
			}
		}
		if (this.annotatedPackages != null) {
			for (String annotatedPackage : this.annotatedPackages) {
				invokeConfigurationMethod("addPackage", String.class, annotatedPackage);
			}
		}
		scanPackages();
	}

	/**
	 * Delegates to {@link #postProcessAnnotationConfiguration}.
	 */
	@Override
	protected void postProcessConfiguration() throws HibernateException {
		postProcessAnnotationConfiguration();
	}

	/**
	 * To be implemented by subclasses which want to to perform custom
	 * post-processing of the AnnotationConfiguration object after this
	 * FactoryBean performed its default initialization.
	 * <p>Note: As of Hibernate 3.6, AnnotationConfiguration's features
	 * have been rolled into Configuration itself. Simply overriding
	 * {@link #postProcessConfiguration(org.hibernate.cfg.Configuration)}
	 * becomes an option as well then.
	 * @param config the current AnnotationConfiguration object
	 * @throws HibernateException in case of Hibernate initialization errors
	 */
	protected void postProcessAnnotationConfiguration() throws HibernateException {
	}

	/**
	 * Reflectively invoke the given method against the underlying Configuration
	 * instance. In order to support the consolidation of Hibernate's
	 * AnnotationConfiguration into Configuration in Hibernate 3.6 while continuing
	 * to to compile against Hibernate 3.3.x, we must reflectively invoke in order
	 * to avoid compilation failure.
	 */
	private <T> void invokeConfigurationMethod(String methodName, Class<T> parameterType, T parameter) {
		org.hibernate.cfg.Configuration cfg = getConfiguration();
		try {
			Method m = cfg.getClass().getMethod(methodName, parameterType);
			m.invoke(cfg, parameter);
		} catch (Exception ex) {
			throw new IllegalStateException(String.format("Hibernate Configuration class [%s] does not support the '%s(%s)' method.",
					cfg.getClass().getSimpleName(), methodName, parameterType.getSimpleName()));
		}
	}

}
