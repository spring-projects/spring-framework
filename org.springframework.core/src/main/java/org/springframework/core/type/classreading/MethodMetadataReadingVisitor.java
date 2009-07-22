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

package org.springframework.core.type.classreading;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodAdapter;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.asm.commons.EmptyVisitor;
import org.springframework.core.type.MethodMetadata;

/**
 * ASM method visitor which looks for the annotations defined on the method,
 * exposing them through the {@link org.springframework.core.type.MethodMetadata}
 * interface.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @since 3.0
 */
final class MethodMetadataReadingVisitor extends MethodAdapter implements MethodMetadata {

	private final String name;

	private final int access;

	private final ClassLoader classLoader;

	private final Map<String, Map<String, Object>> attributeMap = new LinkedHashMap<String, Map<String, Object>>();


	public MethodMetadataReadingVisitor(String name, int access, ClassLoader classLoader) {
		super(new EmptyVisitor());
		this.name = name;
		this.access = access;
		this.classLoader = classLoader;
	}


	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		String className = Type.getType(desc).getClassName();
		return new AnnotationAttributesReadingVisitor(className, this.attributeMap, null, this.classLoader);
	}


	public String getMethodName() {
		return this.name;
	}

	public boolean isStatic() {
		return ((this.access & Opcodes.ACC_STATIC) != 0);
	}

	public boolean isFinal() {
		return ((this.access & Opcodes.ACC_FINAL) != 0);
	}

	public boolean isOverridable() {
		return (!isStatic() && !isFinal() && ((this.access & Opcodes.ACC_PRIVATE) == 0));
	}

	public boolean isAnnotated(String annotationType) {
		return this.attributeMap.containsKey(annotationType);
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		return this.attributeMap.get(annotationType);
	}

}
