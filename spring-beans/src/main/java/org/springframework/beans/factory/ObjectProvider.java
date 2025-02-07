/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.beans.factory;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.core.OrderComparator;

/**
 * A variant of {@link ObjectFactory} designed specifically for injection points,
 * allowing for programmatic optionality and lenient not-unique handling.
 *
 * <p>In a {@link BeanFactory} environment, every {@code ObjectProvider} obtained
 * from the factory will be bound to its {@code BeanFactory} for a specific bean
 * type, matching all provider calls against factory-registered bean definitions.
 * Note that all such calls dynamically operate on the underlying factory state,
 * freshly resolving the requested target object on every call.
 *
 * <p>As of 5.1, this interface extends {@link Iterable} and provides {@link Stream}
 * support. It can be therefore be used in {@code for} loops, provides {@link #forEach}
 * iteration and allows for collection-style {@link #stream} access.
 *
 * <p>As of 6.2, this interface declares default implementations for all methods.
 * This makes it easier to implement in a custom fashion, for example, for unit tests.
 * For typical purposes, implement {@link #stream()} to enable all other methods.
 * Alternatively, you may implement the specific methods that your callers expect,
 * for example, just {@link #getObject()} or {@link #getIfAvailable()}.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @param <T> the object type
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * A predicate for unfiltered type matches, including non-default candidates
	 * but still excluding non-autowire candidates when used on injection points.
	 * @since 6.2.3
	 * @see #stream(Predicate)
	 * @see #orderedStream(Predicate)
	 * @see org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#isDefaultCandidate()
	 */
	Predicate<Class<?>> UNFILTERED = (clazz -> true);


	@Override
	default T getObject() throws BeansException {
		Iterator<T> it = iterator();
		if (!it.hasNext()) {
			throw new NoSuchBeanDefinitionException(Object.class);
		}
		T result = it.next();
		if (it.hasNext()) {
			throw new NoUniqueBeanDefinitionException(Object.class, 2, "more than 1 matching bean");
		}
		return result;
	}

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
	default T getObject(@Nullable Object... args) throws BeansException {
		throw new UnsupportedOperationException("Retrieval with arguments not supported -" +
				"for custom ObjectProvider classes, implement getObject(Object...) for your purposes");
	}

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @return an instance of the bean, or {@code null} if not available
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	default @Nullable T getIfAvailable() throws BeansException {
		try {
			return getObject();
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw ex;
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

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
	default @Nullable T getIfUnique() throws BeansException {
		try {
			return getObject();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

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
	 * Return an {@link Iterator} over all matching object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 * @since 5.1
	 * @see #stream()
	 */
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * Return a sequential {@link Stream} over all matching object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 * <p>Note: The result may be filtered by default according to qualifiers on the
	 * injection point versus target beans and the general autowire candidate status
	 * of matching beans. For custom filtering against type-matching candidates, use
	 * {@link #stream(Predicate)} instead (potentially with {@link #UNFILTERED}).
	 * @since 5.1
	 * @see #iterator()
	 * @see #orderedStream()
	 */
	default Stream<T> stream() {
		throw new UnsupportedOperationException("Element access not supported - " +
				"for custom ObjectProvider classes, implement stream() to enable all other methods");
	}

	/**
	 * Return a sequential {@link Stream} over all matching object instances,
	 * pre-ordered according to the factory's common order comparator.
	 * <p>In a standard Spring application context, this will be ordered
	 * according to {@link org.springframework.core.Ordered} conventions,
	 * and in case of annotation-based configuration also considering the
	 * {@link org.springframework.core.annotation.Order} annotation,
	 * analogous to multi-element injection points of list/array type.
	 * <p>The default method applies an {@link OrderComparator} to the
	 * {@link #stream()} method. You may override this to apply an
	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}
	 * if necessary.
	 * <p>Note: The result may be filtered by default according to qualifiers on the
	 * injection point versus target beans and the general autowire candidate status
	 * of matching beans. For custom filtering against type-matching candidates, use
	 * {@link #stream(Predicate)} instead (potentially with {@link #UNFILTERED}).
	 * @since 5.1
	 * @see #stream()
	 * @see org.springframework.core.OrderComparator
	 */
	default Stream<T> orderedStream() {
		return stream().sorted(OrderComparator.INSTANCE);
	}

	/**
	 * Return a custom-filtered {@link Stream} over all matching object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 * @param customFilter a custom type filter for selecting beans among the raw
	 * bean type matches (or {@link #UNFILTERED} for all raw type matches without
	 * any default filtering)
	 * @since 6.2.3
	 * @see #stream()
	 * @see #orderedStream(Predicate)
	 */
	default Stream<T> stream(Predicate<Class<?>> customFilter) {
		return stream().filter(obj -> customFilter.test(obj.getClass()));
	}

	/**
	 * Return a custom-filtered {@link Stream} over all matching object instances,
	 * pre-ordered according to the factory's common order comparator.
	 * @param customFilter a custom type filter for selecting beans among the raw
	 * bean type matches (or {@link #UNFILTERED} for all raw type matches without
	 * any default filtering)
	 * @since 6.2.3
	 * @see #orderedStream()
	 * @see #stream(Predicate)
	 */
	default Stream<T> orderedStream(Predicate<Class<?>> customFilter) {
		return orderedStream().filter(obj -> customFilter.test(obj.getClass()));
	}

}
