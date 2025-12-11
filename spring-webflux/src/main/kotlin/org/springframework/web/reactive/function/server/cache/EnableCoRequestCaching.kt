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

package org.springframework.web.reactive.function.server.cache

import org.springframework.context.annotation.AdviceMode
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.web.reactive.function.server.cache.config.CoRequestCacheConfigurationSelector

/**
 * Enables request-scoped cache management capability for Spring WebFlux servers with
 * [Kotlin coroutines support enabled](https://docs.spring.io/spring-framework/reference/languages/kotlin/coroutines.html#dependencies).
 *
 * To be used together with @[Configuration](org.springframework.context.annotation.Configuration) classes as follows:
 *
 * ```
 * @Configuration
 * @EnableCoRequestCaching
 * class AppConfig {
 *
 * 	@Bean
 * 	fun myService(): MyService {
 * 		// configure and return a class having @CoRequestCacheable suspend methods
 * 		return MyService()
 * 	}
 *
 * }
 * ```
 *
 * Note that the only supported advice [mode] is [AdviceMode.PROXY],
 * so local calls within the same class cannot get intercepted.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 * @see CoRequestCacheable
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(CoRequestCacheConfigurationSelector::class)
annotation class EnableCoRequestCaching(

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is `false`.
	 */
	val proxyTargetClass: Boolean = false,

	/**
	 * Indicate the ordering of the execution of the co-request caching advisor
	 * when multiple advices are applied at a specific joinpoint.
	 *
	 * The default is [Ordered.LOWEST_PRECEDENCE].
	 */
	val order: Int = Ordered.LOWEST_PRECEDENCE,

	/**
	 * Indicate how caching advice should be applied.
	 * The default and *only supported mode* is [AdviceMode.PROXY].
	 */
	val mode: AdviceMode = AdviceMode.PROXY
)
