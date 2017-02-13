package org.springframework.web.reactive.function.server

import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import reactor.core.publisher.Mono

/**
 * Provide a routing DSL for [RouterFunctions] and [RouterFunction] in order to be able to
 * write idiomatic Kotlin code as below:
 *
 * ```kotlin
 * import org.springframework.web.reactive.function.server.RequestPredicates.*
 * ...
 *
 * @Controller
 * class FooController : RouterFunction<ServerResponse> {
 *
 * 		override fun route(req: ServerRequest) = route(req) {
 * 			html().apply {
 * 				(GET("/user/") or GET("/users/")) { findAllView() }
 * 				GET("/user/{login}", this@FooController::findViewById)
 * 			}
 * 			json().apply {
 * 				(GET("/api/user/") or GET("/api/users/")) { findAll() }
 * 				POST("/api/user/", this@FooController::create)
 * 			}
 * 		}
 *
 * 		fun findAllView() = ...
 * 		fun findViewById(req: ServerRequest) = ...
 * 		fun findAll() = ...
 * 		fun create(req: ServerRequest) =
 * 	}
 * ```
 *
 * @since 5.0
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-15667">Kotlin issue about supporting ::foo for member functions</a>
 * @author Sebastien Deleuze
 * @author Yevhenii Melnyk
 */
fun RouterFunction<*>.route(request: ServerRequest, configure: RouterDsl.() -> Unit) =
		RouterDsl().apply(configure).invoke(request)

class RouterDsl {

	val children = mutableListOf<RouterDsl>()
	val routes = mutableListOf<RouterFunction<ServerResponse>>()

	infix fun RequestPredicate.and(other: RequestPredicate): RequestPredicate = this.and(other)

	infix fun RequestPredicate.or(other: RequestPredicate): RequestPredicate = this.or(other)

	operator fun RequestPredicate.not(): RequestPredicate = this.negate()

	operator fun RequestPredicate.invoke(f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(this, HandlerFunction { f(it) })
	}

	fun GET(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.GET(pattern), HandlerFunction { f(it) })
	}

	fun HEAD(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.HEAD(pattern), HandlerFunction { f(it) })
	}

	fun POST(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.POST(pattern), HandlerFunction { f(it) })
	}

	fun PUT(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.PUT(pattern), HandlerFunction { f(it) })
	}

	fun PATCH(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.PATCH(pattern), HandlerFunction { f(it) })
	}

	fun DELETE(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.DELETE(pattern), HandlerFunction { f(it) })
	}

	fun OPTIONS(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.OPTIONS(pattern), HandlerFunction { f(it) })
	}

	fun accept(mediaType: MediaType, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.accept(mediaType), HandlerFunction { f(it) })
	}

	fun contentType(mediaType: MediaType, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.contentType(mediaType), HandlerFunction { f(it) })
	}

	fun headers(headerPredicate: (ServerRequest.Headers) -> Boolean, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.headers(headerPredicate), HandlerFunction { f(it) })
	}

	fun method(httpMethod: HttpMethod, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.method(httpMethod), HandlerFunction { f(it) })
	}

	fun path(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.path(pattern), HandlerFunction { f(it) })
	}

	fun pathExtension(extension: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.pathExtension(extension), HandlerFunction { f(it) })
	}

	fun pathExtension(predicate: (String) -> Boolean, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.pathExtension(predicate), HandlerFunction { f(it) })
	}

	fun queryParam(name: String, predicate: (String) -> Boolean, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.queryParam(name, predicate), HandlerFunction { f(it) })
	}

	fun json(f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.json(), HandlerFunction { f(it) })
	}

	fun html(f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.html(), HandlerFunction { f(it) })
	}

	fun xml(f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.xml(), HandlerFunction { f(it) })
	}

	fun resources(path: String, location: Resource) {
		routes += RouterFunctions.resources(path, location)
	}

	fun resources(lookupFunction: (ServerRequest) -> Mono<Resource>) {
		routes += RouterFunctions.resources(lookupFunction)
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
