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

import static java.lang.String.*;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.ConfigurationModel;
import org.springframework.config.java.MalformedConfigurationException;
import org.springframework.config.java.UsageError;
import org.springframework.config.java.internal.enhancement.ConfigurationEnhancer;
import org.springframework.config.java.internal.parsing.ConfigurationParser;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.Assert;


/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping {@link Configuration
 * @Configuration} beans. Usually used in conjunction with Spring XML files.
 */
public class ConfigurationPostProcessor extends AbstractConfigurationClassProcessor
                                        implements Ordered, BeanFactoryPostProcessor {

	private static final Log logger = LogFactory.getLog(ConfigurationPostProcessor.class);
	private DefaultListableBeanFactory beanFactory;


	/**
	 * Returns the order in which this {@link BeanPostProcessor} will be executed. Returns
	 * {@link Ordered#HIGHEST_PRECEDENCE}.
	 */
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
	
	public void postProcessBeanFactory(ConfigurableListableBeanFactory clBeanFactory) throws BeansException {
		Assert.isInstanceOf(DefaultListableBeanFactory.class, clBeanFactory);
		beanFactory = (DefaultListableBeanFactory) clBeanFactory;
		
		BeanDefinitionRegistry factoryBeanDefs = processConfigBeanDefinitions();
		
		for(String beanName : factoryBeanDefs.getBeanDefinitionNames())
			beanFactory.registerBeanDefinition(beanName, factoryBeanDefs.getBeanDefinition(beanName));
		
		enhanceConfigurationClasses();
	}
	
	@Override
    protected ConfigurationParser createConfigurationParser() {
	    return new ConfigurationParser(beanFactory.getBeanClassLoader());
    }

	/**
	 * @return map of all non-abstract {@link BeanDefinition}s in the enclosing {@link #beanFactory}
	 */
	@Override
    protected BeanDefinitionRegistry getConfigurationBeanDefinitions(boolean includeAbstractBeanDefs) {
		
		BeanDefinitionRegistry configBeanDefs = new DefaultListableBeanFactory();
		
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			
			if (beanDef.isAbstract() && !includeAbstractBeanDefs)
				continue;

			if (isConfigClass(beanDef))
				configBeanDefs.registerBeanDefinition(beanName, beanDef);
		}
		
	    return configBeanDefs;
    }
	
	@Override
	protected void validateModel(ConfigurationModel model) {
		ArrayList<UsageError> errors = new ArrayList<UsageError>();
		model.validate(errors);
		if (errors.size() > 0)
			throw new MalformedConfigurationException(errors.toArray(new UsageError[] {}));
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
	private void enhanceConfigurationClasses() {

		ConfigurationEnhancer enhancer = new ConfigurationEnhancer(beanFactory);
		
		BeanDefinitionRegistry configBeanDefs = getConfigurationBeanDefinitions(true);
		
		for(String beanName : configBeanDefs.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			String configClassName = beanDef.getBeanClassName();
			String enhancedClassName = enhancer.enhance(configClassName);

			if (logger.isDebugEnabled())
				logger.debug(format("Replacing bean definition '%s' existing class name '%s' with enhanced class name '%s'",
			                        beanName, configClassName, enhancedClassName));

			beanDef.setBeanClassName(enhancedClassName);
		}
	}

	/**
	 * Determines whether the class for <var>beanDef</var> is a {@link Configuration}
	 * -annotated class. Returns false if <var>beanDef</var> has no class specified.
	 */
	private static boolean isConfigClass(BeanDefinition beanDef) {
		
		String className = beanDef.getBeanClassName();
		
		if(className == null)
			return false;
		
		try {
			MetadataReader metadataReader = new SimpleMetadataReaderFactory().getMetadataReader(className);
			AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
			return annotationMetadata.hasAnnotation(Configuration.class.getName());
		} catch (IOException ex) {
			throw new RuntimeException(ex); 
		}
	}

}
