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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.Attribute;
import org.springframework.asm.ClassAdapter;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodAdapter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.asm.commons.EmptyVisitor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.classreading.SimpleMetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * ASM {@link ClassVisitor} that visits a {@link Configuration} class, populating a
 * {@link ConfigurationClass} instance with information gleaned along the way.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassParser
 * @see ConfigurationClass
 */
class ConfigurationClassVisitor implements ClassVisitor {

	private static final String OBJECT_DESC = ClassUtils.convertClassNameToResourcePath(Object.class.getName());

	private final ConfigurationClass configClass;

	private final Set<ConfigurationClass> model;

	private final ProblemReporter problemReporter;

	private final SimpleMetadataReaderFactory metadataReaderFactory;

	private final Stack<ConfigurationClass> importStack;


	public ConfigurationClassVisitor(ConfigurationClass configClass, Set<ConfigurationClass> model,
			ProblemReporter problemReporter, SimpleMetadataReaderFactory metadataReaderFactory) {

		this.configClass = configClass;
		this.model = model;
		this.problemReporter = problemReporter;
		this.metadataReaderFactory = metadataReaderFactory;
		this.importStack = new ImportStack();
	}

	private ConfigurationClassVisitor(ConfigurationClass configClass, Set<ConfigurationClass> model,
			ProblemReporter problemReporter, SimpleMetadataReaderFactory metadataReaderFactory,
			Stack<ConfigurationClass> importStack) {
		this.configClass = configClass;
		this.model = model;
		this.problemReporter = problemReporter;
		this.metadataReaderFactory = metadataReaderFactory;
		this.importStack = importStack;
	}


	public void visit(int classVersion, int modifiers, String classTypeDesc, String arg3, String superTypeDesc, String[] arg5) {
		visitSuperType(superTypeDesc);

		configClass.setName(ClassUtils.convertResourcePathToClassName(classTypeDesc));

		// ASM always adds ACC_SUPER to the opcodes/modifiers for class definitions.
		// Unknown as to why (JavaDoc is silent on the matter), but it should be
		// eliminated in order to comply with java.lang.reflect.Modifier values.
		configClass.setModifiers(modifiers - Opcodes.ACC_SUPER);
	}

	private void visitSuperType(String superTypeDesc) {
		// traverse up the type hierarchy unless the next ancestor is java.lang.Object
		if (OBJECT_DESC.equals(superTypeDesc)) {
			return;
		}
		ConfigurationClassVisitor visitor = new ConfigurationClassVisitor(configClass, model, problemReporter, metadataReaderFactory, importStack);
		String superClassName = ClassUtils.convertResourcePathToClassName(superTypeDesc);
		try {
			SimpleMetadataReader reader = this.metadataReaderFactory.getMetadataReader(superClassName);
			reader.getClassReader().accept(visitor, false);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Failed to load bean super class [" + superClassName + "]", ex);
		}
	}

	public void visitSource(String sourceFile, String debug) {
		String resourcePath =
			ClassUtils.convertClassNameToResourcePath(configClass.getName())
				.substring(0, configClass.getName().lastIndexOf('.') + 1).concat(sourceFile);

		configClass.setSource(resourcePath);
	}

	public void visitOuterClass(String s, String s1, String s2) {
	}

	/**
	 * Visits a class level annotation on a {@link Configuration @Configuration} class.
	 * <p>Upon encountering such an annotation, updates the {@link #configClass} model
	 * object appropriately, and then returns an {@link AnnotationVisitor} implementation
	 * that can populate the annotation appropriately with its attribute data as parsed
	 * by ASM.
	 * @see ConfigurationClassAnnotation
	 * @see Configuration
	 * @see Lazy
	 * @see Import
	 */
	@SuppressWarnings("unchecked")
	public AnnotationVisitor visitAnnotation(String annoTypeDesc, boolean visible) {
		ClassLoader classLoader = metadataReaderFactory.getResourceLoader().getClassLoader();
		Class<? extends Annotation> annoClass = ConfigurationClassReaderUtils.loadAnnotationType(annoTypeDesc, classLoader);
		if (annoClass == null) {
			// annotation was unable to be loaded -> probably Spring IDE unable to load a user-defined annotation
			return new EmptyVisitor();
		}
		if (Import.class.equals(annoClass)) {
			if (!importStack.contains(configClass)) {
				importStack.push(configClass);
				return new ImportAnnotationVisitor(model, problemReporter, classLoader);
			}
			problemReporter.error(new CircularImportProblem(configClass, importStack));
		}

		ConfigurationClassAnnotation mutableAnnotation = ConfigurationClassReaderUtils.createMutableAnnotation(annoClass, classLoader);
		configClass.addAnnotation(mutableAnnotation);
		return new ConfigurationClassAnnotationVisitor(mutableAnnotation, classLoader);
	}

