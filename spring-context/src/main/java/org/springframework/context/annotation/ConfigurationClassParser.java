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

import static org.springframework.context.annotation.MetadataUtils.attributesFor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderLog;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may import
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering BeanDefinition objects based on the
 * content of that model.
 *
 * <p>This ASM-based implementation avoids reflection and eager class loading in order to
 * interoperate effectively with lazy class loading in a Spring ApplicationContext.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
class ConfigurationClassParser {

	private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
			new Comparator<ConfigurationClassParser.DeferredImportSelectorHolder>() {
		public int compare(DeferredImportSelectorHolder o1,
				DeferredImportSelectorHolder o2) {
			return AnnotationAwareOrderComparator.INSTANCE.compare(
					o1.getImportSelector(), o2.getImportSelector());
		}
	};

	protected final Log logger = LogFactory.getLog(getClass());

	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final ImportStack importStack = new ImportStack();

	private final Set<String> knownSuperclasses = new LinkedHashSet<String>();

	private final Set<ConfigurationClass> configurationClasses =
		new LinkedHashSet<ConfigurationClass>();

	private final Stack<PropertySource<?>> propertySources =
		new Stack<PropertySource<?>>();

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;

	private final ComponentScanAnnotationParser componentScanParser;

	private final BeanNameGenerator beanNameGenerator;

	private final List<DeferredImportSelectorHolder> deferredImportSelectors =
			new LinkedList<DeferredImportSelectorHolder>();

	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used
	 * to populate the set of configuration classes.
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
			ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
			BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {

		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.beanNameGenerator = componentScanBeanNameGenerator;
		this.componentScanParser = new ComponentScanAnnotationParser(
				resourceLoader, environment, componentScanBeanNameGenerator, registry);
	}

