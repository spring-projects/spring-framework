/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.lang.reflect.Field;
import java.security.AccessControlException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ReflectionUtils;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1.1
 */
abstract class AbstractRecursiveAnnotationVisitor extends AnnotationVisitor {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final AnnotationAttributes attributes;

	protected final ClassLoader classLoader;


	public AbstractRecursiveAnnotationVisitor(ClassLoader classLoader, AnnotationAttributes attributes) {
		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
		this.attributes = attributes;
	}


	@Override
	public void visit(String attributeName, Object attributeValue) {
		this.attributes.put(attributeName, attributeValue);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String attributeName, String asmTypeDescriptor) {
		String annotationType = Type.getType(asmTypeDescriptor).getClassName();
		AnnotationAttributes nestedAttributes = new AnnotationAttributes(annotationType, this.classLoader);
		this.attributes.put(attributeName, nestedAttributes);
		return new RecursiveAnnotationAttributesVisitor(annotationType, nestedAttributes, this.classLoader);
	}

	@Override
	public AnnotationVisitor visitArray(String attributeName) {
		return new RecursiveAnnotationArrayVisitor(attributeName, this.attributes, this.classLoader);
	}

	@Override
	public void visitEnum(String attributeName, String asmTypeDescriptor, String attributeValue) {
		Object newValue = getEnumValue(asmTypeDescriptor, attributeValue);
		visit(attributeName, newValue);
	}

	protected Object getEnumValue(String asmTypeDescriptor, String attributeValue) {
		Object valueToUse = attributeValue;
		try {
			Class<?> enumType = this.classLoader.loadClass(Type.getType(asmTypeDescriptor).getClassName());
			Field enumConstant = ReflectionUtils.findField(enumType, attributeValue);
			if (enumConstant != null) {
				ReflectionUtils.makeAccessible(enumConstant);
				valueToUse = enumConstant.get(null);
			}
		}
		catch (ClassNotFoundException ex) {
			logger.debug("Failed to classload enum type while reading annotation metadata", ex);
		}
		catch (NoClassDefFoundError ex) {
			logger.debug("Failed to classload enum type while reading annotation metadata", ex);
		}
		catch (IllegalAccessException ex) {
			logger.debug("Could not access enum value while reading annotation metadata", ex);
		}
		catch (AccessControlException ex) {
			logger.debug("Could not access enum value while reading annotation metadata", ex);
		}
		return valueToUse;
	}

}
