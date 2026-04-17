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

package org.springframework.web.servlet.mvc.method.annotation

import org.assertj.core.api.Assertions.assertThat
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.WebAsyncUtils
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest
import org.springframework.web.testfixture.servlet.MockHttpServletRequest
import org.springframework.web.testfixture.servlet.MockHttpServletResponse
import java.util.UUID
import java.util.stream.Stream

class ServletAnnotationControllerHandlerMethodValueClassKotlinTests : AbstractServletHandlerMethodTests() {

	companion object {
		@JvmStatic
		fun pathPatternsArguments(): Stream<Boolean> {
			return Stream.of(true, false)
		}
	}

	@PathPatternsParameterizedTest
	fun suspendingValueClass(usePathPatterns: Boolean) {
		initDispatcherServlet(CoroutinesController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/suspending-value-class")
		request.isAsyncSupported = true
		request.addParameter("value", "550e8400-e29b-41d4-a716-446655440000")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(WebAsyncUtils.getAsyncManager(request).concurrentResult).isEqualTo("550e8400-e29b-41d4-a716-446655440000")
	}

	@PathPatternsParameterizedTest
	fun suspendingValueClassOmitted(usePathPatterns: Boolean) {
		initDispatcherServlet(CoroutinesController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/suspending-value-class")
		request.isAsyncSupported = true
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(WebAsyncUtils.getAsyncManager(request).concurrentResult).isEqualTo("outer-null")
	}

	@PathPatternsParameterizedTest
	fun suspendingNullableInnerValueClass(usePathPatterns: Boolean) {
		initDispatcherServlet(CoroutinesController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/suspending-nullable-inner-value-class")
		request.isAsyncSupported = true
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(WebAsyncUtils.getAsyncManager(request).concurrentResult).isEqualTo("inner-null")
	}

	@PathPatternsParameterizedTest
	fun suspendingOptionalNullableInnerValueClass(usePathPatterns: Boolean) {
		initDispatcherServlet(CoroutinesController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/suspending-nullable-inner-value-class-optional")
		request.isAsyncSupported = true
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(WebAsyncUtils.getAsyncManager(request).concurrentResult).isEqualTo("outer-null")
	}

	@RestController
	class CoroutinesController {

		@Suppress("RedundantSuspendModifier")
		@RequestMapping("/suspending-value-class")
		suspend fun handleValueClass(@RequestParam value: ValueClass?): String {
			return when (value) {
				null -> "outer-null"
				else -> value.value.toString()
			}
		}

		@Suppress("RedundantSuspendModifier")
		@RequestMapping("/suspending-nullable-inner-value-class")
		suspend fun handleNullableInnerValueClass(@RequestParam(required = false) value: NullableInnerValueClass): String {
			return value.value?.toString() ?: "inner-null"
		}

		@Suppress("RedundantSuspendModifier")
		@RequestMapping("/suspending-nullable-inner-value-class-optional")
		suspend fun handleOptionalNullableInnerValueClass(@RequestParam(required = false) value: NullableInnerValueClass?): String {
			return when {
				value == null -> "outer-null"
				value.value == null -> "inner-null"
				else -> value.value.toString()
			}
		}
	}

	@JvmInline
	value class ValueClass(val value: UUID)

	@JvmInline
	value class NullableInnerValueClass(val value: UUID?)

}
