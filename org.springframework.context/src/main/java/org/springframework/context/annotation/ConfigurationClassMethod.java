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

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.type.MethodMetadata;

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
final class ConfigurationClassMethod {

	private final MethodMetadata metadata;

	private final ConfigurationClass declaringClass;


	public ConfigurationClassMethod(MethodMetadata metadata, ConfigurationClass declaringClass) {
		this.metadata = metadata;
		this.declaringClass = declaringClass;
	}


	public MethodMetadata getMetadata() {
		return this.metadata;
	}

	public ConfigurationClass getDeclaringClass() {
		return this.declaringClass;
	}

	public Location getResourceLocation() {
		return new Location(this.declaringClass.getResource(), metadata);
	}

	public void validate(ProblemReporter problemReporter) {
		if (this.declaringClass.getMetadata().isAnnotated(Configuration.class.getName()) && !getMetadata().isOverridable()) {
			problemReporter.error(new NonOverridableMethodError());
		}
	}


	/**
	 * {@link Bean} methods must be overridable in order to accommodate CGLIB.
	 */
	private class NonOverridableMethodError extends Problem {

		public NonOverridableMethodError() {
			super(String.format("Method '%s' must not be private, final or static; change the method's modifiers to continue",
					getMetadata().getMethodName()), getResourceLocation());
		}
	}

}
