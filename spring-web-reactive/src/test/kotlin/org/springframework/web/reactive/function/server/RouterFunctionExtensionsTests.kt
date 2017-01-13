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

		override fun route(request: ServerRequest) = route(request) {
			(GET("/foo/") or GET("/foos/")) { handle() }
			accept(APPLICATION_JSON).apply {
				POST("/api/foo/") { handle() }
				PUT("/api/foo/") { handle() }
				DELETE("/api/foo/") { handle() }
			}
			accept(APPLICATION_ATOM_XML) { handle() }
			contentType(APPLICATION_OCTET_STREAM) { handle() }
			method(HttpMethod.PATCH) { handle() }
			headers({ it.accept().contains(APPLICATION_JSON) }).apply {
				GET("/api/foo/") { handle() }
			}
			headers({ it.header("bar").isNotEmpty() }) { handle() }
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
			path("/baz") { handle() }
		}

		fun handle() = HandlerFunction { ok().build() }

	}

}
