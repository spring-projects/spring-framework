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
import static org.springframework.config.java.Util.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.ConfigurationClass;
import org.springframework.config.java.ConfigurationModel;
import org.springframework.config.java.FactoryMethod;
import org.springframework.config.java.ModelMethod;
import org.springframework.config.java.plugin.Extension;
import org.springframework.config.java.plugin.ExtensionAnnotationBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;


/**
 * Reads a given fully-populated {@link ConfigurationModel}, registering bean definitions
 * with the given {@link BeanDefinitionRegistry} based on its contents.
 * <p>
 * This class was modeled after the {@link BeanDefinitionReader} hierarchy, but does not
 * implement/extend any of its artifacts as {@link ConfigurationModel} is not a
 * {@link Resource}.
 * 
 * @author Chris Beams
 */
class ConfigurationModelBeanDefinitionReader {

	private static final Log log = LogFactory.getLog(ConfigurationModelBeanDefinitionReader.class);

	private final DefaultListableBeanFactory beanFactory;


	/**
	 * Creates a new {@link ConfigurationModelBeanDefinitionReader} instance.
	 */
	public ConfigurationModelBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Reads {@code model}, registering bean definitions with {@link #beanFactory} based on
	 * its contents.
	 * 
	 * @return number of bean definitions generated
	 */
	public int loadBeanDefinitions(ConfigurationModel model) {
		int initialBeanDefCount = beanFactory.getBeanDefinitionCount();

		for (ConfigurationClass configClass : model.getAllConfigurationClasses())
			loadBeanDefinitionsForConfigurationClass(configClass);

		return beanFactory.getBeanDefinitionCount() - initialBeanDefCount;
	}

	/**
	 * Reads a particular {@link ConfigurationClass}, registering bean definitions for the
	 * class itself, all its {@link FactoryMethod} methods and all its {@link Extension}
	 * annotations.
	 */
	private void loadBeanDefinitionsForConfigurationClass(ConfigurationClass configClass) {
		doLoadBeanDefinitionForConfigurationClass(configClass);

		for (ModelMethod method : configClass.getMethods())
			loadBeanDefinitionsForModelMethod(method);

		Annotation[] pluginAnnotations = configClass.getPluginAnnotations();
		Arrays.sort(pluginAnnotations, new PluginComparator());
		for (Annotation annotation : pluginAnnotations)
			loadBeanDefinitionsForExtensionAnnotation(annotation);
	}

	/**
	 * Registers the {@link Configuration} class itself as a bean definition.
	 */
	private void doLoadBeanDefinitionForConfigurationClass(ConfigurationClass configClass) {
		Configuration metadata = configClass.getMetadata();

		if (metadata.checkRequired() == true) {
			RootBeanDefinition requiredAnnotationPostProcessor = new RootBeanDefinition();
			Class<?> beanClass = RequiredAnnotationBeanPostProcessor.class;
			String beanName = beanClass.getName() + "#0";
			requiredAnnotationPostProcessor.setBeanClass(beanClass);
			requiredAnnotationPostProcessor
			        .setResourceDescription("ensures @Required methods have been invoked");
			beanFactory.registerBeanDefinition(beanName, requiredAnnotationPostProcessor);
		}

		GenericBeanDefinition configBeanDef = new GenericBeanDefinition();
		configBeanDef.setBeanClassName(configClass.getName());

		String configBeanName = configClass.getBeanName();

		// consider the case where it's already been defined (probably in XML)
		// and potentially has PropertyValues and ConstructorArgs)
		if (beanFactory.containsBeanDefinition(configBeanName)) {
			if (log.isInfoEnabled())
				log.info(format(
				        "Copying property and constructor arg values from existing bean definition for "
				                + "@Configuration class %s to new bean definition", configBeanName));
			AbstractBeanDefinition existing = (AbstractBeanDefinition) beanFactory
			        .getBeanDefinition(configBeanName);
			configBeanDef.setPropertyValues(existing.getPropertyValues());
			configBeanDef.setConstructorArgumentValues(existing.getConstructorArgumentValues());
			configBeanDef.setResource(existing.getResource());
		}

		if (log.isInfoEnabled())
			log.info(format("Registering bean definition for @Configuration class %s", configBeanName));

		beanFactory.registerBeanDefinition(configBeanName, configBeanDef);
	}


	/**
	 * Reads a particular {@link ModelMethod}, registering bean definitions with
	 * {@link #beanFactory} based on its contents.
	 * 
	 * @see FactoryMethod
	 */
	private void loadBeanDefinitionsForModelMethod(ModelMethod method) {
		method.getRegistrar().register(method, beanFactory);
	}

	@SuppressWarnings("unchecked")
	private void loadBeanDefinitionsForExtensionAnnotation(Annotation anno) {
		// ExtensionAnnotationUtils.getRegistrarFor(anno).registerBeanDefinitionsWith(beanFactory);
		// there is a fixed assumption that in order for this annotation to have
		// been registered in the first place, it must be meta-annotated with @Plugin
		// assert this as an invariant now
		Class<?> annoClass = anno.getClass();
		Extension extensionAnno = AnnotationUtils.findAnnotation(annoClass, Extension.class);
		Assert.isTrue(extensionAnno != null, format("%s annotation is not annotated as a @%s", annoClass,
		        Extension.class.getSimpleName()));

		Class<? extends ExtensionAnnotationBeanDefinitionRegistrar> extHandlerClass = extensionAnno.handler();

		ExtensionAnnotationBeanDefinitionRegistrar extHandler = getInstance(extHandlerClass);
		extHandler.handle(anno, beanFactory);
	}

	private static class PluginComparator implements Comparator<Annotation> {
		public int compare(Annotation a1, Annotation a2) {
			Integer i1 = getOrder(a1);
			Integer i2 = getOrder(a2);
			return i1.compareTo(i2);
		}

		private Integer getOrder(Annotation a) {
			Extension plugin = a.annotationType().getAnnotation(Extension.class);
			if (plugin == null)
				throw new IllegalArgumentException("annotation was not annotated with @Plugin: "
				        + a.annotationType());
			return plugin.order();
		}
	}


}
