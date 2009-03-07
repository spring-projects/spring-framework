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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.Bean;
import org.springframework.config.java.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 * <p>
 * Registered by default when using {@literal <context:annotation-config/>} or
 * {@literal <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other BeanFactoryPostProcessor.
 * <p>
 * This post processor is {@link Ordered#HIGHEST_PRECEDENCE} as it's important
 * that any {@link Bean} methods declared in Configuration classes have their
 * respective bean definitions registered before any other BeanFactoryPostProcessor
 * executes.
 * 
 * @author Chris Beams
 * @since 3.0
 */
public class ConfigurationClassPostProcessor extends AbstractConfigurationClassProcessor
                                             implements Ordered, BeanFactoryPostProcessor {

	private static final Log logger = LogFactory.getLog(ConfigurationClassPostProcessor.class);

	/**
	 * A well-known class in the CGLIB API used when testing to see if CGLIB
	 * is present on the classpath. Package-private visibility allows for
	 * manipulation by tests.
	 * @see #assertCglibIsPresent(BeanDefinitionRegistry)
	 */
	static String CGLIB_TEST_CLASS = "net.sf.cglib.proxy.Callback";

	/**
	 * Holder for the calling BeanFactory
	 * @see #postProcessBeanFactory(ConfigurableListableBeanFactory)
	 */
	private DefaultListableBeanFactory beanFactory;


	/**
	 * @return {@link Ordered#HIGHEST_PRECEDENCE}.
	 */
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/**
	 * Finds {@link Configuration} bean definitions within <var>clBeanFactory</var>
	 * and processes them in order to register bean definitions for each Bean method
	 * found within; also prepares the the Configuration classes for servicing
	 * bean requests at runtime by replacing them with CGLIB-enhanced subclasses.
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory clBeanFactory) throws BeansException {
		Assert.isInstanceOf(DefaultListableBeanFactory.class, clBeanFactory);
		beanFactory = (DefaultListableBeanFactory) clBeanFactory;

		BeanDefinitionRegistry factoryBeanDefs = processConfigBeanDefinitions();

		for(String beanName : factoryBeanDefs.getBeanDefinitionNames())
			beanFactory.registerBeanDefinition(beanName, factoryBeanDefs.getBeanDefinition(beanName));

		enhanceConfigurationClasses();
	}

	/**
	 * @return a ConfigurationParser that uses the enclosing BeanFactory's
	 * ClassLoader to load all Configuration class artifacts.
	 */
	@Override
	protected ConfigurationParser createConfigurationParser() {
		return new ConfigurationParser(beanFactory.getBeanClassLoader());
	}

	/**
	 * @return map of all non-abstract {@link BeanDefinition}s in the
	 * enclosing {@link #beanFactory}
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

	/**
	 * Validates the given <var>model</var>. Any problems found are delegated
	 * to {@link #getProblemReporter()}.
	 */
	@Override
	protected void validateModel(ConfigurationModel model) {
		model.validate(this.getProblemReporter());
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

		BeanDefinitionRegistry configBeanDefs = getConfigurationBeanDefinitions(true);

		assertCglibIsPresent(configBeanDefs);

		ConfigurationEnhancer enhancer = new ConfigurationEnhancer(beanFactory);

		for(String beanName : configBeanDefs.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			String configClassName = beanDef.getBeanClassName();
			String enhancedClassName = enhancer.enhance(configClassName);

			if (logger.isDebugEnabled())
				logger.debug(format("Replacing bean definition '%s' existing class name '%s' "
				                  + "with enhanced class name '%s'", beanName, configClassName, enhancedClassName));

			beanDef.setBeanClassName(enhancedClassName);
		}
	}

	/**
	 * Tests for the presence of CGLIB on the classpath by trying to
	 * classload {@link #CGLIB_TEST_CLASS}.
	 */
	private void assertCglibIsPresent(BeanDefinitionRegistry configBeanDefs) {
		try {
			Class.forName(CGLIB_TEST_CLASS);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("CGLIB is required to process @Configuration classes. " +
					"Either add CGLIB v2.2.3 to the classpath or remove the following " +
					"@Configuration bean definitions: ["
					+ StringUtils.arrayToCommaDelimitedString(configBeanDefs.getBeanDefinitionNames()) + "]");
		}
	}

	/**
	 * @return whether the BeanDefinition's beanClass is Configuration-annotated,
	 * false if no beanClass is specified.
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
