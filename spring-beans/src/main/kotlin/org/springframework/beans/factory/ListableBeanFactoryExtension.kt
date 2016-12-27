package org.springframework.beans.factory

import kotlin.reflect.KClass

/**
 * Extension for [ListableBeanFactory] providing [KClass] based API.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
object ListableBeanFactoryExtension {

	/**
	 * @see ListableBeanFactory.getBeanNamesForType(Class<?>)
	 */
	fun <T : Any> ListableBeanFactory.getBeanNamesForType(type: KClass<T>) =
			getBeanNamesForType(type.java)

	/**
	 * @see ListableBeanFactory.getBeanNamesForType(Class<?>, boolean, boolean)
	 */
	fun <T : Any> ListableBeanFactory.getBeanNamesForType(type: KClass<T>,
			includeNonSingletons: Boolean, allowEagerInit: Boolean) =
					getBeanNamesForType(type.java, includeNonSingletons, allowEagerInit)

	/**
	 * @see ListableBeanFactory.getBeansOfType(Class<T>)
	 */
	fun <T : Any> ListableBeanFactory.getBeansOfType(type: KClass<T>) =
			getBeansOfType(type.java)

	/**
	 * @see ListableBeanFactory.getBeansOfType(Class<T>, boolean, boolean)
	 */
	fun <T : Any> ListableBeanFactory.getBeansOfType(type: KClass<T>,
			includeNonSingletons: Boolean, allowEagerInit: Boolean) =
					getBeansOfType(type.java, includeNonSingletons, allowEagerInit)

	/**
	 * @see ListableBeanFactory.getBeanNamesForAnnotation
	 */
	fun <T : Annotation> ListableBeanFactory.getBeanNamesForAnnotation(type: KClass<T>) =
			getBeanNamesForAnnotation(type.java)

	/**
	 * @see ListableBeanFactory.getBeansWithAnnotation
	 */
	fun <T : Annotation> ListableBeanFactory.getBeansWithAnnotation(type: KClass<T>) =
			getBeansWithAnnotation(type.java)

	/**
	 * @see ListableBeanFactoryExtension.findAnnotationOnBean
	 */
	fun <T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String, type: KClass<T>) =
			findAnnotationOnBean(beanName, type.java)

}
