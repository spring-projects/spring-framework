/*
 * Copyright 2002-2020 the original author or authors.
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
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.core.annotation.SynthesizingMethodParameter
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer
import org.springframework.web.bind.support.DefaultDataBinderFactory
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.testfixture.servlet.MockHttpServletRequest
import org.springframework.web.testfixture.servlet.MockHttpServletResponse
import org.springframework.web.testfixture.servlet.MockMultipartFile
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest

/**
 * Kotlin test fixture for [RequestParamMethodArgumentResolver].
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class RequestParamMethodArgumentResolverKotlinTests {

	lateinit var resolver: RequestParamMethodArgumentResolver
	lateinit var webRequest: NativeWebRequest
	lateinit var binderFactory: WebDataBinderFactory
	lateinit var request: MockHttpServletRequest

	lateinit var nullableParamRequired: MethodParameter
	lateinit var nullableParamNotRequired: MethodParameter
	lateinit var nonNullableParamRequired: MethodParameter
	lateinit var nonNullableParamNotRequired: MethodParameter

	lateinit var nullableMultipartParamRequired: MethodParameter
	lateinit var nullableMultipartParamNotRequired: MethodParameter
	lateinit var nonNullableMultipartParamRequired: MethodParameter
	lateinit var nonNullableMultipartParamNotRequired: MethodParameter


	@BeforeEach
	fun setup() {
		resolver = RequestParamMethodArgumentResolver(null, true)
		request = MockHttpServletRequest()
		val initializer = ConfigurableWebBindingInitializer()
		initializer.conversionService = DefaultConversionService()
		binderFactory = DefaultDataBinderFactory(initializer)
		webRequest = ServletWebRequest(request, MockHttpServletResponse())

		val method = ReflectionUtils.findMethod(javaClass, "handle", String::class.java,
				String::class.java, String::class.java, String::class.java,
				MultipartFile::class.java, MultipartFile::class.java,
				MultipartFile::class.java, MultipartFile::class.java)!!

		nullableParamRequired = SynthesizingMethodParameter(method, 0)
		nullableParamNotRequired = SynthesizingMethodParameter(method, 1)
		nonNullableParamRequired = SynthesizingMethodParameter(method, 2)
		nonNullableParamNotRequired = SynthesizingMethodParameter(method, 3)

		nullableMultipartParamRequired = SynthesizingMethodParameter(method, 4)
		nullableMultipartParamNotRequired = SynthesizingMethodParameter(method, 5)
		nonNullableMultipartParamRequired = SynthesizingMethodParameter(method, 6)
		nonNullableMultipartParamNotRequired = SynthesizingMethodParameter(method, 7)
	}

	@Test
	fun resolveNullableRequiredWithParameter() {
		request.addParameter("name", "123")
		var result = resolver.resolveArgument(nullableParamRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo("123")
	}

	@Test
	fun resolveNullableRequiredWithoutParameter() {
		var result = resolver.resolveArgument(nullableParamRequired, null, webRequest, binderFactory)
		assertThat(result).isNull()
	}

	@Test
	fun resolveNullableNotRequiredWithParameter() {
		request.addParameter("name", "123")
		var result = resolver.resolveArgument(nullableParamNotRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo("123")
	}

	@Test
	fun resolveNullableNotRequiredWithoutParameter() {
		var result = resolver.resolveArgument(nullableParamNotRequired, null, webRequest, binderFactory)
		assertThat(result).isNull()
	}

	@Test
	fun resolveNonNullableRequiredWithParameter() {
		request.addParameter("name", "123")
		var result = resolver.resolveArgument(nonNullableParamRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo("123")
	}

	@Test
	fun resolveNonNullableRequiredWithoutParameter() {
		assertThatExceptionOfType(MissingServletRequestParameterException::class.java).isThrownBy {
			resolver.resolveArgument(nonNullableParamRequired, null, webRequest, binderFactory)
		}
	}

	@Test
	fun resolveNonNullableNotRequiredWithParameter() {
		request.addParameter("name", "123")
		var result = resolver.resolveArgument(nonNullableParamNotRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo("123")
	}

	@Test
	fun resolveNonNullableNotRequiredWithoutParameter() {
		assertThatExceptionOfType(NullPointerException::class.java).isThrownBy {
			resolver.resolveArgument(nonNullableParamNotRequired, null, webRequest, binderFactory) as String
		}
	}

	@Test
	fun resolveNullableRequiredWithMultipartParameter() {
		val request = MockMultipartHttpServletRequest()
		val expected = MockMultipartFile("mfile", "Hello World".toByteArray())
		request.addFile(expected)
		webRequest = ServletWebRequest(request)

		var result = resolver.resolveArgument(nullableMultipartParamRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo(expected)
	}

	@Test
	fun resolveNullableRequiredWithoutMultipartParameter() {
		request.method = HttpMethod.POST.name()
		request.contentType = MediaType.MULTIPART_FORM_DATA_VALUE

		var result = resolver.resolveArgument(nullableMultipartParamRequired, null, webRequest, binderFactory)
		assertThat(result).isNull()
	}

	@Test
	fun resolveNullableNotRequiredWithMultipartParameter() {
		val request = MockMultipartHttpServletRequest()
		val expected = MockMultipartFile("mfile", "Hello World".toByteArray())
		request.addFile(expected)
		webRequest = ServletWebRequest(request)

		var result = resolver.resolveArgument(nullableMultipartParamNotRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo(expected)
	}

	@Test
	fun resolveNullableNotRequiredWithoutMultipartParameter() {
		request.method = HttpMethod.POST.name()
		request.contentType = MediaType.MULTIPART_FORM_DATA_VALUE

		var result = resolver.resolveArgument(nullableMultipartParamNotRequired, null, webRequest, binderFactory)
		assertThat(result).isNull()
	}

	@Test
	fun resolveNonNullableRequiredWithMultipartParameter() {
		val request = MockMultipartHttpServletRequest()
		val expected = MockMultipartFile("mfile", "Hello World".toByteArray())
		request.addFile(expected)
		webRequest = ServletWebRequest(request)

		var result = resolver.resolveArgument(nonNullableMultipartParamRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo(expected)
	}

	@Test
	fun resolveNonNullableRequiredWithoutMultipartParameter() {
		request.method = HttpMethod.POST.name()
		request.contentType = MediaType.MULTIPART_FORM_DATA_VALUE

		assertThatExceptionOfType(MissingServletRequestPartException::class.java).isThrownBy {
			resolver.resolveArgument(nonNullableMultipartParamRequired, null, webRequest, binderFactory)
		}
	}

	@Test
	fun resolveNonNullableNotRequiredWithMultipartParameter() {
		val request = MockMultipartHttpServletRequest()
		val expected = MockMultipartFile("mfile", "Hello World".toByteArray())
		request.addFile(expected)
		webRequest = ServletWebRequest(request)

		var result = resolver.resolveArgument(nonNullableMultipartParamNotRequired, null, webRequest, binderFactory)
		assertThat(result).isEqualTo(expected)
	}

	@Test
	fun resolveNonNullableNotRequiredWithoutMultipartParameter() {
		request.method = HttpMethod.POST.name()
		request.contentType = MediaType.MULTIPART_FORM_DATA_VALUE

		assertThatExceptionOfType(NullPointerException::class.java).isThrownBy {
			resolver.resolveArgument(nonNullableMultipartParamNotRequired, null, webRequest, binderFactory) as MultipartFile
		}
	}


	@Suppress("unused_parameter")
	fun handle(
			@RequestParam("name") nullableParamRequired: String?,
			@RequestParam("name", required = false) nullableParamNotRequired: String?,
			@RequestParam("name") nonNullableParamRequired: String,
			@RequestParam("name", required = false) nonNullableParamNotRequired: String,

			@RequestParam("mfile") nullableMultipartParamRequired: MultipartFile?,
			@RequestParam("mfile", required = false) nullableMultipartParamNotRequired: MultipartFile?,
			@RequestParam("mfile") nonNullableMultipartParamRequired: MultipartFile,
			@RequestParam("mfile", required = false) nonNullableMultipartParamNotRequired: MultipartFile) {
	}

}

