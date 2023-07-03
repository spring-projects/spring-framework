/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.beans.factory.getBeanProvider
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils
import org.springframework.context.ApplicationContextInitializer
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Profiles
import java.util.function.Supplier

/**
 * Functional bean definition Kotlin DSL.
 *
 * Example:
 *
 * ```
 * beans {
 * 	bean<UserHandler>()
 * 	bean<Routes>()
 * 	bean<WebHandler>("webHandler") {
 * 	RouterFunctions.toWebHandler(
 * 		ref<Routes>().router(),
 * 		HandlerStrategies.builder().viewResolver(ref()).build())
 * 	}
 * 	bean("messageSource") {
 * 		ReloadableResourceBundleMessageSource().apply {
 * 			setBasename("messages")
 * 			setDefaultEncoding("UTF-8")
 * 		}
 * 	}
 * 	bean {
 * 		val prefix = "classpath:/templates/"
 * 		val suffix = ".mustache"
 * 		val loader = MustacheResourceTemplateLoader(prefix, suffix)
 * 		MustacheViewResolver(Mustache.compiler().withLoader(loader)).apply {
 * 			setPrefix(prefix)
 * 			setSuffix(suffix)
 * 		}
 * 	}
 * 	profile("foo") {
 * 		bean<Foo>()
 * 	}
 * }
 * ```
 *
 * @author Sebastien Deleuze
 * @see BeanDefinitionDsl
 * @since 5.0
 */
fun beans(init: BeanDefinitionDsl.() -> Unit) = BeanDefinitionDsl(init)

/**
 * Class implementing functional bean definition Kotlin DSL.
 *
 * @constructor Create a new bean definition DSL.
 * @param condition the predicate to fulfill in order to take in account the inner
 * bean definition block
 * @author Sebastien Deleuze
 * @since 5.0
 */