	public void parse(Set<BeanDefinitionHolder> configCandidates) {
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException("Failed to load bean class: " + bd.getBeanClassName(), ex);
			}
		}
		processDeferredImportSelectors();
	}

	/**
	 * Parse the specified {@link Configuration @Configuration} class.
	 * @param className the name of the class to parse
	 * @param beanName may be null, but if populated represents the bean id
	 * (assumes that this configuration class was configured via XML)
	 */
	protected void parse(String className, String beanName) throws IOException {
		ConfigurationMetadataReader reader = new ConfigurationMetadataReader(className);
		processConfigurationClass(reader.getConfigurationClass(beanName));
	}

	/**
	 * Parse the specified {@link Configuration @Configuration} class.
	 * @param clazz the Class to parse
	 * @param beanName must not be null (as of Spring 3.1.1)
	 */
	protected void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}

	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		AnnotationMetadata metadata = configClass.getMetadata();

		if (ConditionalAnnotationHelper.shouldSkip(configClass, this.registry,
				this.environment, this.beanNameGenerator)) {
			return;
		}

		// recursively process the configuration class and its superclass hierarchy
		do {
			metadata = doProcessConfigurationClass(configClass, metadata);
		}
		while (metadata != null);

		if (this.configurationClasses.contains(configClass) && configClass.getBeanName() != null) {
			// Explicit bean definition found, probably replacing an import.
			// Let's remove the old one and go with the new one.
			this.configurationClasses.remove(configClass);
		}

		this.configurationClasses.add(configClass);
	}

	/**
	 * @return annotation metadata of superclass, null if none found or previously processed
	 */
	protected AnnotationMetadata doProcessConfigurationClass(
			ConfigurationClass configClass, AnnotationMetadata metadata) throws IOException {

		// recursively process any member (nested) classes first
		for (String memberClassName : metadata.getMemberClassNames()) {
			ConfigurationMetadataReader reader = new ConfigurationMetadataReader(memberClassName);
			AnnotationMetadata memberClassMetadata = reader.getReader().getAnnotationMetadata();
			if (ConfigurationClassUtils.isConfigurationCandidate(memberClassMetadata)) {
				processConfigurationClass(reader.getConfigurationClass(configClass));
			}
		}

		// process any @PropertySource annotations
		AnnotationAttributes propertySource =
				attributesFor(metadata, org.springframework.context.annotation.PropertySource.class);
		if (propertySource != null) {
			String name = propertySource.getString("name");
			String[] locations = propertySource.getStringArray("value");
			int nLocations = locations.length;
			if (nLocations == 0) {
				throw new IllegalArgumentException("At least one @PropertySource(value) location is required");
			}
			for (int i = 0; i < nLocations; i++) {
				locations[i] = this.environment.resolveRequiredPlaceholders(locations[i]);
			}
			ClassLoader classLoader = this.resourceLoader.getClassLoader();
			if (!StringUtils.hasText(name)) {
				for (String location : locations) {
					this.propertySources.push(new ResourcePropertySource(location, classLoader));
				}
			}
			else {
				if (nLocations == 1) {
					this.propertySources.push(new ResourcePropertySource(name, locations[0], classLoader));
				}
				else {
					CompositePropertySource ps = new CompositePropertySource(name);
					for (String location : locations) {
						ps.addPropertySource(new ResourcePropertySource(location, classLoader));
					}
					this.propertySources.push(ps);
				}
			}
		}

		// process any @ComponentScan annotions
		AnnotationAttributes componentScan = attributesFor(metadata, ComponentScan.class);
		if (componentScan != null) {
			// the config class is annotated with @ComponentScan -> perform the scan immediately
			Set<BeanDefinitionHolder> scannedBeanDefinitions =
					this.componentScanParser.parse(componentScan, metadata.getClassName());

			// check the set of scanned definitions for any further config classes and parse recursively if necessary
			for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
				if (ConfigurationClassUtils.checkConfigurationClassCandidate(holder.getBeanDefinition(), this.metadataReaderFactory)) {
					this.parse(holder.getBeanDefinition().getBeanClassName(), holder.getBeanName());
				}
			}
		}

		// process any @Import annotations
		Set<String> imports = getImports(metadata.getClassName(), null, new HashSet<String>());
		if (!CollectionUtils.isEmpty(imports)) {
			processImport(configClass, imports.toArray(new String[imports.size()]), true);
		}

		// process any @ImportResource annotations
		if (metadata.isAnnotated(ImportResource.class.getName())) {
			AnnotationAttributes importResource = attributesFor(metadata, ImportResource.class);
			String[] resources = importResource.getStringArray("value");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			for (String resource : resources) {
				configClass.addImportedResource(resource, readerClass);
			}
		}

		// process individual @Bean methods
		Set<MethodMetadata> beanMethods = metadata.getAnnotatedMethods(Bean.class.getName());
		for (MethodMetadata methodMetadata : beanMethods) {
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// process superclass, if any
		if (metadata.hasSuperClass()) {
			String superclass = metadata.getSuperClassName();
			if (this.knownSuperclasses.add(superclass)) {
				// superclass found, return its annotation metadata and recurse
				if (metadata instanceof StandardAnnotationMetadata) {
					Class<?> clazz = ((StandardAnnotationMetadata) metadata).getIntrospectedClass();
					return new StandardAnnotationMetadata(clazz.getSuperclass(), true);
				}
				else if (superclass.startsWith("java")) {
					// never load core JDK classes via ASM, in particular not java.lang.Object!
					try {
						return new StandardAnnotationMetadata(
								this.resourceLoader.getClassLoader().loadClass(superclass), true);
					}
					catch (ClassNotFoundException ex) {
						throw new IllegalStateException(ex);
					}
				}
				else {
					MetadataReader reader = this.metadataReaderFactory.getMetadataReader(superclass);
					return reader.getAnnotationMetadata();
				}
			}
		}

		// no superclass, processing is complete
		return null;
	}

	/**
	 * Recursively collect all declared {@code @Import} values. Unlike most
	 * meta-annotations it is valid to have several {@code @Import}s declared with
	 * different values, the usual process or returning values from the first
	 * meta-annotation on a class is not sufficient.
	 * <p>For example, it is common for a {@code @Configuration} class to declare direct
	 * {@code @Import}s in addition to meta-imports originating from an {@code @Enable}
	 * annotation.
	 * @param className the class name to search
	 * @param imports the imports collected so far or {@code null}
	 * @param visited used to track visited classes to prevent infinite recursion (must not be null)
	 * @return a set of all {@link Import#value() import values} or {@code null}
	 * @throws IOException if there is any problem reading metadata from the named class
	 */
	private Set<String> getImports(String className, Set<String> imports, Set<String> visited) throws IOException {
		if (visited.add(className) && !className.startsWith("java")) {
			ConfigurationMetadataReader reader = new ConfigurationMetadataReader(className);
			AnnotationMetadata metadata = reader.getReader().getAnnotationMetadata();
			for (String annotationType : metadata.getAnnotationTypes()) {
				imports = getImports(annotationType, imports, visited);
			}
			Map<String, Object> attributes = metadata.getAnnotationAttributes(Import.class.getName(), true);
			if (attributes != null) {
				String[] value = (String[]) attributes.get("value");
				if (value != null && value.length > 0) {
					imports = (imports == null ? new LinkedHashSet<String>() : imports);
					imports.addAll(Arrays.asList(value));
				}
			}
		}
		return imports;
	}

	private void processDeferredImportSelectors() {
		Collections.sort(this.deferredImportSelectors, DEFERRED_IMPORT_COMPARATOR);
		for (DeferredImportSelectorHolder deferredImport : this.deferredImportSelectors) {
			try {
				ConfigurationClass configClass = deferredImport.getConfigurationClass();
				String[] imports = deferredImport.getImportSelector().selectImports(configClass.getMetadata());
				processImport(configClass, imports, false);
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException("Failed to load bean class: ", ex);
			}
		}
		deferredImportSelectors.clear();
	}

	private void processImport(ConfigurationClass configClass, String[] classesToImport, boolean checkForCircularImports) throws IOException {
		if (checkForCircularImports && this.importStack.contains(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack, configClass.getMetadata()));
		}
		else {
			this.importStack.push(configClass);
			AnnotationMetadata importingClassMetadata = configClass.getMetadata();
			for (String candidate : classesToImport) {
				ConfigurationMetadataReader reader = new ConfigurationMetadataReader(candidate);
				if (reader.match(ImportSelector.class)) {
					// the candidate class is an ImportSelector -> delegate to it to determine imports
					reader.flushLog();
					try {
						ImportSelector selector = BeanUtils.instantiateClass(
								this.resourceLoader.getClassLoader().loadClass(candidate), ImportSelector.class);
						invokeAwareMethods(selector);
						if(selector instanceof DeferredImportSelector) {
							this.deferredImportSelectors.add(new DeferredImportSelectorHolder(
									configClass, (DeferredImportSelector) selector));
						} else {
							processImport(configClass, selector.selectImports(importingClassMetadata), false);
						}
					}
					catch (ClassNotFoundException ex) {
						throw new IllegalStateException(ex);
					}
				}
				else if (reader.match(ImportBeanDefinitionRegistrar.class)) {
					// the candidate class is an ImportBeanDefinitionRegistrar -> delegate to it to register additional bean definitions
					reader.flushLog();
					try {
						ImportBeanDefinitionRegistrar registrar = BeanUtils.instantiateClass(
								this.resourceLoader.getClassLoader().loadClass(candidate), ImportBeanDefinitionRegistrar.class);
						invokeAwareMethods(registrar);
						registrar.registerBeanDefinitions(importingClassMetadata, registry);
					}
					catch (ClassNotFoundException ex) {
						throw new IllegalStateException(ex);
					}
				}
				else {
					// the candidate class not an ImportSelector or ImportBeanDefinitionRegistrar -> process it as a @Configuration class
					this.importStack.registerImport(importingClassMetadata.getClassName(), candidate);
					processConfigurationClass(reader.getConfigurationClass(configClass));
				}
			}
			this.importStack.pop();
		}
	}

	/**
	 * Invoke {@link ResourceLoaderAware}, {@link BeanClassLoaderAware} and
	 * {@link BeanFactoryAware} contracts if implemented by the given {@code bean}.
	 */
	private void invokeAwareMethods(Object importStrategyBean) {
		if (importStrategyBean instanceof Aware) {
			if (importStrategyBean instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) importStrategyBean).setResourceLoader(this.resourceLoader);
			}
			if (importStrategyBean instanceof BeanClassLoaderAware) {
				ClassLoader classLoader = (this.registry instanceof ConfigurableBeanFactory ?
						((ConfigurableBeanFactory) this.registry).getBeanClassLoader() :
						this.resourceLoader.getClassLoader());
				((BeanClassLoaderAware) importStrategyBean).setBeanClassLoader(classLoader);
			}
			if (importStrategyBean instanceof BeanFactoryAware && this.registry instanceof BeanFactory) {
				((BeanFactoryAware) importStrategyBean).setBeanFactory((BeanFactory) this.registry);
			}
		}
	}


	/**
	 * Validate each {@link ConfigurationClass} object.
	 * @see ConfigurationClass#validate
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.configurationClasses) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getConfigurationClasses() {
		return this.configurationClasses;
	}

	public Stack<PropertySource<?>> getPropertySources() {
		return this.propertySources;
	}

	public ImportRegistry getImportRegistry() {
		return this.importStack;
	}


	interface ImportRegistry {

		String getImportingClassFor(String importedClass);
	}


	@SuppressWarnings("serial")
	private static class ImportStack extends Stack<ConfigurationClass> implements ImportRegistry {

		private final Map<String, String> imports = new HashMap<String, String>();

		public void registerImport(String importingClass, String importedClass) {
			this.imports.put(importedClass, importingClass);
		}

		public String getImportingClassFor(String importedClass) {
			return this.imports.get(importedClass);
		}

		/**
		 * Simplified contains() implementation that tests to see if any {@link ConfigurationClass}
		 * exists within this stack that has the same name as <var>elem</var>. Elem must be of
		 * type ConfigurationClass.
		 */
		@Override
		public boolean contains(Object elem) {
			ConfigurationClass configClass = (ConfigurationClass) elem;
			Comparator<ConfigurationClass> comparator = new Comparator<ConfigurationClass>() {
				public int compare(ConfigurationClass first, ConfigurationClass second) {
					return first.getMetadata().getClassName().equals(second.getMetadata().getClassName()) ? 0 : 1;
				}
			};
			return (Collections.binarySearch(this, configClass, comparator) != -1);
		}

		/**
		 * Given a stack containing (in order)
		 * <ul>
		 * <li>com.acme.Foo</li>
		 * <li>com.acme.Bar</li>
		 * <li>com.acme.Baz</li>
		 * </ul>
		 * return "ImportStack: [Foo->Bar->Baz]".
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder("ImportStack: [");
			Iterator<ConfigurationClass> iterator = iterator();
			while (iterator.hasNext()) {
				builder.append(iterator.next().getSimpleName());
				if (iterator.hasNext()) {
					builder.append("->");
				}
			}
			return builder.append(']').toString();
		}
	}


	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Stack<ConfigurationClass> importStack, AnnotationMetadata metadata) {
			super(String.format("A circular @Import has been detected: " +
					"Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
					"already present in the current import stack [%s]", importStack.peek().getSimpleName(),
					attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
					new Location(importStack.peek().getResource(), metadata));
		}
	}


	private class ConfigurationMetadataReader {

		private final MetadataReader reader;

		private List<MetadataReaderLogEntry> logEntries;

		public ConfigurationMetadataReader(String className) throws IOException {
			MetadataReaderFactory factory = metadataReaderFactory;
			if (factory instanceof SimpleMetadataReaderFactory) {
				this.reader = ((SimpleMetadataReaderFactory) factory).getMetadataReader(
						className, getLogger());
			}
			else {
				this.reader = factory.getMetadataReader(className);
			}
		}

		private MetadataReaderLog getLogger() {
			return new MetadataReaderLog() {

				public void log(String message, Throwable t) {
					add(false, message, t);
				}

				private void add(boolean debug, String message, Throwable t) {
					if (logEntries == null) {
						logEntries = new ArrayList<MetadataReaderLogEntry>();
					}
					logEntries.add(new MetadataReaderLogEntry(debug, message, t));
				}
			};
		}

		public boolean match(Class<?> targetType) throws IOException {
			return new AssignableTypeFilter(targetType).match(reader,
					metadataReaderFactory);
		}

		public ConfigurationClass getConfigurationClass(ConfigurationClass importedBy) {
			return flushLogIfNotSkipped(new ConfigurationClass(reader, importedBy));
		}

		public ConfigurationClass getConfigurationClass(String beanName) {
			return flushLogIfNotSkipped(new ConfigurationClass(reader, beanName));
		}

		private ConfigurationClass flushLogIfNotSkipped(
				ConfigurationClass configurationClass) {
			if (!ConditionalAnnotationHelper.shouldSkip(configurationClass, registry,
					environment, beanNameGenerator)) {
				flushLog();
			}
			return configurationClass;
		}

		public MetadataReader getReader() {
			return this.reader;
		}

		public void flushLog() {
			if(this.logEntries != null) {
				for (MetadataReaderLogEntry logEntry : logEntries) {
					logEntry.log();
				}
				this.logEntries = null;
			}
		}
	}


	private class MetadataReaderLogEntry {

		private String message;

		private Throwable t;

		public MetadataReaderLogEntry(boolean debug, String message, Throwable t) {
			this.message = message;
			this.t = t;
		}

		public void log() {
			logger.debug(message, t);
		}
	}


	private static class DeferredImportSelectorHolder {

		private ConfigurationClass configurationClass;

		private DeferredImportSelector importSelector;

		public DeferredImportSelectorHolder(ConfigurationClass configurationClass, DeferredImportSelector importSelector) {
			this.configurationClass = configurationClass;
			this.importSelector = importSelector;
		}

		public ConfigurationClass getConfigurationClass() {
			return configurationClass;
		}

		public DeferredImportSelector getImportSelector() {
			return importSelector;
		}
	}
}
