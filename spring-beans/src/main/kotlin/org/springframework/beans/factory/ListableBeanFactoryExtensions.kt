/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory

/**
 * Extension for [ListableBeanFactory.getBeanNamesForType] providing a
 * `getBeanNamesForType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ListableBeanFactory.getBeanNamesForType(includeNonSingletons: Boolean = true,
		allowEagerInit: Boolean = true): Array<out String> =
		getBeanNamesForType(T::class.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeansOfType] providing a `getBeansOfType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ListableBeanFactory.getBeansOfType(includeNonSingletons: Boolean = true,
		allowEagerInit: Boolean = true): Map<String, T> =
		getBeansOfType(T::class.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeanNamesForAnnotation] providing a
 * `getBeansOfType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.getBeanNamesForAnnotation(): Array<out String> =
		getBeanNamesForAnnotation(T::class.java)

/**
 * Extension for [ListableBeanFactory.getBeansWithAnnotation] providing a
 * `getBeansWithAnnotation<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.getBeansWithAnnotation(): Map<String, Any> =
		getBeansWithAnnotation(T::class.java)

/**
 * Extension for [ListableBeanFactory.findAnnotationOnBean] providing a
 * `findAnnotationOnBean<Foo>("foo")` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String): Annotation? =
		findAnnotationOnBean(beanName, T::class.java)

