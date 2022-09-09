/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.reflect.Field;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A hint that describes the need for reflection on a {@link Field}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class FieldHint extends MemberHint {

	private final FieldMode mode;

	private final boolean allowUnsafeAccess;


	private FieldHint(Builder builder) {
		super(builder.name);
		this.mode = (builder.mode != null ? builder.mode : FieldMode.WRITE);
		this.allowUnsafeAccess = builder.allowUnsafeAccess;
	}

	/**
	 * Return whether setting the value of the field should be allowed.
	 * @return {@code true} to allow {@link Field#set(Object, Object)}.
	 * @deprecated in favor of {@link #getMode()}
	 */
	@Deprecated
	public boolean isAllowWrite() {
		return this.mode == FieldMode.WRITE;
	}

	/**
	 * Return the {@linkplain FieldMode mode} that applies to this hint.
	 * @return the mode
	 */
	public FieldMode getMode() {
		return this.mode;
	}

	/**
	 * Return whether using {@code Unsafe} on the field should be allowed.
	 * @return {@code true} to allow unsafe access
	 */
	public boolean isAllowUnsafeAccess() {
		return this.allowUnsafeAccess;
	}

	/**
	 * Return a {@link Consumer} that applies the given {@link FieldMode}
	 * to the accepted {@link Builder}.
	 * @param mode the mode to apply
	 * @return a consumer to apply the mode
	 */
	public static Consumer<Builder> builtWith(FieldMode mode) {
		return builder -> builder.withMode(mode);
	}


	/**
	 * Builder for {@link FieldHint}.
	 */
	public static class Builder {

		private final String name;

		@Nullable
		private FieldMode mode;

		private boolean allowUnsafeAccess;


		Builder(String name) {
			this.name = name;
		}

		/**
		 * Specify if setting the value of the field should be allowed.
		 * @param allowWrite {@code true} to allow {@link Field#set(Object, Object)}
		 * @return {@code this}, to facilitate method chaining
		 * @deprecated in favor of {@link #withMode(FieldMode)}
		 */
		@Deprecated
		public Builder allowWrite(boolean allowWrite) {
			if (allowWrite) {
				return withMode(FieldMode.WRITE);
			}
			return this;
		}

		/**
		 * Specify that the {@linkplain FieldMode mode} is required.
		 * @param mode the required mode
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder withMode(FieldMode mode) {
			Assert.notNull(mode, "'mode' must not be null");
			if ((this.mode == null || !this.mode.includes(mode))) {
				this.mode = mode;
			}
			return this;
		}

		/**
		 * Specify whether using {@code Unsafe} on the field should be allowed.
		 * @param allowUnsafeAccess {@code true} to allow unsafe access
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder allowUnsafeAccess(boolean allowUnsafeAccess) {
			this.allowUnsafeAccess = allowUnsafeAccess;
			return this;
		}

		/**
		 * Create a {@link FieldHint} based on the state of this builder.
		 * @return a field hint
		 */
		FieldHint build() {
			return new FieldHint(this);
		}

	}
}
