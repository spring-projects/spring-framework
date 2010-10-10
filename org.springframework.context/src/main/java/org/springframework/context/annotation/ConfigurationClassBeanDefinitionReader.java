/*
 * Copyright 2002-2010 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reads a given fully-populated configuration model, registering bean definitions
 * with the given {@link BeanDefinitionRegistry} based on its contents.
 *
 * <p>This class was modeled after the {@link BeanDefinitionReader} hierarchy, but does
 * not implement/extend any of its artifacts as a configuration model is not a {@link Resource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassParser
 */
class ConfigurationClassBeanDefinitionReader {

	private static final String CONFIGURATION_CLASS_FULL = "full";

	private static final String CONFIGURATION_CLASS_LITE = "lite";

	private static final String CONFIGURATION_CLASS_ATTRIBUTE =
		Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	private static final Log logger = LogFactory.getLog(ConfigurationClassBeanDefinitionReader.class);
	
	private final BeanDefinitionRegistry registry;

	private final SourceExtractor sourceExtractor;

	private final ProblemReporter problemReporter;

	private final MetadataReaderFactory metadataReaderFactory;


	/**
	 * Create a new {@link ConfigurationClassBeanDefinitionReader} instance that will be used
	 * to populate the given {@link BeanDefinitionRegistry}.
	 * @param problemReporter 
	 * @param metadataReaderFactory 
	 */
	public ConfigurationClassBeanDefinitionReader(BeanDefinitionRegistry registry, SourceExtractor sourceExtractor,
			ProblemReporter problemReporter, MetadataReaderFactory metadataReaderFactory) {

		this.registry = registry;
		this.sourceExtractor = sourceExtractor;
		this.problemReporter = problemReporter;
		this.metadataReaderFactory = metadataReaderFactory;
	}