open class BeanDefinitionDsl internal constructor (private val init: BeanDefinitionDsl.() -> Unit,
							 private val condition: (ConfigurableEnvironment) -> Boolean = { true })
	: ApplicationContextInitializer<GenericApplicationContext> {

	@PublishedApi
	internal val children = arrayListOf<BeanDefinitionDsl>()

	/**
	 * @see BeanSupplierContext
	 */
	@PublishedApi
	internal lateinit var context: GenericApplicationContext

	/**
	 * Shortcut for `context.environment`
	 * @since 5.1
	 */
	val env : ConfigurableEnvironment
		get() = context.environment

	/**
	 * Scope enum constants.
	 */
	enum class Scope {

		/**
		 * Scope constant for the standard singleton scope
		 * @see org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON
		 */
		SINGLETON,

		/**
		 * Scope constant for the standard prototype scope
		 * @see org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
		 */
		PROTOTYPE
	}

	/**
	 * Role enum constants.
	 */
	enum class Role {

		/**
		 * Role hint indicating that a [BeanDefinition] is a major part
		 * of the application. Typically corresponds to a user-defined bean.
		 * @see org.springframework.beans.factory.config.BeanDefinition.ROLE_APPLICATION
		 */
		APPLICATION,

		/**
		 * Role hint indicating that a [BeanDefinition] is a supporting
		 * part of some larger configuration, typically an outer
		 * [org.springframework.beans.factory.parsing.ComponentDefinition].
		 * [SUPPORT] beans are considered important enough to be aware of
		 * when looking more closely at a particular
		 * [org.springframework.beans.factory.parsing.ComponentDefinition],
		 * but not when looking at the overall configuration of an application.
		 * @see org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT
		 */
		SUPPORT,

		/**
		 * Role hint indicating that a [BeanDefinition] is providing an
		 * entirely background role and has no relevance to the end-user. This hint is
		 * used when registering beans that are completely part of the internal workings
		 * of a [org.springframework.beans.factory.parsing.ComponentDefinition].
		 * @see org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE
		 */
		INFRASTRUCTURE
	}

	/**
	 * Declare a bean definition from the given bean class which can be inferred when possible.
	 *
	 * <p>The preferred constructor (Kotlin primary constructor and standard public constructors)
	 * are evaluated for autowiring before falling back to default instantiation.
	 *
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 */
	inline fun <reified T : Any> bean(name: String? = null,
									  scope: Scope? = null,
									  isLazyInit: Boolean? = null,
									  isPrimary: Boolean? = null,
									  isAutowireCandidate: Boolean? = null,
									  initMethodName: String? = null,
									  destroyMethodName: String? = null,
									  description: String? = null,
									  role: Role? = null) {

		val customizer = BeanDefinitionCustomizer { bd ->
			scope?.let { bd.scope = scope.name.toLowerCase() }
			isLazyInit?.let { bd.isLazyInit = isLazyInit }
			isPrimary?.let { bd.isPrimary = isPrimary }
			isAutowireCandidate?.let { bd.isAutowireCandidate = isAutowireCandidate }
			initMethodName?.let { bd.initMethodName = initMethodName }
			destroyMethodName?.let { bd.destroyMethodName = destroyMethodName }
			description?.let { bd.description = description }
			role?. let { bd.role = role.ordinal }
		}

		val beanName = name ?: BeanDefinitionReaderUtils.uniqueBeanName(T::class.java.name, context);
		context.registerBean(beanName, T::class.java, customizer)
	}

	/**
	 * Declare a bean definition using the given supplier for obtaining a new instance.
	 *
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @param function the bean supplier function
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 */
	inline fun <reified T : Any> bean(name: String? = null,
									  scope: Scope? = null,
									  isLazyInit: Boolean? = null,
									  isPrimary: Boolean? = null,
									  isAutowireCandidate: Boolean? = null,
									  initMethodName: String? = null,
									  destroyMethodName: String? = null,
									  description: String? = null,
									  role: Role? = null,
									  crossinline function: BeanSupplierContext.() -> T) {

		val customizer = BeanDefinitionCustomizer { bd ->
			scope?.let { bd.scope = scope.name.toLowerCase() }
			isLazyInit?.let { bd.isLazyInit = isLazyInit }
			isPrimary?.let { bd.isPrimary = isPrimary }
			isAutowireCandidate?.let { bd.isAutowireCandidate = isAutowireCandidate }
			initMethodName?.let { bd.initMethodName = initMethodName }
			destroyMethodName?.let { bd.destroyMethodName = destroyMethodName }
			description?.let { bd.description = description }
			role?. let { bd.role = role.ordinal }
		}


		val beanName = name ?: BeanDefinitionReaderUtils.uniqueBeanName(T::class.java.name, context);
		context.registerBean(beanName, T::class.java, Supplier { function.invoke(BeanSupplierContext(context)) }, customizer)
	}

	/**
	 * Declare a bean definition using the given callable reference with no parameter
	 * for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2.3
	 */
	inline fun <reified T: Any>
			bean(crossinline f: () -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke()
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 1 parameter
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any>
			bean(crossinline f: (A) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 2 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any>
			bean(crossinline f: (A, B) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 3 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any>
			bean(crossinline f: (A, B, C) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 4 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any>
			bean(crossinline f: (A, B, C, D) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 5 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any>
			bean(crossinline f: (A, B, C, D, E) -> T,
				 name: String? = null,
								   scope: BeanDefinitionDsl.Scope? = null,
								   isLazyInit: Boolean? = null,
								   isPrimary: Boolean? = null,
								   isAutowireCandidate: Boolean? = null,
								   initMethodName: String? = null,
								   destroyMethodName: String? = null,
								   description: String? = null,
								   role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 6 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any>
			bean(crossinline f: (A, B, C, D, E, F) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 7 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any>
			bean(crossinline f: (A, B, C, D, E, F, G) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 8 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 9 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 10 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 11 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 12 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 13 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 14 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 15 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 16 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any, reified P: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 17 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any, reified P: Any, reified Q: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 18 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any, reified P: Any, reified Q: Any, reified R: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 19 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any, reified P: Any, reified Q: Any, reified R: Any, reified S: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 20 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any, reified P: Any, reified Q: Any, reified R: Any, reified S: Any, reified U: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 21 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any, reified P: Any, reified Q: Any, reified R: Any, reified S: Any, reified U: Any,
			reified V: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Declare a bean definition using the given callable reference with 22 parameters
	 * autowired by type for obtaining a new instance.
	 *
	 * @param f the callable reference
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param initMethodName Set the name of the initializer method
	 * @param destroyMethodName Set the name of the destroy method
	 * @param description Set a human-readable description of this bean definition
	 * @param role Set the role hint for this bean definition
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @since 5.2
	 */
	inline fun <reified T: Any, reified A: Any, reified B: Any, reified C: Any, reified D: Any, reified E: Any, reified F: Any,
			reified G: Any, reified H: Any, reified I: Any, reified J: Any, reified K: Any, reified L: Any, reified M: Any,
			reified N: Any, reified O: Any, reified P: Any, reified Q: Any, reified R: Any, reified S: Any, reified U: Any,
			reified V: Any, reified W: Any>
			bean(crossinline f: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W) -> T,
				 name: String? = null,
				 scope: BeanDefinitionDsl.Scope? = null,
				 isLazyInit: Boolean? = null,
				 isPrimary: Boolean? = null,
				 isAutowireCandidate: Boolean? = null,
				 initMethodName: String? = null,
				 destroyMethodName: String? = null,
				 description: String? = null,
				 role: BeanDefinitionDsl.Role? = null) {

		bean(name, scope, isLazyInit, isPrimary, isAutowireCandidate, initMethodName, destroyMethodName, description, role) {
			f.invoke(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref())
		}
	}

	/**
	 * Limit access to `ref()` and `provider()` to bean supplier lambdas.
	 * @since 5.2
	 */
	open class BeanSupplierContext(@PublishedApi internal val context: GenericApplicationContext) {

		/**
		 * Get a reference to the bean by type or type + name with the syntax
		 * `ref<Foo>()` or `ref<Foo>("foo")`. When leveraging Kotlin type inference
		 * it could be as short as `ref()` or `ref("foo")`.
		 * @param name the name of the bean to retrieve
		 * @param T type the bean must match, can be an interface or superclass
		 */
		inline fun <reified T : Any> ref(name: String? = null) : T = when (name) {
			null -> context.getBean(T::class.java)
			else -> context.getBean(name, T::class.java)
		}

		/**
		 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
		 * of instances, including availability and uniqueness options.
		 * @see org.springframework.beans.factory.BeanFactory.getBeanProvider
		 */
		inline fun <reified T : Any> provider() : ObjectProvider<T> = context.getBeanProvider()

	}

	/**
	 * Take in account bean definitions enclosed in the provided lambda when the
	 * profile is accepted.
	 * @see org.springframework.core.env.Profiles.of
	 */
	fun profile(profile: String, init: BeanDefinitionDsl.() -> Unit) {
		val beans = BeanDefinitionDsl(init, { it.acceptsProfiles(Profiles.of(profile)) })
		children.add(beans)
	}

	/**
	 * Take in account bean definitions enclosed in the provided lambda only when the
	 * specified environment-based predicate is true.
	 * @param condition the predicate to fulfill in order to take in account the inner
	 * bean definition block
	 */
	fun environment(condition: ConfigurableEnvironment.() -> Boolean,
					init: BeanDefinitionDsl.() -> Unit) {
		val beans = BeanDefinitionDsl(init, condition::invoke)
		children.add(beans)
	}

	/**
	 * Register the bean defined via the DSL on the provided application context.
	 * @param context The `ApplicationContext` to use for registering the beans
	 */
	override fun initialize(context: GenericApplicationContext) {
		this.context = context
		init()
		for (child in children) {
			if (child.condition.invoke(context.environment)) {
				child.initialize(context)
			}
		}
	}
}
