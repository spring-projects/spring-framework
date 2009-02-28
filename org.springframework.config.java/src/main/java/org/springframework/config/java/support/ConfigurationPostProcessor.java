/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.springframework.config.java.Util.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.ConfigurationModel;
import org.springframework.config.java.MalformedConfigurationException;
import org.springframework.config.java.UsageError;
import org.springframework.config.java.internal.enhancement.ConfigurationEnhancer;
import org.springframework.config.java.internal.parsing.ConfigurationParser;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;


/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping {@link Configuration
 * @Configuration} beans from Spring XML files.
 */
public class ConfigurationPostProcessor implements Ordered, BeanFactoryPostProcessor {

	private static final Log logger = LogFactory.getLog(ConfigurationPostProcessor.class);


	/**
	 * Returns the order in which this {@link BeanPostProcessor} will be executed. Returns
	 * {@link Ordered#HIGHEST_PRECEDENCE}.
	 */
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}


	/**
	 * Searches <var>beanFactory</var> for any {@link Configuration} classes in order to
	 * parse and enhance them. Also registers any {@link BeanPostProcessor} objects
	 * necessary to fulfill JavaConfig requirements.
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory clBeanFactory) throws BeansException {
		if (!(clBeanFactory instanceof DefaultListableBeanFactory))
			throw new IllegalStateException("beanFactory must be of type "
			        + DefaultListableBeanFactory.class.getSimpleName());

		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) clBeanFactory;

		ConfigurationModel model = new ConfigurationModel();

		parseAnyConfigurationClasses(beanFactory, model);

		enhanceAnyConfigurationClasses(beanFactory, model);
	}

	private void parseAnyConfigurationClasses(DefaultListableBeanFactory beanFactory, ConfigurationModel model) {

		// linked map is important for maintaining predictable ordering of configuration
		// classes.
		// this is important in bean / value override situations.
		LinkedHashMap<String, ClassPathResource> configClassResources = new LinkedHashMap<String, ClassPathResource>();

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			if (beanDef.isAbstract())
				continue;

			if (isConfigClass(beanDef)) {
				String path = ClassUtils.convertClassNameToResourcePath(beanDef.getBeanClassName());
				configClassResources.put(beanName, new ClassPathResource(path));
			}
		}

		ConfigurationModelBeanDefinitionReader modelBeanDefinitionReader = new ConfigurationModelBeanDefinitionReader(
		        beanFactory);
		ConfigurationParser parser = new ConfigurationParser(model);

		for (String id : configClassResources.keySet())
			parser.parse(configClassResources.get(id), id);

		ArrayList<UsageError> errors = new ArrayList<UsageError>();
		model.validate(errors);
		if (errors.size() > 0)
			throw new MalformedConfigurationException(errors.toArray(new UsageError[] {}));

		modelBeanDefinitionReader.loadBeanDefinitions(model);
	}

	/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions; any
	 * candidates are then enhanced by a {@link ConfigurationEnhancer}. Candidate status is
	 * determined by BeanDefinition attribute metadata.
	 * 
	 * @author Chris Beams
	 * @see ConfigurationEnhancer
	 * @see BeanFactoryPostProcessor
	 */
	private void enhanceAnyConfigurationClasses(DefaultListableBeanFactory beanFactory,
	        ConfigurationModel model) {

		ConfigurationEnhancer enhancer = new ConfigurationEnhancer(beanFactory, model);

		int configClassesEnhanced = 0;

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);

			if (!isConfigClass(beanDef))
				continue;

			String configClassName = beanDef.getBeanClassName();

			String enhancedClassName = enhancer.enhance(configClassName);

			if (logger.isDebugEnabled())
				logger
				        .debug(String
				                .format(
				                        "Replacing bean definition '%s' existing class name '%s' with enhanced class name '%s'",
				                        beanName, configClassName, enhancedClassName));

			beanDef.setBeanClassName(enhancedClassName);

			configClassesEnhanced++;
		}

		if (configClassesEnhanced == 0)
			logger.warn("Found no @Configuration class BeanDefinitions within " + beanFactory);
	}

	/**
	 * Determines whether the class for <var>beanDef</var> is a {@link Configuration}
	 * -annotated class. Returns false if <var>beanDef</var> has no class specified.
	 * <p>
	 * Note: the classloading used within should not be problematic or interfere with
	 * tooling in any way. BeanFactoryPostProcessing happens only during actual runtime
	 * processing via {@link JavaConfigApplicationContext} or via XML using
	 * {@link ConfigurationPostProcessor}. In any case, tooling (Spring IDE) will hook in at
	 * a lower level than this class and thus never encounter this classloading. Should this
	 * become problematic, it would not be too difficult to replace the following with ASM
	 * logic that traverses the class hierarchy in order to find whether the class is
	 * directly or indirectly annotated with {@link Configuration}.
	 */
	private static boolean isConfigClass(BeanDefinition beanDef) {
		String className = beanDef.getBeanClassName();
		return className != null && loadRequiredClass(className).isAnnotationPresent(Configuration.class);
	}

}
