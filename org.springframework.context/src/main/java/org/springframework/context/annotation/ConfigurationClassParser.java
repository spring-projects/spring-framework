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

import static org.springframework.context.annotation.AsmUtils.*;

import org.springframework.asm.ClassReader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.util.ClassUtils;


/**
 * Parses a {@link Configuration} class definition, populating a {@link ConfigurationModel}.
 * This ASM-based implementation avoids reflection and eager classloading in order to
 * interoperate effectively with tooling (Spring IDE) and OSGi environments.
 * <p>
 * This class helps separate the concern of parsing the structure of a Configuration class
 * from the concern of registering {@link BeanDefinition} objects based on the content of
 * that model.
 * 
 * @author Chris Beams
 * @see ConfigurationModel
 * @see ConfigurationModelBeanDefinitionReader
 */
class ConfigurationClassParser {

	/**
	 * Model to be populated during calls to {@link #parse(Object, String)}
	 */
	private final ConfigurationModel model;
	private final ProblemReporter problemReporter;
	private final ClassLoader classLoader;

	/**
	 * Creates a new {@link ConfigurationClassParser} instance that will be used to populate a
	 * {@link ConfigurationModel}.
	 * 
	 * @param model model to be populated by each successive call to {@link #parse}
	 * @see #getConfigurationModel()
	 */
	public ConfigurationClassParser(ProblemReporter problemReporter, ClassLoader classLoader) {
		this.model = new ConfigurationModel();
		this.problemReporter = problemReporter;
		this.classLoader = classLoader;
	}

	/**
	 * Parse the {@link Configuration @Configuration} class encapsulated by
	 * <var>configurationSource</var>.
	 * 
	 * @param configurationSource reader for Configuration class being parsed
	 * @param configurationId may be null, but if populated represents the bean id (assumes
	 *        that this configuration class was configured via XML)
	 */
	public void parse(String className, String configurationId) {

		String resourcePath = ClassUtils.convertClassNameToResourcePath(className);

		ClassReader configClassReader = newAsmClassReader(getClassAsStream(resourcePath, classLoader));

		ConfigurationClass configClass = new ConfigurationClass();
		configClass.setBeanName(configurationId);

		configClassReader.accept(new ConfigurationClassVisitor(configClass, model, problemReporter, classLoader), false);
		model.add(configClass);
	}

	/**
	 * Returns the current {@link ConfigurationModel}, to be called after {@link #parse}.
	 */
	public ConfigurationModel getConfigurationModel() {
		return model;
	}

}
