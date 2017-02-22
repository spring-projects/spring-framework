/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.MockServerRequest.builder
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI

class RouterFunctionExtensionsTests {

	@Test
	fun header() {
		val request = builder().header("bar", "bar").build()
		StepVerifier.create(FooController().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun accept() {
		val request = builder().header(ACCEPT, APPLICATION_ATOM_XML_VALUE).build()
		StepVerifier.create(FooController().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun acceptAndPOST() {
		val request = builder().method(POST).uri(URI("/api/foo/")).header(ACCEPT, APPLICATION_JSON_VALUE).build()
		StepVerifier.create(FooController().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun contentType() {
		val request = builder().header(CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE).build()
		StepVerifier.create(FooController().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resourceByPath() {
		val request = builder().uri(URI("/org/springframework/web/reactive/function/response.txt")).build()
		StepVerifier.create(FooController().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun method() {
		val request = builder().method(PATCH).build()
		StepVerifier.create(FooController().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun path() {
		val request = builder().uri(URI("/baz")).build()
		StepVerifier.create(FooController().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resource() {
		val request = builder().uri(URI("/response.txt")).build()
		StepVerifier.create(FooController().route(request))
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
		StepVerifier.create(FooController().route(request))
				.verifyComplete()
	}

	class FooController : RouterFunction<ServerResponse> {

		override fun route(req: ServerRequest) = route(req) {
			(GET("/foo/") or GET("/foos/")) { handle(req) }
			(pathPrefix("/api") and accept(APPLICATION_JSON)).route {
				POST("/foo/")  { handleFromClass(req) }
				PUT("/foo/") { handleFromClass(req) }
				DELETE("/foo/")  { handleFromClass(req) }
			}
			accept(APPLICATION_ATOM_XML, ::handle)
			contentType(APPLICATION_OCTET_STREAM) { handle(req) }
			method(HttpMethod.PATCH) { handle(req) }
			headers({ it.accept().contains(APPLICATION_JSON) }).route {
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
			path("/baz") { handle(req) }
		}

		fun handleFromClass(req: ServerRequest) = ok().build()
	}
}

fun handle(req: ServerRequest) = ok().build()

