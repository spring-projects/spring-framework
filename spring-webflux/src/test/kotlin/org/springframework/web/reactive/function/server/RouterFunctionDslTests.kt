/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.function.server

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.support.ServerRequestWrapper
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.*
import org.springframework.web.testfixture.server.MockServerWebExchange
import org.springframework.web.reactive.function.server.AttributesTestVisitor
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.security.Principal

/**
 * Tests for [RouterFunctionDsl].
 *
 * @author Sebastien Deleuze
 */
class RouterFunctionDslTests {

	@Test
	fun header() {
		val mockRequest = get("https://example.com")
				.header("bar","bar").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun accept() {
		val mockRequest = get("https://example.com/content")
				.header(ACCEPT, APPLICATION_ATOM_XML_VALUE).build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun acceptAndPOST() {
		val mockRequest = post("https://example.com/api/foo/")
				.header(ACCEPT, APPLICATION_JSON_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun acceptAndPOSTWithRequestPredicate() {
		val mockRequest = post("https://example.com/api/bar/")
				.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun contentType() {
		val mockRequest = get("https://example.com/content/")
				.header(CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resourceByPath() {
		val mockRequest = get("https://example.com/org/springframework/web/reactive/function/response.txt")
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun method() {
		val mockRequest = patch("https://example.com/")
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun path() {
		val mockRequest = get("https://example.com/baz").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resource() {
		val mockRequest = get("https://example.com/response.txt").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun noRoute() {
		val mockRequest = get("https://example.com/bar")
				.header(ACCEPT, APPLICATION_PDF_VALUE)
				.header(CONTENT_TYPE, APPLICATION_PDF_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.verifyComplete()
	}

	@Test
	fun rendering() {
		val mockRequest = get("https://example.com/rendering").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request).flatMap { it.handle(request) })
				.expectNextMatches { it is RenderingResponse}
				.verifyComplete()
	}

	@Test
	fun emptyRouter() {
		assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
			router { }
		}
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

	@Test
	fun acceptFilterAndPOST() {
		val mockRequest = post("https://example.com/filter")
			.header(ACCEPT, APPLICATION_JSON_VALUE)
			.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(filteredRouter.route(request).flatMap { it.handle(request) })
			.expectNextCount(1)
			.verifyComplete()
	}

	private val filteredRouter = router {
		POST("/filter", ::handleRequestWrapper)

		filter (TestFilterProvider.provide())
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

	private class TestServerRequestWrapper(delegate: ServerRequest, private val principalOverride: String = "foo"): ServerRequestWrapper(delegate) {
		override fun principal(): Mono<out Principal> = Mono.just(Principal { principalOverride })
	}

	private object TestFilterProvider {
		fun provide(): (ServerRequest, (ServerRequest) -> Mono<ServerResponse>) -> Mono<ServerResponse> = { request, next ->
			next(TestServerRequestWrapper(request))
		}
	}

	private fun handleRequestWrapper(req: ServerRequest): Mono<ServerResponse> {
		return req.principal()
			.flatMap {
				assertThat(it.name).isEqualTo("foo")
				ServerResponse.ok().build()
			}
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
		resources("/org/springframework/web/reactive/function/**",
				ClassPathResource("/org/springframework/web/reactive/function/response.txt"))
		resources {
			if (it.path() == "/response.txt") {
				Mono.just(ClassPathResource("/org/springframework/web/reactive/function/response.txt"))
			}
			else {
				Mono.empty()
			}
		}
		path("/baz", ::handle)
		GET("/rendering") { RenderingResponse.create("index").build() }
		add(otherRouter)
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
