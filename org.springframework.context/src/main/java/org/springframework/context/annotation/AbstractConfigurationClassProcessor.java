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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;


/**
 * Abstract superclass for processing {@link Configuration}-annotated classes and registering
 * bean definitions based on {@link Bean}-annotated methods within those classes.
 * 
 * <p>Provides template method {@link #processConfigBeanDefinitions()} that orchestrates calling each
 * of several abstract methods to be overriden by concrete implementations that allow for
 * customizing how {@link Configuration} classes are found ({@link #getConfigurationBeanDefinitions}),
 * customizing the creation of a {@link ConfigurationParser} ({@link #createConfigurationParser}),
 * and customizing {@link ConfigurationModel} validation logic ({@link #validateModel}).
 * 
 * <p>This class was expressly designed with tooling in mind. Spring IDE will maintain it's
 * own implementation of this class but still take advantage of the generic parsing algorithm
 * defined here by {@link #processConfigBeanDefinitions()}.
 *
 * @author Chris Beams
 * @since 3.0
 * @see Configuration
 * @see ConfigurationClassPostProcessor
 */
public abstract class AbstractConfigurationClassProcessor {

	/**
	 * Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, a Bean method marked as {@literal final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter},
	 * but is overridable with {@link #setProblemReporter}
	 */
	private ProblemReporter problemReporter = new FailFastProblemReporter();

	/**
	 * Populate and return a registry containing all {@link Configuration} bean definitions
	 * to be processed.
	 * 
	 * @param includeAbstractBeanDefs whether abstract Configuration bean definitions should
	 * be included in the resulting BeanDefinitionRegistry. Usually false, but called as true
	 * during the enhancement phase.
	 * @see #processConfigBeanDefinitions()
	 */
	protected abstract BeanDefinitionRegistry getConfigurationBeanDefinitions(boolean includeAbstractBeanDefs);

	/**
	 * Create and return a new {@link ConfigurationParser}, allowing for customization of
	 * type (ASM/JDT/Reflection) as well as providing specialized ClassLoader during
	 * construction.
	 * @see #processConfigBeanDefinitions() 
	 */
	protected abstract ConfigurationParser createConfigurationParser();

	/**
	 * Override the default {@link ProblemReporter}.
	 * @param problemReporter custom problem reporter
	 */
	protected final void setProblemReporter(ProblemReporter problemReporter) {
		this.problemReporter = problemReporter;
	}

	/**
	 * Get the currently registered {@link ProblemReporter}.
	 */
	protected final ProblemReporter getProblemReporter() {
		return problemReporter;
	}

	/**
	 * Build and validate a {@link ConfigurationModel} based on the registry of
	 * {@link Configuration} classes provided by {@link #getConfigurationBeanDefinitions},
	 * then, based on the content of that model, create and register bean definitions
	 * against a new {@link BeanDefinitionRegistry}, then return the registry.
	 * 
	 * @return registry containing one bean definition per {@link Bean} method declared
	 * within the Configuration classes
	 */
	protected final BeanDefinitionRegistry processConfigBeanDefinitions() {
		BeanDefinitionRegistry configBeanDefs = getConfigurationBeanDefinitions(false);

		// return an empty registry immediately if no @Configuration classes were found
		if(configBeanDefs.getBeanDefinitionCount() == 0)
			return configBeanDefs;

		// populate a new ConfigurationModel by parsing each @Configuration classes
		ConfigurationParser parser = createConfigurationParser();

		for(String beanName : configBeanDefs.getBeanDefinitionNames()) {
			BeanDefinition beanDef = configBeanDefs.getBeanDefinition(beanName);
			String className = beanDef.getBeanClassName();

			parser.parse(className, beanName);
		}

		ConfigurationModel configModel = parser.getConfigurationModel();

		configModel.validate(problemReporter);

		// read the model and create bean definitions based on its content
		return new ConfigurationModelBeanDefinitionReader().loadBeanDefinitions(configModel);
	}

}