	public void visitAttribute(Attribute attribute) {
	}

	public void visitInnerClass(String s, String s1, String s2, int i) {
	}

	public FieldVisitor visitField(int i, String s, String s1, String s2, Object o) {
		return new EmptyVisitor();
	}

	/**
	 * Delegates all {@link Configuration @Configuration} class method parsing to
	 * {@link ConfigurationClassMethodVisitor}.
	 */
	public MethodVisitor visitMethod(int modifiers, String methodName, String methodDescriptor, String arg3, String[] arg4) {
		ClassLoader classLoader = metadataReaderFactory.getResourceLoader().getClassLoader();
		return new ConfigurationClassMethodVisitor(configClass, methodName, methodDescriptor, modifiers, classLoader);
	}

	public void visitEnd() {
	}


	/**
	 * ASM {@link MethodVisitor} that visits a single method declared in a given
	 * {@link Configuration} class. Determines whether the method is a {@link Bean}
	 * method and if so, adds it to the {@link ConfigurationClass}.
	 */
	private class ConfigurationClassMethodVisitor extends MethodAdapter {

		private final ConfigurationClass configClass;
		private final String methodName;
		private final int modifiers;
		private final ConfigurationClassMethod.ReturnType returnType;
		private final List<Annotation> annotations = new ArrayList<Annotation>();
		private final ClassLoader classLoader;

		private int lineNumber;

		/**
		 * Create a new {@link ConfigurationClassMethodVisitor} instance.
		 * @param configClass model object to which this method will be added
		 * @param methodName name of the method declared in the {@link Configuration} class
		 * @param methodDescriptor ASM representation of the method signature
		 * @param modifiers modifiers for this method
		 */
		public ConfigurationClassMethodVisitor(ConfigurationClass configClass, String methodName,
				String methodDescriptor, int modifiers, ClassLoader classLoader) {
			super(new EmptyVisitor());
			this.configClass = configClass;
			this.methodName = methodName;
			this.classLoader = classLoader;
			this.modifiers = modifiers;
			this.returnType = initReturnTypeFromMethodDescriptor(methodDescriptor);
		}

		/**
		 * Visits a single annotation on this method. Will be called once for each annotation
		 * present (regardless of its RetentionPolicy).
		 */
		@Override
		@SuppressWarnings("unchecked")
		public AnnotationVisitor visitAnnotation(String annoTypeDesc, boolean visible) {
			Class<? extends Annotation> annoClass = ConfigurationClassReaderUtils.loadAnnotationType(annoTypeDesc, classLoader);
			if (annoClass == null) {
				return super.visitAnnotation(annoTypeDesc, visible);
			}
			ConfigurationClassAnnotation annotation = ConfigurationClassReaderUtils.createMutableAnnotation(annoClass, classLoader);
			annotations.add(annotation);
			return new ConfigurationClassAnnotationVisitor(annotation, classLoader);
		}

		/**
		 * Provides the line number of this method within its declaring class. In reality, this
		 * number is always inaccurate - <var>lineNo</var> represents the line number of the
		 * first instruction in this method. Method declaration line numbers are not in any way
		 * tracked in the bytecode. Any tooling or output that reads this value will have to
		 * compensate and estimate where the actual method declaration is.
		 */
		@Override
		public void visitLineNumber(int lineNo, Label start) {
			this.lineNumber = lineNo;
		}

		/**
		 * Parses through all {@link #annotations} on this method in order to determine whether
		 * it is a {@link Bean} method and if so adds it to the enclosing {@link #configClass}.
		 */
		@Override
		public void visitEnd() {
			for (Annotation anno : annotations) {
				if (Bean.class.equals(anno.annotationType())) {
					// this method is annotated with @Bean -> add it to the ConfigurationClass model
					Annotation[] annoArray = annotations.toArray(new Annotation[annotations.size()]);
					ConfigurationClassMethod method = new ConfigurationClassMethod(methodName, modifiers, returnType, annoArray);
					method.setSource(lineNumber);
					configClass.addMethod(method);
					break;
				}
			}
		}

