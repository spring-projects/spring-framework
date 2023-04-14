/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.classreading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.ParameterMetadata;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;

/**
 * {@link MethodMetadata} created from a {@link SimpleMethodMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
final class SimpleMethodMetadata implements MethodMetadata {

	private final String methodName;

	private final int access;

	private final String declaringClassName;

	private final int declaringClassAccess;

	private final TypeMetadata returnType;

	private final Object source;

	private final MergedAnnotations annotations;

	private final List<ParameterMetadata> methodArguments;

	SimpleMethodMetadata(String declaringClassName, int declaringClassAccess, String methodName, int access,
		TypeMetadata returnType, Object source, MergedAnnotations annotations, List<ParameterMetadata> methodArguments) {
		this.declaringClassName = declaringClassName;
		this.declaringClassAccess = declaringClassAccess;
		this.methodName = methodName;
		this.access = access;
		this.returnType = returnType;
		this.source = source;
		this.annotations = annotations;
		this.methodArguments = Collections.unmodifiableList(methodArguments);
	}

	@Override
	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public TypeMetadata getAnnotatedType() {
		return this.returnType;
	}

	@Override
	public TypeMetadata getReturnType() {
		return this.returnType;
	}

	@Override
	public String getReturnTypeName() {
		return this.returnType.getTypeName();
	}

	@Override
	public boolean isAbstract() {
		return (this.access & Opcodes.ACC_ABSTRACT) != 0;
	}

	@Override
	public boolean isStatic() {
		return (this.access & Opcodes.ACC_STATIC) != 0;
	}

	@Override
	public boolean isFinal() {
		return (this.access & Opcodes.ACC_FINAL) != 0;
	}

	@Override
	public boolean isPrivate() {
		return (this.access & Opcodes.ACC_PRIVATE) != 0;
	}

	@Override
	public boolean isDefault() {
		// Default methods are public non-abstract instance methods declared in an
		// interface.
		return ((this.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC)
				&& (this.declaringClassAccess & Opcodes.ACC_INTERFACE) != 0;
	}

	@Override
	public boolean isOverridable() {
		return !isStatic() && !isFinal() && !isPrivate();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	@Override
	public List<ParameterMetadata> getParameters() {
		return this.methodArguments;
	}

	@Override
	public List<ParameterMetadata> getAnnotatedParameters(String annotationName) {
		List<ParameterMetadata> result = new ArrayList<>(this.methodArguments.size());
		for (ParameterMetadata metadata : this.methodArguments) {
			if (metadata.isAnnotated(annotationName)) {
				result.add(metadata);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return ((this == obj) || ((obj instanceof SimpleMethodMetadata)
				&& this.source.equals(((SimpleMethodMetadata) obj).source)));
	}

	@Override
	public int hashCode() {
		return this.source.hashCode();
	}

	@Override
	public String toString() {
		return this.source.toString();
	}

}
