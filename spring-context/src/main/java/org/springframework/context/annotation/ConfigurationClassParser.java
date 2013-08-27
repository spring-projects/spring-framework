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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.core.NestedIOException;
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
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may import
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering BeanDefinition objects based on the
 * content of that model (with the exception of {@code @ComponentScan} annotations which
 * need to be registered immediately).
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
				@Override
				public int compare(DeferredImportSelectorHolder o1, DeferredImportSelectorHolder o2) {
					return AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());
				}
			};


	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;

	private final ComponentScanAnnotationParser componentScanParser;

	private final Set<ConfigurationClass> configurationClasses = new LinkedHashSet<ConfigurationClass>();

	private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<String, ConfigurationClass>();

	private final Stack<PropertySource<?>> propertySources = new Stack<PropertySource<?>>();

	private final ImportStack importStack = new ImportStack();

	private final List<DeferredImportSelectorHolder> deferredImportSelectors = new LinkedList<DeferredImportSelectorHolder>();

	private final ConditionEvaluator conditionEvaluator;


	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used
	 * to populate the set of configuration classes.
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
			ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
			BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry,
			ApplicationContext applicationContext) {

		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.componentScanParser = new ComponentScanAnnotationParser(
				resourceLoader, environment, componentScanBeanNameGenerator, registry);
		this.conditionEvaluator = new ConditionEvaluator(registry, environment,
				applicationContext, null, resourceLoader);
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
	protected final void parse(String className, String beanName) throws IOException {
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
		processConfigurationClass(new ConfigurationClass(reader, beanName));
	}

	/**
	 * Parse the specified {@link Configuration @Configuration} class.
	 * @param clazz the Class to parse
	 * @param beanName must not be null (as of Spring 3.1.1)
	 */
	protected final void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}


	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		if (shouldSkip(asSourceClass(configClass), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}

		if (this.configurationClasses.contains(configClass) && configClass.getBeanName() != null) {
			// Explicit bean definition found, probably replacing an import.
			// Let's remove the old one and go with the new one.
			this.configurationClasses.remove(configClass);
			for (Iterator<ConfigurationClass> it = this.knownSuperclasses.values().iterator(); it.hasNext();) {
				if (configClass.equals(it.next())) {
					it.remove();
				}
			}
		}

		// Recursively process the configuration class and its superclass hierarchy.
		SourceClass sourceClass = asSourceClass(configClass);
		do {
			configClass.addMetadataHierarchy(sourceClass.getMetadata());
			sourceClass = doProcessConfigurationClass(configClass, sourceClass);
		}
		while (sourceClass != null);

		this.configurationClasses.add(configClass);
	}

	/**
	 * Apply processing and build a complete {@link ConfigurationClass} by reading the
	 * annotations, members and methods from the source class. This method can be called
	 * multiple times as relevant sources are discovered.
	 * @param configClass the configuration class being build
	 * @param sourceClass a source class
	 * @return the superclass, {@code null} if none found or previously processed
	 */
	protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		// recursively process any member (nested) classes first
		processMemberClasses(configClass, sourceClass);

		// process any @PropertySource annotations
		AnnotationAttributes propertySource = AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), org.springframework.context.annotation.PropertySource.class);
		if (propertySource != null) {
			processPropertySource(propertySource);
		}

		// process any @ComponentScan annotations
		AnnotationAttributes componentScan = AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ComponentScan.class);
		if (componentScan != null) {
			// the config class is annotated with @ComponentScan -> perform the scan immediately
			if (!shouldSkip(sourceClass, ConfigurationPhase.REGISTER_BEAN)) {
				Set<BeanDefinitionHolder> scannedBeanDefinitions =
						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());

				// check the set of scanned definitions for any further config classes and parse recursively if necessary
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					if (ConfigurationClassUtils.checkConfigurationClassCandidate(holder.getBeanDefinition(), this.metadataReaderFactory)) {
						parse(holder.getBeanDefinition().getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}

		// process any @Import annotations
		processImports(configClass, getImports(sourceClass), true);

		// process any @ImportResource annotations
		if (sourceClass.getMetadata().isAnnotated(ImportResource.class.getName())) {
			AnnotationAttributes importResource = AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
			String[] resources = importResource.getStringArray("value");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			for (String resource : resources) {
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}

		// process individual @Bean methods
		Set<MethodMetadata> beanMethods = sourceClass.getMetadata().getAnnotatedMethods(Bean.class.getName());
		for (MethodMetadata methodMetadata : beanMethods) {
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// process superclass, if any
		if (sourceClass.getMetadata().hasSuperClass()) {
			String superclass = sourceClass.getMetadata().getSuperClassName();
			if (!this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
				// superclass found, return its annotation metadata and recurse
				return sourceClass.getSuperClass();
			}
		}

		// no superclass, processing is complete
		return null;
	}

	private boolean shouldSkip(SourceClass sourceClass, ConfigurationPhase phase)
			throws IOException {
		while (sourceClass != null) {
			if (conditionEvaluator.shouldSkip(sourceClass.getMetadata(), phase)) {
				return true;
			}
			sourceClass = sourceClass.getSuperClass();
		}
		return false;
	}

	/**
	 * Register member (nested) classes that happen to be configuration classes themselves.
	 * @param sourceClass the source class to process
	 * @throws IOException if there is any problem reading metadata from a member class
	 */
	private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		for (SourceClass memberClass : sourceClass.getMemberClasses()) {
			if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata())) {
				processConfigurationClass(memberClass.asConfigClass(configClass));
			}
		}
	}

	/**
	 * Process the given <code>@PropertySource</code> annotation metadata.
	 * @param propertySource metadata for the <code>@PropertySource</code> annotation found
	 * @throws IOException if loading a property source failed
	 */
	private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
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

	/**
	 * Returns {@code @Import} class, considering all meta-annotations.
	 */
	private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
		Set<SourceClass> imports = new LinkedHashSet<SourceClass>();
		Set<SourceClass> visited = new LinkedHashSet<SourceClass>();
		collectImports(sourceClass, imports, visited);
		return imports;
	}

	/**
	 * Recursively collect all declared {@code @Import} values. Unlike most
	 * meta-annotations it is valid to have several {@code @Import}s declared with
	 * different values, the usual process or returning values from the first
	 * meta-annotation on a class is not sufficient.
	 * <p>For example, it is common for a {@code @Configuration} class to declare direct
	 * {@code @Import}s in addition to meta-imports originating from an {@code @Enable}
	 * annotation.
	 * @param sourceClass the class to search
	 * @param imports the imports collected so far
	 * @param visited used to track visited classes to prevent infinite recursion
	 * @throws IOException if there is any problem reading metadata from the named class
	 */
	private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited) throws IOException {
		try {
			if (visited.add(sourceClass)) {
				for (SourceClass annotation : sourceClass.getAnnotations()) {
					if(!annotation.getMetadata().getClassName().startsWith("java") && !annotation.isAssignable(Import.class)) {
						collectImports(annotation, imports, visited);
					}
				}
				imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
			}
		}
		catch (ClassNotFoundException ex) {
			throw new NestedIOException("Unable to collect imports", ex);
		}
	}

	private void processDeferredImportSelectors() {
		Collections.sort(this.deferredImportSelectors, DEFERRED_IMPORT_COMPARATOR);
		for (DeferredImportSelectorHolder deferredImport : this.deferredImportSelectors) {
			try {
				ConfigurationClass configClass = deferredImport.getConfigurationClass();
				String[] imports = deferredImport.getImportSelector().selectImports(configClass.getMetadata());
				processImports(configClass, asSourceClasses(imports), false);
			}
			catch (Exception ex) {
				throw new BeanDefinitionStoreException("Failed to load bean class: ", ex);
			}
		}
		this.deferredImportSelectors.clear();
	}

	private void processImports(ConfigurationClass configClass, Collection<SourceClass> sourceClasses, boolean checkForCircularImports)
			throws IOException {

		if(sourceClasses.isEmpty()) {
			return;
		}
		if (checkForCircularImports && this.importStack.contains(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack, configClass.getMetadata()));
		}
		else {
			this.importStack.push(configClass);
			AnnotationMetadata importingClassMetadata = configClass.getMetadata();
			try {
				for (SourceClass candidate : sourceClasses) {
					if (candidate.isAssignable(ImportSelector.class)) {
						// the candidate class is an ImportSelector -> delegate to it to determine imports
						Class<?> candidateClass = candidate.loadClass();
						ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
						invokeAwareMethods(selector);
						if(selector instanceof DeferredImportSelector) {
							this.deferredImportSelectors.add(new DeferredImportSelectorHolder(configClass, (DeferredImportSelector) selector));
						}
						else {
							String[] importClassNames = selector.selectImports(importingClassMetadata);
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
							processImports(configClass, importSourceClasses, false);
						}
					}
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// the candidate class is an ImportBeanDefinitionRegistrar -> delegate to it to register additional bean definitions
						Class<?> candidateClass = candidate.loadClass();
						ImportBeanDefinitionRegistrar registrar = BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
						invokeAwareMethods(registrar);
						configClass.addImportBeanDefinitionRegistrar(registrar);
					}
					else {
						// candidate class not an ImportSelector or ImportBeanDefinitionRegistrar -> process it as a @Configuration class
						this.importStack.registerImport(importingClassMetadata.getClassName(),
								candidate.getMetadata().getClassName());
						processConfigurationClass(candidate.asConfigClass(configClass));
					}
				}
			}
			catch (ClassNotFoundException ex) {
				throw new NestedIOException("Failed to load import candidate class", ex);
			}
			finally {
				this.importStack.pop();
			}
		}
	}

	/**
	 * Invoke {@link ResourceLoaderAware}, {@link BeanClassLoaderAware} and
	 * {@link BeanFactoryAware} contracts if implemented by the given {@code bean}.
	 */
	private void invokeAwareMethods(Object importStrategyBean) {
		if (importStrategyBean instanceof Aware) {
			if (importStrategyBean instanceof EnvironmentAware) {
				((EnvironmentAware) importStrategyBean).setEnvironment(this.environment);
			}
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

	ImportRegistry getImportRegistry() {
		return this.importStack;
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a {@link ConfigurationClass}.
	 */
	public SourceClass asSourceClass(ConfigurationClass configurationClass) throws IOException {
		try {
			AnnotationMetadata metadata = configurationClass.getMetadata();
			if (metadata instanceof StandardAnnotationMetadata) {
				return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
			}
			return asSourceClass(configurationClass.getMetadata().getClassName());
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a {@link Class}.
	 */
	public SourceClass asSourceClass(Class<?> classType) throws IOException, ClassNotFoundException {
		try {
			// Sanity test that we can read annotations, if not fall back to ASM
			classType.getAnnotations();
		}
		catch (Throwable ex) {
			return asSourceClass(classType.getName());
		}
		return new SourceClass(classType);
	}

	/**
	 * Factory method to obtain {@link SourceClass}s from class names.
	 */
	public Collection<SourceClass> asSourceClasses(String[] classNames) throws IOException, ClassNotFoundException {
		List<SourceClass> annotatedClasses = new ArrayList<SourceClass>();
		for (String className : classNames) {
			annotatedClasses.add(asSourceClass(className));
		}
		return annotatedClasses;
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a class name.
	 */
	public SourceClass asSourceClass(String className) throws IOException, ClassNotFoundException {
		if (className.startsWith("java")) {
			// Never use ASM for core java types
			return new SourceClass(this.resourceLoader.getClassLoader().loadClass( className));
		}
		return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
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

		@Override
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
				@Override
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


	private static class DeferredImportSelectorHolder {

		private final ConfigurationClass configurationClass;

		private final DeferredImportSelector importSelector;

		public DeferredImportSelectorHolder(ConfigurationClass configurationClass, DeferredImportSelector importSelector) {
			this.configurationClass = configurationClass;
			this.importSelector = importSelector;
		}

		public ConfigurationClass getConfigurationClass() {
			return this.configurationClass;
		}

		public DeferredImportSelector getImportSelector() {
			return this.importSelector;
		}
	}


	/**
	 * Simple wrapper that allows annotated source classes to be dealt with in a uniform
	 * manor, regardless of how they are loaded.
	 */
	private class SourceClass {

		private final Object source; // Class or MetaDataReader

		private final AnnotationMetadata metadata;

		public SourceClass(Object source) {
			this.source = source;
			if (source instanceof Class<?>) {
				this.metadata = new StandardAnnotationMetadata((Class<?>) source, true);
			}
			else {
				this.metadata = ((MetadataReader) source).getAnnotationMetadata();
			}
		}

		public final AnnotationMetadata getMetadata() {
			return this.metadata;
		}

		public Class<?> loadClass() throws ClassNotFoundException {
			if (this.source instanceof Class<?>) {
				return (Class<?>) this.source;
			}
			String className = ((MetadataReader) source).getClassMetadata().getClassName();
			return resourceLoader.getClassLoader().loadClass(className);
		}

		public boolean isAssignable(Class<?> clazz) throws IOException {
			if (this.source instanceof Class) {
				return clazz.isAssignableFrom((Class) this.source);
			}
			return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
		}

		public ConfigurationClass asConfigClass(ConfigurationClass importedBy) throws IOException {
			if (this.source instanceof Class<?>) {
				return new ConfigurationClass((Class<?>) this.source, importedBy);
			}
			return new ConfigurationClass((MetadataReader) this.source, importedBy);
		}

		public Collection<SourceClass> getMemberClasses() throws IOException {
			List<SourceClass> members = new ArrayList<SourceClass>();
			if (this.source instanceof Class<?>) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Class<?> declaredClass : sourceClass.getDeclaredClasses()) {
					try {
						members.add(asSourceClass(declaredClass));
					}
					catch (ClassNotFoundException ex) {
						// ignore
					}
				}
			}
			else {
				MetadataReader sourceReader = (MetadataReader) source;
				for (String memberClassName : sourceReader.getClassMetadata().getMemberClassNames()) {
					try {
						members.add(asSourceClass(memberClassName));
					}
					catch (ClassNotFoundException ex) {
						// ignore
					}
				}
			}
			return members;
		}

		public SourceClass getSuperClass() throws IOException {
			if (!getMetadata().hasSuperClass()) {
				return null;
			}
			try {
				if (this.source instanceof Class<?>) {
					return asSourceClass(((Class<?>) this.source).getSuperclass());
				}
				return asSourceClass(((MetadataReader) this.source).getClassMetadata().getSuperClassName());
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(ex);
			}
		}

		public Set<SourceClass> getAnnotations() throws IOException, ClassNotFoundException {
			Set<SourceClass> annotations = new LinkedHashSet<SourceClass>();
			for (String annotation : this.metadata.getAnnotationTypes()) {
				annotations.add(getRelated(annotation));
			}
			return annotations;
		}

		public Collection<SourceClass> getAnnotationAttributes(String annotationType, String attribute)
				throws IOException, ClassNotFoundException {

			Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annotationType, true);
			if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
				return Collections.emptySet();
			}
			String[] classNames = (String[]) annotationAttributes.get(attribute);
			Set<SourceClass> rtn = new LinkedHashSet<SourceClass>();
			for (String className : classNames) {
				rtn.add(getRelated(className));
			}
			return rtn;
		}

		private SourceClass getRelated(String className) throws IOException, ClassNotFoundException {
			if (this.source instanceof Class<?>) {
				try {
					Class<?> clazz = resourceLoader.getClassLoader().loadClass(className);
					return asSourceClass(clazz);
				}
				catch (ClassNotFoundException ex) {
					// ignore
				}
			}
			return asSourceClass(className);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof SourceClass &&
					this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
		}

		@Override
		public int hashCode() {
			return this.metadata.getClassName().hashCode();
		}

		@Override
		public String toString() {
			return this.metadata.getClassName();
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

}
