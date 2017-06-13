package org.springframework.beans.factory

/**
 * Extension for [ListableBeanFactory.getBeanNamesForType] providing a `getBeanNamesForType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ListableBeanFactory.getBeanNamesForType(includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Array<out String> =
	getBeanNamesForType(T::class.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeansOfType] providing a `getBeansOfType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ListableBeanFactory.getBeansOfType(includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Map<String, Any> =
				getBeansOfType(T::class.java, includeNonSingletons, allowEagerInit)

/**
 * Extension for [ListableBeanFactory.getBeanNamesForAnnotation] providing a `getBeansOfType<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.getBeanNamesForAnnotation(): Array<out String> =
		getBeanNamesForAnnotation(T::class.java)

/**
 * Extension for [ListableBeanFactory.getBeansWithAnnotation] providing a `getBeansWithAnnotation<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.getBeansWithAnnotation(): MutableMap<String, Any> =
		getBeansWithAnnotation(T::class.java)

/**
 * Extension for [ListableBeanFactory.findAnnotationOnBean] providing a `findAnnotationOnBean<Foo>("foo")` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String): Annotation? =
		findAnnotationOnBean(beanName, T::class.java)

