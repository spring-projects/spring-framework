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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * Includes a set of {@link Bean} methods, including all such methods defined in the
 * ancestry of the class, in a 'flattened-out' manner. Note that each {@link ConfigurationClassMethod}
 * representation contains source information about where it was originally detected
 * (for the purpose of tooling with Spring IDE).
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassMethod
 * @see ConfigurationClassParser
 */
final class ConfigurationClass implements BeanMetadataElement {

	private String name;

	private ConfigurationClass declaringClass;

	private Object source;

	private String beanName;

	private int modifiers;

	private final Set<Annotation> annotations = new HashSet<Annotation>();

	private final Set<ConfigurationClassMethod> methods = new LinkedHashSet<ConfigurationClassMethod>();

	private final Map<String, Integer> overloadedMethodMap = new LinkedHashMap<String, Integer>();


	/**
	 * Sets the fully-qualified name of this class.
	 */
	public void setName(String className) {
		this.name = className;
	}

	/**
	 * Returns the fully-qualified name of this class.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the non-qualified name of this class. Given com.acme.Foo, returns 'Foo'.
	 */
	public String getSimpleName() {
		return name == null ? null : ClassUtils.getShortName(name);
	}

	public void setDeclaringClass(ConfigurationClass configurationClass) {
		this.declaringClass = configurationClass;
	}

	public ConfigurationClass getDeclaringClass() {
		return declaringClass;
	}

	/**
	 * Set the source location for this class. Must be a resource-path formatted string.
	 * @param source resource path to the .java file that declares this class.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	/**
	 * Returns a resource path-formatted representation of the .java file that declares this
	 * class
	 */
	public Object getSource() {
		return source;
	}

	public Location getLocation() {
		if (getName() == null) {
			throw new IllegalStateException("'name' property is null. Call setName() before calling getLocation()");
		}
		return new Location(new ClassPathResource(ClassUtils.convertClassNameToResourcePath(getName())), getSource());
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setModifiers(int modifiers) {
		Assert.isTrue(modifiers >= 0, "modifiers must be non-negative");
		this.modifiers = modifiers;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void addAnnotation(Annotation annotation) {
		this.annotations.add(annotation);
	}

	/**
	 * @return the annotation on this class matching <var>annoType</var> or
	 * {@literal null} if not present.
	 * @see #getRequiredAnnotation(Class)
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> annoType) {
		for (Annotation annotation : annotations) {
			if (annotation.annotationType().equals(annoType)) {
				return (A) annotation;
			}
		}
		return null;
	}

	/**
	 * @return the annotation on this class matching <var>annoType</var>
	 * @throws {@link IllegalStateException} if not present
	 * @see #getAnnotation(Class)
	 */
	public <A extends Annotation> A getRequiredAnnotation(Class<A> annoType) {
		A anno = getAnnotation(annoType);
		if (anno == null) {
			throw new IllegalStateException(
					String.format("Required annotation %s is not present on %s", annoType.getSimpleName(), this));
		}
		return anno;
	}

	public ConfigurationClass addMethod(ConfigurationClassMethod method) {
		method.setDeclaringClass(this);
		methods.add(method);
		Integer count = overloadedMethodMap.get(method.getName());
		if (count != null) {
			overloadedMethodMap.put(method.getName(), count + 1);
		}
		else {
			overloadedMethodMap.put(method.getName(), 1);
		}
		return this;
	}

	public Set<ConfigurationClassMethod> getBeanMethods() {
		return methods;
	}

	public void validate(ProblemReporter problemReporter) {
		// No overloading of factory methods allowed
		for (Map.Entry<String, Integer> entry : overloadedMethodMap.entrySet()) {
			String methodName = entry.getKey();
			int count = entry.getValue();
			if (count > 1) {
				problemReporter.error(new OverloadedMethodProblem(methodName, count));
			}
		}

		// A configuration class may not be final (CGLIB limitation)
		if (getAnnotation(Configuration.class) != null) {
			if (Modifier.isFinal(modifiers)) {
				problemReporter.error(new FinalConfigurationProblem());
			}
			for (ConfigurationClassMethod method : methods) {
				method.validate(problemReporter);
			}
		}
	}


	/** Configuration classes must be non-final to accommodate CGLIB subclassing. */
	private class FinalConfigurationProblem extends Problem {

		public FinalConfigurationProblem() {
			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
					getSimpleName()), ConfigurationClass.this.getLocation());
		}
	}


	/** Factory methods on configuration classes must not be overloaded. */
	private class OverloadedMethodProblem extends Problem {

		public OverloadedMethodProblem(String methodName, int count) {
			super(String.format("@Configuration class '%s' has %s overloaded factory methods of name '%s'. " +
					"Only one factory method of the same name allowed.",
					getSimpleName(), count, methodName), ConfigurationClass.this.getLocation());
		}
	}


}
