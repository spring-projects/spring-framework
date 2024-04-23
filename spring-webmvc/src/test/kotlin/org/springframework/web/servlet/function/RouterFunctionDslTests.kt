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

package org.springframework.web.servlet.function

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.web.servlet.handler.PathPatternsTestUtils

/**
 * Tests for WebMvc.fn [RouterFunctionDsl].
 *
 * @author Sebastien Deleuze
 */
class RouterFunctionDslTests {

	@Test
	fun header() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "", true)
		servletRequest.addHeader("bar", "bar")
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun accept() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/content", true)
		servletRequest.addHeader(ACCEPT, APPLICATION_ATOM_XML_VALUE)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun acceptAndPOST() {
		val servletRequest = PathPatternsTestUtils.initRequest("POST", "/api/foo/", true)
		servletRequest.addHeader(ACCEPT, APPLICATION_JSON_VALUE)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun acceptAndPOSTWithRequestPredicate() {
		val servletRequest = PathPatternsTestUtils.initRequest("POST", "/api/bar/", true)
		servletRequest.addHeader(ACCEPT, APPLICATION_JSON_VALUE)
		servletRequest.addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun contentType() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/content", true)
		servletRequest.addHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun method() {
		val servletRequest = PathPatternsTestUtils.initRequest("PATCH", "/", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun path() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/baz", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun pathExtension() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/test.properties", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun resource() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET","/response2.txt", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun resources() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/resources/response.txt", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun resourcesLookupFunction() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/response.txt", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isPresent()
	}

	@Test
	fun noRoute() {

		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/bar", true)
		servletRequest.addHeader(ACCEPT, APPLICATION_PDF_VALUE)
		servletRequest.addHeader(CONTENT_TYPE, APPLICATION_PDF_VALUE)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request)).isNotPresent()
	}

	@Test
	fun rendering() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/rendering", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request).get().handle(request) is RenderingResponse).isTrue()
	}

	@Test
	fun emptyRouter() {
		assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
			router { }
		}
	}

	@Test
	fun filtering() {
		val servletRequest = PathPatternsTestUtils.initRequest("GET", "/filter", true)
		val request = DefaultServerRequest(servletRequest, emptyList())
		assertThat(sampleRouter().route(request).get().handle(request).headers().getFirst("foo")).isEqualTo("bar")
	}

	@Test
	fun attributes() {
		val visitor = AttributesTestVisitor()
		attributesRouter.accept(visitor)
		assertThat(visitor.routerFunctionsAttributes()).containsExactly(
			listOf(mapOf("foo" to "bar", "baz" to "qux")),
			listOf(mapOf("foo" to "bar", "baz" to "qux")),
			listOf(mapOf("foo" to "bar"), mapOf("foo" to "n1")),
			listOf(mapOf("baz" to "qux"), mapOf("foo" to "n1")),
			listOf(mapOf("foo" to "n3"), mapOf("foo" to "n2"), mapOf("foo" to "n1"))
		);
		assertThat(visitor.visitCount()).isEqualTo(7);
	}

	private fun sampleRouter() = router {
		(GET("/foo/") or GET("/foos/")) { req -> handle(req) }
		"/api".nest {
			POST("/foo/", ::handleFromClass)
			POST("/bar/", contentType(APPLICATION_JSON), ::handleFromClass)
			PUT("/foo/", :: handleFromClass)
			PATCH("/foo/") {
				ok().build()
			}
			"/foo/"  { handleFromClass(it) }
		}
		"/content".nest {
			accept(APPLICATION_ATOM_XML, ::handle)
			contentType(APPLICATION_OCTET_STREAM, ::handle)
		}
		method(PATCH, ::handle)
		headers { it.accept().contains(APPLICATION_JSON) }.nest {
			GET("/api/foo/", ::handle)
		}
		headers({ it.header("bar").isNotEmpty() }, ::handle)
		resource(path("/response2.txt"), ClassPathResource("/org/springframework/web/servlet/function/response.txt"))
		resources("/resources/**",
			ClassPathResource("/org/springframework/web/servlet/function/"))
		resources {
			if (it.path() == "/response.txt") {
				ClassPathResource("/org/springframework/web/servlet/function/response.txt")
			}
			else {
				null
			}
		}
		GET(pathExtension { it == "properties" }) {
			ok().body("foo=bar")
		}
		path("/baz", ::handle)
		GET("/rendering") { RenderingResponse.create("index").build() }
		add(otherRouter)
		add(filterRouter)
	}

	private val filterRouter = router {
		"/filter" { request ->
			ok().header("foo", request.headers().firstHeader("foo")).build()
		}

		filter { request, next ->
			val newRequest = ServerRequest.from(request).apply { header("foo", "bar") }.build()
			next(newRequest)
		}
	}

	private val otherRouter = router {
		"/other" {
			ok().build()
		}
		filter { request, next ->
			next(request)
		}
		before {
			it
		}
		after { _, response ->
			response
		}
		onError({it is IllegalStateException}) { _, _ ->
			ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
		onError<IllegalStateException> { _, _ ->
			ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}

	private val attributesRouter = router {
		GET("/atts/1") {
			ok().build()
		}
		withAttribute("foo", "bar")
		withAttribute("baz", "qux")
		GET("/atts/2") {
			ok().build()
		}
		withAttributes { atts ->
			atts["foo"] = "bar"
			atts["baz"] = "qux"
		}
		"/atts".nest {
			GET("/3") {
				ok().build()
			}
			withAttribute("foo", "bar")
			GET("/4") {
				ok().build()
			}
			withAttribute("baz", "qux")
			"/5".nest {
				GET {
					ok().build()
				}
				withAttribute("foo", "n3")
			}
			withAttribute("foo", "n2")
		}
		withAttribute("foo", "n1")
	}

	@Suppress("UNUSED_PARAMETER")
	private fun handleFromClass(req: ServerRequest) = ServerResponse.ok().build()
}

@Suppress("UNUSED_PARAMETER")
private fun handle(req: ServerRequest) = ServerResponse.ok().build()