		/**
		 * Determines return type from ASM <var>methodDescriptor</var> and determines whether
		 * that type is an interface.
		 */
		private ConfigurationClassMethod.ReturnType initReturnTypeFromMethodDescriptor(String methodDescriptor) {
			final ConfigurationClassMethod.ReturnType returnType = new ConfigurationClassMethod.ReturnType(ConfigurationClassReaderUtils.getReturnTypeFromAsmMethodDescriptor(methodDescriptor));
			// detect whether the return type is an interface
			try {
				metadataReaderFactory.getMetadataReader(returnType.getName()).getClassReader().accept(
						new ClassAdapter(new EmptyVisitor()) {
							@Override
							public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
								returnType.setInterface((arg1 & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE);
							}
						}, false);
				return returnType;
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException("Failed to load bean return type [" + returnType.getName() + "]", ex);
			}
		}
	}


	/**
	 * ASM {@link AnnotationVisitor} implementation that reads an {@link Import} annotation
	 * for all its specified classes and then one by one processes each class with a new
	 * {@link ConfigurationClassVisitor}.
	 */
	private class ImportAnnotationVisitor implements AnnotationVisitor{

		private final Set<ConfigurationClass> model;

		private final ProblemReporter problemReporter;

		private final List<String> classesToImport = new ArrayList<String>();

		public ImportAnnotationVisitor(
				Set<ConfigurationClass> model, ProblemReporter problemReporter, ClassLoader classLoader) {

			this.model = model;
			this.problemReporter = problemReporter;
		}

		public void visit(String s, Object o) {
		}

		public void visitEnum(String s, String s1, String s2) {
		}

		public AnnotationVisitor visitAnnotation(String s, String s1) {
			return null;
		}

		public AnnotationVisitor visitArray(String attribName) {
			Assert.isTrue("value".equals(attribName));
			return new AnnotationVisitor() {
				public void visit(String na, Object type) {
					Assert.isInstanceOf(Type.class, type);
					classesToImport.add(((Type) type).getClassName());
				}
				public void visitEnum(String s, String s1, String s2) {
				}
				public AnnotationVisitor visitAnnotation(String s, String s1) {
					return new EmptyVisitor();
				}
				public AnnotationVisitor visitArray(String s) {
					return new EmptyVisitor();
				}
				public void visitEnd() {
				}
			};
		}

		public void visitEnd() {
			for (String classToImport : classesToImport) {
				processClassToImport(classToImport);
			}
			importStack.pop();
		}

		private void processClassToImport(String classToImport) {
			ConfigurationClass configClass = new ConfigurationClass();
			try {
				ClassReader reader = metadataReaderFactory.getMetadataReader(classToImport).getClassReader();
				reader.accept(new ConfigurationClassVisitor(configClass, model, problemReporter, metadataReaderFactory, importStack), false);
				if (configClass.getAnnotation(Configuration.class) == null) {
					problemReporter.error(new NonAnnotatedConfigurationProblem(configClass.getName(), configClass.getLocation()));
				}
				else {
					model.add(configClass);
				}
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException("Failed to load imported configuration class [" + classToImport + "]", ex);
			}
		}
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
			if (!(elem instanceof ConfigurationClass)) {
				return false;
			}
			ConfigurationClass configClass = (ConfigurationClass) elem;
			Comparator<ConfigurationClass> comparator = new Comparator<ConfigurationClass>() {
				public int compare(ConfigurationClass first, ConfigurationClass second) {
					return first.getName().equals(second.getName()) ? 0 : 1;
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
		public synchronized String toString() {
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


	/** Configuration classes must be annotated with {@link Configuration @Configuration}. */
	private static class NonAnnotatedConfigurationProblem extends Problem {

		public NonAnnotatedConfigurationProblem(String className, Location location) {
			super(String.format("%s was imported as a @Configuration class but was not actually annotated " +
					"with @Configuration. Annotate the class or do not attempt to process it.", className), location);
		}

	}


	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 * @see Import
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Stack<ConfigurationClass> importStack) {
			super(String.format("A circular @Import has been detected: " +
			             "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
			             "already present in the current import stack [%s]",
			             importStack.peek().getSimpleName(), attemptedImport.getSimpleName(),
			             attemptedImport.getSimpleName(), importStack),
			      new Location(new ClassPathResource(
						  ClassUtils.convertClassNameToResourcePath(importStack.peek().getName())),
						  importStack.peek().getSource())
			);
		}
	}

}
