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
package org.springframework.config.java.support;

import static org.springframework.config.java.support.MutableAnnotationUtils.*;
import static org.springframework.config.java.support.Util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.springframework.asm.AnnotationVisitor;


class MutableAnnotationArrayVisitor extends AnnotationAdapter {

	private final ArrayList<Object> values = new ArrayList<Object>();
	private final MutableAnnotation mutableAnno;
	private final String attribName;

	private final ClassLoader classLoader;

	public MutableAnnotationArrayVisitor(MutableAnnotation mutableAnno, String attribName, ClassLoader classLoader) {
		super(AsmUtils.EMPTY_VISITOR);

		this.mutableAnno = mutableAnno;
		this.attribName = attribName;
		this.classLoader = classLoader;
	}

	@Override
	public void visit(String na, Object value) {
		values.add(value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String na, String annoTypeDesc) {
		String annoTypeName = AsmUtils.convertTypeDescriptorToClassName(annoTypeDesc);
		Class<? extends Annotation> annoType = loadToolingSafeClass(annoTypeName, classLoader);

		if (annoType == null)
			return super.visitAnnotation(na, annoTypeDesc);

		Annotation anno = createMutableAnnotation(annoType);
		values.add(anno);
		return new MutableAnnotationVisitor(anno, classLoader);
	}

	@Override
	public void visitEnd() {
		Class<?> arrayType = mutableAnno.getAttributeType(attribName);
		Object[] array = (Object[]) Array.newInstance(arrayType.getComponentType(), 0);
		mutableAnno.setAttributeValue(attribName, values.toArray(array));
	}

}
