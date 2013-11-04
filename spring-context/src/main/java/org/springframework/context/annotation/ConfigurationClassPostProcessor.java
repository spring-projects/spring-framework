/*
 * Copyright 2002-2013 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.context.annotation.ConfigurationClassParser.ImportRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static org.springframework.context.annotation.AnnotationConfigUtils.*;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@code <context:annotation-config/>} or
 * {@code <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other BeanFactoryPostProcessor.
 *
 * <p>This post processor is {@link Ordered#HIGHEST_PRECEDENCE} as it is important
 * that any {@link Bean} methods declared in Configuration classes have their
 * respective bean definitions registered before any other BeanFactoryPostProcessor
 * executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
		PriorityOrdered, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

	private static final String IMPORT_AWARE_PROCESSOR_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".importAwareProcessor";

	private static final String IMPORT_REGISTRY_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".importRegistry";

	private static final String ENHANCED_CONFIGURATION_PROCESSOR_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".enhancedConfigurationProcessor";


	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private Environment environment;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private boolean setMetadataReaderFactoryCalled = false;

	private final Set<Integer> registriesPostProcessed = new HashSet<Integer>();

	private final Set<Integer> factoriesPostProcessed = new HashSet<Integer>();

	private ConfigurationClassBeanDefinitionReader reader;

	private boolean localBeanNameGeneratorSet = false;

	/* using short class names as default bean names */
	private BeanNameGenerator componentScanBeanNameGenerator = new AnnotationBeanNameGenerator();

	/* using fully qualified class names as default bean names */
	private BeanNameGenerator importBeanNameGenerator = new AnnotationBeanNameGenerator() {
		@Override
		protected String buildDefaultBeanName(BeanDefinition definition) {
			return definition.getBeanClassName();
		}
	};


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
	}

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
	 * declarations. For instance, an @Bean method marked as {@code final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@linkplain #setBeanClassLoader bean class loader}.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.setMetadataReaderFactoryCalled = true;
	}

	/**
	 * Set the {@link BeanNameGenerator} to be used when triggering component scanning
	 * from {@link Configuration} classes and when registering {@link Import}'ed
	 * configuration classes. The default is a standard {@link AnnotationBeanNameGenerator}
	 * for scanned components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
	 * and a variant thereof for imported configuration classes (using unique fully-qualified
	 * class names instead of standard component overriding).
	 * <p>Note that this strategy does <em>not</em> apply to {@link Bean} methods.
	 * <p>This setter is typically only appropriate when configuring the post-processor as
	 * a standalone bean definition in XML, e.g. not using the dedicated
	 * {@code AnnotationConfig*} application contexts or the {@code
	 * <context:annotation-config>} element. Any bean name generator specified against
	 * the application context will take precedence over any value set here.
	 * @since 3.1.1
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
		this.localBeanNameGeneratorSet = true;
		this.componentScanBeanNameGenerator = beanNameGenerator;
		this.importBeanNameGenerator = beanNameGenerator;
	}

	@Override
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}


	/**
	 * Derive further bean definitions from the configuration classes in the registry.
	 */
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		RootBeanDefinition iabpp = new RootBeanDefinition(ImportAwareBeanPostProcessor.class);
		iabpp.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(IMPORT_AWARE_PROCESSOR_BEAN_NAME, iabpp);

		RootBeanDefinition ecbpp = new RootBeanDefinition(EnhancedConfigurationBeanPostProcessor.class);
		ecbpp.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(ENHANCED_CONFIGURATION_PROCESSOR_BEAN_NAME, ecbpp);

		int registryId = System.identityHashCode(registry);
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanDefinitionRegistry already called for this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called for this post-processor against " + registry);
		}
		this.registriesPostProcessed.add(registryId);

		processConfigBeanDefinitions(registry);
	}

	/**
	 * Prepare the Configuration classes for servicing bean requests at runtime
	 * by replacing them with CGLIB-enhanced subclasses.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		int factoryId = System.identityHashCode(beanFactory);
		if (this.factoriesPostProcessed.contains(factoryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called for this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add((factoryId));
		if (!this.registriesPostProcessed.contains((factoryId))) {
			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
			// Simply call processConfigurationClasses lazily at this point then.
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}
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
			if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		if (configCandidates.isEmpty()) {
			return;
		}

		// Detect any custom bean name generation strategy supplied through the enclosing application context
		SingletonBeanRegistry singletonRegistry = null;
		if (registry instanceof SingletonBeanRegistry) {
			singletonRegistry = (SingletonBeanRegistry) registry;
			if (!this.localBeanNameGeneratorSet && singletonRegistry.containsSingleton(CONFIGURATION_BEAN_NAME_GENERATOR)) {
				BeanNameGenerator generator = (BeanNameGenerator) singletonRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
				this.componentScanBeanNameGenerator = generator;
				this.importBeanNameGenerator = generator;
			}
		}

		// Parse each @Configuration class
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);
		parser.parse(configCandidates);
		parser.validate();

		// Handle any @PropertySource annotations
		List<PropertySource<?>> parsedPropertySources = parser.getPropertySources();
		if (!parsedPropertySources.isEmpty()) {
			if (!(this.environment instanceof ConfigurableEnvironment)) {
				logger.warn("Ignoring @PropertySource annotations. " +
						"Reason: Environment must implement ConfigurableEnvironment");
			}
			else {
				MutablePropertySources envPropertySources = ((ConfigurableEnvironment)this.environment).getPropertySources();
				for (PropertySource<?> propertySource : parsedPropertySources) {
					envPropertySources.addLast(propertySource);
				}
			}
		}

		// Read the model and create bean definitions based on its content
		if (this.reader == null) {
			this.reader = new ConfigurationClassBeanDefinitionReader(registry, this.sourceExtractor,
					this.problemReporter, this.metadataReaderFactory, this.resourceLoader, this.environment,
					this.importBeanNameGenerator);
		}

		this.reader.loadBeanDefinitions(parser.getConfigurationClasses());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
		if (singletonRegistry != null) {
			if (!singletonRegistry.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
				singletonRegistry.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
			}
		}

		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
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
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
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
		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			try {
				Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				Class<?> enhancedClass = enhancer.enhance(configClass);
				if (configClass != enhancedClass) {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Replacing bean definition '%s' existing class name '%s' " +
								"with enhanced class name '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
					}
					beanDef.setBeanClass(enhancedClass);
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}


	private static class ImportAwareBeanPostProcessor implements BeanPostProcessor, PriorityOrdered, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)  {
			if (bean instanceof ImportAware) {
				ImportRegistry importRegistry = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				String importingClass = importRegistry.getImportingClassFor(bean.getClass().getSuperclass().getName());
				if (importingClass != null) {
					try {
						AnnotationMetadata metadata =
								new SimpleMetadataReaderFactory().getMetadataReader(importingClass).getAnnotationMetadata();
						((ImportAware) bean).setImportMetadata(metadata);
					}
					catch (IOException ex) {
						// should never occur -> at this point we know the class is present anyway
						throw new IllegalStateException(ex);
					}
				}
				else {
					// no importing class was found
				}
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			return bean;
		}
	}


	/**
	 * {@link InstantiationAwareBeanPostProcessorAdapter} that ensures
	 * {@link EnhancedConfiguration} beans are injected with the {@link BeanFactory}
	 * before the {@link AutowiredAnnotationBeanPostProcessor} runs (SPR-10668).
	 */
	private static class EnhancedConfigurationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
			implements PriorityOrdered, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {
			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
			// postProcessPropertyValues method attempts to auto-wire other configuration beans.
			if (bean instanceof EnhancedConfiguration) {
				((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

	}
}
