/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
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

	private final ImportStack importStack = new ImportStack();

	private final Set<ConfigurationClass> configurationClasses =
		new LinkedHashSet<ConfigurationClass>();

	private final Stack<PropertySource<?>> propertySources =
		new Stack<PropertySource<?>>();

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;

	private final ComponentScanAnnotationParser componentScanParser;


	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used
	 * to populate the set of configuration classes.
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
			ProblemReporter problemReporter, Environment environment,
			ResourceLoader resourceLoader, BeanDefinitionRegistry registry) {
		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.componentScanParser =
			new ComponentScanAnnotationParser(this.resourceLoader, this.environment, this.registry);
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

		while (metadata != null) {
			doProcessConfigurationClass(configClass, metadata);
			String superClassName = metadata.getSuperClassName();
			if (superClassName != null && !Object.class.getName().equals(superClassName)) {
				if (metadata instanceof StandardAnnotationMetadata) {
					Class<?> clazz = ((StandardAnnotationMetadata) metadata).getIntrospectedClass();
					metadata = new StandardAnnotationMetadata(clazz.getSuperclass(), true);
				}
				else {
					MetadataReader reader = this.metadataReaderFactory.getMetadataReader(superClassName);
					metadata = reader.getAnnotationMetadata();
				}
			}
			else {
				metadata = null;
			}
		}
		if (this.configurationClasses.contains(configClass) && configClass.getBeanName() != null) {
			// Explicit bean definition found, probably replacing an import.
			// Let's remove the old one and go with the new one.
			this.configurationClasses.remove(configClass);
		}

		this.configurationClasses.add(configClass);
	}

	protected void doProcessConfigurationClass(ConfigurationClass configClass, AnnotationMetadata metadata) throws IOException {

		// recursively process any member (nested) classes first
		for (String memberClassName : metadata.getMemberClassNames()) {
			MetadataReader reader = this.metadataReaderFactory.getMetadataReader(memberClassName);
			AnnotationMetadata memberClassMetadata = reader.getAnnotationMetadata();
			if (ConfigurationClassUtils.isConfigurationCandidate(memberClassMetadata)) {
				processConfigurationClass(new ConfigurationClass(reader, true));
			}
		}

		// process any @PropertySource annotations
		AnnotationAttributes propertySource =
				attributesFor(metadata, org.springframework.context.annotation.PropertySource.class);
		if (propertySource != null) {
			String name = propertySource.getString("name");
			String[] locations = propertySource.getStringArray("value");
			ClassLoader classLoader = this.resourceLoader.getClassLoader();
			for (String location : locations) {
				location = this.environment.resolveRequiredPlaceholders(location);
				ResourcePropertySource ps = StringUtils.hasText(name) ?
						new ResourcePropertySource(name, location, classLoader) :
						new ResourcePropertySource(location, classLoader);
				this.propertySources.push(ps);
			}
		}

		// process any @ComponentScan annotions
		AnnotationAttributes componentScan = attributesFor(metadata, ComponentScan.class);
		if (componentScan != null) {
			// the config class is annotated with @ComponentScan -> perform the scan immediately
			Set<BeanDefinitionHolder> scannedBeanDefinitions = this.componentScanParser.parse(componentScan);

			// check the set of scanned definitions for any further config classes and parse recursively if necessary
			for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
				if (ConfigurationClassUtils.checkConfigurationClassCandidate(holder.getBeanDefinition(), this.metadataReaderFactory)) {
					this.parse(holder.getBeanDefinition().getBeanClassName(), holder.getBeanName());
				}
			}
		}

		// process any @Import annotations
		List<AnnotationAttributes> imports =
			findAllAnnotationAttributes(Import.class, metadata.getClassName(), true);
		for (AnnotationAttributes importAnno : imports) {
			processImport(configClass, importAnno.getStringArray("value"), true);
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
	}


	/**
	 * Return a list of attribute maps for all declarations of the given annotation
	 * on the given annotated class using the given MetadataReaderFactory to introspect
	 * annotation metadata. Meta-annotations are ordered first in the list, and if the
	 * target annotation is declared directly on the class, its map of attributes will be
	 * ordered last in the list.
	 * @param targetAnnotation the annotation to search for, both locally and as a meta-annotation
	 * @param annotatedClassName the class to inspect
	 * @param classValuesAsString whether class attributes should be returned as strings
	 */
	private List<AnnotationAttributes> findAllAnnotationAttributes(
			Class<? extends Annotation> targetAnnotation, String annotatedClassName,
			boolean classValuesAsString) throws IOException {

		List<AnnotationAttributes> allAttribs = new ArrayList<AnnotationAttributes>();

		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(annotatedClassName);
		AnnotationMetadata metadata = reader.getAnnotationMetadata();
		String targetAnnotationType = targetAnnotation.getName();

		for (String annotationType : metadata.getAnnotationTypes()) {
			if (annotationType.equals(targetAnnotationType)) {
				continue;
			}
			AnnotationMetadata metaAnnotations =
					this.metadataReaderFactory.getMetadataReader(annotationType).getAnnotationMetadata();
			AnnotationAttributes targetAttribs =
					AnnotationAttributes.fromMap(metaAnnotations.getAnnotationAttributes(targetAnnotationType, classValuesAsString));
			if (targetAttribs != null) {
				allAttribs.add(targetAttribs);
			}
		}

		AnnotationAttributes localAttribs =
				AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(targetAnnotationType, classValuesAsString));
		if (localAttribs != null) {
			allAttribs.add(localAttribs);
		}

		return allAttribs;
	}

	private void processImport(ConfigurationClass configClass, String[] classesToImport, boolean checkForCircularImports) throws IOException {
		if (checkForCircularImports && this.importStack.contains(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack, configClass.getMetadata()));
		}
		else {
			this.importStack.push(configClass);
			AnnotationMetadata importingClassMetadata = configClass.getMetadata();
			for (String candidate : classesToImport) {
				MetadataReader reader = this.metadataReaderFactory.getMetadataReader(candidate);
				if (new AssignableTypeFilter(ImportSelector.class).match(reader, metadataReaderFactory)) {
					// the candidate class is an ImportSelector -> delegate to it to determine imports
					try {
						ImportSelector selector = BeanUtils.instantiateClass(Class.forName(candidate), ImportSelector.class);
						processImport(configClass, selector.selectImports(importingClassMetadata), false);
					} catch (ClassNotFoundException ex) {
						throw new IllegalStateException(ex);
					}
				}
				else if (new AssignableTypeFilter(ImportBeanDefinitionRegistrar.class).match(reader, metadataReaderFactory)) {
					// the candidate class is an ImportBeanDefinitionRegistrar -> delegate to it to register additional bean definitions
					try {
						ImportBeanDefinitionRegistrar registrar = BeanUtils.instantiateClass(Class.forName(candidate), ImportBeanDefinitionRegistrar.class);
						registrar.registerBeanDefinitions(importingClassMetadata, registry);
					} catch (ClassNotFoundException ex) {
						throw new IllegalStateException(ex);
					}
				}
				else {
					// the candidate class not an ImportSelector or ImportBeanDefinitionRegistrar -> process it as a @Configuration class
					this.importStack.registerImport(importingClassMetadata.getClassName(), candidate);
					processConfigurationClass(new ConfigurationClass(reader, true));
				}
			}
			this.importStack.pop();
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

		private Map<String, String> imports = new HashMap<String, String>();

		public String getImportingClassFor(String importedClass) {
			return imports.get(importedClass);
		}

		public void registerImport(String importingClass, String importedClass) {
			imports.put(importedClass, importingClass);
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
