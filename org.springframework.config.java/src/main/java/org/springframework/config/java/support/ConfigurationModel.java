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
package org.springframework.config.java.support;

import static java.lang.String.*;

import java.util.ArrayList;

import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.config.java.Configuration;


/**
 * An abstract representation of a set of user-provided "Configuration classes", usually but
 * not necessarily annotated with {@link Configuration @Configuration}. The model is
 * populated with a
 * {@link org.springframework.config.java.support.ConfigurationParser}
 * implementation which may be reflection-based or ASM-based. Once a model has been
 * populated, it can then be rendered out to a set of BeanDefinitions. The model provides an
 * important layer of indirection between the complexity of parsing a set of classes and the
 * complexity of representing the contents of those classes as BeanDefinitions.
 * 
 * <p>
 * Interface follows the builder pattern for method chaining.
 * </p>
 * 
 * @author Chris Beams
 * @see org.springframework.config.java.support.ConfigurationParser
 */
final class ConfigurationModel {

	/* list is used because order and collection equality matters. */
	private final ArrayList<ConfigurationClass> configurationClasses = new ArrayList<ConfigurationClass>();

	/**
	 * Add a {@link Configuration @Configuration} class to the model. Classes may be added
	 * at will and without any particular validation. Malformed classes will be caught and
	 * errors processed during {@link #validate() validation}
	 * 
	 * @param configurationClass user-supplied Configuration class
	 */
	public ConfigurationModel add(ConfigurationClass configurationClass) {
		configurationClasses.add(configurationClass);
		return this;
	}

	public ConfigurationClass[] getAllConfigurationClasses() {
		return configurationClasses.toArray(new ConfigurationClass[configurationClasses.size()]);
	}

	/**
	 * Recurses through the model validating each object along the way and aggregating any
	 * <var>errors</var>.
	 * 
	 * @see ConfigurationClass#validate
	 */
	public void validate(ProblemReporter problemReporter) {
		for (ConfigurationClass configClass : configurationClasses)
			configClass.validate(problemReporter);
	}

	@Override
	public String toString() {
		return format("%s: configurationClasses=%s", getClass().getSimpleName(), configurationClasses);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((configurationClasses == null) ? 0 : configurationClasses.hashCode());
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
		ConfigurationModel other = (ConfigurationModel) obj;
		if (configurationClasses == null) {
			if (other.configurationClasses != null)
				return false;
		} else if (!configurationClasses.equals(other.configurationClasses))
			return false;
		return true;
	}

}