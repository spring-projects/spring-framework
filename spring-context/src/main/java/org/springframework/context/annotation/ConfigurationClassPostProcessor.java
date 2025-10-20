/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.aot.InstanceSupplierCodeGenerator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.BeanRegistryAdapter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertySourceDescriptor;
import org.springframework.core.io.support.PropertySourceProcessor;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.NameAllocator;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@code <context:annotation-config/>} or
 * {@code <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other {@link BeanFactoryPostProcessor}.
 *
 * <p>This post processor is priority-ordered as it is important that any
 * {@link Bean @Bean} methods declared in {@code @Configuration} classes have
 * their corresponding bean definitions registered before any other
 * {@code BeanFactoryPostProcessor} executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
		BeanRegistrationAotProcessor, BeanFactoryInitializationAotProcessor, PriorityOrdered,
		ResourceLoaderAware, ApplicationStartupAware, BeanClassLoaderAware, EnvironmentAware {

	/**
	 * A {@code BeanNameGenerator} using fully qualified class names as default bean names.
	 * <p>This default for configuration-level import purposes may be overridden through
	 * {@link #setBeanNameGenerator}. Note that the default for component scanning purposes
	 * is a plain {@link AnnotationBeanNameGenerator#INSTANCE}, unless overridden through
	 * {@link #setBeanNameGenerator} with a unified user-level bean name generator.
	 * @since 5.2
	 * @see #setBeanNameGenerator
	 */
	public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR =
			FullyQualifiedAnnotationBeanNameGenerator.INSTANCE;

	private static final String IMPORT_REGISTRY_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".importRegistry";


	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private @Nullable Environment environment;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private boolean setMetadataReaderFactoryCalled = false;

	private final Set<Integer> registriesPostProcessed = new HashSet<>();

	private final Set<Integer> factoriesPostProcessed = new HashSet<>();

	private @Nullable ConfigurationClassBeanDefinitionReader reader;

	private boolean localBeanNameGeneratorSet = false;

	/* Using short class names as default bean names by default. */
	private BeanNameGenerator componentScanBeanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	/* Using fully qualified class names as default bean names by default. */
	private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;

	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	private List<PropertySourceDescriptor> propertySourceDescriptors = Collections.emptyList();

	private final MultiValueMap<String, BeanRegistrar> beanRegistrars = new LinkedMultiValueMap<>();


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
	}

	/**
	 * Set the {@link SourceExtractor} to use for generated bean definitions
	 * that correspond to {@link Bean} factory methods.
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
	}

	/**
	 * Set the {@link ProblemReporter} to use.
	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, an @Bean method marked as {@code final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
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
	 * <p>This setter is typically only appropriate when configuring the post-processor as a
	 * standalone bean definition in XML, for example, not using the dedicated {@code AnnotationConfig*}
	 * application contexts or the {@code <context:annotation-config>} element. Any bean name
	 * generator specified against the application context will take precedence over any set here.
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
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		this.applicationStartup = applicationStartup;
	}

	/**
	 * Derive further bean definitions from the configuration classes in the registry.
	 */
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		int registryId = System.identityHashCode(registry);
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + registry);
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
					"postProcessBeanFactory already called on this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add(factoryId);
		if (!this.registriesPostProcessed.contains(factoryId)) {
			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
			// Simply call processConfigurationClasses lazily at this point then.
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}

		enhanceConfigurationClasses(beanFactory);
		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
	}

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Object configClassAttr = registeredBean.getMergedBeanDefinition()
				.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
		if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
			return BeanRegistrationAotContribution.withCustomCodeFragments(codeFragments ->
					new ConfigurationClassProxyBeanRegistrationCodeFragments(codeFragments, registeredBean));
		}
		return null;
	}

	@Override
	public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		boolean hasPropertySourceDescriptors = !CollectionUtils.isEmpty(this.propertySourceDescriptors);
		boolean hasImportRegistry = beanFactory.containsBean(IMPORT_REGISTRY_BEAN_NAME);
		boolean hasBeanRegistrars = !this.beanRegistrars.isEmpty();
		if (hasPropertySourceDescriptors || hasImportRegistry || hasBeanRegistrars) {
			return (generationContext, code) -> {
				if (hasPropertySourceDescriptors) {
					new PropertySourcesAotContribution(this.propertySourceDescriptors, this::resolvePropertySourceLocation)
							.applyTo(generationContext, code);
				}
				if (hasImportRegistry) {
					new ImportAwareAotContribution(beanFactory).applyTo(generationContext, code);
				}
				if (hasBeanRegistrars) {
					new BeanRegistrarAotContribution(this.beanRegistrars, beanFactory).applyTo(generationContext, code);
				}
			};
		}
		return null;
	}

	private @Nullable Resource resolvePropertySourceLocation(String location) {
		try {
			String resolvedLocation = (this.environment != null ?
					this.environment.resolveRequiredPlaceholders(location) : location);
			return this.resourceLoader.getResource(resolvedLocation);
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Build and validate a configuration model based on the registry of
	 * {@link Configuration} classes.
	 */
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
		String[] candidateNames = registry.getBeanDefinitionNames();

		for (String beanName : candidateNames) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		if (configCandidates.isEmpty()) {
			return;
		}

		// Sort by previously determined @Order value, if applicable
		configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});

		// Detect any custom bean name generation strategy supplied through the enclosing application context
		SingletonBeanRegistry singletonRegistry = null;
		if (registry instanceof SingletonBeanRegistry sbr) {
			singletonRegistry = sbr;
			BeanNameGenerator configurationGenerator = (BeanNameGenerator) singletonRegistry.getSingleton(
					AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
			if (configurationGenerator != null) {
				if (this.localBeanNameGeneratorSet) {
					if (configurationGenerator instanceof ConfigurationBeanNameGenerator &
							configurationGenerator != this.importBeanNameGenerator) {
						throw new IllegalStateException("Context-level ConfigurationBeanNameGenerator [" +
								configurationGenerator + "] must not be overridden with processor-level generator [" +
								this.importBeanNameGenerator + "]");
					}
				}
				else {
					this.componentScanBeanNameGenerator = configurationGenerator;
					this.importBeanNameGenerator = configurationGenerator;
				}
			}
		}

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		// Parse each @Configuration class
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);

		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
		Set<ConfigurationClass> alreadyParsed = CollectionUtils.newHashSet(configCandidates.size());
		do {
			StartupStep processConfig = this.applicationStartup.start("spring.context.config-classes.parse");
			parser.parse(candidates);
			parser.validate();

			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
			configClasses.removeAll(alreadyParsed);

			// Read the model and create bean definitions based on its content
			if (this.reader == null) {
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}
			this.reader.loadBeanDefinitions(configClasses);
			for (ConfigurationClass configClass : configClasses) {
				this.beanRegistrars.addAll(configClass.getBeanRegistrars());
			}
			alreadyParsed.addAll(configClasses);
			processConfig.tag("classCount", () -> String.valueOf(configClasses.size())).end();

			candidates.clear();
			if (registry.getBeanDefinitionCount() > candidateNames.length) {
				String[] newCandidateNames = registry.getBeanDefinitionNames();
				Set<String> oldCandidateNames = Set.of(candidateNames);
				Set<String> alreadyParsedClasses = CollectionUtils.newHashSet(alreadyParsed.size());
				for (ConfigurationClass configurationClass : alreadyParsed) {
					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
				}
				for (String candidateName : newCandidateNames) {
					if (!oldCandidateNames.contains(candidateName)) {
						BeanDefinition bd = registry.getBeanDefinition(candidateName);
						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
								!alreadyParsedClasses.contains(bd.getBeanClassName())) {
							candidates.add(new BeanDefinitionHolder(bd, candidateName));
						}
					}
				}
				candidateNames = newCandidateNames;
			}
		}
		while (!candidates.isEmpty());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
		if (singletonRegistry != null && !singletonRegistry.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			singletonRegistry.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}

		// Store the PropertySourceDescriptors to contribute them Ahead-of-time if necessary
		this.propertySourceDescriptors = parser.getPropertySourceDescriptors();

		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory cachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			cachingMetadataReaderFactory.clearCache();
		}
	}

	/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		StartupStep enhanceConfigClasses = this.applicationStartup.start("spring.context.config-classes.enhance");
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			Object configClassAttr = beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
			AnnotationMetadata annotationMetadata = null;
			MethodMetadata methodMetadata = null;
			if (beanDef instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
				annotationMetadata = annotatedBeanDefinition.getMetadata();
				methodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
			}
			if ((configClassAttr != null || methodMetadata != null) &&
					(beanDef instanceof AbstractBeanDefinition abd) && !abd.hasBeanClass()) {
				// Configuration class (full or lite) or a configuration-derived @Bean method
				// -> eagerly resolve bean class at this point, unless it's a 'lite' configuration
				// or component class without @Bean methods.
				boolean liteConfigurationCandidateWithoutBeanMethods =
						(ConfigurationClassUtils.CONFIGURATION_CLASS_LITE.equals(configClassAttr) &&
							annotationMetadata != null && !ConfigurationClassUtils.hasBeanMethods(annotationMetadata));
				if (!liteConfigurationCandidateWithoutBeanMethods) {
					try {
						abd.resolveBeanClass(this.beanClassLoader);
					}
					catch (Throwable ex) {
						throw new IllegalStateException(
								"Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
					}
				}
			}
			if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
				if (!(beanDef instanceof AbstractBeanDefinition abd)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}
				else if (beanFactory.containsSingleton(beanName)) {
					if (logger.isWarnEnabled()) {
						logger.warn("Cannot enhance @Configuration bean definition '" + beanName +
								"' since its singleton instance has been created too early. The typical cause " +
								"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
								"return type: Consider declaring such methods as 'static' and/or marking the " +
								"containing configuration class as 'proxyBeanMethods=false'.");
					}
				}
				else {
					configBeanDefs.put(beanName, abd);
				}
			}
		}
		if (configBeanDefs.isEmpty()) {
			// nothing to enhance -> return immediately
			enhanceConfigClasses.end();
			return;
		}

		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			// If a @Configuration class gets proxied, always proxy the target class
			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// Set enhanced subclass of the user-specified bean class
			Class<?> configClass = beanDef.getBeanClass();
			Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
			if (configClass != enhancedClass) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
							"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
				}
				beanDef.setBeanClass(enhancedClass);
			}
		}
		enhanceConfigClasses.tag("classCount", () -> String.valueOf(configBeanDefs.keySet().size())).end();
	}


	private static class ImportAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

		private final BeanFactory beanFactory;

		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public @Nullable PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {
			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
			// postProcessProperties method attempts to autowire other configuration beans.
			if (bean instanceof EnhancedConfiguration enhancedConfiguration) {
				enhancedConfiguration.setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof ImportAware importAware) {
				ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				AnnotationMetadata importingClass = ir.getImportingClassFor(ClassUtils.getUserClass(bean).getName());
				if (importingClass != null) {
					importAware.setImportMetadata(importingClass);
				}
			}
			return bean;
		}
	}


	private static class ImportAwareAotContribution implements BeanFactoryInitializationAotContribution {

		private static final String BEAN_FACTORY_VARIABLE = BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE;

		private static final ParameterizedTypeName STRING_STRING_MAP =
				ParameterizedTypeName.get(Map.class, String.class, String.class);

		private static final String MAPPINGS_VARIABLE = "mappings";

		private static final String BEAN_DEFINITION_VARIABLE = "beanDefinition";

		private static final String BEAN_NAME = "org.springframework.context.annotation.internalImportAwareAotProcessor";

		private final ConfigurableListableBeanFactory beanFactory;

		public ImportAwareAotContribution(ConfigurableListableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {

			Map<String, String> mappings = buildImportAwareMappings();
			if (!mappings.isEmpty()) {
				GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods().add(
						"addImportAwareBeanPostProcessors", method -> generateAddPostProcessorMethod(method, mappings));
				beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
				ResourceHints hints = generationContext.getRuntimeHints().resources();
				mappings.forEach((target, from) -> hints.registerType(TypeReference.of(from)));
			}
		}

		private void generateAddPostProcessorMethod(MethodSpec.Builder method, Map<String, String> mappings) {
			method.addJavadoc("Add ImportAwareBeanPostProcessor to support ImportAware beans.");
			method.addModifiers(Modifier.PRIVATE);
			method.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_VARIABLE);
			method.addCode(generateAddPostProcessorCode(mappings));
		}

		private CodeBlock generateAddPostProcessorCode(Map<String, String> mappings) {
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement("$T $L = new $T<>()", STRING_STRING_MAP,
					MAPPINGS_VARIABLE, HashMap.class);
			mappings.forEach((type, from) -> code.addStatement("$L.put($S, $S)",
					MAPPINGS_VARIABLE, type, from));
			code.addStatement("$T $L = new $T($T.class)", RootBeanDefinition.class,
					BEAN_DEFINITION_VARIABLE, RootBeanDefinition.class, ImportAwareAotBeanPostProcessor.class);
			code.addStatement("$L.setRole($T.ROLE_INFRASTRUCTURE)",
					BEAN_DEFINITION_VARIABLE, BeanDefinition.class);
			code.addStatement("$L.setInstanceSupplier(() -> new $T($L))",
					BEAN_DEFINITION_VARIABLE, ImportAwareAotBeanPostProcessor.class, MAPPINGS_VARIABLE);
			code.addStatement("$L.registerBeanDefinition($S, $L)",
					BEAN_FACTORY_VARIABLE, BEAN_NAME, BEAN_DEFINITION_VARIABLE);
			return code.build();
		}

		private Map<String, String> buildImportAwareMappings() {
			ImportRegistry importRegistry = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
			Map<String, String> mappings = new LinkedHashMap<>();
			for (String name : this.beanFactory.getBeanDefinitionNames()) {
				Class<?> beanType = this.beanFactory.getType(name);
				if (beanType != null && ImportAware.class.isAssignableFrom(beanType)) {
					String target = ClassUtils.getUserClass(beanType).getName();
					AnnotationMetadata from = importRegistry.getImportingClassFor(target);
					if (from != null) {
						mappings.put(target, from.getClassName());
					}
				}
			}
			return mappings;
		}
	}


	private static class PropertySourcesAotContribution implements BeanFactoryInitializationAotContribution {

		private static final String ENVIRONMENT_VARIABLE = "environment";

		private static final String RESOURCE_LOADER_VARIABLE = "resourceLoader";

		private final Log logger = LogFactory.getLog(getClass());

		private final List<PropertySourceDescriptor> descriptors;

		private final Function<String, @Nullable Resource> resourceResolver;

		PropertySourcesAotContribution(List<PropertySourceDescriptor> descriptors, Function<String, @Nullable Resource> resourceResolver) {
			this.descriptors = descriptors;
			this.resourceResolver = resourceResolver;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			registerRuntimeHints(generationContext.getRuntimeHints());
			GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods()
					.add("processPropertySources", this::generateAddPropertySourceProcessorMethod);
			beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
		}

		private void registerRuntimeHints(RuntimeHints hints) {
			for (PropertySourceDescriptor descriptor : this.descriptors) {
				Class<?> factoryClass = descriptor.propertySourceFactory();
				if (factoryClass != null) {
					hints.reflection().registerType(factoryClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
				}
				for (String location : descriptor.locations()) {
					if (location.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX) ||
							(location.startsWith(ResourcePatternResolver.CLASSPATH_URL_PREFIX) &&
									(location.contains("*") || location.contains("?")))) {

						if (logger.isWarnEnabled()) {
							logger.warn("""
									Runtime hint registration is not supported for the 'classpath*:' \
									prefix or wildcards in @PropertySource locations. Please manually \
									register a resource hint for each property source location represented \
									by '%s'.""".formatted(location));
						}
					}
					else {
						Resource resource = this.resourceResolver.apply(location);
						if (resource instanceof ClassPathResource classPathResource && classPathResource.exists()) {
							hints.resources().registerPattern(classPathResource.getPath());
						}
					}
				}
			}
		}

		private void generateAddPropertySourceProcessorMethod(MethodSpec.Builder method) {
			method.addJavadoc("Apply known @PropertySources to the environment.");
			method.addModifiers(Modifier.PRIVATE);
			method.addParameter(ConfigurableEnvironment.class, ENVIRONMENT_VARIABLE);
			method.addParameter(ResourceLoader.class, RESOURCE_LOADER_VARIABLE);
			method.addCode(generateAddPropertySourceProcessorCode());
		}

		private CodeBlock generateAddPropertySourceProcessorCode() {
			Builder code = CodeBlock.builder();
			String processorVariable = "processor";
			code.addStatement("$T $L = new $T($L, $L)", PropertySourceProcessor.class,
					processorVariable, PropertySourceProcessor.class, ENVIRONMENT_VARIABLE,
					RESOURCE_LOADER_VARIABLE);
			code.beginControlFlow("try");
			for (PropertySourceDescriptor descriptor : this.descriptors) {
				code.addStatement("$L.processPropertySource($L)", processorVariable,
						generatePropertySourceDescriptorCode(descriptor));
			}
			code.nextControlFlow("catch ($T ex)", IOException.class);
			code.addStatement("throw new $T(ex)", UncheckedIOException.class);
			code.endControlFlow();
			return code.build();
		}

		private CodeBlock generatePropertySourceDescriptorCode(PropertySourceDescriptor descriptor) {
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("new $T(", PropertySourceDescriptor.class);
			CodeBlock values = descriptor.locations().stream()
					.map(value -> CodeBlock.of("$S", value)).collect(CodeBlock.joining(", "));
			if (descriptor.name() == null && descriptor.propertySourceFactory() == null &&
					descriptor.encoding() == null && !descriptor.ignoreResourceNotFound()) {
				code.add("$L)", values);
			}
			else {
				List<CodeBlock> arguments = new ArrayList<>();
				arguments.add(CodeBlock.of("$T.of($L)", List.class, values));
				arguments.add(CodeBlock.of("$L", descriptor.ignoreResourceNotFound()));
				arguments.add(handleNull(descriptor.name(), () -> CodeBlock.of("$S", descriptor.name())));
				arguments.add(handleNull(descriptor.propertySourceFactory(),
						() -> CodeBlock.of("$T.class", descriptor.propertySourceFactory())));
				arguments.add(handleNull(descriptor.encoding(),
						() -> CodeBlock.of("$S", descriptor.encoding())));
				code.add(CodeBlock.join(arguments, ", "));
				code.add(")");
			}
			return code.build();
		}

		private CodeBlock handleNull(@Nullable Object value, Supplier<CodeBlock> nonNull) {
			return (value == null ? CodeBlock.of("null") : nonNull.get());
		}
	}


	private static class ConfigurationClassProxyBeanRegistrationCodeFragments extends BeanRegistrationCodeFragmentsDecorator {

		private final RegisteredBean registeredBean;

		private final Class<?> proxyClass;

		public ConfigurationClassProxyBeanRegistrationCodeFragments(
				BeanRegistrationCodeFragments codeFragments, RegisteredBean registeredBean) {

			super(codeFragments);
			this.registeredBean = registeredBean;
			this.proxyClass = registeredBean.getBeanType().toClass();
		}

		@Override
		public CodeBlock generateSetBeanDefinitionPropertiesCode(
				GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
				RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {

			CodeBlock.Builder code = CodeBlock.builder();
			code.add(super.generateSetBeanDefinitionPropertiesCode(generationContext,
					beanRegistrationCode, beanDefinition, attributeFilter));
			code.addStatement("$T.initializeConfigurationClass($T.class)",
					ConfigurationClassUtils.class, ClassUtils.getUserClass(this.proxyClass));
			return code.build();
		}

		@Override
		public CodeBlock generateInstanceSupplierCode(
				GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
				boolean allowDirectSupplierShortcut) {

			InstantiationDescriptor instantiationDescriptor = proxyInstantiationDescriptor(
					generationContext.getRuntimeHints(), this.registeredBean.resolveInstantiationDescriptor());

			return new InstanceSupplierCodeGenerator(generationContext,
					beanRegistrationCode.getClassName(), beanRegistrationCode.getMethods(), allowDirectSupplierShortcut)
					.generateCode(this.registeredBean, instantiationDescriptor);
		}

		private InstantiationDescriptor proxyInstantiationDescriptor(
				RuntimeHints runtimeHints, InstantiationDescriptor instantiationDescriptor) {

			Executable userExecutable = instantiationDescriptor.executable();
			if (userExecutable instanceof Constructor<?> userConstructor) {
				try {
					runtimeHints.reflection().registerType(userConstructor.getDeclaringClass());
					Constructor<?> constructor = this.proxyClass.getConstructor(userExecutable.getParameterTypes());
					return new InstantiationDescriptor(constructor);
				}
				catch (NoSuchMethodException ex) {
					throw new IllegalStateException("No matching constructor found on proxy " + this.proxyClass, ex);
				}
			}
			return instantiationDescriptor;
		}
	}


	private static class BeanRegistrarAotContribution implements BeanFactoryInitializationAotContribution {

		private static final String CUSTOMIZER_MAP_VARIABLE = "customizers";

		private static final String ENVIRONMENT_VARIABLE = "environment";

		private final MultiValueMap<String, BeanRegistrar> beanRegistrars;

		private final ConfigurableListableBeanFactory beanFactory;

		private final AotServices<BeanRegistrationAotProcessor> aotProcessors;

		public BeanRegistrarAotContribution(MultiValueMap<String, BeanRegistrar> beanRegistrars, ConfigurableListableBeanFactory beanFactory) {
			this.beanRegistrars = beanRegistrars;
			this.beanFactory = beanFactory;
			this.aotProcessors = AotServices.factoriesAndBeans(this.beanFactory).load(BeanRegistrationAotProcessor.class);
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods().add(
					"applyBeanRegistrars", builder -> this.generateApplyBeanRegistrarsMethod(builder, generationContext));
			beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
		}

		private void generateApplyBeanRegistrarsMethod(MethodSpec.Builder method, GenerationContext generationContext) {
			ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
			method.addJavadoc("Apply bean registrars.");
			method.addModifiers(Modifier.PRIVATE);
			method.addParameter(ListableBeanFactory.class, BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE);
			method.addParameter(Environment.class, ENVIRONMENT_VARIABLE);
			method.addCode(generateCustomizerMap());

			for (String name : this.beanFactory.getBeanDefinitionNames()) {
				BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition(name);
				if (beanDefinition.getSource() instanceof Class<?> sourceClass &&
						BeanRegistrar.class.isAssignableFrom(sourceClass)) {

					for (BeanRegistrationAotProcessor aotProcessor : this.aotProcessors) {
						BeanRegistrationAotContribution contribution =
								aotProcessor.processAheadOfTime(RegisteredBean.of(this.beanFactory, name));
						if (contribution != null) {
							contribution.applyTo(generationContext,
									new UnsupportedBeanRegistrationCode(name, aotProcessor.getClass()));
						}
					}
					if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
						if (rootBeanDefinition.getPreferredConstructors() != null) {
							for (Constructor<?> constructor : rootBeanDefinition.getPreferredConstructors()) {
								reflectionHints.registerConstructor(constructor, ExecutableMode.INVOKE);
							}
						}
						if (!ObjectUtils.isEmpty(rootBeanDefinition.getInitMethodNames())) {
							method.addCode(generateInitDestroyMethods(name, rootBeanDefinition,
									rootBeanDefinition.getInitMethodNames(), "setInitMethodNames", reflectionHints));
						}
						if (!ObjectUtils.isEmpty(rootBeanDefinition.getDestroyMethodNames())) {
							method.addCode(generateInitDestroyMethods(name, rootBeanDefinition,
									rootBeanDefinition.getDestroyMethodNames(), "setDestroyMethodNames", reflectionHints));
						}
						checkUnsupportedFeatures(rootBeanDefinition);
					}
				}
			}
			method.addCode(generateRegisterCode());
		}

		private void checkUnsupportedFeatures(AbstractBeanDefinition beanDefinition) {
			if (!ObjectUtils.isEmpty(beanDefinition.getFactoryBeanName())) {
				throw new UnsupportedOperationException("AOT post processing of the factory bean name is not supported yet with BeanRegistrar");
			}
			if (beanDefinition.hasConstructorArgumentValues()) {
				throw new UnsupportedOperationException("AOT post processing of argument values is not supported yet with BeanRegistrar");
			}
			if (!beanDefinition.getQualifiers().isEmpty()) {
				throw new UnsupportedOperationException("AOT post processing of qualifiers is not supported yet with BeanRegistrar");
			}
			for (String attributeName : beanDefinition.attributeNames()) {
				if (!attributeName.equals(AbstractBeanDefinition.ORDER_ATTRIBUTE) &&
						!attributeName.equals("aotProcessingIgnoreRegistration")) {
					throw new UnsupportedOperationException("AOT post processing of attribute " + attributeName +
							" is not supported yet with BeanRegistrar");
				}
			}
		}

		private CodeBlock generateCustomizerMap() {
			Builder code = CodeBlock.builder();
			code.addStatement("$T<$T, $T> $L = new $T<>()", MultiValueMap.class, String.class, BeanDefinitionCustomizer.class,
					CUSTOMIZER_MAP_VARIABLE, LinkedMultiValueMap.class);
			return code.build();
		}

		private CodeBlock generateRegisterCode() {
			Builder code = CodeBlock.builder();
			Builder metadataReaderFactoryCode = null;
			NameAllocator nameAllocator = new NameAllocator();
			for (Map.Entry<String, List<BeanRegistrar>> beanRegistrarEntry : this.beanRegistrars.entrySet()) {
				for (BeanRegistrar beanRegistrar : beanRegistrarEntry.getValue()) {
					String beanRegistrarName = nameAllocator.newName(StringUtils.uncapitalize(beanRegistrar.getClass().getSimpleName()));
					code.addStatement("$T $L = new $T()", beanRegistrar.getClass(), beanRegistrarName, beanRegistrar.getClass());
					if (beanRegistrar instanceof ImportAware) {
						if (metadataReaderFactoryCode == null) {
							metadataReaderFactoryCode = CodeBlock.builder();
							metadataReaderFactoryCode.addStatement("$T metadataReaderFactory = new $T()",
									MetadataReaderFactory.class, CachingMetadataReaderFactory.class);
						}
						code.beginControlFlow("try")
								.addStatement("$L.setImportMetadata(metadataReaderFactory.getMetadataReader($S).getAnnotationMetadata())",
										beanRegistrarName, beanRegistrarEntry.getKey())
								.nextControlFlow("catch ($T ex)", IOException.class)
								.addStatement("throw new $T(\"Failed to read metadata for '$L'\", ex)",
										IllegalStateException.class, beanRegistrarEntry.getKey())
								.endControlFlow();
					}
					code.addStatement("$L.register(new $T(($T)$L, $L, $L, $T.class, $L), $L)", beanRegistrarName,
							BeanRegistryAdapter.class, BeanDefinitionRegistry.class, BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE,
							BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE, ENVIRONMENT_VARIABLE, beanRegistrar.getClass(),
							CUSTOMIZER_MAP_VARIABLE, ENVIRONMENT_VARIABLE);
				}
			}
			return (metadataReaderFactoryCode == null ? code.build() : metadataReaderFactoryCode.add(code.build()).build());
		}

		private CodeBlock generateInitDestroyMethods(String beanName, AbstractBeanDefinition beanDefinition,
				String[] methodNames, String method, ReflectionHints reflectionHints) {

			Builder code = CodeBlock.builder();
			// For Publisher-based destroy methods
			reflectionHints.registerType(TypeReference.of("org.reactivestreams.Publisher"));
			Class<?> beanType = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
			Arrays.stream(methodNames).forEach(methodName -> addInitDestroyHint(beanType, methodName, reflectionHints));
			CodeBlock arguments = Arrays.stream(methodNames)
					.map(name -> CodeBlock.of("$S", name))
					.collect(CodeBlock.joining(", "));

			code.addStatement("$L.add($S, $L -> (($T)$L).$L($L))", CUSTOMIZER_MAP_VARIABLE, beanName, "bd",
					AbstractBeanDefinition.class, "bd", method, arguments);
			return code.build();
		}

		// Inspired from BeanDefinitionPropertiesCodeGenerator#addInitDestroyHint
		private static void addInitDestroyHint(Class<?> beanUserClass, String methodName, ReflectionHints reflectionHints) {
			Class<?> methodDeclaringClass = beanUserClass;

			// Parse fully-qualified method name if necessary.
			int indexOfDot = methodName.lastIndexOf('.');
			if (indexOfDot > 0) {
				String className = methodName.substring(0, indexOfDot);
				methodName = methodName.substring(indexOfDot + 1);
				if (!beanUserClass.getName().equals(className)) {
					try {
						methodDeclaringClass = ClassUtils.forName(className, beanUserClass.getClassLoader());
					}
					catch (Throwable ex) {
						throw new IllegalStateException("Failed to load Class [" + className +
								"] from ClassLoader [" + beanUserClass.getClassLoader() + "]", ex);
					}
				}
			}

			Method method = ReflectionUtils.findMethod(methodDeclaringClass, methodName);
			if (method != null) {
				reflectionHints.registerMethod(method, ExecutableMode.INVOKE);
				Method publiclyAccessibleMethod = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, beanUserClass);
				if (!publiclyAccessibleMethod.equals(method)) {
					reflectionHints.registerMethod(publiclyAccessibleMethod, ExecutableMode.INVOKE);
				}
			}
		}


		static class UnsupportedBeanRegistrationCode implements BeanRegistrationCode {

			private final String message;

			public UnsupportedBeanRegistrationCode(String beanName, Class<?> aotProcessorClass) {
				this.message = "Code generation attempted for bean " + beanName + " by the AOT Processor " +
						aotProcessorClass + " is not supported with BeanRegistrar yet";
			}

			@Override
			public ClassName getClassName() {
				throw new UnsupportedOperationException(this.message);
			}

			@Override
			public GeneratedMethods getMethods() {
				throw new UnsupportedOperationException(this.message);
			}

			@Override
			public void addInstancePostProcessor(MethodReference methodReference) {
				throw new UnsupportedOperationException(this.message);
			}
		}
	}

}
