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

import java.util.Objects;
import java.util.ResourceBundle;

import org.springframework.lang.Nullable;

/**
 * A hint that describes the need to access a {@link ResourceBundle}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 */
public final class ResourceBundleHint implements ConditionalHint {

	private final String baseName;

	@Nullable
	private final TypeReference reachableType;


	ResourceBundleHint(Builder builder) {
		this.baseName = builder.baseName;
		this.reachableType = builder.reachableType;
	}

	/**
	 * Return the {@code baseName} of the resource bundle.
	 * @return the base name
	 */
	public String getBaseName() {
		return this.baseName;
	}

	@Nullable
	@Override
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
		ResourceBundleHint that = (ResourceBundleHint) o;
		return this.baseName.equals(that.baseName)
				&& Objects.equals(this.reachableType, that.reachableType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.baseName, this.reachableType);
	}

	/**
	 * Builder for {@link ResourceBundleHint}.
	 */
	public static class Builder {

		private String baseName;

		@Nullable
		private TypeReference reachableType;

		Builder(String baseName) {
			this.baseName = baseName;
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
		 * Use the {@code baseName} of the resource bundle.
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder baseName(String baseName) {
			this.baseName = baseName;
			return this;
		}

		/**
		 * Creates a {@link ResourceBundleHint} based on the state of this
		 * builder.
		 * @return a resource bundle hint
		 */
		ResourceBundleHint build() {
			return new ResourceBundleHint(this);
		}

	}

}
