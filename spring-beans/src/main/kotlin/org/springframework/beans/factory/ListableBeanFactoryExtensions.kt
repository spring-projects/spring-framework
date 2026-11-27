/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory

import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.ResolvableType

/**
 * Extension for [ListableBeanFactory.getBeanNamesForType] providing a
 * `getBeanNamesForType<Foo>()` or `getBeanNamesForType<List<Foo>>()` variant.
 *
 * @author Sebastien Deleuze
 * @author Yanming Zhou
 * @since 5.0
 */
inline fun <reified T : Any> ListableBeanFactory.getBeanNamesForType(includeNonSingletons: Boolean = true,
		allowEagerInit: Boolean = true): Array<out String> =
		getBeanNamesForType(ResolvableType.forType(object : ParameterizedTypeReference<T>() {}),
		includeNonSingletons, allowEagerInit)

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
 * `getBeanNamesForAnnotation<Foo>()` variant.
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
inline fun <reified T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String): T? =
		findAnnotationOnBean(beanName, T::class.java)

