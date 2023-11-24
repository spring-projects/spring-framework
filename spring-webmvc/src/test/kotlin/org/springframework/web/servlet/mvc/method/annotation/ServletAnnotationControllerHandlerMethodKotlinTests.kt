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

package org.springframework.web.servlet.mvc.method.annotation

import org.assertj.core.api.Assertions.assertThat
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.WebAsyncUtils
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest
import org.springframework.web.testfixture.servlet.MockHttpServletRequest
import org.springframework.web.testfixture.servlet.MockHttpServletResponse
import java.util.stream.Stream

/**
 * @author Sebastien Deleuze
 */
class ServletAnnotationControllerHandlerMethodKotlinTests : AbstractServletHandlerMethodTests() {

	companion object {
		@JvmStatic
		fun pathPatternsArguments(): Stream<Boolean> {
			return Stream.of(true, false)
		}
	}

	@PathPatternsParameterizedTest
	fun dataClassBinding(usePathPatterns: Boolean) {
		initDispatcherServlet(DataClassController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/bind")
		request.addParameter("param1", "value1")
		request.addParameter("param2", "2")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(response.contentAsString).isEqualTo("value1-2")
	}

	@PathPatternsParameterizedTest
	fun dataClassBindingWithOptionalParameterAndAllParameters(usePathPatterns: Boolean) {
		initDispatcherServlet(DataClassController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/bind-optional-parameter")
		request.addParameter("param1", "value1")
		request.addParameter("param2", "2")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(response.contentAsString).isEqualTo("value1-2")
	}

	@PathPatternsParameterizedTest
	fun dataClassBindingWithOptionalParameterAndOnlyMissingParameters(usePathPatterns: Boolean) {
		initDispatcherServlet(DataClassController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/bind-optional-parameter")
		request.addParameter("param1", "value1")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(response.contentAsString).isEqualTo("value1-12")
	}

	@PathPatternsParameterizedTest
	fun suspendingMethod(usePathPatterns: Boolean) {
		initDispatcherServlet(CoroutinesController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/suspending")
		request.isAsyncSupported = true
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(WebAsyncUtils.getAsyncManager(request).concurrentResult).isEqualTo("foo")
	}

	@PathPatternsParameterizedTest
	fun defaultValue(usePathPatterns: Boolean) {
		initDispatcherServlet(DefaultValueController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/default-value")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(response.contentAsString).isEqualTo("default")
	}

	@PathPatternsParameterizedTest
	fun defaultValueOverridden(usePathPatterns: Boolean) {
		initDispatcherServlet(DefaultValueController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/default-value")
		request.addParameter("value", "override")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(response.contentAsString).isEqualTo("override")
	}

	@PathPatternsParameterizedTest
	fun defaultValues(usePathPatterns: Boolean) {
		initDispatcherServlet(DefaultValueController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/default-values")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(response.contentAsString).isEqualTo("10-20")
	}

	@PathPatternsParameterizedTest
	fun defaultValuesOverridden(usePathPatterns: Boolean) {
		initDispatcherServlet(DefaultValueController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/default-values")
		request.addParameter("limit2", "40")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(response.contentAsString).isEqualTo("10-40")
	}

	@PathPatternsParameterizedTest
	fun suspendingDefaultValue(usePathPatterns: Boolean) {
		initDispatcherServlet(DefaultValueController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/suspending-default-value")
		request.isAsyncSupported = true
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(WebAsyncUtils.getAsyncManager(request).concurrentResult).isEqualTo("default")
	}

	@PathPatternsParameterizedTest
	fun suspendingDefaultValueOverridden(usePathPatterns: Boolean) {
		initDispatcherServlet(DefaultValueController::class.java, usePathPatterns)

		val request = MockHttpServletRequest("GET", "/suspending-default-value")
		request.isAsyncSupported = true
		request.addParameter("value", "override")
		val response = MockHttpServletResponse()
		servlet.service(request, response)
		assertThat(WebAsyncUtils.getAsyncManager(request).concurrentResult).isEqualTo("override")
	}


	data class DataClass(val param1: String, val param2: Int)

	data class DataClassWithOptionalParameter(val param1: String, val param2: Int = 12)

	@RestController
	class DataClassController {

		@RequestMapping("/bind")
		fun handle(data: DataClass) = "${data.param1}-${data.param2}"

		@RequestMapping("/bind-optional-parameter")
		fun handle(data: DataClassWithOptionalParameter) = "${data.param1}-${data.param2}"
	}

	@RestController
	class CoroutinesController {

		@Suppress("RedundantSuspendModifier")
		@RequestMapping("/suspending")
		suspend fun handle(): String {
			return "foo"
		}
	}

	@RestController
	class DefaultValueController {

		@RequestMapping("/default-value")
		fun handle(@RequestParam value: String = "default") = value

		@RequestMapping("/default-values")
		fun handleMultiple(@RequestParam(defaultValue = "10") limit1: Int, @RequestParam limit2: Int = 20) = "${limit1}-${limit2}"

		@Suppress("RedundantSuspendModifier")
		@RequestMapping("/suspending-default-value")
		suspend fun handleSuspending(@RequestParam value: String = "default") = value

	}

}
