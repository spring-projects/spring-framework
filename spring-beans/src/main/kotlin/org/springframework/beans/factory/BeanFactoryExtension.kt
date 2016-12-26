package org.springframework.beans.factory

import kotlin.reflect.KClass

/**
 * Extension for [BeanFactory] providing [KClass] based API.
 *
 * @since 5.0
 */
object BeanFactoryExtension {

	/**
	 * @see BeanFactory.getBean(Class<T>)
	 */
	fun <T : Any> BeanFactory.getBean(requiredType: KClass<T>) = getBean(requiredType.java)

	/**
	 * @see BeanFactory.getBean(String, Class<T>)
	 */
	fun <T : Any> BeanFactory.getBean(name: String, requiredType: KClass<T>) =
			getBean(name, requiredType.java)

	/**
	 * @see BeanFactory.getBean(Class<T>, Object...)
	 */
	fun <T : Any> BeanFactory.getBean(requiredType: KClass<T>, vararg args:Any) =
			getBean(requiredType.java, *args)

}
