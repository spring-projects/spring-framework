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

import java.util.LinkedHashSet;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ProblemReporter;


/**
 * Represents the set of all user-defined {@link Configuration} classes. Once this model
 * is populated using a {@link ConfigurationParser}, it can be rendered out to a set of
 * {@link BeanDefinition} objects. This model provides an important layer of indirection
 * between the complexity of parsing a set of classes and the complexity of representing
 * the contents of those classes as BeanDefinitions.
 * 
 * @author Chris Beams
 * @see ConfigurationClass
 * @see ConfigurationParser
 * @see ConfigurationModelBeanDefinitionReader
 */
@SuppressWarnings("serial")
final class ConfigurationModel extends LinkedHashSet<ConfigurationClass> {

	/**
	 * Recurses through the model validating each {@link ConfigurationClass}.
	 * 
	 * @param problemReporter {@link ProblemReporter} against which any validation errors
	 *        will be registered
	 * @see ConfigurationClass#validate
	 */
	public void validate(ProblemReporter problemReporter) {
		for (ConfigurationClass configClass : this)
			configClass.validate(problemReporter);
	}

	@Override
	public String toString() {
		return format("%s containing @Configuration classes: %s", getClass().getSimpleName(), super.toString());
	}

}
