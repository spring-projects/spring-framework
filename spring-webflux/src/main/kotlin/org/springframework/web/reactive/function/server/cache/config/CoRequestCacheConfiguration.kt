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

package org.springframework.web.reactive.function.server.cache.config

import org.aopalliance.intercept.MethodInterceptor
import org.springframework.aop.Advisor
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportAware
import org.springframework.context.annotation.Role
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.type.AnnotationMetadata
import org.springframework.web.reactive.function.server.cache.EnableCoRequestCaching
import org.springframework.web.reactive.function.server.cache.context.CoRequestCacheWebFilter
import org.springframework.web.reactive.function.server.cache.interceptor.CoRequestCacheAdvisor
import org.springframework.web.reactive.function.server.cache.interceptor.CoRequestCacheInterceptor
import org.springframework.web.reactive.function.server.cache.interceptor.CoRequestCacheKeyGenerator
import org.springframework.web.reactive.function.server.cache.operation.CoRequestCacheOperationSource
import org.springframework.web.server.CoWebFilter
import kotlin.reflect.jvm.jvmName

/**
 * `@Configuration` class that registers the Spring infrastructure beans necessary
 * to enable proxy-based request-scoped cache for Spring WebFlux servers
 * with Kotlin coroutines support enabled.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 * @see CoRequestCacheConfigurationSelector
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
internal class CoRequestCacheConfiguration : ImportAware {
	lateinit var enableCoRequestCaching: AnnotationAttributes

	override fun setImportMetadata(importMetadata: AnnotationMetadata) {
		enableCoRequestCaching =
			AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableCoRequestCaching::class.jvmName)
			) ?: throw IllegalArgumentException(
				"@EnableCoRequestCaching is not present on importing class ${importMetadata.className}"
			)
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	fun coRequestCacheOperationSource(): CoRequestCacheOperationSource = CoRequestCacheOperationSource()

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	fun coRequestCacheInterceptor(coRequestCacheOperationSource: CoRequestCacheOperationSource): MethodInterceptor =
		CoRequestCacheInterceptor(
			CoRequestCacheKeyGenerator(
				coRequestCacheOperationSource
			)
		)

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	fun coRequestCacheAdvisor(
		coRequestCacheOperationSource: CoRequestCacheOperationSource,
		coRequestCacheInterceptor: MethodInterceptor
	): Advisor =
		CoRequestCacheAdvisor(
			coRequestCacheOperationSource,
			coRequestCacheInterceptor
		)
			.apply {
				if (::enableCoRequestCaching.isInitialized) {
					order = enableCoRequestCaching.getNumber("order")
				}
			}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	fun coRequestCacheWebFilter(): CoWebFilter = CoRequestCacheWebFilter()
}
