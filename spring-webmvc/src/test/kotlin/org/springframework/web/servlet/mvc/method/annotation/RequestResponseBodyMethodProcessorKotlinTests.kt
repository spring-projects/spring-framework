/*
 * Copyright 2025 the original author or authors.
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

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.testfixture.servlet.MockHttpServletRequest
import org.springframework.web.testfixture.servlet.MockHttpServletResponse
import kotlin.reflect.jvm.javaMethod

/**
 * Kotlin tests for [RequestResponseBodyMethodProcessor].
 */
class RequestResponseBodyMethodProcessorKotlinTests {

	private val container = ModelAndViewContainer()

	private val servletRequest = MockHttpServletRequest()

	private val servletResponse = MockHttpServletResponse()

	private val request: NativeWebRequest = ServletWebRequest(servletRequest, servletResponse)

	@Test
	fun writeWithKotlinSerializationJsonMessageConverter() {
		val method = SampleController::writeMessage::javaMethod.get()!!
		val handlerMethod = HandlerMethod(SampleController(), method)
		val methodReturnType = handlerMethod.returnType

		val converters = listOf(KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, null)

		val returnValue: Any = SampleController().writeMessage()
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request)

		Assertions.assertThat(this.servletResponse.contentAsString)
			.contains("\"value\":\"foo\"")
	}

	@Test
	fun writeGenericTypeWithKotlinSerializationJsonMessageConverter() {
		val method = SampleController::writeMessages::javaMethod.get()!!
		val handlerMethod = HandlerMethod(SampleController(), method)
		val methodReturnType = handlerMethod.returnType

		val converters = listOf(KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, null)

		val returnValue: Any = SampleController().writeMessages()
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request)

		Assertions.assertThat(this.servletResponse.contentAsString)
			.contains("\"value\":\"foo\"")
			.contains("\"value\":\"bar\"")
	}

	@Test
	fun readWithKotlinSerializationJsonMessageConverter() {
		val method = SampleController::readMessage::javaMethod.get()!!
		val handlerMethod = HandlerMethod(SampleController(), method)
		val methodReturnType = handlerMethod.returnType

		val converters = listOf(StringHttpMessageConverter(), KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, null)

		val returnValue: Any = SampleController().readMessage(Message("foo"))
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request)

		Assertions.assertThat(this.servletResponse.contentAsString).isEqualTo("foo")
	}

	@Test
	fun readGenericTypeWithKotlinSerializationJsonMessageConverter() {
		val method = SampleController::readMessages::javaMethod.get()!!
		val handlerMethod = HandlerMethod(SampleController(), method)
		val methodReturnType = handlerMethod.returnType

		val converters = listOf(StringHttpMessageConverter(), KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, null)

		val returnValue: Any = SampleController().readMessages(listOf(Message("foo"), Message("bar")))
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request)

		Assertions.assertThat(this.servletResponse.contentAsString)
			.isEqualTo("foo bar")
	}


	private class SampleController {

		@RequestMapping
		@ResponseBody
		fun writeMessage() = Message("foo")

		@RequestMapping
		@ResponseBody
		fun writeMessages() = listOf(Message("foo"), Message("bar"))

		@RequestMapping
		@ResponseBody
		fun readMessage(message: Message) = message.value

		@RequestMapping
		@ResponseBody fun readMessages(messages: List<Message>) = messages.map { it.value }.reduce { acc, string -> "$acc $string" }

	}

	@Serializable
	data class Message(val value: String)
}