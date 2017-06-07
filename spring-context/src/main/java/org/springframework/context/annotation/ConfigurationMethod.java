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

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.type.MethodMetadata;

/**
 * @author Chris Beams
 * @since 3.1
 */
abstract class ConfigurationMethod {

	protected final MethodMetadata metadata;

	protected final ConfigurationClass configurationClass;


	public ConfigurationMethod(MethodMetadata metadata, ConfigurationClass configurationClass) {
		this.metadata = metadata;
		this.configurationClass = configurationClass;
	}


	public MethodMetadata getMetadata() {
		return this.metadata;
	}

	public ConfigurationClass getConfigurationClass() {
		return this.configurationClass;
	}

	public Location getResourceLocation() {
		return new Location(this.configurationClass.getResource(), this.metadata);
	}

	String getFullyQualifiedMethodName() {
		return this.metadata.getDeclaringClassName() + "#" + this.metadata.getMethodName();
	}

	static String getShortMethodName(String fullyQualifiedMethodName) {
		return fullyQualifiedMethodName.substring(fullyQualifiedMethodName.indexOf('#') + 1);
	}

	public void validate(ProblemReporter problemReporter) {
	}


	@Override
	public String toString() {
		return String.format("[%s:name=%s,declaringClass=%s]",
				getClass().getSimpleName(), getMetadata().getMethodName(), getMetadata().getDeclaringClassName());
	}

}
