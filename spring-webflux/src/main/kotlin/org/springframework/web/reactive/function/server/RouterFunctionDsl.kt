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

import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import java.net.URI
import java.util.function.Supplier

/**
 * Allow to create easily a WebFlux.fn [RouterFunction] with a [Reactive router Kotlin DSL][RouterFunctionDsl].
 *
 * Example:
 *
 * ```
 * @Configuration
 * class RouterConfiguration {
 *
 * 	@Bean
 * 	fun mainRouter(userHandler: UserHandler) = router {
 * 		accept(TEXT_HTML).nest {
 * 			(GET("/user/") or GET("/users/")).invoke(userHandler::findAllView)
 * 			GET("/users/{login}", userHandler::findViewById)
 * 		}
 * 		accept(APPLICATION_JSON).nest {
 * 			(GET("/api/user/") or GET("/api/users/")).invoke(userHandler::findAll)
 * 			POST("/api/users/", userHandler::create)
 * 		}
 * 	}
 *
 * }
 * ```
 * @author Sebastien Deleuze
 * @see coRouter
 * @since 5.0
 */
fun router(routes: RouterFunctionDsl.() -> Unit) = RouterFunctionDsl(routes).build()

/**
 * Provide a WebFlux.fn [RouterFunction] Reactive Kotlin DSL created by [`router { }`][router] in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @author Yevhenii Melnyk
 * @author Arjen Poutsma
 * @since 5.0
 */
class RouterFunctionDsl internal constructor (private val init: RouterFunctionDsl.() -> Unit) {

	@PublishedApi
	internal val builder = RouterFunctions.route()

	/**
	 * Return a composed request predicate that tests against both this predicate AND
	 * the [other] predicate (String processed as a path predicate). When evaluating the
	 * composed predicate, if this predicate is `false`, then the [other] predicate is not
	 * evaluated.
	 * @see RequestPredicate.and
	 * @see RequestPredicates.path
	 */
	infix fun RequestPredicate.and(other: String): RequestPredicate = this.and(path(other))

	/**
	 * Return a composed request predicate that tests against both this predicate OR
	 * the [other] predicate (String processed as a path predicate). When evaluating the
	 * composed predicate, if this predicate is `true`, then the [other] predicate is not
	 * evaluated.
	 * @see RequestPredicate.or
	 * @see RequestPredicates.path
	 */
	infix fun RequestPredicate.or(other: String): RequestPredicate = this.or(path(other))

	/**
	 * Return a composed request predicate that tests against both this predicate (String
	 * processed as a path predicate) AND the [other] predicate. When evaluating the
	 * composed predicate, if this predicate is `false`, then the [other] predicate is not
	 * evaluated.
	 * @see RequestPredicate.and
	 * @see RequestPredicates.path
	 */
	infix fun String.and(other: RequestPredicate): RequestPredicate = path(this).and(other)

	/**
	 * Return a composed request predicate that tests against both this predicate (String
	 * processed as a path predicate) OR the [other] predicate. When evaluating the
	 * composed predicate, if this predicate is `true`, then the [other] predicate is not
	 * evaluated.
	 * @see RequestPredicate.or
	 * @see RequestPredicates.path
	 */
	infix fun String.or(other: RequestPredicate): RequestPredicate = path(this).or(other)

	/**
	 * Return a composed request predicate that tests against both this predicate AND
	 * the [other] predicate. When evaluating the composed predicate, if this
	 * predicate is `false`, then the [other] predicate is not evaluated.
	 * @see RequestPredicate.and
	 */
	infix fun RequestPredicate.and(other: RequestPredicate): RequestPredicate = this.and(other)

	/**
	 * Return a composed request predicate that tests against both this predicate OR
	 * the [other] predicate. When evaluating the composed predicate, if this
	 * predicate is `true`, then the [other] predicate is not evaluated.
	 * @see RequestPredicate.or
	 */
	infix fun RequestPredicate.or(other: RequestPredicate): RequestPredicate = this.or(other)

	/**
	 * Return a predicate that represents the logical negation of this predicate.
	 */
	operator fun RequestPredicate.not(): RequestPredicate = this.negate()

	/**
	 * Route to the given router function if the given request predicate applies. This
	 * method can be used to create *nested routes*, where a group of routes share a
	 * common path (prefix), header, or other request predicate.
	 * @see RouterFunctions.nest
	 */
	fun RequestPredicate.nest(init: RouterFunctionDsl.() -> Unit) {
		builder.nest(this, Supplier { RouterFunctionDsl(init).build() })
	}

