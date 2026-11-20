/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;


import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A hint that describes the need for Java serialization at runtime.
 *
 * @author Brian Clozel
 * @since 6.0
 * @deprecated in favor of {@link TypeHint}
 */
@Deprecated(since = "7.0.6", forRemoval = true)
public final class JavaSerializationHint implements ConditionalHint {

	private final TypeReference type;

	private final @Nullable TypeReference reachableType;


	JavaSerializationHint(Builder builder) {
		this.type = builder.type;
		this.reachableType = builder.reachableType;
	}


	/**
	 * Return the {@link TypeReference type} that needs to be serialized using Java serialization at runtime.
	 * @return a {@link Serializable} type
	 */
	public TypeReference getType() {
		return this.type;
	}

	@Override
	public @Nullable TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof JavaSerializationHint that &&
				this.type.equals(that.type) && Objects.equals(this.reachableType, that.reachableType)));
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

		private @Nullable TypeReference reachableType;

		Builder(TypeReference type) {
			this.type = type;
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
		 * Create a {@link JavaSerializationHint} based on the state of this builder.
		 * @return a java serialization hint
		 */
		JavaSerializationHint build() {
			return new JavaSerializationHint(this);
		}
	}

}
