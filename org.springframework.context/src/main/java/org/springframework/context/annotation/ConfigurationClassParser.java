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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may import
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering {@link BeanDefinition} objects based on the
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

	private final Stack<ConfigurationClass> importStack = new ImportStack();

	private final Set<ConfigurationClass> configurationClasses =
		new LinkedHashSet<ConfigurationClass>();


	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used
	 * to populate the set of configuration classes.
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory, ProblemReporter problemReporter) {
		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
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
	 * @param beanName may be null, but if populated represents the bean id
	 * (assumes that this configuration class was configured via XML)
	 */
	public void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}


	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		AnnotationMetadata metadata = configClass.getMetadata();
		while (metadata != null) {
			doProcessConfigurationClass(configClass, metadata);
			String superClassName = metadata.getSuperClassName();
			if (superClassName != null && !Object.class.getName().equals(superClassName)) {
				if (metadata instanceof StandardAnnotationMetadata) {
					Class<?> clazz = ((StandardAnnotationMetadata) metadata).getIntrospectedClass();
					metadata = new StandardAnnotationMetadata(clazz.getSuperclass());
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
		if (metadata.isAnnotated(Import.class.getName())) {
			processImport(configClass, (String[]) metadata.getAnnotationAttributes(Import.class.getName(), true).get("value"));
		}
		if (metadata.isAnnotated(ImportResource.class.getName())) {
			String[] resources = (String[]) metadata.getAnnotationAttributes(ImportResource.class.getName()).get("value");
			Class readerClass = (Class) metadata.getAnnotationAttributes(ImportResource.class.getName()).get("reader");
			if (readerClass == null) {
				throw new IllegalStateException("No reader class associated with imported resources: " +
						StringUtils.arrayToCommaDelimitedString(resources));
			}
			for (String resource : resources) {
				configClass.addImportedResource(resource, readerClass);
			}
		}
		Set<MethodMetadata> methods = metadata.getAnnotatedMethods(Bean.class.getName());
		for (MethodMetadata methodMetadata : methods) {
			configClass.addMethod(new ConfigurationClassMethod(methodMetadata, configClass));
		}
	}

	private void processImport(ConfigurationClass configClass, String[] classesToImport) throws IOException {
		if (this.importStack.contains(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack, configClass.getMetadata()));
		}
		else {
			this.importStack.push(configClass);
			for (String classToImport : classesToImport) {
				processClassToImport(classToImport);
			}
			this.importStack.pop();
		}
	}

	private void processClassToImport(String classToImport) throws IOException {
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(classToImport);
		AnnotationMetadata metadata = reader.getAnnotationMetadata();
		if (!metadata.isAnnotated(Configuration.class.getName())) {
			this.problemReporter.error(
					new NonAnnotatedConfigurationProblem(metadata.getClassName(), reader.getResource(), metadata));
		}
		else {
			processConfigurationClass(new ConfigurationClass(reader, null));
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


	@SuppressWarnings("serial")
	private static class ImportStack extends Stack<ConfigurationClass> {

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
		 * <ol>
		 * <li>com.acme.Foo</li>
		 * <li>com.acme.Bar</li>
		 * <li>com.acme.Baz</li>
		 * </ol>
		 * Returns "Foo->Bar->Baz". In the case of an empty stack, returns empty string.
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			Iterator<ConfigurationClass> iterator = iterator();
			while (iterator.hasNext()) {
				builder.append(iterator.next().getSimpleName());
				if (iterator.hasNext()) {
					builder.append("->");
				}
			}
			return builder.toString();
		}
	}


	/**
	 * Configuration classes must be annotated with {@link Configuration @Configuration}.
	 */
	private static class NonAnnotatedConfigurationProblem extends Problem {

		public NonAnnotatedConfigurationProblem(String className, Resource resource, AnnotationMetadata metadata) {
			super(String.format("%s was imported as a @Configuration class but was not actually annotated " +
					"with @Configuration. Annotate the class or do not attempt to process it.", className),
					new Location(resource, metadata));
		}

	}


	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Stack<ConfigurationClass> importStack, AnnotationMetadata metadata) {
			super(String.format("A circular @Import has been detected: " +
			             "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
			             "already present in the current import stack [%s]",
			             importStack.peek().getSimpleName(), attemptedImport.getSimpleName(),
			             attemptedImport.getSimpleName(), importStack),
			      new Location(importStack.peek().getResource(), metadata)
			);
		}
	}

}
