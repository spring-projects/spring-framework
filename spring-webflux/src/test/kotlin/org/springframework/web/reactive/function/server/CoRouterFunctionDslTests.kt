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

package org.springframework.web.reactive.function.server

import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.*
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.test.StepVerifier

/**
 * Tests for [CoRouterFunctionDsl].
 *
 * @author Sebastien Deleuze
 */
class CoRouterFunctionDslTests {

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
	fun pathExtension() {
		val mockRequest = get("https://example.com/test.properties").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
			.expectNextCount(1)
			.verifyComplete()
	}

	@Test
	fun resource() {
		val mockRequest = get("https://example.com/response2.txt").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
			.expectNextCount(1)
			.verifyComplete()
	}

	@Test
	fun resources() {
		val mockRequest = get("https://example.com/resources/response.txt").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
			.expectNextCount(1)
			.verifyComplete()
	}

	@Test
	fun resourcesLookupFunction() {
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
	fun filtering() {
		val mockRequest = get("https://example.com/filter").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request).flatMap { it.handle(request) })
			.expectNextMatches { response ->
				response.headers().getFirst("foo") == "bar"
			}
			.verifyComplete()
	}

	@Test
	fun filteringWithContext() {
		val mockRequest = get("https://example.com/").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(filterRouterWithContext.route(request).flatMap { it.handle(request) })
			.expectNextMatches { response ->
				response.headers().getFirst("context")!!.contains("Filter context")
			}
			.verifyComplete()
	}

	@Test
	fun contextProvider() {
		val mockRequest = get("https://example.com/")
			.header("Custom-Header", "foo")
			.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(routerWithContextProvider.route(request).flatMap { it.handle(request) })
			.expectNextMatches { response ->
				response.headers().getFirst("context")!!.contains("foo")
			}
			.verifyComplete()
	}

	@Test
	fun nestedContextProvider() {
		val mockRequest = get("https://example.com/nested/")
			.header("Custom-Header", "foo")
			.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(nestedRouterWithContextProvider.route(request).flatMap { it.handle(request) })
			.expectNextMatches { response ->
				response.headers().getFirst("context")!!.contains("foo")
			}
			.verifyComplete()
	}

	@Test
	fun nestedContextProviderWithOverride() {
		val mockRequest = get("https://example.com/nested/")
			.header("Custom-Header", "foo")
			.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(nestedRouterWithContextProviderOverride.route(request).flatMap { it.handle(request) })
			.expectNextMatches { response ->
				response.headers().getFirst("context")!!.contains("foo")
			}
			.verifyComplete()
	}

	@Test
	fun doubleNestedContextProvider() {
		val mockRequest = get("https://example.com/nested/nested/")
			.header("Custom-Header", "foo")
			.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(nestedRouterWithContextProvider.route(request).flatMap { it.handle(request) })
			.expectNextMatches { response ->
				response.headers().getFirst("context")!!.contains("foo")
			}
			.verifyComplete()
	}

	@Test
	fun contextProviderAndFilter() {
		val mockRequest = get("https://example.com/")
			.header("Custom-Header", "bar")
			.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(routerWithContextProvider.route(request).flatMap { it.handle(request) })
			.expectNextMatches { response ->
				response.headers().getFirst("context")!!.let {
					it.contains("bar") && it.contains("Dispatchers.Default")
				}
			}
			.verifyComplete()
	}

	@Test
	fun webFilterAndContext() {
		val strategies = HandlerStrategies.builder().webFilter(MyCoWebFilterWithContext()).build()
		val httpHandler = RouterFunctions.toHttpHandler(routerWithoutContext, strategies)
		val mockRequest = get("https://example.com/").build()
		val mockResponse = MockServerHttpResponse()
		StepVerifier.create(httpHandler.handle(mockRequest, mockResponse)).verifyComplete()
		assertThat(mockResponse.headers.getFirst("context")).contains("Filter context")
	}

	@Test
	fun multipleContextProviders() {
		assertThatIllegalStateException().isThrownBy {
			coRouter {
				context {
					CoroutineName("foo")
				}
				context {
					Dispatchers.Default
				}
			}
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
		)
		assertThat(visitor.visitCount()).isEqualTo(7)
	}

	private fun sampleRouter() = coRouter {
		(GET("/foo/") or GET("/foos/")) { req -> handle(req) }
		"/api".nest {
			POST("/foo/", ::handleFromClass)
			POST("/bar/", contentType(APPLICATION_JSON), ::handleFromClass)
			PUT("/foo/", :: handleFromClass)
			PATCH("/foo/") {
				ok().buildAndAwait()
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
		resource(path("/response2.txt"), ClassPathResource("/org/springframework/web/reactive/function/response.txt"))
		resources("/resources/**",
			ClassPathResource("/org/springframework/web/reactive/function/"))
		resources {
			if (it.path() == "/response.txt") {
				ClassPathResource("/org/springframework/web/reactive/function/response.txt")
			}
			else {
				null
			}
		}
		GET(pathExtension { it == "properties" }) {
			ok().bodyValueAndAwait("foo=bar")
		}
		path("/baz", ::handle)
		GET("/rendering") { RenderingResponse.create("index").buildAndAwait() }
		add(otherRouter)
		add(filterRouter)
	}

	private val filterRouter = coRouter {
		"/filter" { request ->
			ok().header("foo", request.headers().firstHeader("foo")).buildAndAwait()
		}

		filter { request, next ->
			val newRequest = ServerRequest.from(request).apply { header("foo", "bar") }.build()
			next(newRequest)
		}
	}

	private val filterRouterWithContext = coRouter {
		filter { request, next ->
			withContext(CoroutineName("Filter context")) {
				next(request)
			}
		}
		GET("/") {
			ok().header("context", currentCoroutineContext().toString()).buildAndAwait()
		}
	}

	private val routerWithContextProvider = coRouter {
		context {
			CoroutineName(it.headers().firstHeader("Custom-Header")!!)
		}
		GET("/") {
			ok().header("context", currentCoroutineContext().toString()).buildAndAwait()
		}
		filter { request, next ->
			if (request.headers().firstHeader("Custom-Header") == "bar") {
				withContext(currentCoroutineContext() + Dispatchers.Default) {
					next.invoke(request)
				}
			}
			else {
				next.invoke(request)
			}
		}
	}

	private val nestedRouterWithContextProvider = coRouter {
		context {
			CoroutineName(it.headers().firstHeader("Custom-Header")!!)
		}
		"/nested".nest {
			GET("/") {
				ok().header("context", currentCoroutineContext().toString()).buildAndAwait()
			}
			"/nested".nest {
				GET("/") {
					ok().header("context", currentCoroutineContext().toString()).buildAndAwait()
				}
			}
		}
	}

	private val nestedRouterWithContextProviderOverride = coRouter {
		context {
			CoroutineName("parent-context")
		}
		"/nested".nest {
			context {
				CoroutineName(it.headers().firstHeader("Custom-Header")!!)
			}
			GET("/") {
				ok().header("context", currentCoroutineContext().toString()).buildAndAwait()
			}
		}
	}

	private val routerWithoutContext = coRouter {
		GET("/") {
			ok().header("context", currentCoroutineContext().toString()).buildAndAwait()
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
	private suspend fun handleFromClass(req: ServerRequest) = ServerResponse.ok().buildAndAwait()
}

@Suppress("UNUSED_PARAMETER")
private suspend fun handle(req: ServerRequest) = ServerResponse.ok().buildAndAwait()


private class MyCoWebFilterWithContext : CoWebFilter() {
	override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
		withContext(CoroutineName("Filter context")) {
			chain.filter(exchange)
		}
	}
}
