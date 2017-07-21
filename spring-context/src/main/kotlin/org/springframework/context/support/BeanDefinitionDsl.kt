/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support

import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.core.env.ConfigurableEnvironment
import java.util.function.Supplier

/**
 * Class implementing functional bean definition Kotlin DSL.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
open class BeanDefinitionDsl(val condition: (ConfigurableEnvironment) -> Boolean = { true }) : (GenericApplicationContext) -> Unit {

	protected val registrations = arrayListOf<(GenericApplicationContext) -> Unit>()
	
	protected val children = arrayListOf<BeanDefinitionDsl>()
	
	enum class Scope {
		SINGLETON,
		PROTOTYPE
	}

	class BeanDefinitionContext(val context: GenericApplicationContext) {

		
		inline fun <reified T : Any> ref(name: String? = null) : T = when (name) {
			null -> context.getBean(T::class.java)
			else -> context.getBean(name, T::class.java)
		}

		/**
		 * Get the [ConfigurableEnvironment] associated to the underlying [GenericApplicationContext].
		 */
		val env : ConfigurableEnvironment
			get() = context.environment
		
	}

	/**
	 * Declare a bean definition from the given bean class which can be inferred when possible.
	 * 
	 * @See GenericApplicationContext.registerBean
	 */
	inline fun <reified T : Any> bean(name: String? = null,
									  scope: Scope? = null,
									  isLazyInit: Boolean? = null,
									  isPrimary: Boolean? = null,
									  isAutowireCandidate: Boolean? = null) {
		
		registrations.add {
			val customizer = BeanDefinitionCustomizer { bd -> 
				scope?.let { bd.scope = scope.name.toLowerCase() }
				isLazyInit?.let { bd.isLazyInit = isLazyInit }
				isPrimary?.let { bd.isPrimary = isPrimary }
				isAutowireCandidate?.let { bd.isAutowireCandidate = isAutowireCandidate }
			}
			
			when (name) {
				null -> it.registerBean(T::class.java, customizer)
				else -> it.registerBean(name, T::class.java, customizer)
			}
		}
	}

	/**
	 * Declare a bean definition using the given supplier for obtaining a new instance.
	 *
	 * @See GenericApplicationContext.registerBean
	 */
	inline fun <reified T : Any> bean(name: String? = null,
									  scope: Scope? = null,
									  isLazyInit: Boolean? = null,
									  isPrimary: Boolean? = null,
									  isAutowireCandidate: Boolean? = null,
									  crossinline function: (BeanDefinitionContext) -> T) {
		
		val customizer = BeanDefinitionCustomizer { bd ->
			scope?.let { bd.scope = scope.name.toLowerCase() }
			isLazyInit?.let { bd.isLazyInit = isLazyInit }
			isPrimary?.let { bd.isPrimary = isPrimary }
			isAutowireCandidate?.let { bd.isAutowireCandidate = isAutowireCandidate }
		}
		
		registrations.add {
			val beanContext = BeanDefinitionContext(it)
			when (name) {
				null -> it.registerBean(T::class.java, Supplier { function.invoke(beanContext) }, customizer)
				else -> it.registerBean(name, T::class.java, Supplier { function.invoke(beanContext) }, customizer)
			}
		}
	}

	/**
	 * Take in account bean definitions enclosed in the provided lambda only when the
	 * specified profile is active.
	 */
	fun profile(profile: String, init: BeanDefinitionDsl.() -> Unit): BeanDefinitionDsl {
		val beans = BeanDefinitionDsl({ it.activeProfiles.contains(profile) })
		beans.init()
		children.add(beans)
		return beans
	}

	/**
	 * Take in account bean definitions enclosed in the provided lambda only when the
	 * specified environment-based predicate is true.
	 */
	fun environment(condition: (ConfigurableEnvironment) -> Boolean, init: BeanDefinitionDsl.() -> Unit): BeanDefinitionDsl {
		val beans = BeanDefinitionDsl(condition::invoke)
		beans.init()
		children.add(beans)
		return beans
	}

	override fun invoke(context: GenericApplicationContext) {
		for (registration in registrations) {
			if (condition.invoke(context.environment)) {
				registration.invoke(context)
			}
		}
		for (child in children) {
			child.invoke(context)
		}
	}
}

/**
 * Functional bean definition Kotlin DSL.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun beans(init: BeanDefinitionDsl.() -> Unit): BeanDefinitionDsl {
	val beans = BeanDefinitionDsl()
	beans.init()
	return beans
}
