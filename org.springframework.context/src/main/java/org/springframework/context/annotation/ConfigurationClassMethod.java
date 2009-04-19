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
import java.util.ArrayList;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.core.io.ClassPathResource;

/**
 * Represents a {@link Configuration} class method marked with the {@link Bean} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClass
 * @see ConfigurationClassParser
 * @see ConfigurationClassBeanDefinitionReader
 */
final class ConfigurationClassMethod implements BeanMetadataElement {

	private final String name;

	private final int modifiers;

	private final ReturnType returnType;

	private final ArrayList<Annotation> annotations = new ArrayList<Annotation>();

	private transient ConfigurationClass declaringClass;

	private transient Object source;


	public ConfigurationClassMethod(String name, int modifiers, ReturnType returnType, Annotation... annotations) {
		Assert.hasText(name);
		this.name = name;

		Assert.notNull(annotations);
		for (Annotation annotation : annotations) {
			this.annotations.add(annotation);
		}

		Assert.isTrue(modifiers >= 0, "modifiers must be non-negative: " + modifiers);
		this.modifiers = modifiers;

		Assert.notNull(returnType);
		this.returnType = returnType;
	}

	public String getName() {
		return name;
	}

	public ReturnType getReturnType() {
		return returnType;
	}

	/**
	 * @see java.lang.reflect.Modifier
	 */
	public int getModifiers() {
		return modifiers;
	}

	/**
	 * @return the annotation on this method matching <var>annoType</var> or
	 * {@literal null} if not present.
	 * @see #getRequiredAnnotation(Class)
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> annoType) {
		for (Annotation anno : annotations)
			if (anno.annotationType().equals(annoType))
				return (A) anno;

		return null;
	}

	/**
	 * @return the annotation on this method matching <var>annoType</var>
	 * @throws {@link IllegalStateException} if not present
	 * @see #getAnnotation(Class)
	 */
	public <T extends Annotation> T getRequiredAnnotation(Class<T> annoType) {
		T anno = getAnnotation(annoType);
		if (anno == null) {
			throw new IllegalStateException(
					String.format("required annotation %s is not present on %s", annoType.getSimpleName(), this));
		}
		return anno;
	}

	/**
	 * Set up a bi-directional relationship between this method and its declaring class.
	 * 
	 * @see ConfigurationClass#addMethod(ConfigurationClassMethod)
	 */
	public void setDeclaringClass(ConfigurationClass declaringClass) {
		this.declaringClass = declaringClass;
	}

	public ConfigurationClass getDeclaringClass() {
		return declaringClass;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public Object getSource() {
		return source;
	}

	public Location getLocation() {
		if (declaringClass == null) {
			throw new IllegalStateException(
					"declaringClass property is null. Call setDeclaringClass() before calling getLocation()");
		}
		return new Location(declaringClass.getLocation().getResource(), getSource());
	}

	public void validate(ProblemReporter problemReporter) {
		if (Modifier.isPrivate(getModifiers())) {
			problemReporter.error(new PrivateMethodError());
		}
		if (Modifier.isFinal(getModifiers())) {
			problemReporter.error(new FinalMethodError());
		}
	}


	static class ReturnType implements BeanMetadataElement {

		private String name;

		private boolean isInterface;

		private transient Object source;


		public ReturnType(String name) {
			this.name = name;
		}

		/**
		 * Returns the fully-qualified name of this class.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Sets the fully-qualified name of this class.
		 */
		public void setName(String className) {
			this.name = className;
		}

		/**
		 * Returns the non-qualified name of this class. Given com.acme.Foo, returns 'Foo'.
		 */
		public String getSimpleName() {
			return name == null ? null : ClassUtils.getShortName(name);
		}

		/**
		 * Returns whether the class represented by this ModelClass instance is an interface.
		 */
		public boolean isInterface() {
			return isInterface;
		}

		/**
		 * Signifies that this class is (true) or is not (false) an interface.
		 */
		public void setInterface(boolean isInterface) {
			this.isInterface = isInterface;
		}

		/**
		 * Returns a resource path-formatted representation of the .java file that declares this
		 * class
		 */
		public Object getSource() {
			return source;
		}

		/**
		 * Set the source location for this class. Must be a resource-path formatted string.
		 *
		 * @param source resource path to the .java file that declares this class.
		 */
		public void setSource(Object source) {
			this.source = source;
		}

		public Location getLocation() {
			if (getName() == null) {
				throw new IllegalStateException("'name' property is null. Call setName() before calling getLocation()");
			}
			return new Location(new ClassPathResource(ClassUtils.convertClassNameToResourcePath(getName())), getSource());
		}
	}


	/**
	 * {@link Bean} methods must be non-private in order to accommodate CGLIB.
	 */
	private class PrivateMethodError extends Problem {

		public PrivateMethodError() {
			super(String.format("Method '%s' must not be private; increase the method's visibility to continue",
					getName()), ConfigurationClassMethod.this.getLocation());
		}
	}


	/**
	 * {@link Bean} methods must be non-final in order to accommodate CGLIB.
	 */
	private class FinalMethodError extends Problem {

		public FinalMethodError() {
			super(String.format("Method '%s' must not be final; remove the final modifier to continue",
					getName()), ConfigurationClassMethod.this.getLocation());
		}
	}

}
