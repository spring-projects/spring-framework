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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * ASM method visitor which looks for the annotations defined on a method,
 * exposing them through the {@link org.springframework.core.type.MethodMetadata}
 * interface.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 */
public class MethodMetadataReadingVisitor extends MethodVisitor implements MethodMetadata {

	protected final String methodName;

	protected final int access;

	protected final String declaringClassName;

	protected final String returnTypeName;

	@Nullable
	protected final ClassLoader classLoader;

	protected final Set<MethodMetadata> methodMetadataSet;

	protected final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<>(4);

	protected final LinkedMultiValueMap<String, AnnotationAttributes> attributesMap = new LinkedMultiValueMap<>(4);


	public MethodMetadataReadingVisitor(String methodName, int access, String declaringClassName,
			String returnTypeName, @Nullable ClassLoader classLoader, Set<MethodMetadata> methodMetadataSet) {

		super(SpringAsmInfo.ASM_VERSION);
		this.methodName = methodName;
		this.access = access;
		this.declaringClassName = declaringClassName;
		this.returnTypeName = returnTypeName;
		this.classLoader = classLoader;
		this.methodMetadataSet = methodMetadataSet;
	}


	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		this.methodMetadataSet.add(this);
		String className = Type.getType(desc).getClassName();
		return new AnnotationAttributesReadingVisitor(
				className, this.attributesMap, this.metaAnnotationMap, this.classLoader);
	}


	@Override
	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public boolean isAbstract() {
		return ((this.access & Opcodes.ACC_ABSTRACT) != 0);
	}

	@Override
	public boolean isStatic() {
		return ((this.access & Opcodes.ACC_STATIC) != 0);
	}

	@Override
	public boolean isFinal() {
		return ((this.access & Opcodes.ACC_FINAL) != 0);
	}

	@Override
	public boolean isOverridable() {
		return (!isStatic() && !isFinal() && ((this.access & Opcodes.ACC_PRIVATE) == 0));
	}

	@Override
	public boolean isAnnotated(String annotationName) {
		return this.attributesMap.containsKey(annotationName);
	}

	@Override
	@Nullable
	public AnnotationAttributes getAnnotationAttributes(String annotationName) {
		return getAnnotationAttributes(annotationName, false);
	}

	@Override
	@Nullable
	public AnnotationAttributes getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		AnnotationAttributes raw = AnnotationReadingVisitorUtils.getMergedAnnotationAttributes(
				this.attributesMap, this.metaAnnotationMap, annotationName);
		if (raw == null) {
			return null;
		}
		return AnnotationReadingVisitorUtils.convertClassValues(
				"method '" + getMethodName() + "'", this.classLoader, raw, classValuesAsString);
	}

	@Override
	@Nullable
	public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	@Override
	@Nullable
	public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		if (!this.attributesMap.containsKey(annotationName)) {
			return null;
		}
		MultiValueMap<String, Object> allAttributes = new LinkedMultiValueMap<>();
		List<AnnotationAttributes> attributesList = this.attributesMap.get(annotationName);
		if (attributesList != null) {
			for (AnnotationAttributes annotationAttributes : attributesList) {
				AnnotationAttributes convertedAttributes = AnnotationReadingVisitorUtils.convertClassValues(
						"method '" + getMethodName() + "'", this.classLoader, annotationAttributes, classValuesAsString);
				convertedAttributes.forEach(allAttributes::add);
			}
		}
		return allAttributes;
	}

	@Override
	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public String getReturnTypeName() {
		return this.returnTypeName;
	}

}
