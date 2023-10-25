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

package org.springframework.web.method.annotation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.core.MethodParameter
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.SynthesizingMethodParameter
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.bind.support.WebRequestDataBinder
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.testfixture.servlet.MockHttpServletRequest

/**
 * Kotlin test fixture for [ModelAttributeMethodProcessor].
 *
 * @author Sebastien Deleuze
 */
class ModelAttributeMethodProcessorKotlinTests {

	private lateinit var container: ModelAndViewContainer

	private lateinit var processor: ModelAttributeMethodProcessor

	private lateinit var param: MethodParameter


	@BeforeEach
	fun setup() {
		container = ModelAndViewContainer()
		processor = ModelAttributeMethodProcessor(false)
		val method = ModelAttributeHandler::class.java.getDeclaredMethod("test", Param::class.java)
		param = SynthesizingMethodParameter(method, 0)
	}

	@Test
	fun resolveArgumentWithValue() {
		val mockRequest = MockHttpServletRequest().apply { addParameter("a", "b") }
		val requestWithParam = ServletWebRequest(mockRequest)
		val factory = mock<WebDataBinderFactory>()
		given(factory.createBinder(any(), any(), eq("param"), any()))
			.willAnswer {
				val binder = WebRequestDataBinder(it.getArgument(1))
				binder.setTargetType(ResolvableType.forMethodParameter(this.param))
				binder
			}
		assertThat(processor.resolveArgument(this.param, container, requestWithParam, factory)).isEqualTo(Param("b"))
	}

	@Test
	fun throwMethodArgumentNotValidExceptionWithNull() {
		val mockRequest = MockHttpServletRequest().apply { addParameter("a", null) }
		val requestWithParam = ServletWebRequest(mockRequest)
		val factory = mock<WebDataBinderFactory>()
		given(factory.createBinder(any(), any(), eq("param"), any()))
			.willAnswer {
				val binder = WebRequestDataBinder(it.getArgument(1))
				binder.setTargetType(ResolvableType.forMethodParameter(this.param))
				binder
			}
		assertThatThrownBy {
			processor.resolveArgument(this.param, container, requestWithParam, factory)
		}.isInstanceOf(MethodArgumentNotValidException::class.java)
			.hasMessageContaining("parameter a")
	}

	private data class Param(val a: String)

	private class ModelAttributeHandler {
		@Suppress("UNUSED_PARAMETER")
		fun test(param: Param) { }
	}

}
