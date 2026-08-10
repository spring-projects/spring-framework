/*
 * Copyright 2002-present the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A hint that describes the need of reflection for a Lambda.
 *
 * @author Stephane Nicoll
 * @since 7.0.6
 */
public final class LambdaHint implements ConditionalHint {

	private final TypeReference declaringClass;

	private final @Nullable TypeReference reachableType;

	private final @Nullable DeclaringMethod declaringMethod;

	private final List<TypeReference> interfaces;

	private LambdaHint(Builder builder) {
		this.declaringClass = builder.declaringClass;
		this.reachableType = builder.reachableType;
		this.declaringMethod = builder.declaringMethod;
		this.interfaces = List.copyOf(builder.interfaces);
	}

	/**
	 * Initialize a builder with the class declaring the lambda.
	 * @param declaringClass the type declaring the lambda
	 * @return a builder for the hint
	 */
	public static Builder of(TypeReference declaringClass) {
		return new Builder(declaringClass);
	}

	/**
	 * Initialize a builder with the class declaring the lambda.
	 * @param declaringClass the type declaring the lambda
	 * @return a builder for the hint
	 */
	public static Builder of(Class<?> declaringClass) {
		return new Builder(TypeReference.of(declaringClass));
	}

	/**
	 * Return the type declaring the lambda.
	 * @return the declaring class
	 */
	public TypeReference getDeclaringClass() {
		return this.declaringClass;
	}

	@Override
	public @Nullable TypeReference getReachableType() {
		return this.reachableType;
	}

	/**
	 * Return the method in which the lambda is defined, if any.
	 * @return the declaring method
	 */
	public @Nullable DeclaringMethod getDeclaringMethod() {
		return this.declaringMethod;
	}

	/**
	 * Return the interfaces that are implemented by the lambda.
	 * @return the interfaces
	 */
	public List<TypeReference> getInterfaces() {
		return this.interfaces;
	}

	public static class Builder {

		private final TypeReference declaringClass;

		private @Nullable TypeReference reachableType;

		private @Nullable DeclaringMethod declaringMethod;

		private final List<TypeReference> interfaces = new ArrayList<>();

		Builder(TypeReference declaringClass) {
			this.declaringClass = declaringClass;
		}

		/**
		 * Make this hint conditional on the fact that the specified type is in a
		 * reachable code path from a static analysis point of view.
		 * @param reachableType the type that should be reachable for this hint to apply
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder onReachableType(TypeReference reachableType) {
			this.reachableType = reachableType;
			return this;
		}

		/**
		 * Make this hint conditional on the fact that the specified type is in a
		 * reachable code path from a static analysis point of view.
		 * @param reachableType the type that should be reachable for this hint to apply
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder onReachableType(Class<?> reachableType) {
			this.reachableType = TypeReference.of(reachableType);
			return this;
		}

		/**
		 * Set the method that declares the lambda.
		 * @param name the name of the method
		 * @param parameterTypes the parameter types, if any.
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder withDeclaringMethod(String name, List<TypeReference> parameterTypes) {
			this.declaringMethod = new DeclaringMethod(name, parameterTypes);
			return this;
		}

		/**
		 * Set the method that declares the lambda.
		 * @param name the name of the method
		 * @param parameterTypes the parameter types, if any.
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder withDeclaringMethod(String name, Class<?>... parameterTypes) {
			return withDeclaringMethod(name, Arrays.stream(parameterTypes).map(TypeReference::of).toList());
		}

		/**
		 * Add the specified interfaces that the lambda should implement.
		 * @param interfaces the interfaces the lambda should implement
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder withInterfaces(TypeReference... interfaces) {
			this.interfaces.addAll(Arrays.asList(interfaces));
			return this;
		}

		/**
		 * Add the specified interfaces that the lambda should implement.
		 * @param interfaces the interfaces the lambda should implement
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder withInterfaces(Class<?>... interfaces) {
			this.interfaces.addAll(Arrays.stream(interfaces).map(TypeReference::of).toList());
			return this;
		}

		public LambdaHint build() {
			return new LambdaHint(this);
		}

	}

	/**
	 * Describe a method.
	 * @param name the name of the method
	 * @param parameterTypes the parameter types
	 */
	public record DeclaringMethod(String name, List<TypeReference> parameterTypes) {
	}

}
