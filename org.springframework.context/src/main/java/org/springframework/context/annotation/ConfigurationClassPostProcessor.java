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
import java.util.LinkedHashSet;
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
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

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
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanFactoryPostProcessor, BeanClassLoaderAware {

	public static final String CONFIGURATION_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	/** Whether the CGLIB2 library is present on the classpath */
	private static final boolean cglibAvailable = ClassUtils.isPresent(
			"net.sf.cglib.proxy.Enhancer", ConfigurationClassPostProcessor.class.getClassLoader());


	private static final Log logger = LogFactory.getLog(ConfigurationClassPostProcessor.class);

	/**
	 * Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, a Bean method marked as {@literal final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter},
	 * but is overridable with {@link #setProblemReporter}
	 */
	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * Override the default {@link ProblemReporter}.
	 * @param problemReporter custom problem reporter
	 */
	public void setProblemReporter(ProblemReporter problemReporter) {
		this.problemReporter = problemReporter;
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
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
	protected final void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		Set<BeanDefinitionHolder> configBeanDefs = new LinkedHashSet<BeanDefinitionHolder>();
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (checkConfigurationClassBeanDefinition(beanDef)) {
				configBeanDefs.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		if (configBeanDefs.isEmpty()) {
			return;
		}

		// Populate a new configuration model by parsing each @Configuration classes
		ConfigurationClassParser parser = new ConfigurationClassParser(this.problemReporter, this.beanClassLoader);
		for (BeanDefinitionHolder holder : configBeanDefs) {
			parser.parse(holder.getBeanDefinition().getBeanClassName(), holder.getBeanName());
		}
		parser.validate();

		// Read the model and create bean definitions based on its content
		new ConfigurationClassBeanDefinitionReader(registry).loadBeanDefinitions(parser.getModel());
	}

	/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
	private void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Set<BeanDefinitionHolder> configBeanDefs = new LinkedHashSet<BeanDefinitionHolder>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			if ("full".equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE))) {
				configBeanDefs.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}
		if (configBeanDefs.isEmpty()) {
			// nothing to enhance -> return immediately
			return;
		}
		if (!cglibAvailable) {
			Set<String> beanNames = new LinkedHashSet<String>();
			for (BeanDefinitionHolder holder : configBeanDefs) {
				beanNames.add(holder.getBeanName());
			}
			throw new IllegalStateException("CGLIB is required to process @Configuration classes. " +
					"Either add CGLIB to the classpath or remove the following @Configuration bean definitions: " +
					beanNames);
		}
		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer(beanFactory);
		for (BeanDefinitionHolder holder : configBeanDefs) {
			AbstractBeanDefinition beanDef = (AbstractBeanDefinition) holder.getBeanDefinition();
			try {
				Class configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				Class enhancedClass = enhancer.enhance(configClass);
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Replacing bean definition '%s' existing class name '%s' " +
							"with enhanced class name '%s'", holder.getBeanName(), configClass.getName(), enhancedClass.getName()));
				}
				beanDef.setBeanClass(enhancedClass);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}

	/**
	 * @return whether the BeanDefinition's beanClass (or its ancestry) is
	 * {@link Configuration}-annotated, false if no beanClass is specified.
	 */
	private boolean checkConfigurationClassBeanDefinition(BeanDefinition beanDef) {
		// accommodating SPR-5655
		if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			if (AnnotationUtils.findAnnotation(
					((AbstractBeanDefinition) beanDef).getBeanClass(), Configuration.class) != null) {
				beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, "full");
				return true;
			}
			else if (AnnotationUtils.findAnnotation(
					((AbstractBeanDefinition) beanDef).getBeanClass(), Component.class) != null) {
				beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, "lite");
				return true;
			}
			else {
				return false;
			}
		}
		SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(this.beanClassLoader);
		String className = beanDef.getBeanClassName();
		while (className != null && !(className.equals(Object.class.getName()))) {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
				AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
				if (metadata.hasAnnotation(Configuration.class.getName())) {
					beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, "full");
					return true;
				}
				if (metadata.hasAnnotation(Component.class.getName())) {
					beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, "lite");
					return true;
				}
				className = metadata.getSuperClassName();
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException("Failed to load class file [" + className + "]", ex);
			}
		}

		return false;
	}

}
