package org.springframework.beans.factory

import kotlin.reflect.KClass


/**
 * Extension for [BeanFactory.getBean] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> BeanFactory.getBean(requiredType: KClass<T>): T = getBean(requiredType.java)

/**
 * Extension for [BeanFactory.getBean] providing a `getBean<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> BeanFactory.getBean(): T = getBean(T::class.java)

/**
 * Extension for [BeanFactory.getBean] providing a [KClass] based variant.
 *
 * @see BeanFactory.getBean(String, Class<T>)
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> BeanFactory.getBean(name: String, requiredType: KClass<T>): T =
		getBean(name, requiredType.java)

/**
 * Extension for [BeanFactory.getBean] providing a `getBean<Foo>("foo")` variant.
 *
 * @see BeanFactory.getBean(String, Class<T>)
 * @author Sebastien Deleuze
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any> BeanFactory.getBean(name: String): T =
		getBean(name, T::class.java)

/**
 * Extension for [BeanFactory.getBean] providing a [KClass] based variant.
 *
 * @see BeanFactory.getBean(Class<T>, Object...)
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> BeanFactory.getBean(requiredType: KClass<T>, vararg args:Any): T =
		getBean(requiredType.java, *args)

/**
 * Extension for [BeanFactory.getBean] providing a `getBean<Foo>(arg1, arg2)` variant.
 *
 * @see BeanFactory.getBean(Class<T>, Object...)
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> BeanFactory.getBean(vararg args:Any): T =
		getBean(T::class.java, *args)
