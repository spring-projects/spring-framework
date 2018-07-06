/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Allow to create easily a `RouterFunction<ServerResponse>` from a Kotlin router DSL based
 * on the same building blocks than the Java one ([RouterFunction], [RequestPredicate],
 * [HandlerFunction]).
 *
 * Example:
 *
 * ```
 * @Configuration
 * class ApplicationRoutes(val userHandler: UserHandler) {
 *
 * 	@Bean
 * 	fun mainRouter() = router {
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
 * @see RouterFunctionDsl
 * @since 5.0
 */
fun router(routes: RouterFunctionDsl.() -> Unit) = RouterFunctionDsl(routes).invoke()

/**
 * Provide a [RouterFunction] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @author Yevhenii Melnyk
 * @since 5.0
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-15667">Kotlin issue about supporting ::foo for member functions</a>
 */
open class RouterFunctionDsl(private val init: RouterFunctionDsl.() -> Unit) : () -> RouterFunction<ServerResponse> {

	private val routes = mutableListOf<RouterFunction<ServerResponse>>()

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
		routes += RouterFunctions.nest(this, RouterFunctionDsl(init).invoke())
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
		routes += RouterFunctions.nest(path(this), RouterFunctionDsl(init).invoke())
	}

	/**
	 * Route to the given handler function if the given request predicate applies.
	 * @see RouterFunctions.route
	 */
	fun GET(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.GET(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code GET}
	 * and the given {@code pattern} matches against the request path.
	 * @see RequestPredicates.GET
	 */
	fun GET(pattern: String): RequestPredicate = RequestPredicates.GET(pattern)

	/**
	 * Route to the given handler function if the given request predicate applies.
	 * @see RouterFunctions.route
	 */
	fun HEAD(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.HEAD(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code HEAD}
	 * and the given {@code pattern} matches against the request path.
	 * @see RequestPredicates.HEAD
	 */
	fun HEAD(pattern: String): RequestPredicate = RequestPredicates.HEAD(pattern)

	/**
	 * Route to the given handler function if the given POST predicate applies.
	 * @see RouterFunctions.route
	 */
	fun POST(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.POST(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code POST}
	 * and the given {@code pattern} matches against the request path.
	 * @see RequestPredicates.POST
	 */
	fun POST(pattern: String): RequestPredicate = RequestPredicates.POST(pattern)

	/**
	 * Route to the given handler function if the given PUT predicate applies.
	 * @see RouterFunctions.route
	 */
	fun PUT(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.PUT(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code PUT}
	 * and the given {@code pattern} matches against the request path.
	 * @see RequestPredicates.PUT
	 */
	fun PUT(pattern: String): RequestPredicate = RequestPredicates.PUT(pattern)

	/**
	 * Route to the given handler function if the given PATCH predicate applies.
	 * @see RouterFunctions.route
	 */
	fun PATCH(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.PATCH(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code PATCH}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is PATCH and if the given pattern
	 * matches against the request path
	 */
	fun PATCH(pattern: String): RequestPredicate = RequestPredicates.PATCH(pattern)

	/**
	 * Route to the given handler function if the given DELETE predicate applies.
	 * @see RouterFunctions.route
	 */
	fun DELETE(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.DELETE(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code DELETE}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is DELETE and if the given pattern
	 * matches against the request path
	 */
	fun DELETE(pattern: String): RequestPredicate = RequestPredicates.DELETE(pattern)

	/**
	 * Route to the given handler function if the given OPTIONS predicate applies.
	 * @see RouterFunctions.route
	 */
	fun OPTIONS(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.OPTIONS(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code OPTIONS}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is OPTIONS and if the given pattern
	 * matches against the request path
	 */
	fun OPTIONS(pattern: String): RequestPredicate = RequestPredicates.OPTIONS(pattern)

	/**
	 * Route to the given handler function if the given accept predicate applies.
	 * @see RouterFunctions.route
	 */
	fun accept(mediaType: MediaType, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.accept(mediaType), HandlerFunction { f(it) })
	}

	/**
	* Return a {@code RequestPredicate} that tests if the request's
	* {@linkplain ServerRequest.Headers#accept() accept} header is
	* {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with any of the given media types.
	* @param mediaTypes the media types to match the request's accept header against
	* @return a predicate that tests the request's accept header against the given media types
	*/
	fun accept(mediaType: MediaType): RequestPredicate = RequestPredicates.accept(mediaType)

	/**
	 * Route to the given handler function if the given contentType predicate applies.
	 * @see RouterFunctions.route
	 */
	fun contentType(mediaType: MediaType, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.contentType(mediaType), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that tests if the request's
	 * {@linkplain ServerRequest.Headers#contentType() content type} is
	 * {@linkplain MediaType#includes(MediaType) included} by any of the given media types.
	 * @param mediaTypes the media types to match the request's content type against
	 * @return a predicate that tests the request's content type against the given media types
	 */
	fun contentType(mediaType: MediaType): RequestPredicate = RequestPredicates.contentType(mediaType)

	/**
	 * Route to the given handler function if the given headers predicate applies.
	 * @see RouterFunctions.route
	 */
	fun headers(headersPredicate: (ServerRequest.Headers) -> Boolean, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.headers(headersPredicate), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request's headers against the given headers predicate.
	 * @param headersPredicate a predicate that tests against the request headers
	 * @return a predicate that tests against the given header predicate
	 */
	fun headers(headersPredicate: (ServerRequest.Headers) -> Boolean): RequestPredicate =
			RequestPredicates.headers(headersPredicate)

	/**
	 * Route to the given handler function if the given method predicate applies.
	 * @see RouterFunctions.route
	 */
	fun method(httpMethod: HttpMethod, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.method(httpMethod), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that tests against the given HTTP method.
	 * @param httpMethod the HTTP method to match to
	 * @return a predicate that tests against the given HTTP method
	 */
	fun method(httpMethod: HttpMethod): RequestPredicate = RequestPredicates.method(httpMethod)

	/**
	 * Route to the given handler function if the given path predicate applies.
	 * @see RouterFunctions.route
	 */
	fun path(pattern: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.path(pattern), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request path against the given path pattern.
	 * @see RequestPredicates.path
	 */
	fun path(pattern: String): RequestPredicate = RequestPredicates.path(pattern)

	/**
	 * Route to the given handler function if the given pathExtension predicate applies.
	 * @see RouterFunctions.route
	 */
	fun pathExtension(extension: String, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.pathExtension(extension), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's path has the given extension.
	 * @param extension the path extension to match against, ignoring case
	 * @return a predicate that matches if the request's path has the given file extension
	 */
	fun pathExtension(extension: String): RequestPredicate = RequestPredicates.pathExtension(extension)

	/**
	 * Route to the given handler function if the given pathExtension predicate applies.
	 * @see RouterFunctions.route
	 */
	fun pathExtension(predicate: (String) -> Boolean, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.pathExtension(predicate), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's path matches the given
	 * predicate.
	 * @see RequestPredicates.pathExtension
	 */
	fun pathExtension(predicate: (String) -> Boolean): RequestPredicate =
			RequestPredicates.pathExtension(predicate)

	/**
	 * Route to the given handler function if the given queryParam predicate applies.
	 * @see RouterFunctions.route
	 */
	fun queryParam(name: String, predicate: (String) -> Boolean, f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.queryParam(name, predicate), HandlerFunction { f(it) })
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request's query parameter of the given name
	 * against the given predicate.
	 * @param name the name of the query parameter to test against
	 * @param predicate predicate to test against the query parameter value
	 * @return a predicate that matches the given predicate against the query parameter of the given name
	 * @see ServerRequest#queryParam(String)
	 */
	fun queryParam(name: String, predicate: (String) -> Boolean): RequestPredicate =
			RequestPredicates.queryParam(name, predicate)

	/**
	 * Route to the given handler function if the given request predicate applies.
	 * @see RouterFunctions.route
	 */
	operator fun RequestPredicate.invoke(f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(this, HandlerFunction { f(it) })
	}

	/**
	 * Route to the given handler function if the given predicate (String
	 * processed as a path predicate) applies.
	 * @see RouterFunctions.route
	 */
	operator fun String.invoke(f: (ServerRequest) -> Mono<ServerResponse>) {
		routes += RouterFunctions.route(RequestPredicates.path(this),  HandlerFunction { f(it) })
	}

	/**
	 * Route requests that match the given pattern to resources relative to the given root location.
	 * @see RouterFunctions.resources
	 */
	fun resources(path: String, location: Resource) {
		routes += RouterFunctions.resources(path, location)
	}

	/**
	 * Route to resources using the provided lookup function. If the lookup function provides a
	 * [Resource] for the given request, it will be it will be exposed using a
	 * [HandlerFunction] that handles GET, HEAD, and OPTIONS requests.
	 */
	fun resources(lookupFunction: (ServerRequest) -> Mono<Resource>) {
		routes += RouterFunctions.resources(lookupFunction)
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
	fun status(status: HttpStatus): ServerResponse.BodyBuilder =
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

	/**
	 * Return a composed routing function created from all the registered routes.
	 * @since 5.1
	 */
	override fun invoke(): RouterFunction<ServerResponse> {
		init()
		return routes.reduce(RouterFunction<ServerResponse>::and)
	}

}