/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.type.MethodMetadata;

/**
 * Base class for a {@link Configuration @Configuration} class method.
 *
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

	void validate(ProblemReporter problemReporter) {
	}

	@Override
	public String toString() {
		return String.format("[%s:name=%s,declaringClass=%s]",
				getClass().getSimpleName(), getMetadata().getMethodName(), getMetadata().getDeclaringClassName());
	}

}
