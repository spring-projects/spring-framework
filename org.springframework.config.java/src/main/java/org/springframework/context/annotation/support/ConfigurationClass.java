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
package org.springframework.context.annotation.support;

import static java.lang.String.*;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;


/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * Includes a set of {@link Bean} methods, including all such methods defined in the
 * ancestry of the class, in a 'flattened-out' manner. Note that each {@link BeanMethod}
 * representation contains source information about where it was originally detected
 * (for the purpose of tooling with Spring IDE).
 * 
 * @author Chris Beams
 * @see ConfigurationModel
 * @see BeanMethod
 * @see ConfigurationParser
 */
final class ConfigurationClass extends ModelClass {

	private String beanName;
	private int modifiers;
	private Configuration configurationAnnotation;
	private HashSet<BeanMethod> methods = new HashSet<BeanMethod>();
	private ConfigurationClass declaringClass;

	public String getBeanName() {
		return beanName == null ? getName() : beanName;
	}

	public void setBeanName(String id) {
		this.beanName = id;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		Assert.isTrue(modifiers >= 0, "modifiers must be non-negative");
		this.modifiers = modifiers;
	}

	public Configuration getConfigurationAnnotation() {
		return this.configurationAnnotation;
	}

	public void setConfigurationAnnotation(Configuration configAnno) {
		Assert.notNull(configAnno, "configuration annotation must be non-null");
		this.configurationAnnotation = configAnno;
	}

	public Set<BeanMethod> getBeanMethods() {
		return methods;
	}

	public ConfigurationClass addBeanMethod(BeanMethod method) {
		method.setDeclaringClass(this);
		methods.add(method);
		return this;
	}

	public ConfigurationClass getDeclaringClass() {
		return declaringClass;
	}

	public void setDeclaringClass(ConfigurationClass configurationClass) {
		this.declaringClass = configurationClass;
	}

	public void validate(ProblemReporter problemReporter) {
		// configuration classes must be annotated with @Configuration
		if (configurationAnnotation == null)
			problemReporter.error(new NonAnnotatedConfigurationProblem());

		// a configuration class may not be final (CGLIB limitation)
		if (Modifier.isFinal(modifiers))
			problemReporter.error(new FinalConfigurationProblem());

		for (BeanMethod method : methods)
			method.validate(problemReporter);
	}

	@Override
	public String toString() {
		return format("%s; modifiers=%d; methods=%s", super.toString(), modifiers, methods);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
		result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
		result = prime * result + ((configurationAnnotation == null) ? 0 : configurationAnnotation.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + modifiers;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfigurationClass other = (ConfigurationClass) obj;
		if (declaringClass == null) {
			if (other.declaringClass != null)
				return false;
		} else if (!declaringClass.equals(other.declaringClass))
			return false;
		if (beanName == null) {
			if (other.beanName != null)
				return false;
		} else if (!beanName.equals(other.beanName))
			return false;
		if (configurationAnnotation == null) {
			if (other.configurationAnnotation != null)
				return false;
		} else if (!configurationAnnotation.equals(other.configurationAnnotation))
			return false;
		if (methods == null) {
			if (other.methods != null)
				return false;
		} else if (!methods.equals(other.methods))
			return false;
		if (modifiers != other.modifiers)
			return false;
		return true;
	}


	/** Configuration classes must be annotated with {@link Configuration @Configuration}. */
	class NonAnnotatedConfigurationProblem extends Problem {

		NonAnnotatedConfigurationProblem() {
			super(format("%s was specified as a @Configuration class but was not actually annotated " +
			             "with @Configuration. Annotate the class or do not attempt to process it.",
			             getSimpleName()),
			      ConfigurationClass.this.getLocation());
		}

	}


	/** Configuration classes must be non-final to accommodate CGLIB subclassing. */
	class FinalConfigurationProblem extends Problem {

		FinalConfigurationProblem() {
			super(format("@Configuration class [%s] may not be final. Remove the final modifier to continue.",
			             getSimpleName()),
			      ConfigurationClass.this.getLocation());
		}

	}

}
