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

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.FieldMetadata;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;

/**
 * Simple implementation of {@link FieldMetadata}.
 *
 * @author Danny Thomas
 */
class SimpleFieldMetadata implements FieldMetadata {

	private final String declaringClassName;

	private final String fieldName;

	private final int access;

	private final TypeMetadata fieldType;

	private final Object source;

	private final MergedAnnotations annotations;

	SimpleFieldMetadata(String declaringClassName, String fieldName, int access, TypeMetadata fieldType, Object source,
			MergedAnnotations annotations) {

		this.declaringClassName = declaringClassName;
		this.fieldName = fieldName;
		this.access = access;
		this.fieldType = fieldType;
		this.source = source;
		this.annotations = annotations;
	}

	@Override
	public String getFieldName() {
		return this.fieldName;
	}

	@Override
	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public TypeMetadata getAnnotatedType() {
		return this.fieldType;
	}

	@Override
	public TypeMetadata getFieldType() {
		return this.fieldType;
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
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return ((this == obj)
				|| ((obj instanceof SimpleFieldMetadata) && this.source.equals(((SimpleFieldMetadata) obj).source)));
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
