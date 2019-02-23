/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.server

import org.junit.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod.*
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.MockServerRequest.builder
import reactor.test.StepVerifier
import java.net.URI

/**
 * Tests for [CoRouterFunctionDsl].
 *
 * @author Sebastien Deleuze
 */
class CoRouterFunctionDslTests {

	@Test
	fun header() {
		val request = builder().header("bar", "bar").build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun accept() {
		val request = builder().uri(URI("/content")).header(ACCEPT, APPLICATION_ATOM_XML_VALUE).build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun acceptAndPOST() {
		val request = builder()
				.method(POST)
				.uri(URI("/api/foo/"))
				.header(ACCEPT, APPLICATION_JSON_VALUE)
				.build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun contentType() {
		val request = builder().uri(URI("/content")).header(CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE).build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resourceByPath() {
		val request = builder().uri(URI("/org/springframework/web/reactive/function/response.txt")).build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun method() {
		val request = builder().method(PATCH).build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun path() {
		val request = builder().uri(URI("/baz")).build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resource() {
		val request = builder().uri(URI("/response.txt")).build()
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun noRoute() {
		val request = builder()
				.uri(URI("/bar"))
				.header(ACCEPT, APPLICATION_PDF_VALUE)
				.header(CONTENT_TYPE, APPLICATION_PDF_VALUE)
				.build()
		StepVerifier.create(sampleRouter().route(request))
				.verifyComplete()
	}

	@Test
	fun rendering() {
		val request = builder().uri(URI("/rendering")).build()
		StepVerifier.create(sampleRouter().route(request).flatMap { it.handle(request) })
				.expectNextMatches { it is RenderingResponse}
				.verifyComplete()
	}

	@Test(expected = IllegalStateException::class)
	fun emptyRouter() {
		router { }
	}


	private fun sampleRouter() = coRouter {
		(GET("/foo/") or GET("/foos/")) { req -> handle(req) }
		"/api".nest {
			POST("/foo/", ::handleFromClass)
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
		resources("/org/springframework/web/reactive/function/**",
				ClassPathResource("/org/springframework/web/reactive/function/response.txt"))
		resources {
			if (it.path() == "/response.txt") {
				ClassPathResource("/org/springframework/web/reactive/function/response.txt")
			}
			else {
				null
			}
		}
		path("/baz", ::handle)
		GET("/rendering") { RenderingResponse.create("index").buildAndAwait() }
	}
}

@Suppress("UNUSED_PARAMETER")
private suspend fun handleFromClass(req: ServerRequest) = ServerResponse.ok().buildAndAwait()

@Suppress("UNUSED_PARAMETER")
private suspend fun handle(req: ServerRequest) = ServerResponse.ok().buildAndAwait()
