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

package org.springframework.aot.hint;


import java.io.Serializable;
import java.util.Objects;

import org.springframework.lang.Nullable;

/**
 * A hint that describes the need for Java serialization at runtime.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class JavaSerializationHint implements ConditionalHint {

	private final TypeReference type;

	@Nullable
	private final TypeReference reachableType;

	JavaSerializationHint(Builder builder) {
		this.type = builder.type;
		this.reachableType = builder.reachableType;
	}

	/**
	 * Return the {@link TypeReference type} that needs to be serialized using
	 * Java serialization at runtime.
	 * @return a {@link Serializable} type
	 */
	public TypeReference getType() {
		return this.type;
	}

	@Override
	@Nullable
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		JavaSerializationHint that = (JavaSerializationHint) o;
		return this.type.equals(that.type)
				&& Objects.equals(this.reachableType, that.reachableType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.reachableType);
	}


	/**
	 * Builder for {@link JavaSerializationHint}.
	 */
	public static class Builder {

		private final TypeReference type;

		@Nullable
		private TypeReference reachableType;


		Builder(TypeReference type) {
			this.type = type;
		}

		/**
		 * Make this hint conditional on the fact that the specified type
		 * can be resolved.
		 * @param reachableType the type that should be reachable for this
		 * hint to apply
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder onReachableType(TypeReference reachableType) {
			this.reachableType = reachableType;
			return this;
		}

		/**
		 * Create a {@link JavaSerializationHint} based on the state of this builder.
		 * @return a java serialization hint
		 */
		JavaSerializationHint build() {
			return new JavaSerializationHint(this);
		}

	}
}
