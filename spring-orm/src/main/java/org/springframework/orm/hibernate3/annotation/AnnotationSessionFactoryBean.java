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

package org.springframework.orm.hibernate3.annotation;

import java.io.IOException;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;

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
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.util.ClassUtils;

/**
 * Subclass of Spring's standard LocalSessionFactoryBean for Hibernate,
 * supporting JDK 1.5+ annotation metadata for mappings.
 *
 * <p>Note: As of Spring 4.0, this class requires Hibernate 3.6 or later,
 * with the Java Persistence API present.
 *
 * <p>Example for an AnnotationSessionFactoryBean bean definition:
 *
 * <pre class="code">
 * &lt;bean id="sessionFactory" class="org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean"&gt;
 *   &lt;property name="dataSource" ref="dataSource"/&gt;
 *   &lt;property name="annotatedClasses"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;test.package.Foo&lt;/value&gt;
 *       &lt;value&gt;test.package.Bar&lt;/value&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Or when using classpath scanning for autodetection of entity classes:
 *
 * <pre class="code">
 * &lt;bean id="sessionFactory" class="org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean"&gt;
 *   &lt;property name="dataSource" ref="dataSource"/&gt;
 *   &lt;property name="packagesToScan" value="test.package"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.2.2
 * @see #setDataSource
 * @see #setHibernateProperties
 * @see #setAnnotatedClasses
 * @see #setAnnotatedPackages
 */
public class AnnotationSessionFactoryBean extends LocalSessionFactoryBean implements ResourceLoaderAware {

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";


	private Class[] annotatedClasses;

	private String[] annotatedPackages;

	private String[] packagesToScan;

	private TypeFilter[] entityTypeFilters = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false),
			new AnnotationTypeFilter(org.hibernate.annotations.Entity.class, false)};

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();


	/**
	 * Specify annotated classes, for which mappings will be read from
	 * class-level JDK 1.5+ annotation metadata.
	 * @see org.hibernate.cfg.Configuration#addAnnotatedClass(Class)
	 */
	public void setAnnotatedClasses(Class[] annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	/**
	 * Specify the names of annotated packages, for which package-level
	 * JDK 1.5+ annotation metadata will be read.
	 * @see org.hibernate.cfg.Configuration#addPackage(String)
	 */
	public void setAnnotatedPackages(String[] annotatedPackages) {
		this.annotatedPackages = annotatedPackages;
	}

	/**
	 * Specify packages to search using Spring-based scanning for entity classes in
	 * the classpath. This is an alternative to listing annotated classes explicitly.
	 * <p>Default is none. Specify packages to search for autodetection of your entity
	 * classes in the classpath. This is analogous to Spring's component-scan feature
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 */
	public void setPackagesToScan(String[] packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * Specify custom type filters for Spring-based scanning for entity classes.
	 * <p>Default is to search all specified packages for classes annotated with
	 * {@code @javax.persistence.Entity}, {@code @javax.persistence.Embeddable}
	 * or {@code @javax.persistence.MappedSuperclass}, as well as for
	 * Hibernate's special {@code @org.hibernate.annotations.Entity}.
	 * @see #setPackagesToScan
	 */
	public void setEntityTypeFilters(TypeFilter[] entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	/**
	 * Reads metadata from annotated classes and packages into the
	 * AnnotationConfiguration instance.
	 */
	@Override
	protected void postProcessMappings(Configuration config) throws HibernateException {
		if (this.annotatedClasses != null) {
			for (Class annotatedClass : this.annotatedClasses) {
				config.addAnnotatedClass(annotatedClass);
			}
		}
		if (this.annotatedPackages != null) {
			for (String annotatedPackage : this.annotatedPackages) {
				config.addPackage(annotatedPackage);
			}
		}
		scanPackages(config);
	}

	/**
	 * Perform Spring-based scanning for entity classes.
	 * @see #setPackagesToScan
	 */
	protected void scanPackages(Configuration config) {
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
							if (matchesEntityTypeFilter(reader, readerFactory)) {
								config.addAnnotatedClass(this.resourcePatternResolver.getClassLoader().loadClass(className));
							}
							else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
								config.addPackage(className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
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

}
