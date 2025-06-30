/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * Interface to be implemented by objects that define a mapping between
 * requests and handler objects.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerMapping {

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} that
	 * contains the mapped handler for the best matching pattern.
	 */
	String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingHandler";

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} that
	 * contains the best matching pattern within the handler mapping.
	 */
	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} that
	 * contains the path within the handler mapping, in case of a pattern match
	 * such as {@code "/static/**"} or the full relevant URI otherwise.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations. URL-based HandlerMappings will
	 * typically support it but handlers should not necessarily expect
	 * this request attribute to be present in all scenarios.
	 */
	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} that
	 * contains the URI templates map mapping variable names to values.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations. URL-based HandlerMappings will
	 * typically support it, but handlers should not necessarily expect
	 * this request attribute to be present in all scenarios.
	 */
	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} that
	 * contains a map with URI variable names and a corresponding MultiValueMap
	 * of URI matrix variables for each.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations and may also not be present depending on
	 * whether the HandlerMapping is configured to keep matrix variable content
	 * in the request URI.
	 */
	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} containing
	 * the set of producible MediaType's applicable to the mapped handler.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations. Handlers should not necessarily expect
	 * this request attribute to be present in all scenarios.
	 */
	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";

	/**
	 * Name of the {@link ServerWebExchange#getAttributes() attribute} containing
	 * the resolved and parsed API version.
	 * @since 7.0
	 */
	String API_VERSION_ATTRIBUTE = HandlerMapping.class.getName() + ".apiVersion";


	/**
	 * Return a handler for this request.
	 * <p>Before returning a handler, an implementing method should check for
	 * CORS configuration associated with the handler, apply validation checks
	 * based on it, and update the response accordingly. For pre-flight requests,
	 * the same should be done based on the handler matching to the expected
	 * actual request.
	 * @param exchange current server exchange
	 * @return a {@link Mono} that emits one value or none in case the request
	 * cannot be resolved to a handler
	 */
	Mono<Object> getHandler(ServerWebExchange exchange);

}
