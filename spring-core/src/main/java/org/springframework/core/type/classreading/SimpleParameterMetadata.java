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

import java.util.Objects;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.ParameterMetadata;
import org.springframework.core.type.TypeMetadata;

/**
 * Simple implementation of {@link ParameterMetadata}.
 *
 * @author Danny Thomas
 */
final class SimpleParameterMetadata implements ParameterMetadata {

	private final String declaringClassName;

	private final String declaringMethodName;

	private final TypeMetadata parameterType;

	private final int parameterIndex;

	private final Object source;

	private final MergedAnnotations annotations;

	SimpleParameterMetadata(String declaringClassName, String declaringMethodName, TypeMetadata parameterType,
							int parameterIndex, Object source, MergedAnnotations annotations) {

		this.declaringClassName = declaringClassName;
		this.declaringMethodName = declaringMethodName;
		this.parameterType = parameterType;
		this.parameterIndex = parameterIndex;
		this.source = source;
		this.annotations = annotations;
	}

	@Override
	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public TypeMetadata getAnnotatedType() {
		return this.parameterType;
	}

	@Override
	public String getDeclaringMethodName() {
		return this.declaringMethodName;
	}

	@Override
	public TypeMetadata getParameterType() {
		return this.parameterType;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SimpleParameterMetadata that = (SimpleParameterMetadata) o;
		return this.parameterIndex == that.parameterIndex && Objects.equals(this.parameterType, that.parameterType)
				&& Objects.equals(this.source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parameterType, this.parameterIndex, this.source);
	}

	@Override
	public String toString() {
		return this.source.toString();
	}

}
