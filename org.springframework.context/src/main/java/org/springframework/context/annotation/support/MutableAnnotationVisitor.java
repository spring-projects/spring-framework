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

import static org.springframework.context.annotation.support.MutableAnnotationUtils.*;
import static org.springframework.context.annotation.support.Util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.Type;
import org.springframework.util.Assert;


/**
 * ASM {@link AnnotationVisitor} that populates a given {@link MutableAnnotation} instance
 * with its attributes.
 *
 * @author Chris Beams
 * @see MutableAnnotation
 * @see MutableAnnotationInvocationHandler
 * @see MutableAnnotationUtils
 */
class MutableAnnotationVisitor implements AnnotationVisitor {

	protected final MutableAnnotation mutableAnno;

	private final ClassLoader classLoader;

	/**
	 * Creates a new {@link MutableAnnotationVisitor} instance that will populate the the
	 * attributes of the given <var>mutableAnno</var>. Accepts {@link Annotation} instead of
	 * {@link MutableAnnotation} to avoid the need for callers to typecast.
	 * 
	 * @param mutableAnno {@link MutableAnnotation} instance to visit and populate
	 * 
	 * @throws IllegalArgumentException if <var>mutableAnno</var> is not of type
	 *         {@link MutableAnnotation}
	 * 
	 * @see MutableAnnotationUtils#createMutableAnnotation(Class)
	 */
	public MutableAnnotationVisitor(Annotation mutableAnno, ClassLoader classLoader) {
		Assert.isInstanceOf(MutableAnnotation.class, mutableAnno, "annotation must be mutable");
		this.mutableAnno = (MutableAnnotation) mutableAnno;
		this.classLoader = classLoader;
	}

	public AnnotationVisitor visitArray(final String attribName) {
		return new MutableAnnotationArrayVisitor(mutableAnno, attribName, classLoader);
	}

	public void visit(String attribName, Object attribValue) {
		Class<?> attribReturnType = mutableAnno.getAttributeType(attribName);

		if (attribReturnType.equals(Class.class)) {
			// the attribute type is Class -> load it and set it.
			String fqClassName = ((Type) attribValue).getClassName();

			Class<?> classVal = loadToolingSafeClass(fqClassName, classLoader);

			if (classVal == null)
				return;

			mutableAnno.setAttributeValue(attribName, classVal);
			return;
		}

		// otherwise, assume the value can be set literally
		mutableAnno.setAttributeValue(attribName, attribValue);
	}

	@SuppressWarnings("unchecked")
	public void visitEnum(String attribName, String enumTypeDescriptor, String strEnumValue) {
		String enumClassName = AsmUtils.convertTypeDescriptorToClassName(enumTypeDescriptor);

		Class<? extends Enum> enumClass = loadToolingSafeClass(enumClassName, classLoader);

		if (enumClass == null)
			return;

		Enum enumValue = Enum.valueOf(enumClass, strEnumValue);
		mutableAnno.setAttributeValue(attribName, enumValue);
	}

	public AnnotationVisitor visitAnnotation(String attribName, String attribAnnoTypeDesc) {
		String annoTypeName = AsmUtils.convertTypeDescriptorToClassName(attribAnnoTypeDesc);
		Class<? extends Annotation> annoType = loadToolingSafeClass(annoTypeName, classLoader);

		if (annoType == null)
			return AsmUtils.EMPTY_VISITOR.visitAnnotation(attribName, attribAnnoTypeDesc);

		Annotation anno = createMutableAnnotation(annoType);

		try {
			Field attribute = mutableAnno.getClass().getField(attribName);
			attribute.set(mutableAnno, anno);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return new MutableAnnotationVisitor(anno, classLoader);
	}

	public void visitEnd() {
	}

}
