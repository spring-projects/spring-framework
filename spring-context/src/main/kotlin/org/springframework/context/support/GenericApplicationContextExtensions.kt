/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support

import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.context.ApplicationContext
import java.util.function.Supplier

/**
 * Extension for [GenericApplicationContext.registerBean] providing a
 * `registerBean<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> GenericApplicationContext.registerBean(vararg customizers: BeanDefinitionCustomizer) {
	registerBean(T::class.java, *customizers)
}

/**
 * Extension for [GenericApplicationContext.registerBean] providing a
 * `registerBean<Foo>("foo")` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> GenericApplicationContext.registerBean(beanName: String,
		vararg customizers: BeanDefinitionCustomizer) {
	registerBean(beanName, T::class.java, *customizers)
}

/**
 * Extension for [GenericApplicationContext.registerBean] providing a `registerBean { Foo() }` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> GenericApplicationContext.registerBean(
		vararg customizers: BeanDefinitionCustomizer, crossinline function: (ApplicationContext) -> T) {
	registerBean(T::class.java, Supplier { function.invoke(this) }, *customizers)
}

/**
 * Extension for [GenericApplicationContext.registerBean] providing a
 * `registerBean("foo") { Foo() }` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> GenericApplicationContext.registerBean(name: String,
		vararg customizers: BeanDefinitionCustomizer, crossinline function: (ApplicationContext) -> T) {
	registerBean(name, T::class.java, Supplier { function.invoke(this) }, *customizers)
}

/**
 * Extension for [GenericApplicationContext] allowing `GenericApplicationContext { ... }`
 * style initialization.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun GenericApplicationContext(configure: GenericApplicationContext.() -> Unit) =
		GenericApplicationContext().apply(configure)

