package org.springframework.beans.factory

import kotlin.reflect.KClass


/**
 * Extension for [ListableBeanFactory.getBeanNamesForType] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ListableBeanFactory.getBeanNamesForType(type: KClass<T>,
		includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Array<out String> =
	getBeanNamesForType(type.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeanNamesForType] providing a `getBeanNamesForType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ListableBeanFactory.getBeanNamesForType(includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Array<out String> =
	getBeanNamesForType(T::class.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeansOfType] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ListableBeanFactory.getBeansOfType(type: KClass<T>, includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Map<String, Any> =
	getBeansOfType(type.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeansOfType] providing a `getBeansOfType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ListableBeanFactory.getBeansOfType(includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Map<String, Any> =
				getBeansOfType(T::class.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeanNamesForAnnotation] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Annotation> ListableBeanFactory.getBeanNamesForAnnotation(type: KClass<T>): Array<out String> =
		getBeanNamesForAnnotation(type.java)

/**
 * Extension for [ListableBeanFactory.getBeanNamesForAnnotation] providing a `getBeansOfType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.getBeanNamesForAnnotation(): Array<out String> =
		getBeanNamesForAnnotation(T::class.java)

/**
 * Extension for [ListableBeanFactory.getBeansWithAnnotation] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Annotation> ListableBeanFactory.getBeansWithAnnotation(type: KClass<T>): MutableMap<String, Any> =
		getBeansWithAnnotation(type.java)

/**
 * Extension for [ListableBeanFactory.getBeansWithAnnotation] providing a `getBeansWithAnnotation<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.getBeansWithAnnotation(): MutableMap<String, Any> =
		getBeansWithAnnotation(T::class.java)

/**
 * Extension for [ListableBeanFactory.findAnnotationOnBean] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String, type: KClass<T>): Annotation? =
		findAnnotationOnBean(beanName, type.java)

/**
 * Extension for [ListableBeanFactory.findAnnotationOnBean] providing a `findAnnotationOnBean<Foo>("foo")` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String): Annotation? =
		findAnnotationOnBean(beanName, T::class.java)

