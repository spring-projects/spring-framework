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
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ConstructorMetadata;
import org.springframework.core.type.ParameterMetadata;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;

/**
 * Simple implementation of {@link ConstructorMetadata}.
 *
 * @author Danny Thomas
 */
final class SimpleConstructorMetadata implements ConstructorMetadata {

	private final int access;

	private final String declaringClassName;

	private final Object source;

	private final MergedAnnotations annotations;

	private final List<ParameterMetadata> parameters;

	private AnnotationMetadata declaringClass;

	SimpleConstructorMetadata(int access, String declaringClassName, Object source, MergedAnnotations annotations,
				List<ParameterMetadata> parameters) {
		this.access = access;
		this.declaringClassName = declaringClassName;
		this.source = source;
		this.annotations = annotations;
		this.parameters = parameters;
	}

	@Override
	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public TypeMetadata getAnnotatedType() {
		throw new UnsupportedOperationException("Constructor methods do not have return types");
	}

	@Override
	public boolean isFinal() {
		return (this.access & Opcodes.ACC_FINAL) != 0;
	}

	@Override
	public boolean isOverridable() {
		return !isFinal() && !isPrivate();
	}

	@Override
	public boolean isPrivate() {
		return (this.access & Opcodes.ACC_PRIVATE) != 0;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	@Override
	public List<ParameterMetadata> getParameters() {
		return this.parameters;
	}

	@Override
	public List<ParameterMetadata> getAnnotatedParameters(String annotationName) {
		List<ParameterMetadata> result = new ArrayList<>(this.parameters.size());
		for (ParameterMetadata metadata : this.parameters) {
			if (metadata.isAnnotated(annotationName)) {
				result.add(metadata);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return ((this == obj) || ((obj instanceof SimpleConstructorMetadata)
				&& this.source.equals(((SimpleConstructorMetadata) obj).source)));
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
