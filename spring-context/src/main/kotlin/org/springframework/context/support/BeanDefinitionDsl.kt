/*
 * Copyright 2002-2018 the original author or authors.
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
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.context.ApplicationContextInitializer
import org.springframework.core.env.ConfigurableEnvironment
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
open class BeanDefinitionDsl(private val init: BeanDefinitionDsl.() -> Unit,
							 private val condition: (ConfigurableEnvironment) -> Boolean = { true })
	: ApplicationContextInitializer<GenericApplicationContext> {

	@PublishedApi
	internal val children = arrayListOf<BeanDefinitionDsl>()

	/**
	 * Access to the context for advanced use-cases.
	 * @since 5.1
	 */
	lateinit var context: GenericApplicationContext

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
		 * Scope constant for the standard singleton scope
		 * @see org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
		 */
		PROTOTYPE
	}

	/**
	 * Autowire enum constants.
	 */
	enum class Autowire {

		/**
		 * Autowire constant that indicates no externally defined autowiring
		 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_NO
		 */
		NO,
		/**
		 * Autowire constant that indicates autowiring bean properties by name
		 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_NAME
		 */
		BY_NAME,
		/**
		 * Autowire constant that indicates autowiring bean properties by type
		 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE
		 */
		BY_TYPE,

		/**
		 * Autowire constant that indicates autowiring the greediest constructor that can be satisfied
		 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR
		 */
		CONSTRUCTOR

	}

	/**
	 * Declare a bean definition from the given bean class which can be inferred when possible.
	 *
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param autowireMode Set the autowire mode, `Autowire.CONSTRUCTOR` by default
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 */
	inline fun <reified T : Any> bean(name: String? = null,
									  scope: Scope? = null,
									  isLazyInit: Boolean? = null,
									  isPrimary: Boolean? = null,
									  autowireMode: Autowire = Autowire.CONSTRUCTOR,
									  isAutowireCandidate: Boolean? = null) {

		val customizer = BeanDefinitionCustomizer { bd ->
			scope?.let { bd.scope = scope.name.toLowerCase() }
			isLazyInit?.let { bd.isLazyInit = isLazyInit }
			isPrimary?.let { bd.isPrimary = isPrimary }
			isAutowireCandidate?.let { bd.isAutowireCandidate = isAutowireCandidate }
			if (bd is AbstractBeanDefinition) {
				bd.autowireMode = autowireMode.ordinal
			}
		}

		when (name) {
			null -> context.registerBean(T::class.java, customizer)
			else -> context.registerBean(name, T::class.java, customizer)
		}

	}

	/**
	 * Declare a bean definition using the given supplier for obtaining a new instance.
	 *
	 * @param name the name of the bean
	 * @param scope Override the target scope of this bean, specifying a new scope name.
	 * @param isLazyInit Set whether this bean should be lazily initialized.
	 * @param isPrimary Set whether this bean is a primary autowire candidate.
	 * @param autowireMode Set the autowire mode, `Autowire.NO` by default
	 * @param isAutowireCandidate Set whether this bean is a candidate for getting
	 * autowired into some other bean.
	 * @param function the bean supplier function
	 * @see GenericApplicationContext.registerBean
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 */
	inline fun <reified T : Any> bean(name: String? = null,
									  scope: Scope? = null,
									  isLazyInit: Boolean? = null,
									  isPrimary: Boolean? = null,
									  autowireMode: Autowire = Autowire.NO,
									  isAutowireCandidate: Boolean? = null,
									  crossinline function: () -> T) {

		val customizer = BeanDefinitionCustomizer { bd ->
			scope?.let { bd.scope = scope.name.toLowerCase() }
			isLazyInit?.let { bd.isLazyInit = isLazyInit }
			isPrimary?.let { bd.isPrimary = isPrimary }
			isAutowireCandidate?.let { bd.isAutowireCandidate = isAutowireCandidate }
			if (bd is AbstractBeanDefinition) {
				bd.autowireMode = autowireMode.ordinal
			}
		}


		when (name) {
			null -> context.registerBean(T::class.java,
					Supplier { function.invoke() }, customizer)
			else -> context.registerBean(name, T::class.java,
					Supplier { function.invoke() }, customizer)
		}

	}

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
	 * Take in account bean definitions enclosed in the provided lambda only when the
	 * specified profile is active.
	 */
	fun profile(profile: String, init: BeanDefinitionDsl.() -> Unit) {
		val beans = BeanDefinitionDsl(init, { it.activeProfiles.contains(profile) })
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
