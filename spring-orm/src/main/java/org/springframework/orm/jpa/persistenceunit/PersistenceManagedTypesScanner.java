/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.orm.jpa.persistenceunit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;

import org.springframework.context.index.CandidateComponentsIndex;
import org.springframework.context.index.CandidateComponentsIndexLoader;
import org.springframework.core.SpringProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.ClassFormatException;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

/**
 * Scanner of {@link PersistenceManagedTypes}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
@SuppressWarnings("removal") // components index
public final class PersistenceManagedTypesScanner {

	private static final String CLASS_RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";

	private static final String IGNORE_CLASSFORMAT_PROPERTY_NAME = "spring.classformat.ignore";

	private static final boolean shouldIgnoreClassFormatException =
			SpringProperties.getFlag(IGNORE_CLASSFORMAT_PROPERTY_NAME);

	private static final Set<AnnotationTypeFilter> entityTypeFilters = CollectionUtils.newLinkedHashSet(4);

	static {
		entityTypeFilters.add(new AnnotationTypeFilter(Entity.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(Embeddable.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(MappedSuperclass.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(Converter.class, false));
	}

	private final ResourcePatternResolver resourcePatternResolver;

	@Nullable
	private final CandidateComponentsIndex componentsIndex;

	private final ManagedClassNameFilter managedClassNameFilter;


	/**
	 * Create a new {@code PersistenceManagedTypesScanner} for the given resource loader.
	 * @param resourceLoader the {@code ResourceLoader} to use
	 */
	public PersistenceManagedTypesScanner(ResourceLoader resourceLoader) {
		this(resourceLoader, null);
	}

	/**
	 * Create a new {@code PersistenceManagedTypesScanner} for the given resource loader.
	 * @param resourceLoader the {@code ResourceLoader} to use
	 * @param managedClassNameFilter an optional predicate to filter entity classes
	 * @since 6.1.4
	 */
	public PersistenceManagedTypesScanner(ResourceLoader resourceLoader,
			@Nullable ManagedClassNameFilter managedClassNameFilter) {

		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		this.componentsIndex = CandidateComponentsIndexLoader.loadIndex(resourceLoader.getClassLoader());
		this.managedClassNameFilter = (managedClassNameFilter != null ? managedClassNameFilter : className -> true);
	}


	/**
	 * Scan the specified packages and return a {@link PersistenceManagedTypes} that
	 * represents the result of the scanning.
	 * @param packagesToScan the packages to scan
	 * @return the {@link PersistenceManagedTypes} instance
	 */
	public PersistenceManagedTypes scan(String... packagesToScan) {
		ScanResult scanResult = new ScanResult();
		for (String pkg : packagesToScan) {
			scanPackage(pkg, scanResult);
		}
		return scanResult.toJpaManagedTypes();
	}

	private void scanPackage(String pkg, ScanResult scanResult) {
		if (this.componentsIndex != null) {
			Set<String> candidates = new HashSet<>();
			for (AnnotationTypeFilter filter : entityTypeFilters) {
				candidates.addAll(this.componentsIndex.getCandidateTypes(pkg, filter.getAnnotationType().getName()));
			}
			scanResult.managedClassNames.addAll(candidates.stream().filter(this.managedClassNameFilter::matches).toList());
			scanResult.managedPackages.addAll(this.componentsIndex.getCandidateTypes(pkg, "package-info"));
			return;
		}

		try {
			String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
					ClassUtils.convertClassNameToResourcePath(pkg) + CLASS_RESOURCE_PATTERN;
			Resource[] resources = this.resourcePatternResolver.getResources(pattern);
			MetadataReaderFactory factory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
			for (Resource resource : resources) {
				try {
					MetadataReader reader = factory.getMetadataReader(resource);
					String className = reader.getClassMetadata().getClassName();
					if (matchesEntityTypeFilter(reader, factory) && this.managedClassNameFilter.matches(className)) {
						scanResult.managedClassNames.add(className);
						if (scanResult.persistenceUnitRootUrl == null) {
							URL url = resource.getURL();
							if (ResourceUtils.isJarURL(url)) {
								scanResult.persistenceUnitRootUrl = ResourceUtils.extractJarFileURL(url);
							}
						}
					}
					else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
						scanResult.managedPackages.add(className.substring(0,
								className.length() - PACKAGE_INFO_SUFFIX.length()));
					}
				}
				catch (FileNotFoundException ex) {
					// Ignore non-readable resource
				}
				catch (ClassFormatException ex) {
					if (!shouldIgnoreClassFormatException) {
						throw new PersistenceException("Incompatible class format in " + resource, ex);
					}
				}
				catch (Throwable ex) {
					throw new PersistenceException("Failed to read candidate component class: " + resource, ex);
				}
			}
		}
		catch (IOException ex) {
			throw new PersistenceException("Failed to scan classpath for unlisted entity classes", ex);
		}
	}

	/**
	 * Check whether any of the configured entity type filters matches
	 * the current class descriptor contained in the metadata reader.
	 */
	private boolean matchesEntityTypeFilter(MetadataReader reader, MetadataReaderFactory factory) throws IOException {
		for (TypeFilter filter : entityTypeFilters) {
			if (filter.match(reader, factory)) {
				return true;
			}
		}
		return false;
	}


	private static class ScanResult {

		private final List<String> managedClassNames = new ArrayList<>();

		private final List<String> managedPackages = new ArrayList<>();

		@Nullable
		private URL persistenceUnitRootUrl;

		PersistenceManagedTypes toJpaManagedTypes() {
			return new SimplePersistenceManagedTypes(this.managedClassNames,
					this.managedPackages, this.persistenceUnitRootUrl);
		}
	}

}
