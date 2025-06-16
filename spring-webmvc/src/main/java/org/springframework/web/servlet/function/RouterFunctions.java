/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * <strong>Central entry point to Spring's functional web framework.</strong>
 * Exposes routing functionality, such as to {@linkplain #route() create} a
 * {@code RouterFunction} using a discoverable builder-style API, to
 * {@linkplain #route(RequestPredicate, HandlerFunction) create} a {@code RouterFunction}
 * given a {@code RequestPredicate} and {@code HandlerFunction}, and to do further
 * {@linkplain #nest(RequestPredicate, RouterFunction) subrouting} on an existing routing
 * function.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.2
 */
public abstract class RouterFunctions {

	private static final Log logger = LogFactory.getLog(RouterFunctions.class);

	/**
	 * Name of the request attribute that contains the {@link ServerRequest}.
	 */
	public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

	/**
	 * Name of the request attribute that contains the URI
	 * templates map, mapping variable names to values.
	 */
	public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
			RouterFunctions.class.getName() + ".uriTemplateVariables";

	/**
	 * Name of the request attribute that contains the matching pattern, as a
	 * {@link org.springframework.web.util.pattern.PathPattern}.
	 */
	public static final String MATCHING_PATTERN_ATTRIBUTE =
			RouterFunctions.class.getName() + ".matchingPattern";


	/**
	 * Offers a discoverable way to create router functions through a builder-style interface.
	 * @return a router function builder
	 */
	public static Builder route() {
		return new RouterFunctionBuilder();
	}

	/**
	 * Route to the given handler function if the given request predicate applies.
	 * <p>For instance, the following example routes GET requests for "/user" to the
	 * {@code listUsers} method in {@code userController}:
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; route =
	 *     RouterFunctions.route(RequestPredicates.GET("/user"), userController::listUsers);
	 * </pre>
	 * @param predicate the predicate to test
	 * @param handlerFunction the handler function to route to if the predicate applies
	 * @param <T> the type of response returned by the handler function
	 * @return a router function that routes to {@code handlerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> route(
			RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return new DefaultRouterFunction<>(predicate, handlerFunction);
	}

	/**
	 * Route to the given router function if the given request predicate applies. This method can be
	 * used to create <strong>nested routes</strong>, where a group of routes share a common path
	 * (prefix), header, or other request predicate.
	 * <p>For instance, the following example first creates a composed route that resolves to
	 * {@code listUsers} for a GET, and {@code createUser} for a POST. This composed route then gets
	 * nested with a "/user" path predicate, so that GET requests for "/user" will list users,
	 * and POST request for "/user" will create a new user.
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; userRoutes =
	 *   RouterFunctions.route(RequestPredicates.method(HttpMethod.GET), this::listUsers)
	 *     .andRoute(RequestPredicates.method(HttpMethod.POST), this::createUser);
	 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
	 *   RouterFunctions.nest(RequestPredicates.path("/user"), userRoutes);
	 * </pre>
	 * @param predicate the predicate to test
	 * @param routerFunction the nested router function to delegate to if the predicate applies
	 * @param <T> the type of response returned by the handler function
	 * @return a router function that routes to {@code routerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> nest(
			RequestPredicate predicate, RouterFunction<T> routerFunction) {

		return new DefaultNestedRouterFunction<>(predicate, routerFunction);
	}

	/**
	 * Route requests that match the given predicate to the given resource.
	 * For instance
	 * <pre class="code">
	 * Resource resource = new ClassPathResource("static/index.html")
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resource(path("/api/**").negate(), resource);
	 * </pre>
	 * @param predicate the predicate to match
	 * @param resource the resources to serve
	 * @return a router function that routes to a resource
	 * @since 6.1.4
	 */
	public static RouterFunction<ServerResponse> resource(RequestPredicate predicate, Resource resource) {
		return resources(new PredicateResourceLookupFunction(predicate, resource), (consumerResource, httpHeaders) -> {});
	}

	/**
	 * Route requests that match the given predicate to the given resource.
	 * For instance
	 * <pre class="code">
	 * Resource resource = new ClassPathResource("static/index.html")
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resource(path("/api/**").negate(), resource);
	 * </pre>
	 * @param predicate the predicate to match
	 * @param resource the resources to serve
	 * @param headersConsumer provides access to the HTTP headers for served resources
	 * @return a router function that routes to a resource
	 * @since 6.1.4
	 */
	public static RouterFunction<ServerResponse> resource(RequestPredicate predicate, Resource resource,
			BiConsumer<Resource, HttpHeaders> headersConsumer) {

		return resources(new PredicateResourceLookupFunction(predicate, resource), headersConsumer);
	}

	/**
	 * Route requests that match the given pattern to resources relative to the given root location.
	 * For instance
	 * <pre class="code">
	 * Resource location = new FileUrlResource("public-resources/");
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
     * </pre>
	 * @param pattern the pattern to match
	 * @param location the location directory relative to which resources should be resolved
	 * @return a router function that routes to resources
	 * @see org.springframework.web.util.pattern.PathPattern
	 * @see #resourceLookupFunction(String, Resource)
	 */
	public static RouterFunction<ServerResponse> resources(String pattern, Resource location) {
		return resources(resourceLookupFunction(pattern, location), (resource, httpHeaders) -> {});
	}

	/**
	 * Route requests that match the given pattern to resources relative to the given root location.
	 * For instance
	 * <pre class="code">
	 * Resource location = new FileUrlResource("public-resources/");
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
     * </pre>
	 * @param pattern the pattern to match
	 * @param location the location directory relative to which resources should be resolved
	 * @param headersConsumer provides access to the HTTP headers for served resources
	 * @return a router function that routes to resources
	 * @since 6.1
	 * @see org.springframework.web.util.pattern.PathPattern
	 * @see #resourceLookupFunction(String, Resource)
	 */
	public static RouterFunction<ServerResponse> resources(String pattern, Resource location,
			BiConsumer<Resource, HttpHeaders> headersConsumer) {

		return resources(resourceLookupFunction(pattern, location), headersConsumer);
	}

	/**
	 * Returns the resource lookup function used by {@link #resources(String, Resource)}.
	 * The returned function can be {@linkplain Function#andThen(Function) composed} on, for
	 * instance to return a default resource when the lookup function does not match:
	 * <pre class="code">
	 * Optional&lt;Resource&gt; defaultResource = Optional.of(new ClassPathResource("index.html"));
	 * Function&lt;ServerRequest, Optional&lt;Resource&gt;&gt; lookupFunction =
	 *   RouterFunctions.resourceLookupFunction("/resources/**", new FileUrlResource("public-resources/"))
	 *     .andThen(resource -&gt; resource.or(() -&gt; defaultResource));
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources(lookupFunction);
     * </pre>
	 * @param pattern the pattern to match
	 * @param location the location directory relative to which resources should be resolved
	 * @return the default resource lookup function for the given parameters.
	 * @see org.springframework.web.util.pattern.PathPattern
	 */
	public static Function<ServerRequest, Optional<Resource>> resourceLookupFunction(String pattern, Resource location) {
		return new PathResourceLookupFunction(pattern, location);
	}

	/**
	 * Route to resources using the provided lookup function. If the lookup function provides a
	 * {@link Resource} for the given request, it will be it will be exposed using a
	 * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
	 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
	 * @return a router function that routes to resources
	 */
	public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
		return new ResourcesRouterFunction(lookupFunction, (resource, httpHeaders) -> {});
	}

	/**
	 * Route to resources using the provided lookup function. If the lookup function provides a
	 * {@link Resource} for the given request, it will be it will be exposed using a
	 * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
	 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
	 * @param headersConsumer provides access to the HTTP headers for served resources
	 * @return a router function that routes to resources
	 * @since 6.1
	 */
	public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Optional<Resource>> lookupFunction,
			BiConsumer<Resource, HttpHeaders> headersConsumer) {

		return new ResourcesRouterFunction(lookupFunction, headersConsumer);
	}


	/**
	 * Changes the {@link PathPatternParser} on the given {@linkplain RouterFunction router function}. This method
	 * can be used to change the {@code PathPatternParser} properties from the defaults, for instance to change
	 * {@linkplain PathPatternParser#setCaseSensitive(boolean) case sensitivity}.
	 * @param routerFunction the router function to change the parser in
	 * @param parser the parser to change to
	 * @param <T> the type of response returned by the handler function
	 * @return the change router function
	 */
	public static <T extends ServerResponse> RouterFunction<T> changeParser(
			RouterFunction<T> routerFunction, PathPatternParser parser) {

		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(parser, "Parser must not be null");

		ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(parser);
		routerFunction.accept(visitor);
		return routerFunction;
	}


	/**
	 * Represents a discoverable builder for router functions.
	 * Obtained via {@link RouterFunctions#route()}.
	 */
	public interface Builder {

		/**
		 * Adds a route to the given handler function that handles HTTP {@code GET} requests.
		 * @param handlerFunction the handler function to handle all {@code GET} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder GET(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code GET} requests
		 * that match the given pattern.
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code GET} requests
		 * that match the given predicate.
		 * @param predicate the predicate to match
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder GET(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code GET} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes GET requests for "/user" that accept JSON
		 * to the {@code listUsers} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/user", RequestPredicates.accept(MediaType.APPLICATION_JSON), userController::listUsers)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 * match {@code pattern} and the predicate
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 * @see RequestPredicates
		 */
		Builder GET(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code HEAD} requests.
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder HEAD(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
		 * that match the given pattern.
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
		 * that match the given predicate.
		 * @param predicate the predicate to match
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder HEAD(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
		 * that match the given pattern and predicate.
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder HEAD(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code POST} requests.
		 * @param handlerFunction the handler function to handle all {@code POST} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder POST(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code POST} requests
		 * that match the given pattern.
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code POST} requests
		 * that match the given predicate.
		 * @param predicate the predicate to match
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder POST(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code POST} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes POST requests for "/user" that contain JSON
		 * to the {@code addUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .POST("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::addUser)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder POST(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code PUT} requests.
		 * @param handlerFunction the handler function to handle all {@code PUT} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder PUT(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
		 * that match the given pattern.
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
		 * that match the given predicate.
		 * @param predicate the predicate to match
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder PUT(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes PUT requests for "/user" that contain JSON
		 * to the {@code editUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PUT("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder PUT(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code PATCH} requests.
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder PATCH(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
		 * that match the given pattern.
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
		 * that match the given predicate.
		 * @param predicate the predicate to match
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder PATCH(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
		 * that match the given pattern and predicate.
		 * <p>For instance, the following example routes PATCH requests for "/user" that contain JSON
		 * to the {@code editUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PATCH("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder PATCH(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code DELETE} requests.
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder DELETE(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
		 * that match the given pattern.
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
		 * that match the given predicate.
		 * @param predicate the predicate to match
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder DELETE(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
		 * that match the given pattern and predicate.
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder DELETE(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles HTTP {@code OPTIONS} requests.
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder OPTIONS(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
		 * that match the given pattern.
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
		 * that match the given predicate.
		 * @param predicate the predicate to match
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder OPTIONS(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
		 * that match the given pattern and predicate.
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 * match {@code pattern}
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder OPTIONS(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds a route to the given handler function that handles all requests that match the
		 * given predicate.
		 * @param predicate the request predicate to match
		 * @param handlerFunction the handler function to handle all requests that match the predicate
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder route(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * Adds the given route to this builder. Can be used to merge externally defined router
		 * functions into this builder, or can be combined with
		 * {@link RouterFunctions#route(RequestPredicate, HandlerFunction)}
		 * to allow for more flexible predicate matching.
		 * <p>For instance, the following example adds the router function returned from
		 * {@code OrderController.routerFunction()}.
		 * to the {@code changeUser} method in {@code userController}:
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/users", userController::listUsers)
		 *     .add(orderController.routerFunction());
		 *     .build();
		 * </pre>
		 * @param routerFunction the router function to be added
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder add(RouterFunction<ServerResponse> routerFunction);

		/**
		 * Route requests that match the given predicate to the given resource.
		 * For instance
		 * <pre class="code">
		 * Resource resource = new ClassPathResource("static/index.html")
		 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resource(path("/api/**").negate(), resource);
		 * </pre>
		 * @param predicate the predicate to match
		 * @param resource the resources to serve
		 * @return a router function that routes to a resource
		 * @since 6.1.4
		 */
		Builder resource(RequestPredicate predicate, Resource resource);

		/**
		 * Route requests that match the given predicate to the given resource.
		 * For instance
		 * <pre class="code">
		 * Resource resource = new ClassPathResource("static/index.html")
		 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resource(path("/api/**").negate(), resource);
		 * </pre>
		 * @param predicate the predicate to match
		 * @param resource the resources to serve
		 * @param headersConsumer provides access to the HTTP headers for served resources
		 * @return a router function that routes to a resource
		 * @since 6.1.4
		 */
		Builder resource(RequestPredicate predicate, Resource resource, BiConsumer<Resource, HttpHeaders> headersConsumer);

		/**
		 * Route requests that match the given pattern to resources relative to the given root location.
		 * For instance
		 * <pre class="code">
		 * Resource location = new FileUrlResource("public-resources/");
		 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
	     * </pre>
		 * @param pattern the pattern to match
		 * @param location the location directory relative to which resources should be resolved
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder resources(String pattern, Resource location);

		/**
		 * Route requests that match the given pattern to resources relative to the given root location.
		 * For instance
		 * <pre class="code">
		 * Resource location = new FileUrlResource("public-resources/");
		 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
	     * </pre>
		 * @param pattern the pattern to match
		 * @param location the location directory relative to which resources should be resolved
		 * @param headersConsumer provides access to the HTTP headers for served resources
		 * @return this builder
		 * @since 6.1
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder resources(String pattern, Resource location, BiConsumer<Resource, HttpHeaders> headersConsumer);

		/**
		 * Route to resources using the provided lookup function. If the lookup function provides a
		 * {@link Resource} for the given request, it will be it will be exposed using a
		 * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
		 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
		 * @return this builder
		 */
		Builder resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

		/**
		 * Route to resources using the provided lookup function. If the lookup function provides a
		 * {@link Resource} for the given request, it will be it will be exposed using a
		 * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
		 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
		 * @param headersConsumer provides access to the HTTP headers for served resources
		 * @return this builder
		 * @since 6.1
		 */
		Builder resources(Function<ServerRequest, Optional<Resource>> lookupFunction, BiConsumer<Resource, HttpHeaders> headersConsumer);

		/**
		 * Route to the supplied router function if the given request predicate applies. This method
		 * can be used to create <strong>nested routes</strong>, where a group of routes share a
		 * common path (prefix), header, or other request predicate.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate, so that GET requests for "/user" will list users,
		 * and POST request for "/user" will create a new user.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .nest(RequestPredicates.path("/user"), () -&gt;
		 *       RouterFunctions.route()
		 *         .GET(this::listUsers)
		 *         .POST(this::createUser)
		 *         .build())
		 *     .build();
		 * </pre>
		 * @param predicate the predicate to test
		 * @param routerFunctionSupplier supplier for the nested router function to delegate to if
		 * the predicate applies
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * Route to a built router function if the given request predicate applies.
		 * This method can be used to create <strong>nested routes</strong>, where a group of routes
		 * share a common path (prefix), header, or other request predicate.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate, so that GET requests for "/user" will list users,
		 * and POST request for "/user" will create a new user.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .nest(RequestPredicates.path("/user"), builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 * @param predicate the predicate to test
		 * @param builderConsumer consumer for a {@code Builder} that provides the nested router
		 * function
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Consumer<Builder> builderConsumer);

		/**
		 * Route to the supplied router function if the given path prefix pattern applies. This method
		 * can be used to create <strong>nested routes</strong>, where a group of routes share a
		 * common path prefix. Specifically, this method can be used to merge externally defined
		 * router functions under a path prefix.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate that delegates to the router function defined in {@code userController},
		 * and with a "/order" path that delegates to {@code orderController}.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", userController::routerFunction)
		 *     .path("/order", orderController::routerFunction)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param routerFunctionSupplier supplier for the nested router function to delegate to if
		 * the pattern matches
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder path(String pattern, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * Route to a built router function if the given path prefix pattern applies.
		 * This method can be used to create <strong>nested routes</strong>, where a group of routes
		 * share a common path prefix.
		 * <p>For instance, the following example creates a nested route with a "/user" path
		 * predicate, so that GET requests for "/user" will list users,
		 * and POST request for "/user" will create a new user.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param builderConsumer consumer for a {@code Builder} that provides the nested router
		 * function
		 * @return this builder
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		Builder path(String pattern, Consumer<Builder> builderConsumer);

		/**
		 * Filters all routes created by this builder with the given filter function. Filter
		 * functions are typically used to address cross-cutting concerns, such as logging,
		 * security, etc.
		 * <p>For instance, the following example creates a filter that returns a 401 Unauthorized
		 * response if the request does not contain the necessary authentication headers.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .filter((request, next) -&gt; {
		 *       // check for authentication headers
		 *       if (isAuthenticated(request)) {
		 *         return next.handle(request);
		 *       }
		 *       else {
		 *         return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
		 *       }
		 *     })
		 *     .build();
		 * </pre>
		 * @param filterFunction the function to filter all routes built by this builder
		 * @return this builder
		 */
		Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction);

		/**
		 * Filter the request object for all routes created by this builder with the given request
		 * processing function. Filters are typically used to address cross-cutting concerns, such
		 * as logging, security, etc.
		 * <p>For instance, the following example creates a filter that logs the request before
		 * the handler function executes.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .before(request -&gt; {
		 *       log(request);
		 *       return request;
		 *     })
		 *     .build();
		 * </pre>
		 * @param requestProcessor a function that transforms the request
		 * @return this builder
		 */
		Builder before(Function<ServerRequest, ServerRequest> requestProcessor);

		/**
		 * Filter the response object for all routes created by this builder with the given response
		 * processing function. Filters are typically used to address cross-cutting concerns, such
		 * as logging, security, etc.
		 * <p>For instance, the following example creates a filter that logs the response after
		 * the handler function executes.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .after((request, response) -&gt; {
		 *       log(response);
		 *       return response;
		 *     })
		 *     .build();
		 * </pre>
		 * @param responseProcessor a function that transforms the response
		 * @return this builder
		 */
		Builder after(BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor);

		/**
		 * Filters all exceptions that match the predicate by applying the given response provider
		 * function.
		 * <p>For instance, the following example creates a filter that returns a 500 response
		 * status when an {@code IllegalStateException} occurs.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(e -&gt; e instanceof IllegalStateException,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 * @param predicate the type of exception to filter
		 * @param responseProvider a function that creates a response
		 * @return this builder
		 */
		Builder onError(Predicate<Throwable> predicate,
				BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

		/**
		 * Filters all exceptions of the given type by applying the given response provider
		 * function.
		 * <p>For instance, the following example creates a filter that returns a 500 response
		 * status when an {@code IllegalStateException} occurs.
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(IllegalStateException.class,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 * @param exceptionType the type of exception to filter
		 * @param responseProvider a function that creates a response
		 * @return this builder
		 */
		Builder onError(Class<? extends Throwable> exceptionType,
				BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

		/**
		 * Add an attribute with the given name and value to the last route built with this builder.
		 * @param name the attribute name
		 * @param value the attribute value
		 * @return this builder
		 * @since 5.3
		 */
		Builder withAttribute(String name, Object value);

		/**
		 * Manipulate the attributes of the last route built with the given consumer.
		 * <p>The map provided to the consumer is "live", so that the consumer can be used
		 * to {@linkplain Map#put(Object, Object) overwrite} existing attributes,
		 * {@linkplain Map#remove(Object) remove} attributes, or use any of the other
		 * {@link Map} methods.
		 * @param attributesConsumer a function that consumes the attributes map
		 * @return this builder
		 * @since 5.3
		 */
		Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * Builds the {@code RouterFunction}. All created routes are
		 * {@linkplain RouterFunction#and(RouterFunction) composed} with one another, and filters
		 * (if any) are applied to the result.
		 * @return the built router function
		 */
		RouterFunction<ServerResponse> build();
	}


	/**
	 * Receives notifications from the logical structure of router functions.
	 */
	public interface Visitor {

		/**
		 * Receive notification of the beginning of a nested router function.
		 * @param predicate the predicate that applies to the nested router functions
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void startNested(RequestPredicate predicate);

		/**
		 * Receive notification of the end of a nested router function.
		 * @param predicate the predicate that applies to the nested router functions
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void endNested(RequestPredicate predicate);

		/**
		 * Receive notification of a standard predicated route to a handler function.
		 * @param predicate the predicate that applies to the handler function
		 * @param handlerFunction the handler function.
		 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
		 */
		void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction);

		/**
		 * Receive notification of a resource router function.
		 * @param lookupFunction the lookup function for the resources
		 * @see RouterFunctions#resources(Function)
		 */
		void resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

		/**
		 * Receive notification of a router function with attributes. The
		 * given attributes apply to the router notification that follows this one.
		 * @param attributes the attributes that apply to the following router
		 * @since 5.3
		 */
		void attributes(Map<String, Object> attributes);

		/**
		 * Receive notification of an unknown router function. This method is called for router
		 * functions that were not created via the various {@link RouterFunctions} methods.
		 * @param routerFunction the router function
		 */
		void unknown(RouterFunction<?> routerFunction);
	}


	abstract static class AbstractRouterFunction<T extends ServerResponse> implements RouterFunction<T> {

		@Override
		public String toString() {
			ToStringVisitor visitor = new ToStringVisitor();
			accept(visitor);
			return visitor.toString();
		}
	}


	/**
	 * A composed routing function that first invokes one function, and then invokes the
	 * another function (of the same response type {@code T}) if this route had
	 * {@linkplain Optional#empty() no result}.
	 * @param <T> the server response type
	 */
	static final class SameComposedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RouterFunction<T> first;

		private final RouterFunction<T> second;

		public SameComposedRouterFunction(RouterFunction<T> first, RouterFunction<T> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest request) {
			Optional<HandlerFunction<T>> firstRoute = this.first.route(request);
			if (firstRoute.isPresent()) {
				return firstRoute;
			}
			else {
				return this.second.route(request);
			}
		}

		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * A composed routing function that first invokes one function, and then invokes
	 * another function (of a different response type) if this route had
	 * {@linkplain Optional#empty() no result}.
	 */
	static final class DifferentComposedRouterFunction extends AbstractRouterFunction<ServerResponse> {

		private final RouterFunction<?> first;

		private final RouterFunction<?> second;

		public DifferentComposedRouterFunction(RouterFunction<?> first, RouterFunction<?> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			Optional<? extends HandlerFunction<?>> firstRoute = this.first.route(request);
			if (firstRoute.isPresent()) {
				return (Optional<HandlerFunction<ServerResponse>>) firstRoute;
			}
			else {
				Optional<? extends HandlerFunction<?>> secondRoute = this.second.route(request);
				return (Optional<HandlerFunction<ServerResponse>>) secondRoute;
			}
		}

		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * Filter the specified {@linkplain HandlerFunction handler functions} with the given
	 * {@linkplain HandlerFilterFunction filter function}.
	 * @param <T> the type of the {@linkplain HandlerFunction handler function} to filter
	 * @param <S> the type of the response of the function
	 */
	static final class FilteredRouterFunction<T extends ServerResponse, S extends ServerResponse>
			implements RouterFunction<S> {

		private final RouterFunction<T> routerFunction;

		private final HandlerFilterFunction<T, S> filterFunction;

		public FilteredRouterFunction(
				RouterFunction<T> routerFunction,
				HandlerFilterFunction<T, S> filterFunction) {
			this.routerFunction = routerFunction;
			this.filterFunction = filterFunction;
		}

		@Override
		public Optional<HandlerFunction<S>> route(ServerRequest request) {
			return this.routerFunction.route(request).map(this.filterFunction::apply);
		}

		@Override
		public void accept(Visitor visitor) {
			this.routerFunction.accept(visitor);
		}

		@Override
		public String toString() {
			return this.routerFunction.toString();
		}
	}


	private static final class DefaultRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RequestPredicate predicate;

		private final HandlerFunction<T> handlerFunction;

		public DefaultRouterFunction(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(handlerFunction, "HandlerFunction must not be null");
			this.predicate = predicate;
			this.handlerFunction = handlerFunction;
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest request) {
			if (this.predicate.test(request)) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Predicate \"%s\" matches against \"%s\"", this.predicate, request));
				}
				return Optional.of(this.handlerFunction);
			}
			else {
				return Optional.empty();
			}
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.route(this.predicate, this.handlerFunction);
		}
	}


	private static final class DefaultNestedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RequestPredicate predicate;

		private final RouterFunction<T> routerFunction;

		public DefaultNestedRouterFunction(RequestPredicate predicate, RouterFunction<T> routerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(routerFunction, "RouterFunction must not be null");
			this.predicate = predicate;
			this.routerFunction = routerFunction;
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest serverRequest) {
			return this.predicate.nest(serverRequest)
					.map(nestedRequest -> {
								if (logger.isTraceEnabled()) {
									logger.trace(String.format("Nested predicate \"%s\" matches against \"%s\"",
											this.predicate, serverRequest));
								}
								Optional<HandlerFunction<T>> result = this.routerFunction.route(nestedRequest);
								if (result.isPresent() && nestedRequest != serverRequest) {
									// new attributes map from nestedRequest.attributes() can be composed of the old attributes,
									// which means that clearing the old attributes will remove those values from new attributes as well
									// so let's make a copy
									Map<String, Object> newAttributes = new LinkedHashMap<>(nestedRequest.attributes());
									Map<String, Object> oldAttributes = serverRequest.attributes();
									oldAttributes.clear();
									oldAttributes.putAll(newAttributes);
								}
								return result;
							}
					)
					.orElseGet(Optional::empty);
		}


		@Override
		public void accept(Visitor visitor) {
			visitor.startNested(this.predicate);
			this.routerFunction.accept(visitor);
			visitor.endNested(this.predicate);
		}
	}


	private static class ResourcesRouterFunction extends AbstractRouterFunction<ServerResponse> {

		private final Function<ServerRequest, Optional<Resource>> lookupFunction;

		private final BiConsumer<Resource, HttpHeaders> headersConsumer;

		public ResourcesRouterFunction(Function<ServerRequest, Optional<Resource>> lookupFunction,
				BiConsumer<Resource, HttpHeaders> headersConsumer) {

			Assert.notNull(lookupFunction, "Lookup function must not be null");
			Assert.notNull(headersConsumer, "Headers consumer must not be null");
			this.lookupFunction = lookupFunction;
			this.headersConsumer = headersConsumer;
		}

		@Override
		public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			return this.lookupFunction.apply(request).map(resource -> new ResourceHandlerFunction(resource, this.headersConsumer));
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.resources(this.lookupFunction);
		}
	}


	static final class AttributesRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RouterFunction<T> delegate;

		private final Map<String,Object> attributes;

		public AttributesRouterFunction(RouterFunction<T> delegate, Map<String, Object> attributes) {
			this.delegate = delegate;
			this.attributes = initAttributes(attributes);
		}

		private static Map<String, Object> initAttributes(Map<String, Object> attributes) {
			if (attributes.isEmpty()) {
				return Collections.emptyMap();
			}
			else {
				return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
			}
		}

		@Override
		public Optional<HandlerFunction<T>> route(ServerRequest request) {
			return this.delegate.route(request);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.attributes(this.attributes);
			this.delegate.accept(visitor);
		}

		@Override
		public RouterFunction<T> withAttribute(String name, Object value) {
			Assert.hasLength(name, "Name must not be empty");
			Assert.notNull(value, "Value must not be null");

			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			attributes.put(name, value);
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}

		@Override
		public RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
			Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			attributesConsumer.accept(attributes);
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}
	}

}
