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

package org.springframework.context.annotation.support;

import static java.lang.String.*;
import static org.springframework.context.annotation.support.AsmUtils.*;
import static org.springframework.util.ClassUtils.*;

import java.util.HashMap;
import java.util.Stack;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassAdapter;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;


/**
 * ASM {@link ClassVisitor} that visits a {@link Configuration} class, populating a
 * {@link ConfigurationClass} instance with information gleaned along the way.
 * 
 * @author Chris Beams
 * @see ConfigurationParser
 * @see ConfigurationClass
 */
class ConfigurationClassVisitor extends ClassAdapter {

	private static final String OBJECT_DESC = convertClassNameToResourcePath(Object.class.getName());

	private final ConfigurationClass configClass;
	private final ConfigurationModel model;
	private final ProblemReporter problemReporter;
	private final ClassLoader classLoader;

	private final HashMap<String, ConfigurationClass> innerClasses = new HashMap<String, ConfigurationClass>();

	private boolean processInnerClasses = true;

	public ConfigurationClassVisitor(ConfigurationClass configClass, ConfigurationModel model,
	                                 ProblemReporter problemReporter, ClassLoader classLoader) {
		super(ASM_EMPTY_VISITOR);
		this.configClass = configClass;
		this.model = model;
		this.problemReporter = problemReporter;
		this.classLoader = classLoader;
	}

	public void setProcessInnerClasses(boolean processInnerClasses) {
		this.processInnerClasses = processInnerClasses;
	}

	@Override
	public void visitSource(String sourceFile, String debug) {
		String resourcePath =
			convertClassNameToResourcePath(configClass.getName())
				.substring(0, configClass.getName().lastIndexOf('.') + 1).concat(sourceFile);

		configClass.setSource(resourcePath);
	}

	@Override
	public void visit(int classVersion, int modifiers, String classTypeDesc, String arg3,
	                  String superTypeDesc, String[] arg5) {
		visitSuperType(superTypeDesc);

		configClass.setName(convertResourcePathToClassName(classTypeDesc));

		// ASM always adds ACC_SUPER to the opcodes/modifiers for class definitions.
		// Unknown as to why (JavaDoc is silent on the matter), but it should be
		// eliminated in order to comply with java.lang.reflect.Modifier values.
		configClass.setModifiers(modifiers - Opcodes.ACC_SUPER);
	}

	private void visitSuperType(String superTypeDesc) {
		// traverse up the type hierarchy unless the next ancestor is java.lang.Object
		if (OBJECT_DESC.equals(superTypeDesc))
			return;

		ConfigurationClassVisitor visitor =
			new ConfigurationClassVisitor(configClass, model, problemReporter, classLoader);

		ClassReader reader = newAsmClassReader(superTypeDesc, classLoader);
		reader.accept(visitor, false);
	}

	/**
	 * Visits a class level annotation on a {@link Configuration @Configuration} class.
	 * Accounts for all possible class-level annotations that are respected by JavaConfig
	 * including AspectJ's {@code @Aspect} annotation.
	 * <p>
	 * Upon encountering such an annotation, update the {@link #configClass} model object
	 * appropriately, and then return an {@link AnnotationVisitor} implementation that can
	 * populate the annotation appropriately with data.
	 * 
	 * @see MutableAnnotation
	 */
	@Override
	public AnnotationVisitor visitAnnotation(String annoTypeDesc, boolean visible) {
		String annoTypeName = convertAsmTypeDescriptorToClassName(annoTypeDesc);

		if (Configuration.class.getName().equals(annoTypeName)) {
			Configuration mutableConfiguration = createMutableAnnotation(Configuration.class, classLoader);
			configClass.setConfigurationAnnotation(mutableConfiguration);
			return new MutableAnnotationVisitor(mutableConfiguration, classLoader);
		}

		if (Import.class.getName().equals(annoTypeName)) {
			ImportStack importStack = ImportStackHolder.getImportStack();

			if (!importStack.contains(configClass)) {
				importStack.push(configClass);
				return new ImportAnnotationVisitor(model, problemReporter, classLoader);
			}

			problemReporter.error(new CircularImportProblem(configClass, importStack));
		}

		return super.visitAnnotation(annoTypeDesc, visible);
	}

	/**
	 * Delegates all {@link Configuration @Configuration} class method parsing to
	 * {@link ConfigurationClassMethodVisitor}.
	 */
	@Override
	public MethodVisitor visitMethod(int modifiers, String methodName, String methodDescriptor,
	                                 String arg3, String[] arg4) {

		return new ConfigurationClassMethodVisitor(configClass, methodName, methodDescriptor, modifiers, classLoader);
	}

	/**
	 * Implementation deals with inner classes here even though it would have been more
	 * intuitive to deal with outer classes. Due to limitations in ASM (resulting from
	 * limitations in the VM spec) we cannot directly look for outer classes in all cases,
	 * so instead build up a model of {@link #innerClasses} and process declaring class
	 * logic in a kind of inverted manner.
	 */
	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (processInnerClasses == false)
			return;

		String innerClassName = convertResourcePathToClassName(name);
		String configClassName = configClass.getName();

		// if the innerClassName is equal to configClassName, we just
		// ran into the outermost inner class look up the outer class
		// associated with this
		if (innerClassName.equals(configClassName)) {
			if (innerClasses.containsKey(outerName)) {
				configClass.setDeclaringClass(innerClasses.get(outerName));
			}
			return;
		}

		ConfigurationClass innerConfigClass = new ConfigurationClass();

		ConfigurationClassVisitor ccVisitor =
			new ConfigurationClassVisitor(innerConfigClass, new ConfigurationModel(), problemReporter, classLoader);
		ccVisitor.setProcessInnerClasses(false);

		ClassReader reader = newAsmClassReader(name, classLoader);
		reader.accept(ccVisitor, false);

		if (innerClasses.containsKey(outerName))
			innerConfigClass.setDeclaringClass(innerClasses.get(outerName));

		// is the inner class a @Configuration class? If so, add it to the list
		if (innerConfigClass.getConfigurationAnnotation() != null)
			innerClasses.put(name, innerConfigClass);
	}


	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 * 
	 * @see Import
	 * @see ImportStack
	 * @see ImportStackHolder
	 */
	class CircularImportProblem extends Problem {

		CircularImportProblem(ConfigurationClass attemptedImport, Stack<ConfigurationClass> importStack) {
			super(format("A circular @Import has been detected: " +
			             "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
			             "already present in the current import stack [%s]",
			             importStack.peek().getSimpleName(), attemptedImport.getSimpleName(),
			             attemptedImport.getSimpleName(), importStack),
			      new Location(new ClassPathResource(convertClassNameToResourcePath(importStack.peek().getName())),
			                   importStack.peek().getSource())
			);
		}

	}

}
