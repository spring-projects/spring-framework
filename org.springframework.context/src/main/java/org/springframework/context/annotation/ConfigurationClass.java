/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * Includes a set of {@link Bean} methods, including all such methods defined in the
 * ancestry of the class, in a 'flattened-out' manner.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassMethod
 * @see ConfigurationClassParser
 */
final class ConfigurationClass {

	private final AnnotationMetadata metadata;

	private final Resource resource;

	private final Map<String, Class> importedResources = new LinkedHashMap<String, Class>();

	private final Set<ConfigurationClassMethod> methods = new LinkedHashSet<ConfigurationClassMethod>();

	private final Map<String, Integer> overloadedMethodMap = new LinkedHashMap<String, Integer>();

	private String beanName;


	public ConfigurationClass(MetadataReader metadataReader, String beanName) {
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.beanName = beanName;
	}

	public ConfigurationClass(Class<?> clazz, String beanName) {
		this.metadata = new StandardAnnotationMetadata(clazz);
		this.resource = new DescriptiveResource(clazz.toString());
		this.beanName = beanName;
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

	public ConfigurationClass addMethod(ConfigurationClassMethod method) {
		this.methods.add(method);
		String name = method.getMetadata().getMethodName();
		Integer count = this.overloadedMethodMap.get(name);
		if (count != null) {
			this.overloadedMethodMap.put(name, count + 1);
		}
		else {
			this.overloadedMethodMap.put(name, 1);
		}
		return this;
	}

	public Set<ConfigurationClassMethod> getMethods() {
		return this.methods;
	}

	public void addImportedResource(String importedResource, Class readerClass) {
		this.importedResources.put(importedResource, readerClass);
	}

	public Map<String, Class> getImportedResources() {
		return this.importedResources;
	}


	public void validate(ProblemReporter problemReporter) {
		// No overloading of factory methods allowed
		for (Map.Entry<String, Integer> entry : this.overloadedMethodMap.entrySet()) {
			String methodName = entry.getKey();
			int count = entry.getValue();
			if (count > 1) {
				problemReporter.error(new OverloadedMethodProblem(methodName, count));
			}
		}

		// A configuration class may not be final (CGLIB limitation)
		if (getMetadata().isAnnotated(Configuration.class.getName())) {
			if (getMetadata().isFinal()) {
				problemReporter.error(new FinalConfigurationProblem());
			}
			for (ConfigurationClassMethod method : this.methods) {
				method.validate(problemReporter);
			}
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


	/** Configuration classes must be non-final to accommodate CGLIB subclassing. */
	private class FinalConfigurationProblem extends Problem {

		public FinalConfigurationProblem() {
			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
					getSimpleName()), new Location(getResource(), getMetadata()));
		}
	}


	/** Factory methods on configuration classes must not be overloaded. */
	private class OverloadedMethodProblem extends Problem {

		public OverloadedMethodProblem(String methodName, int count) {
			super(String.format("@Configuration class '%s' has %s overloaded factory methods of name '%s'. " +
					"Only one factory method of the same name allowed.",
					getSimpleName(), count, methodName), new Location(getResource(), getMetadata()));
		}
	}

}
