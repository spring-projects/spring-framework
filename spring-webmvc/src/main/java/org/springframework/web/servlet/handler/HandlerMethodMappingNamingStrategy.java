/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.handler;

import org.springframework.web.method.HandlerMethod;

/**
 * A strategy for assigning a name to a handler method's mapping.
 *
 * <p>The strategy can be configured on
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping}. It is used to assign a name to the mapping of
 * every registered handler method. The names can then be queried via
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getHandlerMethodsForMappingName(String)
 * AbstractHandlerMethodMapping#getHandlerMethodsForMappingName}.
 *
 * <p>Applications can build a URL to a controller method by name with the help
 * of the static method
 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder#fromMappingName(String)
 * MvcUriComponentsBuilder#fromMappingName} or in JSPs through the "mvcUrl"
 * function registered by the Spring tag library.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @param <T> the mapping type
 */
@FunctionalInterface
public interface HandlerMethodMappingNamingStrategy<T> {

	/**
	 * Determine the name for the given HandlerMethod and mapping.
	 * @param handlerMethod the handler method
	 * @param mapping the mapping
	 * @return the name
	 */
	String getName(HandlerMethod handlerMethod, T mapping);

}
