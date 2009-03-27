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

import static java.lang.String.*;
import static org.springframework.context.annotation.StandardScopes.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.util.Assert;


/**
 * Represents a {@link Configuration} class method marked with the {@link Bean} annotation.
 *
 * @author Chris Beams
 * @see ConfigurationClass
 * @see ConfigurationModel
 * @see ConfigurationParser
 * @see ConfigurationModelBeanDefinitionReader
 */
final class BeanMethod implements BeanMetadataElement {

	private final String name;
	private final int modifiers;
	private final ModelClass returnType;
	private final ArrayList<Annotation> annotations = new ArrayList<Annotation>();

	private transient ConfigurationClass declaringClass;
	private transient Object source;

	public BeanMethod(String name, int modifiers, ModelClass returnType, Annotation... annotations) {
		Assert.hasText(name);
		this.name = name;

		Assert.notNull(annotations);
		for (Annotation annotation : annotations)
			this.annotations.add(annotation);

		Assert.isTrue(modifiers >= 0, "modifiers must be non-negative: " + modifiers);
		this.modifiers = modifiers;

		Assert.notNull(returnType);
		this.returnType = returnType;
	}

	public String getName() {
		return name;
	}

	public ModelClass getReturnType() {
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
	public <T extends Annotation> T getAnnotation(Class<T> annoType) {
		for (Annotation anno : annotations)
			if (anno.annotationType().equals(annoType))
				return (T) anno;

		return null;
	}

	/**
	 * @return the annotation on this method matching <var>annoType</var>
	 * @throws {@link IllegalStateException} if not present
	 * @see #getAnnotation(Class)
	 */
	public <T extends Annotation> T getRequiredAnnotation(Class<T> annoType) {
		T anno = getAnnotation(annoType);

		Assert.notNull(anno, format("annotation %s not found on %s", annoType.getSimpleName(), this));

		return anno;
	}

	/**
	 * Set up a bi-directional relationship between this method and its declaring class.
	 * 
	 * @see ConfigurationClass#addBeanMethod(BeanMethod)
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
		if (declaringClass == null)
			throw new IllegalStateException(
					"declaringClass property is null. Call setDeclaringClass() before calling getLocation()");
		return new Location(declaringClass.getLocation().getResource(), getSource());
	}

	public void validate(ProblemReporter problemReporter) {

		if (Modifier.isPrivate(getModifiers()))
			problemReporter.error(new PrivateMethodError());

		if (Modifier.isFinal(getModifiers()))
			problemReporter.error(new FinalMethodError());
		
		Scope scope = this.getAnnotation(Scope.class);
		if(scope != null
			&& scope.proxyMode() != ScopedProxyMode.NO
			&& (scope.value().equals(SINGLETON) || scope.value().equals(PROTOTYPE)))
				problemReporter.error(new InvalidScopedProxyDeclarationError(this));
	}

	@Override
	public String toString() {
		String returnTypeName = returnType == null ? "<unknown>" : returnType.getSimpleName();
		return format("%s: name=%s; returnType=%s; modifiers=%d",
		              getClass().getSimpleName(), name, returnTypeName, modifiers);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + modifiers;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BeanMethod other = (BeanMethod) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (modifiers != other.modifiers)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (returnType == null) {
			if (other.returnType != null)
				return false;
		} else if (!returnType.equals(other.returnType))
			return false;
		return true;
	}

	/** {@link Bean} methods must be non-private in order to accommodate CGLIB. */
	class PrivateMethodError extends Problem {
		PrivateMethodError() {
			super(format("Method '%s' may not be private; increase the method's visibility to continue", getName()),
			      BeanMethod.this.getLocation());
		}
	}

	/** {@link Bean} methods must be non-final in order to accommodate CGLIB. */
	class FinalMethodError extends Problem {
		FinalMethodError() {
			super(format("Method '%s' may not be final; remove the final modifier to continue", getName()),
			      BeanMethod.this.getLocation());
		}
	}

	class InvalidScopedProxyDeclarationError extends Problem {
		InvalidScopedProxyDeclarationError(BeanMethod method) {
			super(format("Method %s contains an invalid annotation declaration: scoped proxies "
			           + "cannot be created for singleton/prototype beans", method.getName()),
			      BeanMethod.this.getLocation());
		}

	}

}