/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * Includes a set of {@link Bean} methods, including all such methods defined in the
 * ancestry of the class, in a 'flattened-out' manner.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see BeanMethod
 * @see ConfigurationClassParser
 */
final class ConfigurationClass {

	private final AnnotationMetadata metadata;

	private final Resource resource;

	private final Map<String, Class<? extends BeanDefinitionReader>> importedResources =
			new LinkedHashMap<String, Class<? extends BeanDefinitionReader>>();

	private final Set<BeanMethod> beanMethods = new LinkedHashSet<BeanMethod>();

	private String beanName;

	private final boolean imported;


	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param beanName must not be {@code null}
	 * @throws IllegalArgumentException if beanName is null (as of Spring 3.1.1)
	 * @see ConfigurationClass#ConfigurationClass(Class, boolean)
	 */
	public ConfigurationClass(MetadataReader metadataReader, String beanName) {
		Assert.hasText(beanName, "bean name must not be null");
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.beanName = beanName;
		this.imported = false;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if imported is {@code true}).
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param beanName name of the {@code @Configuration} class bean
	 * @since 3.1.1
	 */
	public ConfigurationClass(MetadataReader metadataReader, boolean imported) {
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.imported = imported;
	}

	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param clazz the underlying {@link Class} to represent
	 * @param beanName name of the {@code @Configuration} class bean
	 * @throws IllegalArgumentException if beanName is null (as of Spring 3.1.1)
	 * @see ConfigurationClass#ConfigurationClass(Class, boolean)
	 */
	public ConfigurationClass(Class<?> clazz, String beanName) {
		Assert.hasText(beanName, "bean name must not be null");
		this.metadata = new StandardAnnotationMetadata(clazz, true);
		this.resource = new DescriptiveResource(clazz.toString());
		this.beanName = beanName;
		this.imported = false;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if imported is {@code true}).
	 * @param clazz the underlying {@link Class} to represent
	 * @param beanName name of the {@code @Configuration} class bean
	 * @since 3.1.1
	 */
	public ConfigurationClass(Class<?> clazz, boolean imported) {
		this.metadata = new StandardAnnotationMetadata(clazz, true);
		this.resource = new DescriptiveResource(clazz.toString());
		this.imported = imported;
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

	/**
	 * Return whether this configuration class was registered via @{@link Import} or
	 * automatically registered due to being nested within another configuration class.
	 * @since 3.1.1
	 */
	public boolean isImported() {
		return this.imported;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return this.beanName;
	}

	public void addBeanMethod(BeanMethod method) {
		this.beanMethods.add(method);
	}

	public Set<BeanMethod> getBeanMethods() {
		return this.beanMethods;
	}

	public void addImportedResource(
			String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
		this.importedResources.put(importedResource, readerClass);
	}

	public Map<String, Class<? extends BeanDefinitionReader>> getImportedResources() {
		return this.importedResources;
	}

	public void validate(ProblemReporter problemReporter) {
		// An @Bean method may only be overloaded through inheritance. No single
		// @Configuration class may declare two @Bean methods with the same name.
		final char hashDelim = '#';
		Map<String, Integer> methodNameCounts = new HashMap<String, Integer>();
		for (BeanMethod beanMethod : beanMethods) {
			String dClassName = beanMethod.getMetadata().getDeclaringClassName();
			String methodName = beanMethod.getMetadata().getMethodName();
			String fqMethodName = dClassName + hashDelim + methodName;
			Integer currentCount = methodNameCounts.get(fqMethodName);
			int newCount = currentCount != null ? currentCount + 1 : 1;
			methodNameCounts.put(fqMethodName, newCount);
		}
		
		for (String methodName : methodNameCounts.keySet()) {
			int count = methodNameCounts.get(methodName);
			if (count > 1) {
				String shortMethodName = methodName.substring(methodName.indexOf(hashDelim)+1);
				problemReporter.error(new BeanMethodOverloadingProblem(shortMethodName, count));
			}
		}
		
		// A configuration class may not be final (CGLIB limitation)
		if (getMetadata().isAnnotated(Configuration.class.getName())) {
			if (getMetadata().isFinal()) {
				problemReporter.error(new FinalConfigurationProblem());
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
