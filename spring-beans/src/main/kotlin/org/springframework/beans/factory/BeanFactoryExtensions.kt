/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory

import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.ResolvableType

/**
 * Extension for [BeanFactory.getBean] providing a `getBean<Foo>()` variant.
 * This extension is not subject to type erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @author Yanming Zhou
 * @since 5.0
 */
inline fun <reified T : Any> BeanFactory.getBean(): T =
		getBeanProvider<T>().getObject()

/**
 * Extension for [BeanFactory.getBean] providing a `getBean<Foo>("foo")` variant.
 * This extension is not subject to type erasure and retains actual generic type arguments.
 *
 * @see BeanFactory.getBean(String, Class<T>)
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> BeanFactory.getBean(name: String): T =
		getBean(name, (object : ParameterizedTypeReference<T>() {}))

/**
 * Extension for [BeanFactory.getBean] providing a `getBean<Foo>(arg1, arg2)` variant.
 * This extension is not subject to type erasure and retains actual generic type arguments.
 *
 * @see BeanFactory.getBean(Class<T>, Object...)
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> BeanFactory.getBean(vararg args:Any): T =
		getBeanProvider<T>().getObject(*args)

/**
 * Extension for [BeanFactory.getBeanProvider] providing a `getBeanProvider<Foo>()` variant.
 * This extension is not subject to type erasure and retains actual generic type arguments.
 *
 * @see BeanFactory.getBeanProvider(ResolvableType)
 * @author Sebastien Deleuze
 * @author Yanming Zhou
 * @since 5.1
 */
inline fun <reified T : Any> BeanFactory.getBeanProvider(): ObjectProvider<T> =
		getBeanProvider(object : ParameterizedTypeReference<T>() {})

