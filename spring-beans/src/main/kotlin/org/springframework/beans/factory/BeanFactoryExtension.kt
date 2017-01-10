package org.springframework.beans.factory

import kotlin.reflect.KClass

/**
 * Extension for [BeanFactory] providing [KClass] based API.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
object BeanFactoryExtension {

	/**
	 * @see BeanFactory.getBean(Class<T>)
	 */
	fun <T : Any> BeanFactory.getBean(requiredType: KClass<T>) = getBean(requiredType.java)

	/**
	 * @see BeanFactory.getBean(Class<T>)
	 */
	inline fun <reified T : Any> BeanFactory.getBean() = getBean(T::class.java)

	/**
	 * @see BeanFactory.getBean(String, Class<T>)
	 */
	fun <T : Any> BeanFactory.getBean(name: String, requiredType: KClass<T>) =
			getBean(name, requiredType.java)

	/**
	 * @see BeanFactory.getBean(String, Class<T>)
	 */
	inline fun <reified T : Any> BeanFactory.getBean(name: String) =
			getBean(name, T::class.java)

	/**
	 * @see BeanFactory.getBean(Class<T>, Object...)
	 */
	fun <T : Any> BeanFactory.getBean(requiredType: KClass<T>, vararg args:Any) =
			getBean(requiredType.java, *args)

	/**
	 * @see BeanFactory.getBean(Class<T>, Object...)
	 */
	inline fun <reified T : Any> BeanFactory.getBean(vararg args:Any) =
			getBean(T::class.java, *args)

}
