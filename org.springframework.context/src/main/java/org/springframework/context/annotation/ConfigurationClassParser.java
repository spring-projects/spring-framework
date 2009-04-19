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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.springframework.asm.ClassReader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.util.ClassUtils;

/**
 * Parses a {@link Configuration} class definition, populating a configuration model.
 * This ASM-based implementation avoids reflection and eager classloading in order to
 * interoperate effectively with tooling (Spring IDE) and OSGi environments.
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration class
 * from the concern of registering {@link BeanDefinition} objects based on the content of
 * that model.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
class ConfigurationClassParser {

	private final Set<ConfigurationClass> model;

	private final ProblemReporter problemReporter;

	private final ClassLoader classLoader;


	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used to populate a
	 * configuration model.
	 * @param model model to be populated by each successive call to {@link #parse}
	 */
	public ConfigurationClassParser(ProblemReporter problemReporter, ClassLoader classLoader) {
		this.model = new LinkedHashSet<ConfigurationClass>();
		this.problemReporter = problemReporter;
		this.classLoader = classLoader;
	}


	/**
	 * Parse the specified {@link Configuration @Configuration} class.
	 * @param className the name of the class to parse
	 * @param beanName may be null, but if populated represents the bean id
	 * (assumes that this configuration class was configured via XML)
	 */
	public void parse(String className, String beanName) {
		String resourcePath = ClassUtils.convertClassNameToResourcePath(className);
		ClassReader configClassReader = ConfigurationClassReaderUtils.newAsmClassReader(ConfigurationClassReaderUtils.getClassAsStream(resourcePath, classLoader));
		ConfigurationClass configClass = new ConfigurationClass();
		configClass.setBeanName(beanName);
		configClassReader.accept(new ConfigurationClassVisitor(configClass, model, problemReporter, classLoader), false);
		model.add(configClass);
	}

	/**
	 * Recurse through the model validating each {@link ConfigurationClass}.
	 * @param problemReporter {@link ProblemReporter} against which any validation errors
	 * will be registered
	 * @see ConfigurationClass#validate
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.model) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getModel() {
		return this.model;
	}

}
