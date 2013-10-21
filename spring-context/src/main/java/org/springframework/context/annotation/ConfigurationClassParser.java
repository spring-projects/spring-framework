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
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.NestedIOException;
import org.springframework.core.annotation.AnnotationAttributes;
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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.context.annotation.MetadataUtils.*;

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
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
class ConfigurationClassParser {

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
		this.componentScanParser = new ComponentScanAnnotationParser(
				resourceLoader, environment, componentScanBeanNameGenerator, registry);
	}


	/**
	 * Parse the specified {@link Configuration @Configuration} class.
	 * @param className the name of the class to parse
	 * @param beanName may be null, but if populated represents the bean id
	 * (assumes that this configuration class was configured via XML)
	 */
	public void parse(String className, String beanName) throws IOException {
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
		processConfigurationClass(new ConfigurationClass(reader, beanName));
	}

	/**
	 * Parse the specified {@link Configuration @Configuration} class.
	 * @param clazz the Class to parse
	 * @param beanName must not be null (as of Spring 3.1.1)
	 */
	public void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}

	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		AnnotationMetadata metadata = configClass.getMetadata();
		if (this.environment != null && metadata.isAnnotated(Profile.class.getName())) {
			AnnotationAttributes profile = MetadataUtils.attributesFor(metadata, Profile.class);
			if (!this.environment.acceptsProfiles(profile.getStringArray("value"))) {
				return;
			}
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
		do {
			metadata = doProcessConfigurationClass(configClass, metadata);
		}
		while (metadata != null);

		this.configurationClasses.add(configClass);
	}

	/**
	 * @return annotation metadata of superclass, {@code null} if none found or previously processed
	 */
	protected AnnotationMetadata doProcessConfigurationClass(
			ConfigurationClass configClass, AnnotationMetadata metadata) throws IOException {

		// recursively process any member (nested) classes first
		processMemberClasses(metadata);

		// process any @PropertySource annotations
		AnnotationAttributes propertySource = attributesFor(metadata, org.springframework.context.annotation.PropertySource.class);
		if (propertySource != null) {
			processPropertySource(propertySource);
		}

		// process any @ComponentScan annotations
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
		Set<Object> imports = new LinkedHashSet<Object>();
		Set<Object> visited = new LinkedHashSet<Object>();
		collectImports(metadata, imports, visited);
		if (!imports.isEmpty()) {
			processImport(configClass, imports, true);
		}

		// process any @ImportResource annotations
		if (metadata.isAnnotated(ImportResource.class.getName())) {
			AnnotationAttributes importResource = attributesFor(metadata, ImportResource.class);
			String[] resources = importResource.getStringArray("value");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			for (String resource : resources) {
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				configClass.addImportedResource(resolvedResource, readerClass);
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
			if (!this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
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
	 * Register member (nested) classes that happen to be configuration classes themselves.
	 * @param metadata the metadata representation of the containing class
	 * @throws IOException if there is any problem reading metadata from a member class
	 */
	private void processMemberClasses(AnnotationMetadata metadata) throws IOException {
		if (metadata instanceof StandardAnnotationMetadata) {
			for (Class<?> memberClass : ((StandardAnnotationMetadata) metadata).getIntrospectedClass().getDeclaredClasses()) {
				if (ConfigurationClassUtils.isConfigurationCandidate(new StandardAnnotationMetadata(memberClass))) {
					processConfigurationClass(new ConfigurationClass(memberClass, true));
				}
			}
		}
		else {
			for (String memberClassName : metadata.getMemberClassNames()) {
				MetadataReader reader = this.metadataReaderFactory.getMetadataReader(memberClassName);
				AnnotationMetadata memberClassMetadata = reader.getAnnotationMetadata();
				if (ConfigurationClassUtils.isConfigurationCandidate(memberClassMetadata)) {
					processConfigurationClass(new ConfigurationClass(reader, true));
				}
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
		int locationCount = locations.length;
		if (locationCount == 0) {
			throw new IllegalArgumentException("At least one @PropertySource(value) location is required");
		}
		for (int i = 0; i < locationCount; i++) {
			locations[i] = this.environment.resolveRequiredPlaceholders(locations[i]);
		}
		ClassLoader classLoader = this.resourceLoader.getClassLoader();
		if (!StringUtils.hasText(name)) {
			for (String location : locations) {
				this.propertySources.push(new ResourcePropertySource(location, classLoader));
			}
		}
		else {
			if (locationCount == 1) {
				this.propertySources.push(new ResourcePropertySource(name, locations[0], classLoader));
			}
			else {
				CompositePropertySource ps = new CompositePropertySource(name);
				for (int i = locations.length - 1; i >= 0; i--) {
					ps.addPropertySource(new ResourcePropertySource(locations[i], classLoader));
				}
				this.propertySources.push(ps);
			}
		}
	}

	/**
	 * Recursively collect all declared {@code @Import} values. Unlike most
	 * meta-annotations it is valid to have several {@code @Import}s declared with
	 * different values, the usual process or returning values from the first
	 * meta-annotation on a class is not sufficient.
	 * <p>For example, it is common for a {@code @Configuration} class to declare direct
	 * {@code @Import}s in addition to meta-imports originating from an {@code @Enable}
	 * annotation.
	 * @param metadata the metadata representation of the class to search
	 * @param imports the imports collected so far
	 * @param visited used to track visited classes to prevent infinite recursion
	 * @throws IOException if there is any problem reading metadata from the named class
	 */
	private void collectImports(AnnotationMetadata metadata, Set<Object> imports, Set<Object> visited) throws IOException {
		String className = metadata.getClassName();
		if (visited.add(className)) {
			if (metadata instanceof StandardAnnotationMetadata) {
				StandardAnnotationMetadata stdMetadata = (StandardAnnotationMetadata) metadata;
				for (Annotation ann : stdMetadata.getIntrospectedClass().getAnnotations()) {
					if (!ann.annotationType().getName().startsWith("java") && !(ann instanceof Import)) {
						collectImports(new StandardAnnotationMetadata(ann.annotationType()), imports, visited);
					}
				}
				Map<String, Object> attributes = stdMetadata.getAnnotationAttributes(Import.class.getName(), false);
				if (attributes != null) {
					Class[] value = (Class[]) attributes.get("value");
					if (!ObjectUtils.isEmpty(value)) {
						imports.addAll(Arrays.asList(value));
					}
				}
			}
			else {
				for (String annotationType : metadata.getAnnotationTypes()) {
					if (!className.startsWith("java") && !className.equals(Import.class.getName())) {
						try {
							collectImports(
									new StandardAnnotationMetadata(this.resourceLoader.getClassLoader().loadClass(annotationType)),
									imports, visited);
						}
						catch (ClassNotFoundException ex) {
							//
						}
					}
				}
				Map<String, Object> attributes = metadata.getAnnotationAttributes(Import.class.getName(), true);
				if (attributes != null) {
					String[] value = (String[]) attributes.get("value");
					if (!ObjectUtils.isEmpty(value)) {
						imports.addAll(Arrays.asList(value));
					}
				}
			}
		}
	}

	private void processImport(ConfigurationClass configClass, Collection<?> classesToImport, boolean checkForCircularImports) throws IOException {
		if (checkForCircularImports && this.importStack.contains(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack, configClass.getMetadata()));
		}
		else {
			this.importStack.push(configClass);
			AnnotationMetadata importingClassMetadata = configClass.getMetadata();
			try {
				for (Object candidate : classesToImport) {
					Object candidateToCheck = (candidate instanceof Class ? (Class) candidate :
							this.metadataReaderFactory.getMetadataReader((String) candidate));
					if (checkAssignability(ImportSelector.class, candidateToCheck)) {
						// the candidate class is an ImportSelector -> delegate to it to determine imports
						Class<?> candidateClass = (candidate instanceof Class ? (Class) candidate :
								this.resourceLoader.getClassLoader().loadClass((String) candidate));
						ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
						processImport(configClass, Arrays.asList(selector.selectImports(importingClassMetadata)), false);
					}
					else if (checkAssignability(ImportBeanDefinitionRegistrar.class, candidateToCheck)) {
						// the candidate class is an ImportBeanDefinitionRegistrar -> delegate to it to register additional bean definitions
						Class<?> candidateClass = (candidate instanceof Class ? (Class) candidate :
								this.resourceLoader.getClassLoader().loadClass((String) candidate));
						ImportBeanDefinitionRegistrar registrar = BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
						invokeAwareMethods(registrar);
						registrar.registerBeanDefinitions(importingClassMetadata, this.registry);
					}
					else {
						// candidate class not an ImportSelector or ImportBeanDefinitionRegistrar -> process it as a @Configuration class
						this.importStack.registerImport(importingClassMetadata.getClassName(),
								(candidate instanceof Class ? ((Class) candidate).getName() : (String) candidate));
						processConfigurationClass(candidateToCheck instanceof Class ? new ConfigurationClass((Class) candidateToCheck, true) :
								new ConfigurationClass((MetadataReader) candidateToCheck, true));
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

	private boolean checkAssignability(Class<?> clazz, Object candidate) throws IOException {
		if (candidate instanceof Class) {
			return clazz.isAssignableFrom((Class) candidate);
		}
		else {
			return new AssignableTypeFilter(clazz).match((MetadataReader) candidate, this.metadataReaderFactory);
		}
	}

	/**
	 * Invoke {@link ResourceLoaderAware}, {@link BeanClassLoaderAware} and
	 * {@link BeanFactoryAware} contracts if implemented by the given {@code registrar}.
	 */
	private void invokeAwareMethods(ImportBeanDefinitionRegistrar registrar) {
		if (registrar instanceof Aware) {
			if (registrar instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) registrar).setResourceLoader(this.resourceLoader);
			}
			if (registrar instanceof BeanClassLoaderAware) {
				ClassLoader classLoader = (this.registry instanceof ConfigurableBeanFactory ?
						((ConfigurableBeanFactory) this.registry).getBeanClassLoader() :
						this.resourceLoader.getClassLoader());
				((BeanClassLoaderAware) registrar).setBeanClassLoader(classLoader);
			}
			if (registrar instanceof BeanFactoryAware && this.registry instanceof BeanFactory) {
				((BeanFactoryAware) registrar).setBeanFactory((BeanFactory) this.registry);
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

}
