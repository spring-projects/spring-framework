/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.util.MultiValueMap
import java.security.Principal
import java.util.*
import javax.servlet.http.Cookie

/**
 * Provide a [MockHttpServletRequestBuilder] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @see MockMvc.get
 * @see MockMvc.post
 * @see MockMvc.put
 * @see MockMvc.patch
 * @see MockMvc.delete
 * @see MockMvc.options
 * @see MockMvc.head
 * @see MockMvc.request
 * @author Sebastien Deleuze
 * @since 5.2
 */
open class MockHttpServletRequestDsl(private val builder: MockHttpServletRequestBuilder) {

	/**
	 * @see [MockHttpServletRequestBuilder.contextPath]
	 */
	var contextPath: String? = null

	/**
	 * @see [MockHttpServletRequestBuilder.servletPath]
	 */
	var servletPath: String? = null

	/**
	 * @see [MockHttpServletRequestBuilder.pathInfo]
	 */
	var pathInfo: String? = null

	/**
	 * @see [MockHttpServletRequestBuilder.secure]
	 */
	var secure: Boolean? = null

	/**
	 * @see [MockHttpServletRequestBuilder.characterEncoding]
	 */
	var characterEncoding: String? = null

	/**
	 * @see [MockHttpServletRequestBuilder.content]
	 */
	var content: Any? = null

	/**
	 * @see [MockHttpServletRequestBuilder.accept]
	 */
	var accept: MediaType? = null

	/**
	 * @see [MockHttpServletRequestBuilder.accept]
	 */
	fun accept(vararg mediaTypes: MediaType) {
		builder.accept(*mediaTypes)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.contentType]
	 */
	var contentType: MediaType? = null

	/**
	 * @see [MockHttpServletRequestBuilder.headers]
	 */
	fun headers(headers: HttpHeaders.() -> Unit) {
		builder.headers(HttpHeaders().apply(headers))
	}

	/**
	 * @see [MockHttpServletRequestBuilder.header]
	 */
	fun header(name: String, vararg values: Any) {
		builder.header(name, *values)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.param]
	 */
	fun param(name: String, vararg values: String) {
		builder.param(name, *values)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.params]
	 */
	var params: MultiValueMap<String, String>? = null

	/**
	 * @see [MockHttpServletRequestBuilder.cookie]
	 */
	fun cookie(vararg cookies: Cookie) {
		builder.cookie(*cookies)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.locale]
	 */
	fun locale(vararg locales: Locale) {
		builder.locale(*locales)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.requestAttr]
	 */
	fun requestAttr(name: String, value: Any) {
		builder.requestAttr(name, value)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.sessionAttr]
	 */
	fun sessionAttr(name: String, value: Any) {
		builder.sessionAttr(name, value)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.sessionAttrs]
	 */
	var sessionAttrs: Map<String, Any>? = null

	/**
	 * @see [MockHttpServletRequestBuilder.flashAttr]
	 */
	fun flashAttr(name: String, value: Any) {
		builder.flashAttr(name, value)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.flashAttrs]
	 */
	var flashAttrs: Map<String, Any>? = null

	/**
	 * @see [MockHttpServletRequestBuilder.session]
	 */
	var session: MockHttpSession? = null

	/**
	 * @see [MockHttpServletRequestBuilder.principal]
	 */
	var principal: Principal? = null

	/**
	 * @see [MockHttpServletRequestBuilder.with]
	 */
	fun with(processor: RequestPostProcessor) {
		builder.with(processor)
	}

	/**
	 * @see [MockHttpServletRequestBuilder.merge]
	 */
	fun merge(parent: MockHttpServletRequestBuilder?) {
		builder.merge(parent)
	}

	internal fun perform(mockMvc: MockMvc): ResultActionsDsl {
		contextPath?.also { builder.contextPath(contextPath!!) }
		servletPath?.also { builder.servletPath(servletPath!!) }
		pathInfo?.also { builder.pathInfo(pathInfo) }
		secure?.also { builder.secure(secure!!) }
		characterEncoding?.also { builder.characterEncoding(characterEncoding!!) }
		content?.also {
			when (content) {
				is String -> builder.content(content as String)
				is ByteArray -> builder.content(content as ByteArray)
				else -> builder.content(content.toString())
			}
		}
		accept?.also { builder.accept(accept!!) }
		contentType?.also { builder.contentType(contentType!!) }
		params?.also { builder.params(params!!) }
		sessionAttrs?.also { builder.sessionAttrs(sessionAttrs!!) }
		flashAttrs?.also { builder.flashAttrs(flashAttrs!!) }
		session?.also { builder.session(session!!) }
		principal?.also { builder.principal(principal!!) }
		return ResultActionsDsl(mockMvc.perform(builder))
	}
}
