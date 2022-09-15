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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A hint that describes the need for reflection on a {@link Method} or
 * {@link Constructor}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class ExecutableHint extends MemberHint {

	private final List<TypeReference> parameterTypes;

	private final ExecutableMode mode;


	private ExecutableHint(Builder builder) {
		super(builder.name);
		this.parameterTypes = List.copyOf(builder.parameterTypes);
		this.mode = (builder.mode != null ? builder.mode : ExecutableMode.INVOKE);
	}

	/**
	 * Initialize a builder with the parameter types of a constructor.
	 * @param parameterTypes the parameter types of the constructor
	 * @return a builder
	 */
	static Builder ofConstructor(List<TypeReference> parameterTypes) {
		return new Builder("<init>", parameterTypes);
	}

	/**
	 * Initialize a builder with the name and parameter types of a method.
	 * @param name the name of the method
	 * @param parameterTypes the parameter types of the method
	 * @return a builder
	 */
	static Builder ofMethod(String name, List<TypeReference> parameterTypes) {
		return new Builder(name, parameterTypes);
	}

	/**
	 * Return the parameter types of the executable.
	 * @return the parameter types
	 * @see Executable#getParameterTypes()
	 */
	public List<TypeReference> getParameterTypes() {
		return this.parameterTypes;
	}

	/**
	 * Return the {@linkplain ExecutableMode mode} that applies to this hint.
	 * @return the mode
	 */
	public ExecutableMode getMode() {
		return this.mode;
	}

	/**
	 * Return a {@link Consumer} that applies the given {@link ExecutableMode}
	 * to the accepted {@link Builder}.
	 * @param mode the mode to apply
	 * @return a consumer to apply the mode
	 */
	public static Consumer<Builder> builtWith(ExecutableMode mode) {
		return builder -> builder.withMode(mode);
	}

	/**
	 * Builder for {@link ExecutableHint}.
	 */
	public static class Builder {

		private final String name;

		private final List<TypeReference> parameterTypes;

		@Nullable
		private ExecutableMode mode;


		Builder(String name, List<TypeReference> parameterTypes) {
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		/**
		 * Specify that the {@linkplain ExecutableMode mode} is required.
		 * @param mode the required mode
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder withMode(ExecutableMode mode) {
			Assert.notNull(mode, "'mode' must not be null");
			if ((this.mode == null) || !this.mode.includes(mode)) {
				this.mode = mode;
			}
			return this;
		}

		/**
		 * Create an {@link ExecutableHint} based on the state of this builder.
		 * @return an executable hint
		 */
		ExecutableHint build() {
			return new ExecutableHint(this);
		}

	}

}
