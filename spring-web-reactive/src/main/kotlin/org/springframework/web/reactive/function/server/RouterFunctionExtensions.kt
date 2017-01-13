package org.springframework.web.reactive.function.server

import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import reactor.core.publisher.Mono

/**
 * Provide a routing DSL for [RouterFunctions] and [RouterFunction] in order to be able to
 * write idiomatic Kotlin code as below:
 *
 * * ```kotlin
 * fun route(request: ServerRequest) = route(request) {
 * 		accept(TEXT_HTML).apply {
 * 			(GET("/user/") or GET("/users/")) { findAllView() }
 * 			GET("/user/{login}") { findViewById() }
 * 		}
 * 		accept(APPLICATION_JSON).apply {
 * 			(GET("/api/user/") or GET("/api/users/")) { findAll() }
 * 			POST("/api/user/") { create() }
 * 			POST("/api/user/{login}") { findOne() }
 * 		}
 * 	}
 * ```
 *
 * @since 5.0
 * @author Sebastien Deleuze
 * @author Yevhenii Melnyk
 */
fun RouterFunction<*>.route(request: ServerRequest, configure: RouterDsl.() -> Unit) =
		RouterDsl().apply(configure).invoke(request)

class RouterDsl {

	val children = mutableListOf<RouterDsl>()
	val routes = mutableListOf<RouterFunction<ServerResponse>>()

	operator fun RequestPredicate.invoke(f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(this, f())
	}

	infix fun RequestPredicate.and(other: RequestPredicate): RequestPredicate = this.and(other)

	infix fun RequestPredicate.or(other: RequestPredicate): RequestPredicate = this.or(other)

	operator fun RequestPredicate.not(): RequestPredicate = this.negate()

	fun GET(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.GET(pattern), f())
	}

	fun HEAD(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.HEAD(pattern), f())
	}

	fun POST(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.POST(pattern), f())
	}

	fun PUT(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.PUT(pattern), f())
	}

	fun PATCH(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.PATCH(pattern), f())
	}

	fun DELETE(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.DELETE(pattern), f())
	}

	fun OPTIONS(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.OPTIONS(pattern), f())
	}

	fun accept(vararg mediaType: MediaType, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.accept(*mediaType), f())
	}

	fun contentType(vararg mediaType: MediaType, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.contentType(*mediaType), f())
	}

	fun headers(headerPredicate: (ServerRequest.Headers)->Boolean, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.headers(headerPredicate), f())
	}

	fun method(httpMethod: HttpMethod, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.method(httpMethod), f())
	}

	fun path(pattern: String, f: () -> HandlerFunction<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.path(pattern), f())
	}

	fun resources(path: String, location: Resource) {
		routes +=  RouterFunctions.resources(path, location)
	}

	fun resources(lookupFunction: (ServerRequest) -> Mono<Resource>) {
		routes +=  RouterFunctions.resources(lookupFunction)
	}


	@Suppress("UNCHECKED_CAST")
	fun router(): RouterFunction<ServerResponse> {
		return routes().reduce(RouterFunction<*>::and) as RouterFunction<ServerResponse>
	}

	operator fun invoke(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
		return router().route(request)
	}

	private fun routes(): List<RouterFunction<ServerResponse>> {
		val allRoutes = mutableListOf<RouterFunction<ServerResponse>>()
		allRoutes += routes
		for (child in children) {
			allRoutes += child.routes()
		}
		return allRoutes
	}

}
