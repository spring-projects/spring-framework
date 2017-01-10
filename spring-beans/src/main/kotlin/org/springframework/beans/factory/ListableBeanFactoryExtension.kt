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
	 * @see ListableBeanFactory.getBeanNamesForType(Class<?>)
	 */
	inline fun <reified T : Any> ListableBeanFactory.getBeanNamesForType() =
			getBeanNamesForType(T::class.java)

	/**
	 * @see ListableBeanFactory.getBeanNamesForType(Class<?>, boolean, boolean)
	 */
	fun <T : Any> ListableBeanFactory.getBeanNamesForType(type: KClass<T>,
			includeNonSingletons: Boolean, allowEagerInit: Boolean) =
					getBeanNamesForType(type.java, includeNonSingletons, allowEagerInit)

	/**
	 * @see ListableBeanFactory.getBeanNamesForType(Class<?>, boolean, boolean)
	 */
	inline fun <reified T : Any> ListableBeanFactory.getBeanNamesForType(includeNonSingletons: Boolean, allowEagerInit: Boolean) =
					getBeanNamesForType(T::class.java, includeNonSingletons, allowEagerInit)

	/**
	 * @see ListableBeanFactory.getBeansOfType(Class<T>)
	 */
	fun <T : Any> ListableBeanFactory.getBeansOfType(type: KClass<T>) =
			getBeansOfType(type.java)

	/**
	 * @see ListableBeanFactory.getBeansOfType(Class<T>)
	 */
	inline fun <reified T : Any> ListableBeanFactory.getBeansOfType() =
			getBeansOfType(T::class.java)

	/**
	 * @see ListableBeanFactory.getBeansOfType(Class<T>, boolean, boolean)
	 */
	fun <T : Any> ListableBeanFactory.getBeansOfType(type: KClass<T>,
			includeNonSingletons: Boolean, allowEagerInit: Boolean) =
					getBeansOfType(type.java, includeNonSingletons, allowEagerInit)

	/**
	 * @see ListableBeanFactory.getBeansOfType(Class<T>, boolean, boolean)
	 */
	inline fun <reified T : Any> ListableBeanFactory.getBeansOfType(includeNonSingletons: Boolean, allowEagerInit: Boolean) =
					getBeansOfType(T::class.java, includeNonSingletons, allowEagerInit)

	/**
	 * @see ListableBeanFactory.getBeanNamesForAnnotation
	 */
	fun <T : Annotation> ListableBeanFactory.getBeanNamesForAnnotation(type: KClass<T>) =
			getBeanNamesForAnnotation(type.java)

	/**
	 * @see ListableBeanFactory.getBeanNamesForAnnotation
	 */
	inline fun <reified T : Annotation> ListableBeanFactory.getBeanNamesForAnnotation() =
			getBeanNamesForAnnotation(T::class.java)

	/**
	 * @see ListableBeanFactory.getBeansWithAnnotation
	 */
	fun <T : Annotation> ListableBeanFactory.getBeansWithAnnotation(type: KClass<T>) =
			getBeansWithAnnotation(type.java)

	/**
	 * @see ListableBeanFactory.getBeansWithAnnotation
	 */
	inline fun <reified T : Annotation> ListableBeanFactory.getBeansWithAnnotation() =
			getBeansWithAnnotation(T::class.java)

	/**
	 * @see ListableBeanFactoryExtension.findAnnotationOnBean
	 */
	fun <T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String, type: KClass<T>) =
			findAnnotationOnBean(beanName, type.java)

	/**
	 * @see ListableBeanFactoryExtension.findAnnotationOnBean
	 */
	inline fun <reified T : Annotation> ListableBeanFactory.findAnnotationOnBean(beanName:String) =
			findAnnotationOnBean(beanName, T::class.java)

}