	/**
	 * Route to the given router function if the given request predicate (String
	 * processed as a path predicate) applies. This method can be used to create
	 * *nested routes*, where a group of routes share a common path
	 * (prefix), header, or other request predicate.
	 * @see RouterFunctions.nest
	 * @see RequestPredicates.path
	*/
	fun String.nest(init: RouterFunctionDsl.() -> Unit) {
		builder.path(this, Supplier { RouterFunctionDsl(init).build() })
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `GET` requests.
	 * @since 5.3
	 */
	fun GET(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.GET { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `GET` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun GET(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.GET(pattern) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `GET` requests
	 * that match the given predicate.
	 * @param predicate predicate to match
	 * @since 5.3
	 */
	fun GET(predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.GET(predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `GET` requests
	 * that match the given pattern and predicate.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun GET(pattern: String, predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.GET(pattern, predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `GET`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.GET
	 */
	fun GET(pattern: String): RequestPredicate = RequestPredicates.GET(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `HEAD` requests.
	 * @since 5.3
	 */
	fun HEAD(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.HEAD { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `HEAD` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun HEAD(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.HEAD(pattern) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `HEAD` requests
	 * that match the given predicate.
	 * @param predicate predicate to match
	 * @since 5.3
	 */
	fun HEAD(predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.HEAD(predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `HEAD` requests
	 * that match the given pattern and predicate.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun HEAD(pattern: String, predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.HEAD(pattern, predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `HEAD`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.HEAD
	 */
	fun HEAD(pattern: String): RequestPredicate = RequestPredicates.HEAD(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `POST` requests.
	 * @since 5.3
	 */
	fun POST(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.POST { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `POST` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun POST(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.POST(pattern) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `POST` requests
	 * that match the given predicate.
	 * @param predicate predicate to match
	 * @since 5.3
	 */
	fun POST(predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.POST(predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `POST` requests
	 * that match the given pattern and predicate.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun POST(pattern: String, predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.POST(pattern, predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `POST`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.POST
	 */
	fun POST(pattern: String): RequestPredicate = RequestPredicates.POST(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `PUT` requests.
	 * @since 5.3
	 */
	fun PUT(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PUT { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PUT` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun PUT(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PUT(pattern) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PUT` requests
	 * that match the given predicate.
	 * @param predicate predicate to match
	 * @since 5.3
	 */
	fun PUT(predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PUT(predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PUT` requests
	 * that match the given pattern and predicate.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun PUT(pattern: String, predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PUT(pattern, predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `PUT`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.PUT
	 */
	fun PUT(pattern: String): RequestPredicate = RequestPredicates.PUT(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `PATCH` requests.
	 * @since 5.3
	 */
	fun PATCH(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PATCH { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PATCH` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun PATCH(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PATCH(pattern) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PATCH` requests
	 * that match the given predicate.
	 * @param predicate predicate to match
	 * @since 5.3
	 */
	fun PATCH(predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PATCH(predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PATCH` requests
	 * that match the given pattern and predicate.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun PATCH(pattern: String, predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.PATCH(pattern, predicate, { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `PATCH`
	 * and the given `pattern` matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is `PATCH` and if the given pattern
	 * matches against the request path
	 */
	fun PATCH(pattern: String): RequestPredicate = RequestPredicates.PATCH(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `DELETE` requests.
	 * @since 5.3
	 */
	fun DELETE(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.DELETE { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `DELETE` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun DELETE(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.DELETE(pattern) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `DELETE` requests
	 * that match the given predicate.
	 * @param predicate predicate to match
	 * @since 5.3
	 */
	fun DELETE(predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.DELETE(predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `DELETE` requests
	 * that match the given pattern and predicate.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun DELETE(pattern: String, predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.DELETE(pattern, predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `DELETE`
	 * and the given `pattern` matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is `DELETE` and if the given pattern
	 * matches against the request path
	 */
	fun DELETE(pattern: String): RequestPredicate = RequestPredicates.DELETE(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `OPTIONS` requests.
	 * @since 5.3
	 */
	fun OPTIONS(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.OPTIONS { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `OPTIONS` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun OPTIONS(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.OPTIONS(pattern) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `OPTIONS` requests
	 * that match the given predicate.
	 * @param predicate predicate to match
	 * @since 5.3
	 */
	fun OPTIONS(predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.OPTIONS(predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `OPTIONS` requests
	 * that match the given pattern and predicate.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun OPTIONS(pattern: String, predicate: RequestPredicate, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.OPTIONS(pattern, predicate) { f(it).cast(ServerResponse::class.java) }
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `OPTIONS`
	 * and the given `pattern` matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is `OPTIONS` and if the given pattern
	 * matches against the request path
	 */
	fun OPTIONS(pattern: String): RequestPredicate = RequestPredicates.OPTIONS(pattern)

	/**
	 * Route to the given handler function if the given accept predicate applies.
	 * @see RouterFunctions.route
	 */
	fun accept(mediaType: MediaType, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.accept(mediaType)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	* Return a [RequestPredicate] that tests if the request's
	* [accept][ServerRequest.Headers.accept] header is
	* [compatible][MediaType.isCompatibleWith] with any of the given media types.
	* @param mediaTypes the media types to match the request's accept header against
	* @return a predicate that tests the request's accept header against the given media types
	*/
	fun accept(vararg mediaTypes: MediaType): RequestPredicate = RequestPredicates.accept(*mediaTypes)

	/**
	 * Route to the given handler function if the given contentType predicate applies.
	 * @see RouterFunctions.route
	 */
	fun contentType(mediaTypes: MediaType, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.contentType(mediaTypes)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that tests if the request's
	 * [content type][ServerRequest.Headers.contentType] is
	 * [included][MediaType.includes] by any of the given media types.
	 * @param mediaTypes the media types to match the request's content type against
	 * @return a predicate that tests the request's content type against the given media types
	 */
	fun contentType(vararg mediaTypes: MediaType): RequestPredicate = RequestPredicates.contentType(*mediaTypes)

	/**
	 * Route to the given handler function if the given headers predicate applies.
	 * @see RouterFunctions.route
	 */
	fun headers(headersPredicate: (ServerRequest.Headers) -> Boolean, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.headers(headersPredicate)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that tests the request's headers against the given headers predicate.
	 * @param headersPredicate a predicate that tests against the request headers
	 * @return a predicate that tests against the given header predicate
	 */
	fun headers(headersPredicate: (ServerRequest.Headers) -> Boolean): RequestPredicate =
			RequestPredicates.headers(headersPredicate)

	/**
	 * Route to the given handler function if the given method predicate applies.
	 * @see RouterFunctions.route
	 */
	fun method(httpMethod: HttpMethod, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.method(httpMethod)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that tests against the given HTTP method.
	 * @param httpMethod the HTTP method to match to
	 * @return a predicate that tests against the given HTTP method
	 */
	fun method(httpMethod: HttpMethod): RequestPredicate = RequestPredicates.method(httpMethod)

	/**
	 * Route to the given handler function if the given path predicate applies.
	 * @see RouterFunctions.route
	 */
	fun path(pattern: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.path(pattern)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that tests the request path against the given path pattern.
	 * @see RequestPredicates.path
	 */
	fun path(pattern: String): RequestPredicate = RequestPredicates.path(pattern)

	/**
	 * Route to the given handler function if the given pathExtension predicate applies.
	 * @see RouterFunctions.route
	 */
	fun pathExtension(extension: String, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.pathExtension(extension)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that matches if the request's path has the given extension.
	 * @param extension the path extension to match against, ignoring case
	 * @return a predicate that matches if the request's path has the given file extension
	 */
	fun pathExtension(extension: String): RequestPredicate = RequestPredicates.pathExtension(extension)

	/**
	 * Route to the given handler function if the given pathExtension predicate applies.
	 * @see RouterFunctions.route
	 */
	fun pathExtension(predicate: (String?) -> Boolean, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.pathExtension(predicate)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that matches if the request's path matches the given
	 * predicate.
	 * @see RequestPredicates.pathExtension
	 */
	fun pathExtension(predicate: (String?) -> Boolean): RequestPredicate =
			RequestPredicates.pathExtension(predicate)

	/**
	 * Route to the given handler function if the given queryParam predicate applies.
	 * @see RouterFunctions.route
	 */
	fun queryParam(name: String, predicate: (String) -> Boolean, f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.queryParam(name, predicate)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Return a [RequestPredicate] that tests the request's query parameter of the given name
	 * against the given predicate.
	 * @param name the name of the query parameter to test against
	 * @param predicate the predicate to test against the query parameter value
	 * @return a predicate that matches the given predicate against the query parameter of the given name
	 * @see ServerRequest#queryParam(String)
	 */
	fun queryParam(name: String, predicate: (String) -> Boolean): RequestPredicate =
			RequestPredicates.queryParam(name, predicate)

	/**
	 * Route to the given handler function if the given request predicate applies.
	 * @see RouterFunctions.route
	 */
	operator fun RequestPredicate.invoke(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(this) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Route to the given handler function if the given predicate (String
	 * processed as a path predicate) applies.
	 * @see RouterFunctions.route
	 */
	operator fun String.invoke(f: (ServerRequest) -> Mono<out ServerResponse>) {
		builder.add(RouterFunctions.route(RequestPredicates.path(this)) { f(it).cast(ServerResponse::class.java) })
	}

	/**
	 * Route requests that match the given predicate to the given resource.
	 * @since 6.1.4
	 * @see RouterFunctions.resource
	 */
	fun resource(predicate: RequestPredicate, resource: Resource) {
		builder.resource(predicate, resource)
	}

	/**
	 * Route requests that match the given pattern to resources relative to the given root location.
	 * @see RouterFunctions.resources
	 */
	fun resources(path: String, location: Resource) {
		builder.resources(path, location)
	}

	/**
	 * Route to resources using the provided lookup function. If the lookup function provides a
	 * [Resource] for the given request, it will be it will be exposed using a
	 * [HandlerFunction] that handles GET, HEAD, and OPTIONS requests.
	 */
	fun resources(lookupFunction: (ServerRequest) -> Mono<Resource>) {
		builder.resources(lookupFunction)
	}

	/**
	 * Merge externally defined router functions into this one.
	 * @param routerFunction the router function to be added
	 * @since 5.2
	 */
	fun add(routerFunction: RouterFunction<ServerResponse>) {
		builder.add(routerFunction)
	}

	/**
	 * Filters all routes created by this router with the given filter function. Filter
	 * functions are typically used to address cross-cutting concerns, such as logging,
	 * security, etc.
	 * @param filterFunction the function to filter all routes built by this router
	 * @since 5.2
	 */
	fun filter(filterFunction: (ServerRequest, (ServerRequest) -> Mono<ServerResponse>) -> Mono<ServerResponse>) {
		builder.filter { request, next ->
			filterFunction(request) {
				next.handle(it)
			}
		}
	}

	/**
	 * Filter the request object for all routes created by this builder with the given request
	 * processing function. Filters are typically used to address cross-cutting concerns, such
	 * as logging, security, etc.
	 * @param requestProcessor a function that transforms the request
	 * @since 5.2
	 */
	fun before(requestProcessor: (ServerRequest) -> ServerRequest) {
		builder.before(requestProcessor)
	}

	/**
	 * Filter the response object for all routes created by this builder with the given response
	 * processing function. Filters are typically used to address cross-cutting concerns, such
	 * as logging, security, etc.
	 * @param responseProcessor a function that transforms the response
	 * @since 5.2
	 */
	fun after(responseProcessor: (ServerRequest, ServerResponse) -> ServerResponse) {
		builder.after(responseProcessor)
	}

	/**
	 * Filters all exceptions that match the predicate by applying the given response provider
	 * function.
	 * @param predicate the type of exception to filter
	 * @param responseProvider a function that creates a response
	 * @since 5.2
	 */
	fun onError(predicate: (Throwable) -> Boolean, responseProvider: (Throwable, ServerRequest) -> Mono<ServerResponse>) {
		builder.onError(predicate, responseProvider)
	}

	/**
	 * Filters all exceptions that match the predicate by applying the given response provider
	 * function.
	 * @param E the type of exception to filter
	 * @param responseProvider a function that creates a response
	 * @since 5.2
	 */
	inline fun <reified E : Throwable> onError(noinline responseProvider: (Throwable, ServerRequest) -> Mono<ServerResponse>) {
		builder.onError({it is E}, responseProvider)
	}

	/**
	 * Add an attribute with the given name and value to the last route built with this builder.
	 * @param name the attribute name
	 * @param value the attribute value
	 * @since 6.0
	 */
	fun withAttribute(name: String, value: Any) {
		builder.withAttribute(name, value)
	}

	/**
	 * Manipulate the attributes of the last route built with the given consumer.
	 *
	 * The map provided to the consumer is "live", so that the consumer can be used
	 * to [overwrite][MutableMap.put] existing attributes,
	 * [remove][MutableMap.remove] attributes, or use any of the other
	 * [MutableMap] methods.
	 * @param attributesConsumer a function that consumes the attributes map
	 * @since 6.0
	 */
	fun withAttributes(attributesConsumer: (MutableMap<String, Any>) -> Unit) {
		builder.withAttributes(attributesConsumer)
	}

	/**
	 * Return a composed routing function created from all the registered routes.
	 * @since 5.1
	 */
	internal fun build(): RouterFunction<ServerResponse> {
		init()
		return builder.build()
	}

	/**
	 * Create a builder with the status code and headers of the given response.
	 * @param other the response to copy the status and headers from
	 * @return the created builder
	 * @since 5.1
	 */
	fun from(other: ServerResponse): ServerResponse.BodyBuilder =
			ServerResponse.from(other)

	/**
	 * Create a builder with the given HTTP status.
	 * @param status the response status
	 * @return the created builder
	 * @since 5.1
	 */
	fun status(status: HttpStatusCode): ServerResponse.BodyBuilder =
			ServerResponse.status(status)

	/**
	 * Create a builder with the given HTTP status.
	 * @param status the response status
	 * @return the created builder
	 * @since 5.1
	 */
	fun status(status: Int): ServerResponse.BodyBuilder =
			ServerResponse.status(status)

	/**
	 * Create a builder with the status set to [200 OK][HttpStatus.OK].
	 * @return the created builder
	 * @since 5.1
	 */
	fun ok(): ServerResponse.BodyBuilder =
			ServerResponse.ok()

	/**
	 * Create a new builder with a [201 Created][HttpStatus.CREATED] status
	 * and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 * @since 5.1
	 */
	fun created(location: URI): ServerResponse.BodyBuilder =
			ServerResponse.created(location)

	/**
	 * Create a builder with an [202 Accepted][HttpStatus.ACCEPTED] status.
	 * @return the created builder
	 * @since 5.1
	 */
	fun accepted(): ServerResponse.BodyBuilder =
			ServerResponse.accepted()

	/**
	 * Create a builder with a [204 No Content][HttpStatus.NO_CONTENT] status.
	 * @return the created builder
	 * @since 5.1
	 */
	fun noContent(): ServerResponse.HeadersBuilder<*> =
			ServerResponse.noContent()

	/**
	 * Create a builder with a [303 See Other][HttpStatus.SEE_OTHER]
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 * @since 5.1
	 */
	fun seeOther(location: URI): ServerResponse.BodyBuilder =
			ServerResponse.seeOther(location)

	/**
	 * Create a builder with a [307 Temporary Redirect][HttpStatus.TEMPORARY_REDIRECT]
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 * @since 5.1
	 */
	fun temporaryRedirect(location: URI): ServerResponse.BodyBuilder =
			ServerResponse.temporaryRedirect(location)

	/**
	 * Create a builder with a [308 Permanent Redirect][HttpStatus.PERMANENT_REDIRECT]
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 * @since 5.1
	 */
	fun permanentRedirect(location: URI): ServerResponse.BodyBuilder =
			ServerResponse.permanentRedirect(location)

	/**
	 * Create a builder with a [400 Bad Request][HttpStatus.BAD_REQUEST] status.
	 * @return the created builder
	 * @since 5.1
	 */
	fun badRequest(): ServerResponse.BodyBuilder =
			ServerResponse.badRequest()

	/**
	 * Create a builder with a [404 Not Found][HttpStatus.NOT_FOUND] status.
	 * @return the created builder
	 * @since 5.1
	 */
	fun notFound(): ServerResponse.HeadersBuilder<*> =
			ServerResponse.notFound()

	/**
	 * Create a builder with an
	 * [422 Unprocessable Entity][HttpStatus.UNPROCESSABLE_ENTITY] status.
	 * @return the created builder
	 * @since 5.1
	 */
	fun unprocessableEntity(): ServerResponse.BodyBuilder =
			ServerResponse.unprocessableEntity()

}
