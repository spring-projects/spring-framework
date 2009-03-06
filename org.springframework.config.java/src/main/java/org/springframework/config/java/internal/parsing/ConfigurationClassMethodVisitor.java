/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.config.java.internal.parsing;

import static org.springframework.config.java.Util.*;
import static org.springframework.config.java.internal.parsing.AsmUtils.*;
import static org.springframework.config.java.internal.parsing.MutableAnnotationUtils.*;
import static org.springframework.util.ClassUtils.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;
import org.springframework.config.java.Bean;
import org.springframework.config.java.BeanMethod;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.ConfigurationClass;
import org.springframework.config.java.ModelClass;


/**
 * Visits a single method declared in a given {@link Configuration} class. Determines
 * whether the method is a {@link Bean} method and if so, adds it to the
 * {@link ConfigurationClass}.
 * 
 * @author Chris Beams
 */
class ConfigurationClassMethodVisitor extends MethodAdapter {

	private final ConfigurationClass configClass;
	private final String methodName;
	private final int modifiers;
	private final ModelClass returnType;
	private final ArrayList<Annotation> annotations = new ArrayList<Annotation>();
	private final ClassLoader classLoader;

	private boolean isModelMethod = false;
	private int lineNumber;

	/**
	 * Creates a new {@link ConfigurationClassMethodVisitor} instance.
	 * 
	 * @param configClass model object to which this method will be added
	 * @param methodName name of the method declared in the {@link Configuration} class
	 * @param methodDescriptor ASM representation of the method signature
	 * @param modifiers modifiers for this method
	 */
	public ConfigurationClassMethodVisitor(ConfigurationClass configClass, String methodName,
	        String methodDescriptor, int modifiers, ClassLoader classLoader) {
		super(AsmUtils.EMPTY_VISITOR);

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
	public AnnotationVisitor visitAnnotation(String annoTypeDesc, boolean visible) {
		String annoClassName = AsmUtils.convertTypeDescriptorToClassName(annoTypeDesc);

		Class<? extends Annotation> annoClass = loadToolingSafeClass(annoClassName, classLoader);

		if (annoClass == null)
			return super.visitAnnotation(annoTypeDesc, visible);

		Annotation annotation = createMutableAnnotation(annoClass);

		annotations.add(annotation);

		return new MutableAnnotationVisitor(annotation, classLoader);
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
	 * it is a {@link Bean} method or not and if so adds it to the enclosing {@link #configClass}.
	 */
	@Override
	public void visitEnd() {
		for (Annotation anno : annotations) {
			if (anno.annotationType().equals(Bean.class)) {
				isModelMethod = true;
				break;
			}
		}

		if (!isModelMethod)
			return;

		Annotation[] annoArray = annotations.toArray(new Annotation[] {});
		BeanMethod method = new BeanMethod(methodName, modifiers, returnType, annoArray);
		method.setLineNumber(lineNumber);
		configClass.addMethod(method);
	}

	/**
	 * Determines return type from ASM <var>methodDescriptor</var> and determines whether
	 * that type is an interface.
	 */
	private ModelClass initReturnTypeFromMethodDescriptor(String methodDescriptor) {
		final ModelClass returnType = new ModelClass(getReturnTypeFromMethodDescriptor(methodDescriptor));

		// detect whether the return type is an interface
		newClassReader(convertClassNameToResourcePath(returnType.getName()), classLoader).accept(
		        new ClassAdapter(AsmUtils.EMPTY_VISITOR) {
			        @Override
			        public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
				        returnType.setInterface((arg1 & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE);
			        }
		        }, false);

		return returnType;
	}
}
