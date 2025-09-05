/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans.factory

import org.springframework.beans.factory.BeanRegistry.SupplierContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.ResolvableType
import org.springframework.core.env.Environment

/**
 * Contract for registering programmatically beans.
 *
 * Typically imported with an `@Import` annotation on `@Configuration` classes.
 * ```
 * @Configuration
 * @Import(MyBeanRegistrar::class)
 * class MyConfiguration {
 * }
 * ```
 *
 * In Kotlin, a bean registrar is typically created with a `BeanRegistrarDsl` to register
 * beans programmatically in a concise and flexible way.
 * ```
 * class MyBeanRegistrar : BeanRegistrarDsl({
 * 	   registerBean<Foo>()
 * 	   registerBean(
 *         name = "bar",
 *         prototype = true,
 *         lazyInit = true,
 *         description = "Custom description") {
 *             Bar(bean<Foo>())
 *     }
 *     profile("baz") {
 *         registerBean { Baz("Hello World!") }
 *     }
 * })
 * ```
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
@BeanRegistrarDslMarker
open class BeanRegistrarDsl(private val init: BeanRegistrarDsl.() -> Unit): BeanRegistrar {

	@PublishedApi
	internal lateinit var registry: BeanRegistry

	/**
	 * The environment that can be used to get the active profile or some properties.
	 */
	lateinit var env: Environment


	/**
	 * Apply the nested block if the given profile expression matches the
	 * active profiles.
	 *
	 * A profile expression may contain a simple profile name (for example
	 * `"production"`) or a compound expression. A compound expression allows
	 * for more complicated profile logic to be expressed, for example
	 * `"production & cloud"`.
	 *
	 * The following operators are supported in profile expressions:
	 *  - `!` - A logical *NOT* of the profile name or compound expression
	 *  - `&` - A logical *AND* of the profile names or compound expressions
	 *  - `|` - A logical *OR* of the profile names or compound expressions
	 *
	 * Please note that the `&` and `|` operators may not be mixed
	 * without using parentheses. For example, `"a & b | c"` is not a valid
	 * expression: it must be expressed as `"(a & b) | c"` or `"a & (b | c)"`.
	 * @param expression the profile expressions to evaluate
	 */
	fun profile(expression: String, init: BeanRegistrarDsl.() -> Unit) {
		if (env.matchesProfiles(expression)) {
			init()
		}
	}

	/**
	 * Register beans using the given [BeanRegistrar].
	 * @param registrar the bean registrar that will be called to register
	 * additional beans
	 */
	fun register(registrar: BeanRegistrar) {
		return registry.register(registrar)
	}

	/**
	 * Given a name, register an alias for it.
	 * @param name the canonical name
	 * @param alias the alias to be registered
	 * @throws IllegalStateException if the alias is already in use
	 * and may not be overridden
	 */
	fun registerAlias(name: String, alias: String) {
		registry.registerAlias(name, alias);
	}

	/**
	 * Register a bean of type [T] which will be instantiated using the
	 * related [resolvable constructor]
	 * [org.springframework.beans.BeanUtils.getResolvableConstructor] if any.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any> registerBean(name: String,
											  autowirable: Boolean = true,
											  backgroundInit: Boolean = false,
											  description: String? = null,
											  fallback: Boolean = false,
											  infrastructure: Boolean = false,
											  lazyInit: Boolean = false,
											  order: Int? = null,
											  primary: Boolean = false,
											  prototype: Boolean = false) {

		val customizer: (BeanRegistry.Spec<T>) -> Unit = {
			if (!autowirable) {
				it.notAutowirable()
			}
			if (backgroundInit) {
				it.backgroundInit()
			}
			if (description != null) {
				it.description(description)
			}
			if (fallback) {
				it.fallback()
			}
			if (infrastructure) {
				it.infrastructure()
			}
			if (lazyInit) {
				it.lazyInit()
			}
			if (order != null) {
				it.order(order)
			}
			if (primary) {
				it.primary()
			}
			if (prototype) {
				it.prototype()
			}
			val resolvableType = ResolvableType.forType(object: ParameterizedTypeReference<T>() {});
			if (resolvableType.hasGenerics()) {
				it.targetType(resolvableType)
			}
		}
		registry.registerBean(name, T::class.java, customizer)
	}

	/**
	 * Register a bean of type [T] which will be instantiated using the
	 * related [resolvable constructor]
	 * [org.springframework.beans.BeanUtils.getResolvableConstructor]
	 * if any.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 * @return the generated bean name
	 */
	inline fun <reified T : Any> registerBean(autowirable: Boolean = true,
											  backgroundInit: Boolean = false,
											  description: String? = null,
											  fallback: Boolean = false,
											  infrastructure: Boolean = false,
											  lazyInit: Boolean = false,
											  order: Int? = null,
											  primary: Boolean = false,
											  prototype: Boolean = false): String {

		val customizer: (BeanRegistry.Spec<T>) -> Unit = {
			if (!autowirable) {
				it.notAutowirable()
			}
			if (backgroundInit) {
				it.backgroundInit()
			}
			if (description != null) {
				it.description(description)
			}
			if (fallback) {
				it.fallback()
			}
			if (infrastructure) {
				it.infrastructure()
			}
			if (lazyInit) {
				it.lazyInit()
			}
			if (order != null) {
				it.order(order)
			}
			if (primary) {
				it.primary()
			}
			if (prototype) {
				it.prototype()
			}
			val resolvableType = ResolvableType.forType(object: ParameterizedTypeReference<T>() {});
			if (resolvableType.hasGenerics()) {
				it.targetType(resolvableType)
			}
		}
		return registry.registerBean(T::class.java, customizer)
	}

	/**
	 * Register a bean of type [T] which will be instantiated using the
	 * provided [supplier].
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 * @param supplier the supplier to construct a bean instance
	 */
	inline fun <reified T : Any> registerBean(name: String,
											  autowirable: Boolean = true,
											  backgroundInit: Boolean = false,
											  description: String? = null,
											  fallback: Boolean = false,
											  infrastructure: Boolean = false,
											  lazyInit: Boolean = false,
											  order: Int? = null,
											  primary: Boolean = false,
											  prototype: Boolean = false,
											  crossinline supplier: (SupplierContextDsl<T>.() -> T)) {

		val customizer: (BeanRegistry.Spec<T>) -> Unit = {
			if (!autowirable) {
				it.notAutowirable()
			}
			if (backgroundInit) {
				it.backgroundInit()
			}
			if (description != null) {
				it.description(description)
			}
			if (fallback) {
				it.fallback()
			}
			if (infrastructure) {
				it.infrastructure()
			}
			if (lazyInit) {
				it.lazyInit()
			}
			if (order != null) {
				it.order(order)
			}
			if (primary) {
				it.primary()
			}
			if (prototype) {
				it.prototype()
			}
			it.supplier {
				SupplierContextDsl<T>(it, env).supplier()
			}
			val resolvableType = ResolvableType.forType(object: ParameterizedTypeReference<T>() {});
			if (resolvableType.hasGenerics()) {
				it.targetType(resolvableType)
			}
		}
		registry.registerBean(name, T::class.java, customizer)
	}

	inline fun <reified T : Any> registerBean(autowirable: Boolean = true,
											  backgroundInit: Boolean = false,
											  description: String? = null,
											  fallback: Boolean = false,
											  infrastructure: Boolean = false,
											  lazyInit: Boolean = false,
											  order: Int? = null,
											  primary: Boolean = false,
											  prototype: Boolean = false,
											  crossinline supplier: (SupplierContextDsl<T>.() -> T)): String {
		/**
		 * Register a bean of type [T] which will be instantiated using the
		 * provided [supplier].
		 * @param T the bean type
		 * @param autowirable set whether this bean is a candidate for getting
		 * autowired into some other bean
		 * @param backgroundInit set whether this bean allows for instantiation
		 * on a background thread
		 * @param description a human-readable description of this bean
		 * @param fallback set whether this bean is a fallback autowire candidate
		 * @param infrastructure set whether this bean has an infrastructure role,
		 * meaning it has no relevance to the end-user
		 * @param lazyInit set whether this bean is lazily initialized
		 * @param order the sort order of this bean
		 * @param primary set whether this bean is a primary autowire candidate
		 * @param prototype set whether this bean has a prototype scope
		 * @param supplier the supplier to construct a bean instance
		 */

		val customizer: (BeanRegistry.Spec<T>) -> Unit = {
			if (!autowirable) {
				it.notAutowirable()
			}
			if (backgroundInit) {
				it.backgroundInit()
			}
			if (description != null) {
				it.description(description)
			}
			if (infrastructure) {
				it.infrastructure()
			}
			if (fallback) {
				it.fallback()
			}
			if (lazyInit) {
				it.lazyInit()
			}
			if (order != null) {
				it.order(order)
			}
			if (primary) {
				it.primary()
			}
			if (prototype) {
				it.prototype()
			}
			it.supplier {
				SupplierContextDsl<T>(it, env).supplier()
			}
			val resolvableType = ResolvableType.forType(object: ParameterizedTypeReference<T>() {});
			if (resolvableType.hasGenerics()) {
				it.targetType(resolvableType)
			}
		}
		return registry.registerBean(T::class.java, customizer)
	}

	// Function with 0 parameter

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f].
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any> registerBean(
		crossinline f: () -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke()
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f].
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any> registerBean(
		crossinline f: () -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke()
		}

	// Function with 1 parameter

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any> registerBean(
		crossinline f: (A) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any> registerBean(
		crossinline f: (A) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean())
		}

	// Function with 2 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any> registerBean(
		crossinline f: (A, B) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any> registerBean(
		crossinline f: (A, B) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean())
		}

	// Function with 3 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any> registerBean(
		crossinline f: (A, B, C) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any> registerBean(
		crossinline f: (A, B, C) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean())
		}

	// Function with 4 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any> registerBean(
		crossinline f: (A, B, C, D) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any> registerBean(
		crossinline f: (A, B, C, D) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean())
		}

	// Function with 5 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any> registerBean(
		crossinline f: (A, B, C, D, E) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any> registerBean(
		crossinline f: (A, B, C, D, E) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean())
		}

	// Function with 6 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any> registerBean(
		crossinline f: (A, B, C, D, E, F) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any> registerBean(
		crossinline f: (A, B, C, D, E, F) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean())
		}

	// Function with 7 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any, reified G: Any> registerBean(
		crossinline f: (A, B, C, D, E, F, G) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any, reified G: Any> registerBean(
		crossinline f: (A, B, C, D, E, F, G) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean(), bean())
		}

	// Function with 8 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any, reified G: Any, reified H: Any> registerBean(
		crossinline f: (A, B, C, D, E, F, G, H) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean(), bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any, reified G: Any, reified H: Any> registerBean(
		crossinline f: (A, B, C, D, E, F, G, H) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean(), bean(), bean())
		}

	// Function with 9 parameters

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any, reified G: Any, reified H: Any, reified I: Any> registerBean(
		crossinline f: (A, B, C, D, E, F, G, H, I) -> T,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean(), bean(), bean(), bean())
		}

	/**
	 * Register a bean of type [T] which will be instantiated by invoking the
	 * provided [function][f] with its parameters autowired by type.
	 * @param T the bean type
	 * @param name the name of the bean
	 * @param autowirable set whether this bean is a candidate for getting
	 * autowired into some other bean
	 * @param backgroundInit set whether this bean allows for instantiation
	 * on a background thread
	 * @param description a human-readable description of this bean
	 * @param fallback set whether this bean is a fallback autowire candidate
	 * @param infrastructure set whether this bean has an infrastructure role,
	 * meaning it has no relevance to the end-user
	 * @param lazyInit set whether this bean is lazily initialized
	 * @param order the sort order of this bean
	 * @param primary set whether this bean is a primary autowire candidate
	 * @param prototype set whether this bean has a prototype scope
	 */
	inline fun <reified T : Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any,
			reified F: Any, reified G: Any, reified H: Any, reified I: Any> registerBean(
		crossinline f: (A, B, C, D, E, F, G, H, I) -> T,
		name: String,
		autowirable: Boolean = true,
		backgroundInit: Boolean = false,
		description: String? = null,
		fallback: Boolean = false,
		infrastructure: Boolean = false,
		lazyInit: Boolean = false,
		order: Int? = null,
		primary: Boolean = false,
		prototype: Boolean = false) =
		registerBean(name, autowirable, backgroundInit, description, fallback, infrastructure, lazyInit, order, primary, prototype) {
			f.invoke(bean(), bean(), bean(), bean(), bean(), bean(), bean(), bean(), bean())
		}


	/**
	 * Context available from the bean instance supplier designed to give access
	 * to bean dependencies.
	 */
	@BeanRegistrarDslMarker
	open class SupplierContextDsl<T>(@PublishedApi internal val context: SupplierContext, val env: Environment) {

		/**
		 * Return the bean instance that uniquely matches the given object type,
		 * and potentially the name if provided, if any.
		 * @param T the bean type
		 * @param name the name of the bean
		 */
		inline fun <reified T : Any> bean(name: String? = null) : T = when (name) {
			null -> beanProvider<T>().getObject()
			else -> context.bean(name, T::class.java)
		}

		/**
		 * Return a provider for the specified bean, allowing for lazy on-demand
		 * retrieval of instances, including availability and uniqueness options.
		 * @param T type the bean must match; can be an interface or superclass
		 * @return a corresponding provider handle
		 */
		inline fun <reified T : Any> beanProvider() : ObjectProvider<T> =
			context.beanProvider(ResolvableType.forType((object : ParameterizedTypeReference<T>() {}).type))
	}

	override fun register(registry: BeanRegistry, env: Environment) {
		this.registry = registry
		this.env = env
		init()
	}

}

@DslMarker
internal annotation class BeanRegistrarDslMarker
