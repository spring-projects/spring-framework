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

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.testfixture.servlet.MockHttpServletRequest
import org.springframework.web.testfixture.servlet.MockHttpServletResponse
import java.nio.charset.StandardCharsets
import kotlin.collections.mapOf
import kotlin.reflect.jvm.javaMethod

/**
 * Kotlin tests for [RequestResponseBodyMethodProcessor].
 */
class RequestResponseBodyMethodProcessorKotlinTests {

	private val container = ModelAndViewContainer()

	private val servletRequest = MockHttpServletRequest()

	private val servletResponse = MockHttpServletResponse()

	private val request: NativeWebRequest = ServletWebRequest(servletRequest, servletResponse)

	private val factory = ValidatingBinderFactory()

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
	fun writeEntityWithKotlinSerializationJsonMessageConverter() {
		val method = SampleController::writeMessageEntity::javaMethod.get()!!
		val handlerMethod = HandlerMethod(SampleController(), method)
		val methodReturnType = handlerMethod.returnType

		val converters = listOf(KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, listOf(KotlinResponseBodyAdvice()))

		val returnValue: Any? = SampleController().writeMessageEntity().body
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
	fun writeNullableMapWithKotlinSerializationJsonMessageConverter() {
		val method = SampleController::writeNullableMap::javaMethod.get()!!
		val handlerMethod = HandlerMethod(SampleController(), method)
		val methodReturnType = handlerMethod.returnType

		val converters = listOf(KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, listOf(KotlinResponseBodyAdvice()))

		val returnValue: Any = SampleController().writeNullableMap()
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request)

		Assertions.assertThat(this.servletResponse.contentAsString)
			.isEqualTo("""{"value":null}""")
	}

	@Test
	fun readWithKotlinSerializationJsonMessageConverter() {
		val content = "{\"value\" : \"foo\"}"
		this.servletRequest.setContent(content.toByteArray(StandardCharsets.UTF_8))
		this.servletRequest.setContentType("application/json")

		val converters = listOf(StringHttpMessageConverter(), KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, null)

		val method = SampleController::readMessage::javaMethod.get()!!
		val methodParameter = MethodParameter(method, 0)

		val result = processor.resolveArgument(methodParameter, container, request, factory) as Message

		Assertions.assertThat(result).isEqualTo(Message("foo"))
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun readEntityWithKotlinSerializationJsonMessageConverter() {
		val content = "{\"value\" : \"foo\"}"
		this.servletRequest.setContent(content.toByteArray(StandardCharsets.UTF_8))
		this.servletRequest.setContentType("application/json")

		val converters = listOf(StringHttpMessageConverter(), KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, listOf(KotlinRequestBodyAdvice()))

		val method = SampleController::readMessageEntity::javaMethod.get()!!
		val methodParameter = MethodParameter(method, 0)

		val result = processor.resolveArgument(methodParameter, container, request, factory) as Message

		Assertions.assertThat(result).isEqualTo(Message("foo"))
	}

	@Suppress("UNCHECKED_CAST")
	@Test
	fun readGenericTypeWithKotlinSerializationJsonMessageConverter() {
		val content = "[{\"value\" : \"foo\"}, {\"value\" : \"bar\"}]"
		this.servletRequest.setContent(content.toByteArray(StandardCharsets.UTF_8))
		this.servletRequest.setContentType("application/json")

		val converters = listOf(StringHttpMessageConverter(), KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, null)

		val method = SampleController::readMessages::javaMethod.get()!!
		val methodParameter = MethodParameter(method, 0)

		val result = processor.resolveArgument(methodParameter, container, request, factory) as List<Message>

		Assertions.assertThat(result).containsExactly(Message("foo"), Message("bar"))
	}

	@Suppress("UNCHECKED_CAST")
	@Test
	fun readNullableMapWithKotlinSerializationJsonMessageConverter() {
		val content = "{\"value\" : null}"
		this.servletRequest.setContent(content.toByteArray(StandardCharsets.UTF_8))
		this.servletRequest.setContentType("application/json")

		val converters = listOf(StringHttpMessageConverter(), KotlinSerializationJsonHttpMessageConverter())
		val processor = RequestResponseBodyMethodProcessor(converters, null, listOf(KotlinRequestBodyAdvice()))

		val method = SampleController::readNullableMap::javaMethod.get()!!
		val methodParameter = MethodParameter(method, 0)

		val result = processor.resolveArgument(methodParameter, container, request, factory) as Map<String, String ?>

		Assertions.assertThat(result).isEqualTo(mapOf("value" to null))
	}


	private class SampleController {

		@RequestMapping
		@ResponseBody
		fun writeMessage() = Message("foo")

		@RequestMapping
		@ResponseBody
		fun writeMessageEntity() = ResponseEntity.ok(Message("foo"))

		@RequestMapping
		@ResponseBody
		fun writeMessages() = listOf(Message("foo"), Message("bar"))

		@RequestMapping
		@ResponseBody
		fun readMessage(message: Message) = message.value

		@RequestMapping
		@ResponseBody
		fun readMessageEntity(entity: RequestEntity<Message>) = entity.body!!.value

		@RequestMapping
		@ResponseBody
		fun readMessages(messages: List<Message>) = messages.map { it.value }.reduce { acc, string -> "$acc $string" }

		@RequestMapping
		@ResponseBody
		fun writeNullableMap(): Map<String, String?> {
			return mapOf("value" to null)
		}

		@RequestMapping
		@ResponseBody
		fun readNullableMap(map: Map<String, String?>): String {
			return map.toString()
		}

	}

	@Serializable
	data class Message(val value: String)

	private class ValidatingBinderFactory : WebDataBinderFactory {
		override fun createBinder(request: NativeWebRequest, target: Any?, objectName: String): WebDataBinder {
			val validator = LocalValidatorFactoryBean()
			validator.afterPropertiesSet()
			val dataBinder = WebDataBinder(target, objectName)
			dataBinder.setValidator(validator)
			return dataBinder
		}
	}

}