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

package org.springframework.context.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * Includes a set of {@link Bean} methods, including all such methods
 * defined in the ancestry of the class, in a 'flattened-out' manner.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 * @see BeanMethod
 * @see ConfigurationClassParser
 */
final class ConfigurationClass {

	private final AnnotationMetadata metadata;

	private final Resource resource;

	private String beanName;

	private final ConfigurationClass importedBy;

	private final Set<BeanMethod> beanMethods = new LinkedHashSet<BeanMethod>();

	private final Map<String, Class<? extends BeanDefinitionReader>> importedResources =
			new LinkedHashMap<String, Class<? extends BeanDefinitionReader>>();

	private final Set<ImportBeanDefinitionRegistrar> importBeanDefinitionRegistrars =
			new LinkedHashSet<ImportBeanDefinitionRegistrar>();


	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param beanName must not be {@code null}
	 * @throws IllegalArgumentException if beanName is null (as of Spring 3.1.1)
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	public ConfigurationClass(MetadataReader metadataReader, String beanName) {
		Assert.hasText(beanName, "bean name must not be null");
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.beanName = beanName;
		this.importedBy = null;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if importedBy is not {@code null}).
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param importedBy the configuration class importing this one or {@code null}
	 * @since 3.1.1
	 */
	public ConfigurationClass(MetadataReader metadataReader, ConfigurationClass importedBy) {
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.importedBy = importedBy;
	}

	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param clazz the underlying {@link Class} to represent
	 * @param beanName name of the {@code @Configuration} class bean
	 * @throws IllegalArgumentException if beanName is null (as of Spring 3.1.1)
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	public ConfigurationClass(Class<?> clazz, String beanName) {
		Assert.hasText(beanName, "Bean name must not be null");
		this.metadata = new StandardAnnotationMetadata(clazz, true);
		this.resource = new DescriptiveResource(clazz.toString());
		this.beanName = beanName;
		this.importedBy = null;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if imported is {@code true}).
	 * @param clazz the underlying {@link Class} to represent
	 * @param importedBy the configuration class importing this one or {@code null}
	 * @since 3.1.1
	 */
	public ConfigurationClass(Class<?> clazz, ConfigurationClass importedBy) {
		this.metadata = new StandardAnnotationMetadata(clazz, true);
		this.resource = new DescriptiveResource(clazz.toString());
		this.importedBy = importedBy;
	}


	public AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	public Resource getResource() {
		return this.resource;
	}

	public String getSimpleName() {
		return ClassUtils.getShortName(getMetadata().getClassName());
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return whether this configuration class was registered via @{@link Import} or
	 * automatically registered due to being nested within another configuration class.
	 * @since 3.1.1
	 * @see #getImportedBy()
	 */
	public boolean isImported() {
		return (this.importedBy != null);
	}

	/**
	 * Returns the configuration class that imported this class or {@code null} if
	 * this configuration was not imported.
	 * @since 4.0
	 * @see #isImported()
	 */
	public ConfigurationClass getImportedBy() {
		return this.importedBy;
	}

	public void addBeanMethod(BeanMethod method) {
		this.beanMethods.add(method);
	}

	public Set<BeanMethod> getBeanMethods() {
		return this.beanMethods;
	}

	public void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
		this.importedResources.put(importedResource, readerClass);
	}

	public void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar) {
		this.importBeanDefinitionRegistrars.add(registrar);
	}

	public Set<ImportBeanDefinitionRegistrar> getImportBeanDefinitionRegistrars() {
		return Collections.unmodifiableSet(importBeanDefinitionRegistrars);
	}

	public Map<String, Class<? extends BeanDefinitionReader>> getImportedResources() {
		return this.importedResources;
	}

	public void validate(ProblemReporter problemReporter) {
		// A configuration class may not be final (CGLIB limitation)
		if (getMetadata().isAnnotated(Configuration.class.getName())) {
			if (getMetadata().isFinal()) {
				problemReporter.error(new FinalConfigurationProblem());
			}
		}

		// An @Bean method may only be overloaded through inheritance. No single
		// @Configuration class may declare two @Bean methods with the same name.
		MultiValueMap<String, BeanMethod> methods = new LinkedMultiValueMap<String, BeanMethod>();
		for (BeanMethod beanMethod : this.beanMethods) {
			String methodName = beanMethod.getMetadata().getMethodName();
			methods.add(methodName, beanMethod);
		}
		
		for (String methodName : methods.keySet()) {
			List<BeanMethod> methodList = methods.get(methodName);
			if (methodList.size() > 1) {
				// Check for overloading
				String[] firstParameters = methodList.get(0).getMetadata().getParameterTypes();

				for (int i = 1; i < methodList.size(); i++) {
					BeanMethod method = methodList.get(i);
					String[] methodParameters =  method.getMetadata().getParameterTypes();

					if (!Arrays.equals(firstParameters, methodParameters)) {
						// Overloaded
						problemReporter.error(new BeanMethodOverloadingProblem(methodName, methodList.size()));
					}
				}
			}
		}

		for (BeanMethod beanMethod : this.beanMethods) {
			beanMethod.validate(problemReporter);
		}
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof ConfigurationClass &&
				getMetadata().getClassName().equals(((ConfigurationClass) other).getMetadata().getClassName())));
	}

	@Override
	public int hashCode() {
		return getMetadata().getClassName().hashCode();
	}

	@Override
	public String toString() {
		return String.format("[ConfigurationClass:beanName=%s,resource=%s]", this.beanName, this.resource);
	}


	/**
	 * Configuration classes must be non-final to accommodate CGLIB subclassing.
	 */
	private class FinalConfigurationProblem extends Problem {

		public FinalConfigurationProblem() {
			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
					getSimpleName()), new Location(getResource(), getMetadata()));
		}
	}


	/**
	 * Bean methods on configuration classes may only be overloaded through inheritance.
	 */
	private class BeanMethodOverloadingProblem extends Problem {

		public BeanMethodOverloadingProblem(String methodName, int count) {
			super(String.format("@Configuration class '%s' has %s overloaded @Bean methods named '%s'. " +
					"Only one @Bean method of a given name is allowed within each @Configuration class.",
					getSimpleName(), count, methodName), new Location(getResource(), getMetadata()));
		}
	}

}
