/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * A variant of {@link ObjectFactory} designed specifically for injection points,
 * allowing for programmatic optionality and lenient not-unique handling.
 *
 * <p>As of 5.1, this interface extends {@link Iterable} and provides {@link Stream}
 * support. It can be therefore be used in {@code for} loops, provides {@link #forEach}
 * iteration and allows for collection-style {@link #stream} access.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @param <T> the object type
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * <p>Allows for specifying explicit construction arguments, along the
	 * lines of {@link BeanFactory#getBean(String, Object...)}.
	 * @param args arguments to use when creating a corresponding instance
	 * @return an instance of the bean
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	T getObject(Object... args) throws BeansException;

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @return an instance of the bean, or {@code null} if not available
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	@Nullable
	T getIfAvailable() throws BeansException;

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @param defaultSupplier a callback for supplying a default object
	 * if none is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * Consume an instance (possibly shared or independent) of the object
	 * managed by this factory, if available.
	 * @param dependencyConsumer a callback for processing the target object
	 * if available (not called otherwise)
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfAvailable();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @return an instance of the bean, or {@code null} if not available or
	 * not unique (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	@Nullable
	T getIfUnique() throws BeansException;

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @param defaultSupplier a callback for supplying a default object
	 * if no unique candidate is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available or if it is not unique in the factory
	 * (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfUnique()
	 */
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * Consume an instance (possibly shared or independent) of the object
	 * managed by this factory, if unique.
	 * @param dependencyConsumer a callback for processing the target object
	 * if unique (not called otherwise)
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfUnique();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * Return a sequential {@link Stream} over lazily resolved object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 * @since 5.1
	 * @see #iterator()
	 */
	default Stream<T> stream() {
		throw new UnsupportedOperationException("Multi-element access not supported");
	}

	/**
	 * Return an {@link Iterator} over lazily resolved object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 * @since 5.1
	 * @see #stream()
	 */
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * Return a {@link List} with fully resolved object instances,
	 * potentially pre-ordered according to a common comparator.
	 * <p>In a common Spring application context, this will be ordered
	 * according to {@link org.springframework.core.Ordered} /
	 * {@link org.springframework.core.annotation.Order} conventions,
	 * analogous to multi-element injection points of list/array type.
	 * @since 5.1
	 * @see #stream()
	 */
	default List<T> toList() {
		return stream().collect(Collectors.toList());
	}

}
