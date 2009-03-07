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
package org.springframework.config.java.internal.enhancement;

import net.sf.cglib.asm.Constants;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;


/**
 * Transforms a class by adding bytecode for a class-level annotation. Checks to ensure that
 * the desired annotation is not already present before adding. Used by
 * {@link ConfigurationEnhancer} to dynamically add an {@link org.aspectj.lang.Aspect}
 * annotation to an enhanced Configuration subclass.
 * 
 * <p>This class was originally adapted from examples the ASM 3.0 documentation.
 * 
 * @author Chris Beams
 */
class AddAnnotationAdapter extends ClassAdapter {
	private String annotationDesc;
	private boolean isAnnotationPresent;

	/**
	 * Creates a new AddAnnotationAdapter instance.
	 * 
	 * @param cv the ClassVisitor delegate
	 * @param annotationDesc name of the annotation to be added (in type descriptor format)
	 */
	public AddAnnotationAdapter(ClassVisitor cv, String annotationDesc) {
		super(cv);
		this.annotationDesc = annotationDesc;
	}

	/**
	 * Ensures that the version of the resulting class is Java 5 or better.
	 */
	@Override
	public void visit(int version, int access, String name, String signature, String superName,
	        String[] interfaces) {
		int v = (version & 0xFF) < Constants.V1_5 ? Constants.V1_5 : version;
		cv.visit(v, access, name, signature, superName, interfaces);
	}

	/**
	 * Checks to ensure that the desired annotation is not already present.
	 */
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (visible && desc.equals(annotationDesc)) {
			isAnnotationPresent = true;
		}
		return cv.visitAnnotation(desc, visible);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		addAnnotation();
		cv.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		addAnnotation();
		return cv.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature,
	        String[] exceptions) {
		addAnnotation();
		return cv.visitMethod(access, name, desc, signature, exceptions);
	}

	/**
	 * Kicks off the process of actually adding the desired annotation.
	 * 
	 * @see #addAnnotation()
	 */
	@Override
	public void visitEnd() {
		addAnnotation();
		cv.visitEnd();
	}

	/**
	 * Actually adds the desired annotation.
	 */
	private void addAnnotation() {
		if (!isAnnotationPresent) {
			AnnotationVisitor av = cv.visitAnnotation(annotationDesc, true);
			if (av != null) {
				av.visitEnd();
			}
			isAnnotationPresent = true;
		}
	}
}
