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

/**
 * Parses a {@link Configuration} class definition, populating a configuration model.
 * This ASM-based implementation avoids reflection and eager classloading in order to
 * interoperate effectively with tooling (Spring IDE) and OSGi environments.
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration class
 * from the concern of registering {@link BeanDefinition} objects based on the content of
 * that model.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
class ConfigurationClassParser {

	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final Set<ConfigurationClass> model;

	private final Stack<ConfigurationClass> importStack = new ImportStack();


	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used
	 * to populate a configuration model.
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory, ProblemReporter problemReporter) {
		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.model = new LinkedHashSet<ConfigurationClass>();
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
	 * @param clazz the Clazz to parse
	 * @param beanName may be null, but if populated represents the bean id
	 * (assumes that this configuration class was configured via XML)
	 */
	public void parse(Class clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}


	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		AnnotationMetadata metadata = configClass.getMetadata();
		while (metadata != null) {
			doProcessConfigurationClass(configClass, metadata);
			String superClassName = metadata.getSuperClassName();
			if (superClassName != null && !Object.class.getName().equals(superClassName)) {
				if (metadata instanceof StandardAnnotationMetadata) {
					Class clazz = ((StandardAnnotationMetadata) metadata).getIntrospectedClass();
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
		model.add(configClass);
	}

	protected void doProcessConfigurationClass(ConfigurationClass configClass, AnnotationMetadata metadata) throws IOException {
		if (metadata.hasAnnotation(Import.class.getName())) {
			processImport(configClass, (String[]) metadata.getAnnotationAttributes(Import.class.getName()).get("value"));
		}
		Set<MethodMetadata> methods = metadata.getAnnotatedMethods(Bean.class.getName());
		for (MethodMetadata methodMetadata : methods) {
			configClass.addMethod(new ConfigurationClassMethod(methodMetadata, configClass));
		}
	}

	public void processImport(ConfigurationClass configClass, String[] classesToImport) throws IOException {
		if (this.importStack.contains(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
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
		if (!metadata.hasAnnotation(Configuration.class.getName())) {
			this.problemReporter.error(
					new NonAnnotatedConfigurationProblem(metadata.getClassName(), reader.getResource()));
		}
		else {
			processConfigurationClass(new ConfigurationClass(reader, null));
		}
	}

	/**
	 * Recurse through the model validating each {@link ConfigurationClass}.
	 * @see ConfigurationClass#validate
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.model) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getModel() {
		return this.model;
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

		public NonAnnotatedConfigurationProblem(String className, Resource resource) {
			super(String.format("%s was imported as a @Configuration class but was not actually annotated " +
					"with @Configuration. Annotate the class or do not attempt to process it.", className),
					new Location(resource));
		}

	}


	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Stack<ConfigurationClass> importStack) {
			super(String.format("A circular @Import has been detected: " +
			             "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
			             "already present in the current import stack [%s]",
			             importStack.peek().getSimpleName(), attemptedImport.getSimpleName(),
			             attemptedImport.getSimpleName(), importStack),
			      new Location(importStack.peek().getResource())
			);
		}
	}

}
