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

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.Nullable;

/**
 * A hint that describes the need for a JDK interface-based {@link Proxy}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 */
public final class JdkProxyHint implements ConditionalHint {

	private final List<TypeReference> proxiedInterfaces;

	@Nullable
	private final TypeReference reachableType;


	private JdkProxyHint(Builder builder) {
		this.proxiedInterfaces = List.copyOf(builder.proxiedInterfaces);
		this.reachableType = builder.reachableType;
	}

	/**
	 * Initialize a builder with the proxied interfaces to use.
	 * @param proxiedInterfaces the interfaces the proxy should implement
	 * @return a builder for the hint
	 */
	public static Builder of(TypeReference... proxiedInterfaces) {
		return new Builder().proxiedInterfaces(proxiedInterfaces);
	}

	/**
	 * Initialize a builder with the proxied interfaces to use.
	 * @param proxiedInterfaces the interfaces the proxy should implement
	 * @return a builder for the hint
	 */
	public static Builder of(Class<?>... proxiedInterfaces) {
		return new Builder().proxiedInterfaces(proxiedInterfaces);
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
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		JdkProxyHint that = (JdkProxyHint) o;
		return this.proxiedInterfaces.equals(that.proxiedInterfaces)
				&& Objects.equals(this.reachableType, that.reachableType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.proxiedInterfaces);
	}


	/**
	 * Builder for {@link JdkProxyHint}.
	 */
	public static class Builder {

		private final LinkedList<TypeReference> proxiedInterfaces;

		@Nullable
		private TypeReference reachableType;


		Builder() {
			this.proxiedInterfaces = new LinkedList<>();
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
			this.proxiedInterfaces.addAll(toTypeReferences(proxiedInterfaces));
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
		 * Create a {@link JdkProxyHint} based on the state of this builder.
		 * @return a JDK proxy hint
		 */
		JdkProxyHint build() {
			return new JdkProxyHint(this);
		}

		private static List<TypeReference> toTypeReferences(Class<?>... proxiedInterfaces) {
			List<String> invalidTypes = Arrays.stream(proxiedInterfaces)
					.filter(candidate -> !candidate.isInterface() || candidate.isSealed())
					.map(Class::getName)
					.toList();
			if (!invalidTypes.isEmpty()) {
				throw new IllegalArgumentException("The following must be non-sealed interfaces: " + invalidTypes);
			}
			return TypeReference.listOf(proxiedInterfaces);
		}

	}

}
