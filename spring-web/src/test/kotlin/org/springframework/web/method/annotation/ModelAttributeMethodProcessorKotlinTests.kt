/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
