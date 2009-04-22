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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.Type;
import org.springframework.asm.commons.EmptyVisitor;

/**
 * ASM {@link AnnotationVisitor} that populates a given {@link ConfigurationClassAnnotation} instance
 * with its attributes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassAnnotation
 * @see ConfigurationClassReaderUtils#createMutableAnnotation
 */
class ConfigurationClassAnnotationVisitor implements AnnotationVisitor {

	protected final ConfigurationClassAnnotation mutableAnno;

	private final ClassLoader classLoader;


	/**
	 * Creates a new {@link ConfigurationClassAnnotationVisitor} instance that will populate the the
	 * attributes of the given <var>mutableAnno</var>. Accepts {@link Annotation} instead of
	 * {@link ConfigurationClassAnnotation} to avoid the need for callers to typecast.
	 * @param mutableAnno {@link ConfigurationClassAnnotation} instance to visit and populate
	 * @see ConfigurationClassReaderUtils#createMutableAnnotation
	 */
	public ConfigurationClassAnnotationVisitor(ConfigurationClassAnnotation mutableAnno, ClassLoader classLoader) {
		this.mutableAnno = mutableAnno;
		this.classLoader = classLoader;
	}

	public void visit(String attribName, Object attribValue) {
		Class<?> attribReturnType = mutableAnno.getAttributeType(attribName);
		if (attribReturnType.equals(Class.class)) {
			// the attribute type is Class -> load it and set it.
			String className = ((Type) attribValue).getClassName();
			try {
				Class<?> classVal = classLoader.loadClass(className);
				if (classVal != null) {
					mutableAnno.setAttributeValue(attribName, classVal);
				}
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Cannot resolve attribute type [" + className + "]", ex);
			}
		}
		else {
			// otherwise, assume the value can be set literally
			mutableAnno.setAttributeValue(attribName, attribValue);
		}
	}

	@SuppressWarnings("unchecked")
	public void visitEnum(String attribName, String enumTypeDescriptor, String strEnumValue) {
		String enumTypeName = ConfigurationClassReaderUtils.convertAsmTypeDescriptorToClassName(enumTypeDescriptor);
		try {
			Class<? extends Enum> enumType = (Class<? extends Enum>) classLoader.loadClass(enumTypeName);
			if (enumType == null) {
				return;
			}
			Enum enumValue = Enum.valueOf(enumType, strEnumValue);
			mutableAnno.setAttributeValue(attribName, enumValue);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Cannot resolve enum type [" + enumTypeName + "]", ex);
		}
	}

	public AnnotationVisitor visitAnnotation(String attribName, String annoTypeDesc) {
		Class<? extends Annotation> annoClass = ConfigurationClassReaderUtils.loadAnnotationType(annoTypeDesc, classLoader);
		if (annoClass == null) {
			return new EmptyVisitor();
		}
		ConfigurationClassAnnotation anno = ConfigurationClassReaderUtils.createMutableAnnotation(annoClass, classLoader);
		try {
			Field attribute = mutableAnno.getClass().getField(attribName);
			attribute.set(mutableAnno, anno);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not reflectively set annotation field", ex);
		}
		return new ConfigurationClassAnnotationVisitor(anno, classLoader);
	}

	public AnnotationVisitor visitArray(final String attribName) {
		return new MutableAnnotationArrayVisitor(mutableAnno, attribName, classLoader);
	}

	public void visitEnd() {
	}


	/**
	 * ASM {@link AnnotationVisitor} that visits any annotation array values while populating
	 * a new {@link ConfigurationClassAnnotation} instance.
	 */
	private static class MutableAnnotationArrayVisitor implements AnnotationVisitor {

		private final List<Object> values = new ArrayList<Object>();

		private final ConfigurationClassAnnotation mutableAnno;

		private final String attribName;

		private final ClassLoader classLoader;

		public MutableAnnotationArrayVisitor(ConfigurationClassAnnotation mutableAnno, String attribName, ClassLoader classLoader) {
			this.mutableAnno = mutableAnno;
			this.attribName = attribName;
			this.classLoader = classLoader;
		}

		public void visit(String na, Object value) {
			values.add(value);
		}

		public void visitEnum(String s, String s1, String s2) {
		}

		public AnnotationVisitor visitAnnotation(String na, String annoTypeDesc) {
			Class<? extends Annotation> annoClass = ConfigurationClassReaderUtils.loadAnnotationType(annoTypeDesc, classLoader);
			if (annoClass == null) {
				return new EmptyVisitor();
			}
			ConfigurationClassAnnotation anno = ConfigurationClassReaderUtils.createMutableAnnotation(annoClass, classLoader);
			values.add(anno);
			return new ConfigurationClassAnnotationVisitor(anno, classLoader);
		}

		public AnnotationVisitor visitArray(String s) {
			return new EmptyVisitor();
		}

		public void visitEnd() {
			Class<?> arrayType = mutableAnno.getAttributeType(attribName);
			Object[] array = (Object[]) Array.newInstance(arrayType.getComponentType(), 0);
			mutableAnno.setAttributeValue(attribName, values.toArray(array));
		}
	}

}
