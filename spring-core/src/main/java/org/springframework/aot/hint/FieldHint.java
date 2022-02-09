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

/**
 * A hint that describes the need of reflection on a {@link Field}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class FieldHint extends MemberHint {

	private final boolean allowWrite;

	private final boolean allowUnsafeAccess;


	private FieldHint(Builder builder) {
		super(builder.name);
		this.allowWrite = builder.allowWrite;
		this.allowUnsafeAccess = builder.allowUnsafeAccess;
	}

	/**
	 * Return whether setting the value of the field should be allowed.
	 * @return {@code true} to allow {@link Field#set(Object, Object)}.
	 */
	public boolean isAllowWrite() {
		return this.allowWrite;
	}

	/**
	 * Return whether if using {@code Unsafe} on the field should be allowed.
	 * @return {@code true} to allow unsafe access
	 */
	public boolean isAllowUnsafeAccess() {
		return this.allowUnsafeAccess;
	}


	/**
	 * Builder for {@link FieldHint}.
	 */
	public static class Builder {

		private final String name;

		private boolean allowWrite;

		private boolean allowUnsafeAccess;


		public Builder(String name) {
			this.name = name;
		}

		/**
		 * Specify if setting the value of the field should be allowed.
		 * @param allowWrite {@code true} to allow {@link Field#set(Object, Object)}
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder allowWrite(boolean allowWrite) {
			this.allowWrite = allowWrite;
			return this;
		}

		/**
		 * Specify if using {@code Unsafe} on the field should be allowed.
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
		public FieldHint build() {
			return new FieldHint(this);
		}

	}
}
