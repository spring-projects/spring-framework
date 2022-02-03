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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.ObjectUtils;

/**
 * A hint that describes the need for reflection on a {@link Method} or
 * {@link Constructor}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class ExecutableHint extends MemberHint {

	private final List<TypeReference> parameterTypes;

	private final List<ExecutableMode> modes;


	private ExecutableHint(Builder builder) {
		super(builder.name);
		this.parameterTypes = List.copyOf(builder.parameterTypes);
		this.modes = List.copyOf(builder.modes);
	}

	/**
	 * Initialize a builder with the parameter types of a constructor.
	 * @param parameterTypes the parameter types of the constructor
	 * @return a builder
	 */
	public static Builder ofConstructor(List<TypeReference> parameterTypes) {
		return new Builder("<init>", parameterTypes);
	}

	/**
	 * Initialize a builder with the name and parameters types of a method.
	 * @param name the name of the method
	 * @param parameterTypes the parameter types of the method
	 * @return a builder
	 */
	public static Builder ofMethod(String name, List<TypeReference> parameterTypes) {
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
	 * Return the {@linkplain ExecutableMode modes} that apply to this hint.
	 * @return the modes
	 */
	public List<ExecutableMode> getModes() {
		return this.modes;
	}


	/**
	 * Builder for {@link ExecutableHint}.
	 */
	public static final class Builder {

		private final String name;

		private final List<TypeReference> parameterTypes;

		private final Set<ExecutableMode> modes = new LinkedHashSet<>();


		private Builder(String name, List<TypeReference> parameterTypes) {
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		/**
		 * Add the specified {@linkplain ExecutableMode mode} if necessary.
		 * @param mode the mode to add
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder withMode(ExecutableMode mode) {
			this.modes.add(mode);
			return this;
		}

		/**
		 * Set the {@linkplain ExecutableMode modes} to use.
		 * @param modes the mode to use
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder setModes(ExecutableMode... modes) {
			this.modes.clear();
			if (!ObjectUtils.isEmpty(modes)) {
				this.modes.addAll(Arrays.asList(modes));
			}
			return this;
		}

		/**
		 * Create an {@link ExecutableHint} based on the state of this builder.
		 * @return an executable hint
		 */
		public ExecutableHint build() {
			return new ExecutableHint(this);
		}

	}

}
