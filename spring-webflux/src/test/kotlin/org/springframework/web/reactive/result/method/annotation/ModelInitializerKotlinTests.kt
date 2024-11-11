/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation

import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticApplicationContext
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest
import org.springframework.web.testfixture.method.ResolvableMethod
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Kotlin test fixture for [ModelInitializer].
 *
 * @author Sebastien Deleuze
 */
class ModelInitializerKotlinTests {

	private val timeout = Duration.ofMillis(5000)

	private lateinit var modelInitializer: ModelInitializer

	private val exchange: ServerWebExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"))

	@BeforeEach
	fun setup() {
		val adapterRegistry = ReactiveAdapterRegistry.getSharedInstance()
		val resolverConfigurer = ArgumentResolverConfigurer()
		resolverConfigurer.addCustomResolver(ModelMethodArgumentResolver(adapterRegistry))
		val methodResolver = ControllerMethodResolver(
            resolverConfigurer, adapterRegistry, StaticApplicationContext(),
			RequestedContentTypeResolverBuilder().build(), emptyList(), null, null, null
        )
		modelInitializer = ModelInitializer(methodResolver, adapterRegistry)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun modelAttributeMethods() {
		val controller = TestController()
		val method = ResolvableMethod.on(TestController::class.java).annotPresent(GetMapping::class.java)
			.resolveMethod()
		val handlerMethod = HandlerMethod(controller, method)
		val context  = InitBinderBindingContext(ConfigurableWebBindingInitializer(), emptyList(),
			false, ReactiveAdapterRegistry.getSharedInstance())
		this.modelInitializer.initModel(handlerMethod, context, this.exchange).block(timeout)
		val model = context.model.asMap()
		Assertions.assertThat(model).hasSize(2)
		val monoValue = model["suspendingReturnValue"] as Mono<TestBean>
		Assertions.assertThat(monoValue.block(timeout)!!.name).isEqualTo("Suspending return value")
		val value = model["suspendingModelParameter"] as TestBean
		Assertions.assertThat(value.name).isEqualTo("Suspending model parameter")
	}


	private data class TestBean(val name: String)

	private class TestController {

		@ModelAttribute("suspendingReturnValue")
		suspend fun suspendingReturnValue(): TestBean {
			delay(1)
			return TestBean("Suspending return value")
		}

		@ModelAttribute
		suspend fun suspendingModelParameter(model: Model) {
			delay(1)
			model.addAttribute("suspendingModelParameter", TestBean("Suspending model parameter"))
		}

		@GetMapping
		fun handleGet() {
		}

	}

}
