/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.util.Objects;
import java.util.ResourceBundle;

import org.jspecify.annotations.Nullable;

/**
 * A hint that describes the need to access a {@link ResourceBundle}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 */
public final class ResourceBundleHint implements ConditionalHint {

	private final String baseName;

	private final @Nullable TypeReference reachableType;


	ResourceBundleHint(Builder builder) {
		this.baseName = builder.baseName;
		this.reachableType = builder.reachableType;
	}


	/**
	 * Return the {@code baseName} of the resource bundle.
	 */
	public String getBaseName() {
		return this.baseName;
	}

	@Override
	public @Nullable TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ResourceBundleHint that &&
				this.baseName.equals(that.baseName) && Objects.equals(this.reachableType, that.reachableType)));
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

		private @Nullable TypeReference reachableType;

		Builder(String baseName) {
			this.baseName = baseName;
		}

		/**
		 * Make this hint conditional on the fact that the specified type can be resolved.
		 * @param reachableType the type that should be reachable for this hint to apply
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
		 * Create a {@link ResourceBundleHint} based on the state of this builder.
		 * @return a resource bundle hint
		 */
		ResourceBundleHint build() {
			return new ResourceBundleHint(this);
		}
	}

}
