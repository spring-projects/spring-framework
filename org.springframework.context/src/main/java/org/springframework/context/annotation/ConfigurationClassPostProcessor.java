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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@literal <context:annotation-config/>} or
 * {@literal <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other BeanFactoryPostProcessor.
 *
 * <p>This post processor is {@link Ordered#HIGHEST_PRECEDENCE} as it is important
 * that any {@link Bean} methods declared in Configuration classes have their
 * respective bean definitions registered before any other BeanFactoryPostProcessor
 * executes.
 * 
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanFactoryPostProcessor, BeanClassLoaderAware {

	private static final String CONFIGURATION_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	private static final String CONFIGURATION_CLASS_FULL = "full";

	private static final String CONFIGURATION_CLASS_LITE = "lite";


	/** Whether the CGLIB2 library is present on the classpath */
	private static final boolean cglibAvailable = ClassUtils.isPresent(
			"net.sf.cglib.proxy.Enhancer", ConfigurationClassPostProcessor.class.getClassLoader());


	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private boolean setMetadataReaderFactoryCalled = false;


	/**
	 * Set the {@link SourceExtractor} to use for generated bean definitions
	 * that correspond to {@link Bean} factory methods.
	 */
	public void setSourceExtractor(SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
	}

	/**
	 * Set the {@link ProblemReporter} to use.
	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, an @Bean method marked as {@literal final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@link #setBeanClassLoader bean class loader}.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.setMetadataReaderFactoryCalled = true;
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}

	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}


	/**
	 * Prepare the Configuration classes for servicing bean requests at runtime
	 * by replacing them with CGLIB-enhanced subclasses.
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		if (!(beanFactory instanceof BeanDefinitionRegistry)) {
			throw new IllegalStateException(
					"ConfigurationClassPostProcessor expects a BeanFactory that implements BeanDefinitionRegistry");
		}
		processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		enhanceConfigurationClasses(beanFactory);
	}

	/**
	 * Build and validate a configuration model based on the registry of
	 * {@link Configuration} classes.
	 */
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		Set<BeanDefinitionHolder> configCandidates = new LinkedHashSet<BeanDefinitionHolder>();
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (checkConfigurationClassCandidate(beanDef)) {
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		if (configCandidates.isEmpty()) {
			return;
		}

		// Populate a new configuration model by parsing each @Configuration classes
		ConfigurationClassParser parser = new ConfigurationClassParser(this.metadataReaderFactory, this.problemReporter);
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parser.parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				else {
					parser.parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException("Failed to load bean class: " + bd.getBeanClassName(), ex);
			}
		}
		parser.validate();

		// Read the model and create bean definitions based on its content
		new ConfigurationClassBeanDefinitionReader(registry, this.sourceExtractor).loadBeanDefinitions(parser.getConfigurationClasses());
	}

	/**
	 * Check whether the given bean definition is a candidate for a configuration class,
	 * and mark it accordingly.
	 * @param beanDef the bean definition to check
	 * @return whether the candidate qualifies as (any kind of) configuration class
	 */
	protected boolean checkConfigurationClassCandidate(BeanDefinition beanDef) {
		AnnotationMetadata metadata = null;

		// Check already loaded Class if present...
		// since we possibly can't even load the class file for this Class.
		if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			metadata = new StandardAnnotationMetadata(((AbstractBeanDefinition) beanDef).getBeanClass());
		}
		else {
			String className = beanDef.getBeanClassName();
			if (className != null) {
				try {
					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(className);
					metadata = metadataReader.getAnnotationMetadata();
				}
				catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not find class file for introspecting factory methods: " + className, ex);
					}
					return false;
				}
			}
		}

		if (metadata != null) {
			if (metadata.isAnnotated(Configuration.class.getName())) {
				beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
				return true;
			}
			else if (metadata.isAnnotated(Component.class.getName()) ||
					metadata.hasAnnotatedMethods(Bean.class.getName())) {
				beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
				return true;
			}
		}
		return false;
	}

	/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<String, AbstractBeanDefinition>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			if (CONFIGURATION_CLASS_FULL.equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE))) {
				if (!(beanDef instanceof AbstractBeanDefinition)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}
				configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
			}
		}
		if (configBeanDefs.isEmpty()) {
			// nothing to enhance -> return immediately
			return;
		}
		if (!cglibAvailable) {
			throw new IllegalStateException("CGLIB is required to process @Configuration classes. " +
					"Either add CGLIB to the classpath or remove the following @Configuration bean definitions: " +
					configBeanDefs.keySet());
		}
		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer(beanFactory);
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			try {
				Class configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				Class enhancedClass = enhancer.enhance(configClass);
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Replacing bean definition '%s' existing class name '%s' " +
							"with enhanced class name '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
				}
				beanDef.setBeanClass(enhancedClass);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}

}
