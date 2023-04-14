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

import org.springframework.asm.Type;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.Nullable;

/**
 * Implementation of source object providing equals/hashCode identity for
 * {@link AnnotatedTypeMetadata} classes.
 *
 * @author Danny Thomas
 */
final class SimpleSource {

	private final String declaringClassName;

	@Nullable
	private final String name;

	@Nullable
	private final Type type;

	@Nullable
	private String toStringValue;

	SimpleSource(String declaringClassName) {
		this(declaringClassName, null, null);
	}

	SimpleSource(String declaringClassName, @Nullable String name, @Nullable String descriptor) {
		this.declaringClassName = declaringClassName;
		this.name = name;
		this.type = descriptor == null ? null : Type.getType(descriptor);
	}

	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	public String getName() {
		return this.name;
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.declaringClassName, this.type, this.name, this.type);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SimpleSource that = (SimpleSource) o;
		return Objects.equals(this.declaringClassName, that.declaringClassName) && Objects.equals(this.type, that.type)
				&& Objects.equals(this.name, that.name);
	}

	@Override
	public String toString() {
		String value = this.toStringValue;
		if (value == null) {
			StringBuilder builder = new StringBuilder();
			builder.append(this.declaringClassName);
			if (this.name != null) {
				builder.append('.');
				builder.append(this.name);
			}
			if (this.type != null && this.type.getSort() == Type.METHOD) {
				Type[] argumentTypes = this.type.getArgumentTypes();
				builder.append('(');
				for (int i = 0; i < argumentTypes.length; i++) {
					if (i != 0) {
						builder.append(',');
					}
					builder.append(argumentTypes[i].getClassName());
				}
				builder.append(')');
			}
			value = builder.toString();
			this.toStringValue = value;
		}
		return value;
	}

}