	/**
	 * Reads {@code configurationModel}, registering bean definitions with {@link #registry}
	 * based on its contents.
	 */
	public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
		for (ConfigurationClass configClass : configurationModel) {
			loadBeanDefinitionsForConfigurationClass(configClass);
		}
	}

	/**
	 * Reads a particular {@link ConfigurationClass}, registering bean definitions for the
	 * class itself, all its {@link Bean} methods
	 */
	private void loadBeanDefinitionsForConfigurationClass(ConfigurationClass configClass) {
		doLoadBeanDefinitionForConfigurationClassIfNecessary(configClass);
		for (ConfigurationClassMethod method : configClass.getMethods()) {
			loadBeanDefinitionsForModelMethod(method);
		}
		loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
	}

	/**
	 * Registers the {@link Configuration} class itself as a bean definition.
	 */
	private void doLoadBeanDefinitionForConfigurationClassIfNecessary(ConfigurationClass configClass) {
		if (configClass.getBeanName() != null) {
			// a bean definition already exists for this configuration class -> nothing to do
			return;
		}

		// no bean definition exists yet -> this must be an imported configuration class (@Import).
		GenericBeanDefinition configBeanDef = new GenericBeanDefinition();
		String className = configClass.getMetadata().getClassName();
		configBeanDef.setBeanClassName(className);
		if (checkConfigurationClassCandidate(configBeanDef, this.metadataReaderFactory)) {
			String configBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(configBeanDef, this.registry);
			configClass.setBeanName(configBeanName);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Registered bean definition for imported @Configuration class %s", configBeanName));
			}
		}
		else {
			try {
				MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
				AnnotationMetadata metadata = reader.getAnnotationMetadata();
				this.problemReporter.error(
						new InvalidConfigurationImportProblem(className, reader.getResource(), metadata));
			}
			catch (IOException ex) {
				throw new IllegalStateException("Could not create MetadataReader for class " + className);
			}
		}
	}

	/**
	 * Reads a particular {@link ConfigurationClassMethod}, registering bean definitions
	 * with the BeanDefinitionRegistry based on its contents.
	 */
	private void loadBeanDefinitionsForModelMethod(ConfigurationClassMethod method) {
		ConfigurationClass configClass = method.getConfigurationClass();
		MethodMetadata metadata = method.getMetadata();

		RootBeanDefinition beanDef = new ConfigurationClassBeanDefinition(configClass);
		beanDef.setResource(configClass.getResource());
		beanDef.setSource(this.sourceExtractor.extractSource(metadata, configClass.getResource()));
		beanDef.setFactoryBeanName(configClass.getBeanName());
		beanDef.setUniqueFactoryMethodName(metadata.getMethodName());
		beanDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		beanDef.setAttribute(RequiredAnnotationBeanPostProcessor.SKIP_REQUIRED_CHECK_ATTRIBUTE, Boolean.TRUE);

		// consider name and any aliases
		Map<String, Object> beanAttributes = metadata.getAnnotationAttributes(Bean.class.getName());
		List<String> names = new ArrayList<String>(Arrays.asList((String[]) beanAttributes.get("name")));
		String beanName = (names.size() > 0 ? names.remove(0) : method.getMetadata().getMethodName());
		for (String alias : names) {
			this.registry.registerAlias(beanName, alias);
		}

		// has this already been overridden (e.g. via XML)?
		if (this.registry.containsBeanDefinition(beanName)) {
			BeanDefinition existingBeanDef = registry.getBeanDefinition(beanName);
			// is the existing bean definition one that was created from a configuration class?
			if (!(existingBeanDef instanceof ConfigurationClassBeanDefinition)) {
				// no -> then it's an external override, probably XML
				// overriding is legal, return immediately
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Skipping loading bean definition for %s: a definition for bean " +
							"'%s' already exists. This is likely due to an override in XML.", method, beanName));
				}
				return;
			}
		}

		if (metadata.isAnnotated(Primary.class.getName())) {
			beanDef.setPrimary(true);
		}

		// is this bean to be instantiated lazily?
		if (metadata.isAnnotated(Lazy.class.getName())) {
			beanDef.setLazyInit((Boolean) metadata.getAnnotationAttributes(Lazy.class.getName()).get("value"));
		}
		else if (configClass.getMetadata().isAnnotated(Lazy.class.getName())){
			beanDef.setLazyInit((Boolean) configClass.getMetadata().getAnnotationAttributes(Lazy.class.getName()).get("value"));
		}

		if (metadata.isAnnotated(DependsOn.class.getName())) {
			String[] dependsOn = (String[]) metadata.getAnnotationAttributes(DependsOn.class.getName()).get("value");
			if (dependsOn.length > 0) {
				beanDef.setDependsOn(dependsOn);
			}
		}

		Autowire autowire = (Autowire) beanAttributes.get("autowire");
		if (autowire.isAutowire()) {
			beanDef.setAutowireMode(autowire.value());
		}

		String initMethodName = (String) beanAttributes.get("initMethod");
		if (StringUtils.hasText(initMethodName)) {
			beanDef.setInitMethodName(initMethodName);
		}

		String destroyMethodName = (String) beanAttributes.get("destroyMethod");
		if (StringUtils.hasText(destroyMethodName)) {
			beanDef.setDestroyMethodName(destroyMethodName);
		}

		// consider scoping
		ScopedProxyMode proxyMode = ScopedProxyMode.NO;
		Map<String, Object> scopeAttributes = metadata.getAnnotationAttributes(Scope.class.getName());
		if (scopeAttributes != null) {
			beanDef.setScope((String) scopeAttributes.get("value"));
			proxyMode = (ScopedProxyMode) scopeAttributes.get("proxyMode");
			if (proxyMode == ScopedProxyMode.DEFAULT) {
				proxyMode = ScopedProxyMode.NO;
			}
		}

		// replace the original bean definition with the target one, if necessary
		BeanDefinition beanDefToRegister = beanDef;
		if (proxyMode != ScopedProxyMode.NO) {
			BeanDefinitionHolder proxyDef = ScopedProxyCreator.createScopedProxy(
					new BeanDefinitionHolder(beanDef, beanName), this.registry, proxyMode == ScopedProxyMode.TARGET_CLASS);
			beanDefToRegister = proxyDef.getBeanDefinition();
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registering bean definition for @Bean method %s.%s()", configClass.getMetadata().getClassName(), beanName));
		}

		registry.registerBeanDefinition(beanName, beanDefToRegister);
	}
	
	private void loadBeanDefinitionsFromImportedResources(Map<String, Class<?>> importedResources) {
		Map<Class<?>, BeanDefinitionReader> readerInstanceCache = new HashMap<Class<?>, BeanDefinitionReader>();
		for (Map.Entry<String, Class<?>> entry : importedResources.entrySet()) {
			String resource = entry.getKey();
			Class<?> readerClass = entry.getValue();
			if (!readerInstanceCache.containsKey(readerClass)) {
				try {
					BeanDefinitionReader readerInstance = (BeanDefinitionReader)
							readerClass.getConstructor(BeanDefinitionRegistry.class).newInstance(this.registry);
					readerInstanceCache.put(readerClass, readerInstance);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Could not instantiate BeanDefinitionReader class [" + readerClass.getName() + "]");
				}
			}
			BeanDefinitionReader reader = readerInstanceCache.get(readerClass);
			// TODO SPR-6310: qualify relatively pathed locations as done in AbstractContextLoader.modifyLocations
			reader.loadBeanDefinitions(resource);
		}
	}


	/**
	 * Check whether the given bean definition is a candidate for a configuration class,
	 * and mark it accordingly.
	 * @param beanDef the bean definition to check
	 * @param metadataReaderFactory the current factory in use by the caller
	 * @return whether the candidate qualifies as (any kind of) configuration class
	 */
	public static boolean checkConfigurationClassCandidate(BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
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
					MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
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
	 * Determine whether the given bean definition indicates a full @Configuration class.
	 */
	public static boolean isFullConfigurationClass(BeanDefinition beanDef) {
		return CONFIGURATION_CLASS_FULL.equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE));
	}


	/**
	 * {@link RootBeanDefinition} marker subclass used to signify that a bean definition
	 * created from a configuration class as opposed to any other configuration source.
	 * Used in bean overriding cases where it's necessary to determine whether the bean
	 * definition was created externally.
	 */
	@SuppressWarnings("serial")
	private static class ConfigurationClassBeanDefinition extends RootBeanDefinition implements AnnotatedBeanDefinition {

		private AnnotationMetadata annotationMetadata;

		public ConfigurationClassBeanDefinition(ConfigurationClass configClass) {
			this.annotationMetadata = configClass.getMetadata();
		}

		private ConfigurationClassBeanDefinition(ConfigurationClassBeanDefinition original) {
			super(original);
			this.annotationMetadata = original.annotationMetadata;
		}

		public AnnotationMetadata getMetadata() {
			return this.annotationMetadata;
		}

		@Override
		public boolean isFactoryMethod(Method candidate) {
			return (super.isFactoryMethod(candidate) && AnnotationUtils.findAnnotation(candidate, Bean.class) != null);
		}

		@Override
		public ConfigurationClassBeanDefinition cloneBeanDefinition() {
			return new ConfigurationClassBeanDefinition(this);
		}
	}

	
	/**
	 * Configuration classes must be annotated with {@link Configuration @Configuration} or
	 * declare at least one {@link Bean @Bean} method.
	 */
	private static class InvalidConfigurationImportProblem extends Problem {
	
		public InvalidConfigurationImportProblem(String className, Resource resource, AnnotationMetadata metadata) {
			super(String.format("%s was imported as a Configuration class but is not annotated " +
					"with @Configuration nor does it declare any @Bean methods. Update the class to " +
					"meet either of these requirements or do not attempt to import it.", className),
					new Location(resource, metadata));
		}
	
	}

}
