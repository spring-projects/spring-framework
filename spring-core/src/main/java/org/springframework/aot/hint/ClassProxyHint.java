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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.Nullable;

/**
 * A hint that describes the need for a proxy against a concrete class.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 */
public final class ClassProxyHint implements ConditionalHint {

	private final TypeReference targetClass;

	private final List<TypeReference> proxiedInterfaces;

	@Nullable
	private final TypeReference reachableType;


	private ClassProxyHint(Builder builder) {
		this.targetClass = builder.targetClass;
		this.proxiedInterfaces = builder.proxiedInterfaces.stream().distinct().toList();
		this.reachableType = builder.reachableType;
	}

	/**
	 * Initialize a builder with the target class to use.
	 * @param typeReference the type reference for the target class of the proxy
	 * @return a builder for the hint
	 */
	public static Builder of(TypeReference typeReference) {
		return new Builder(typeReference);
	}

	/**
	 * Initialize a builder with the target class to use.
	 * @param targetClass the target class of the proxy
	 * @return a builder for the hint
	 */
	public static Builder of(Class<?> targetClass) {
		if (targetClass.isInterface()) {
			throw new IllegalArgumentException("Should not be an interface: " + targetClass);
		}
		return of(TypeReference.of(targetClass));
	}

	/**
	 * Return the target class of the proxy.
	 * @return the target class
	 */
	public TypeReference getTargetClass() {
		return this.targetClass;
	}

	/**
	 * Return the interfaces to be proxied.
	 * @return the interfaces that the proxy should implement
	 */
	public List<TypeReference> getProxiedInterfaces() {
		return this.proxiedInterfaces;
	}

	@Nullable
	@Override
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClassProxyHint that = (ClassProxyHint) o;
		return this.targetClass.equals(that.targetClass)
				&& this.proxiedInterfaces.equals(that.proxiedInterfaces)
				&& Objects.equals(this.reachableType, that.reachableType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.targetClass, this.proxiedInterfaces);
	}


	/**
	 * Builder for {@link ClassProxyHint}.
	 */
	public static class Builder {

		private final TypeReference targetClass;

		private final LinkedList<TypeReference> proxiedInterfaces = new LinkedList<>();

		@Nullable
		private TypeReference reachableType;


		Builder(TypeReference targetClass) {
			this.targetClass = targetClass;
		}

		/**
		 * Add the specified interfaces that the proxy should implement.
		 * @param proxiedInterfaces the interfaces the proxy should implement
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder proxiedInterfaces(TypeReference... proxiedInterfaces) {
			this.proxiedInterfaces.addAll(Arrays.asList(proxiedInterfaces));
			return this;
		}

		/**
		 * Add the specified interfaces that the proxy should implement.
		 * @param proxiedInterfaces the interfaces the proxy should implement
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder proxiedInterfaces(Class<?>... proxiedInterfaces) {
			this.proxiedInterfaces.addAll(Arrays.stream(proxiedInterfaces)
					.map(TypeReference::of).toList());
			return this;
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
		 * Create a {@link ClassProxyHint} based on the state of this builder.
		 * @return a class proxy hint
		 */
		ClassProxyHint build() {
			return new ClassProxyHint(this);
		}

	}

}
