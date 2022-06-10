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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunctions.nest
import java.net.URI

/**
 * Allow to create easily a WebFlux.fn [RouterFunction] with a [Coroutines router Kotlin DSL][CoRouterFunctionDsl].
 *
 * Example:
 *
 * ```
 * @Configuration
 * class RouterConfiguration {
 *
 * 	@Bean
 * 	fun mainRouter(userHandler: UserHandler) = coRouter {
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
 *
 * @author Sebastien Deleuze
 * @see router
 * @since 5.2
 */
fun coRouter(routes: (CoRouterFunctionDsl.() -> Unit)) =
		CoRouterFunctionDsl(routes).build()

/**
 * Provide a WebFlux.fn [RouterFunction] Coroutines Kotlin DSL created by [`coRouter { }`][coRouter] in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
class CoRouterFunctionDsl internal constructor (private val init: (CoRouterFunctionDsl.() -> Unit)) {

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
	fun RequestPredicate.nest(r: (CoRouterFunctionDsl.() -> Unit)) {
		builder.add(nest(this, CoRouterFunctionDsl(r).build()))
	}


	/**
	 * Route to the given router function if the given request predicate (String
	 * processed as a path predicate) applies. This method can be used to create
	 * *nested routes*, where a group of routes share a common path
	 * (prefix), header, or other request predicate.
	 * @see RouterFunctions.nest
	 * @see RequestPredicates.path
	 */
	fun String.nest(r: (CoRouterFunctionDsl.() -> Unit)) = path(this).nest(r)

	/**
	 * Adds a route to the given handler function that handles all HTTP `GET` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun GET(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.GET(pattern, asHandlerFunction(f))
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `GET` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun GET(pattern: String, predicate: RequestPredicate, f: suspend (ServerRequest) -> ServerResponse) {
		builder.GET(pattern, predicate, asHandlerFunction(f))
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `GET`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.GET
	 */
	fun GET(pattern: String): RequestPredicate = RequestPredicates.GET(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `HEAD` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun HEAD(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.HEAD(pattern, asHandlerFunction(f))
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `HEAD` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun HEAD(pattern: String, predicate: RequestPredicate, f: suspend (ServerRequest) -> ServerResponse) {
		builder.HEAD(pattern, predicate, asHandlerFunction(f))
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `HEAD`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.HEAD
	 */
	fun HEAD(pattern: String): RequestPredicate = RequestPredicates.HEAD(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `POST` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun POST(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.POST(pattern, asHandlerFunction(f))
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `POST` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun POST(pattern: String, predicate: RequestPredicate, f: suspend (ServerRequest) -> ServerResponse) {
		builder.POST(pattern, predicate, asHandlerFunction(f))
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `POST`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.POST
	 */
	fun POST(pattern: String): RequestPredicate = RequestPredicates.POST(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `PUT` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun PUT(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.PUT(pattern, asHandlerFunction(f))
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PUT` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun PUT(pattern: String, predicate: RequestPredicate, f: suspend (ServerRequest) -> ServerResponse) {
		builder.PUT(pattern, predicate, asHandlerFunction(f))
	}

	/**
	 * Return a [RequestPredicate] that matches if request's HTTP method is `PUT`
	 * and the given `pattern` matches against the request path.
	 * @see RequestPredicates.PUT
	 */
	fun PUT(pattern: String): RequestPredicate = RequestPredicates.PUT(pattern)

	/**
	 * Adds a route to the given handler function that handles all HTTP `PATCH` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun PATCH(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.PATCH(pattern, asHandlerFunction(f))
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `PATCH` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun PATCH(pattern: String, predicate: RequestPredicate, f: suspend (ServerRequest) -> ServerResponse) {
		builder.PATCH(pattern, predicate, asHandlerFunction(f))
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
	 * Adds a route to the given handler function that handles all HTTP `DELETE` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun DELETE(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.DELETE(pattern, asHandlerFunction(f))
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `DELETE` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun DELETE(pattern: String, predicate: RequestPredicate, f: suspend (ServerRequest) -> ServerResponse) {
		builder.DELETE(pattern, predicate, asHandlerFunction(f))
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
	 * Adds a route to the given handler function that handles all HTTP `OPTIONS` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 */
	fun OPTIONS(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.OPTIONS(pattern, asHandlerFunction(f))
	}

	/**
	 * Adds a route to the given handler function that handles all HTTP `OPTIONS` requests
	 * that match the given pattern.
	 * @param pattern the pattern to match to
	 * @param predicate additional predicate to match
	 * @since 5.2
	 */
	fun OPTIONS(pattern: String, predicate: RequestPredicate, f: suspend (ServerRequest) -> ServerResponse) {
		builder.OPTIONS(pattern, predicate, asHandlerFunction(f))
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
	fun accept(mediaType: MediaType, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.accept(mediaType), asHandlerFunction(f)))
	}

	/**
	 * Return a [RequestPredicate] that tests if the request's
	 * [accept][ServerRequest.Headers.accept] } header is
	 * [compatible][MediaType.isCompatibleWith] with any of the given media types.
	 * @param mediaType the media types to match the request's accept header against
	 * @return a predicate that tests the request's accept header against the given media types
	 */
	fun accept(vararg mediaType: MediaType): RequestPredicate = RequestPredicates.accept(*mediaType)

	/**
	 * Route to the given handler function if the given contentType predicate applies.
	 * @see RouterFunctions.route
	 */
	fun contentType(mediaType: MediaType, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.contentType(mediaType), asHandlerFunction(f)))
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
	fun headers(headersPredicate: (ServerRequest.Headers) -> Boolean, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.headers(headersPredicate), asHandlerFunction(f)))
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
	fun method(httpMethod: HttpMethod, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.method(httpMethod), asHandlerFunction(f)))
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
	fun path(pattern: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.path(pattern), asHandlerFunction(f)))
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
	fun pathExtension(extension: String, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.pathExtension(extension), asHandlerFunction(f)))
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
	fun pathExtension(predicate: (String) -> Boolean, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.pathExtension(predicate), asHandlerFunction(f)))
	}

	/**
	 * Return a [RequestPredicate] that matches if the request's path matches the given
	 * predicate.
	 * @see RequestPredicates.pathExtension
	 */
	fun pathExtension(predicate: (String) -> Boolean): RequestPredicate =
			RequestPredicates.pathExtension(predicate)

	/**
	 * Route to the given handler function if the given queryParam predicate applies.
	 * @see RouterFunctions.route
	 */
	fun queryParam(name: String, predicate: (String) -> Boolean, f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.queryParam(name, predicate), asHandlerFunction(f)))
	}

	/**
	 * Return a [RequestPredicate] that tests the request's query parameter of the given name
	 * against the given predicate.
	 * @param name the name of the query parameter to test against
	 * @param predicate the predicate to test against the query parameter value
	 * @return a predicate that matches the given predicate against the query parameter of the given name
	 * @see ServerRequest#queryParam
	 */
	fun queryParam(name: String, predicate: (String) -> Boolean): RequestPredicate =
			RequestPredicates.queryParam(name, predicate)

	/**
	 * Route to the given handler function if the given request predicate applies.
	 * @see RouterFunctions.route
	 */
	operator fun RequestPredicate.invoke(f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(this, asHandlerFunction(f)))
	}

	/**
	 * Route to the given handler function if the given predicate (String
	 * processed as a path predicate) applies.
	 * @see RouterFunctions.route
	 */
	operator fun String.invoke(f: suspend (ServerRequest) -> ServerResponse) {
		builder.add(RouterFunctions.route(RequestPredicates.path(this),  asHandlerFunction(f)))
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
	fun resources(lookupFunction: suspend (ServerRequest) -> Resource?) {
		builder.resources {
			mono(Dispatchers.Unconfined) {
				lookupFunction.invoke(it)
			}
		}
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
	fun filter(filterFunction: suspend (ServerRequest, suspend (ServerRequest) -> ServerResponse) -> ServerResponse) {
		builder.filter { serverRequest, handlerFunction ->
			mono(Dispatchers.Unconfined) {
				filterFunction(serverRequest) { handlerRequest ->
					handlerFunction.handle(handlerRequest).awaitSingle()
				}
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
	fun onError(predicate: (Throwable) -> Boolean, responseProvider: suspend (Throwable, ServerRequest) -> ServerResponse) {
		builder.onError(predicate) { throwable, request ->
			mono(Dispatchers.Unconfined) { responseProvider.invoke(throwable, request) }
		}
	}

	/**
	 * Filters all exceptions that match the predicate by applying the given response provider
	 * function.
	 * @param E the type of exception to filter
	 * @param responseProvider a function that creates a response
	 * @since 5.2
	 */
	inline fun <reified E : Throwable> onError(noinline responseProvider: suspend (Throwable, ServerRequest) -> ServerResponse) {
		builder.onError({it is E}) { throwable, request ->
			mono(Dispatchers.Unconfined) { responseProvider.invoke(throwable, request) }
		}
	}

	/**
	 * Add an attribute with the given name and value to the last route built with this builder.
	 * @param name the attribute name
	 * @param value the attribute value
-	 * @since 6.0
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
	 */
	internal fun build(): RouterFunction<ServerResponse> {
		init()
		return builder.build()
	}

	private fun asHandlerFunction(init: suspend (ServerRequest) -> ServerResponse) = HandlerFunction {
		mono(Dispatchers.Unconfined) {
			init(it)
		}
	}

	/**
	 * @see ServerResponse.from
	 */
	fun from(other: ServerResponse) =
			ServerResponse.from(other)

	/**
	 * @see ServerResponse.created
	 */
	fun created(location: URI) =
			ServerResponse.created(location)

	/**
	 * @see ServerResponse.ok
	 */
	fun ok() = ServerResponse.ok()

	/**
	 * @see ServerResponse.noContent
	 */
	fun noContent() = ServerResponse.noContent()

	/**
	 * @see ServerResponse.accepted
	 */
	fun accepted() = ServerResponse.accepted()

	/**
	 * @see ServerResponse.permanentRedirect
	 */
	fun permanentRedirect(location: URI) = ServerResponse.permanentRedirect(location)

	/**
	 * @see ServerResponse.temporaryRedirect
	 */
	fun temporaryRedirect(location: URI) = ServerResponse.temporaryRedirect(location)

	/**
	 * @see ServerResponse.seeOther
	 */
	fun seeOther(location: URI) = ServerResponse.seeOther(location)

	/**
	 * @see ServerResponse.badRequest
	 */
	fun badRequest() = ServerResponse.badRequest()

	/**
	 * @see ServerResponse.notFound
	 */
	fun notFound() = ServerResponse.notFound()

	/**
	 * @see ServerResponse.unprocessableEntity
	 */
	fun unprocessableEntity() = ServerResponse.unprocessableEntity()

	/**
	 * @see ServerResponse.status
	 */
	fun status(status: HttpStatusCode) = ServerResponse.status(status)

	/**
	 * @see ServerResponse.status
	 */
	fun status(status: Int) = ServerResponse.status(status)

}

/**
 * Equivalent to [RouterFunction.and].
 */
operator fun <T: ServerResponse> RouterFunction<T>.plus(other: RouterFunction<T>) =
		this.and(other)
