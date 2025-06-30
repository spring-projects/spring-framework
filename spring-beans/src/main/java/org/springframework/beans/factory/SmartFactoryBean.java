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

package org.springframework.beans.factory;

import org.jspecify.annotations.Nullable;

/**
 * Extension of the {@link FactoryBean} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their {@link #isSingleton()} implementation returning
 * {@code false} does not clearly indicate independent instances.
 * Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their {@link #isSingleton()} implementation returns
 * {@code false}; the exposed object is only accessed on demand.
 *
 * <p>As of 7.0, this interface also allows for exposing additional object
 * types for dependency injection through implementing a pair of methods:
 * {@link #getObject(Class)} as well as {@link #supportsType(Class)}.
 * The primary {@link #getObjectType()} will be exposed for regular access.
 * Only if a specific type is requested, additional types are considered.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @param <T> the bean type
 * @see #isPrototype()
 * @see #isSingleton()
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * Return an instance of the given type, if supported by this factory.
	 * <p>By default, this supports the primary type exposed by the factory, as
	 * indicated by {@link #getObjectType()} and returned by {@link #getObject()}.
	 * Specific factories may support additional types for dependency injection.
	 * @param type the requested type
	 * @return a corresponding instance managed by this factory,
	 * or {@code null} if none available
	 * @throws Exception in case of creation errors
	 * @since 7.0
	 * @see #getObject()
	 * @see #supportsType(Class)
	 */
	@SuppressWarnings("unchecked")
	default <S> @Nullable S getObject(Class<S> type) throws Exception{
		Class<?> objectType = getObjectType();
		return (objectType != null && type.isAssignableFrom(objectType) ? (S) getObject() : null);
	}

	/**
	 * Determine whether this factory supports the requested type.
	 * <p>By default, this supports the primary type exposed by the factory, as
	 * indicated by {@link #getObjectType()}. Specific factories may support
	 * additional types for dependency injection.
	 * @param type the requested type
	 * @return {@code true} if {@link #getObject(Class)} is able to
	 * return a corresponding instance, {@code false} otherwise
	 * @since 7.0
	 * @see #getObject(Class)
	 * @see #getObjectType()
	 */
	default boolean supportsType(Class<?> type) {
		Class<?> objectType = getObjectType();
		return (objectType != null && type.isAssignableFrom(objectType));
	}

	/**
	 * Is the object managed by this factory a prototype? That is,
	 * will {@link #getObject()} always return an independent instance?
	 * <p>The prototype status of the FactoryBean itself will generally
	 * be provided by the owning {@link BeanFactory}; usually, it has to be
	 * defined as singleton there.
	 * <p>This method is supposed to strictly check for independent instances;
	 * it should not return {@code true} for scoped objects or other
	 * kinds of non-singleton, non-independent objects. For this reason,
	 * this is not simply the inverted form of {@link #isSingleton()}.
	 * <p>The default implementation returns {@code false}.
	 * @return whether the exposed object is a prototype
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	default boolean isPrototype() {
		return false;
	}

	/**
	 * Does this FactoryBean expect eager initialization, that is,
	 * eagerly initialize itself as well as expect eager initialization
	 * of its singleton object (if any)?
	 * <p>A standard FactoryBean is not expected to initialize eagerly:
	 * Its {@link #getObject()} will only be called for actual access, even
	 * in case of a singleton object. Returning {@code true} from this
	 * method suggests that {@link #getObject()} should be called eagerly,
	 * also applying post-processors eagerly. This may make sense in case
	 * of a {@link #isSingleton() singleton} object, in particular if
	 * post-processors expect to be applied on startup.
	 * <p>The default implementation returns {@code false}.
	 * @return whether eager initialization applies
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
