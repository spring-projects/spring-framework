/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.support;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Strategy for contributing to the building of a {@link UriComponents} by
 * looking at a method parameter and an argument value and deciding what
 * part of the target URL should be updated.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface UriComponentsContributor {

	/**
	 * Whether this contributor supports the given method parameter.
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * Process the given method argument and either update the
	 * {@link UriComponentsBuilder} or add to the map with URI variables
	 * to use to expand the URI after all arguments are processed.
	 * @param parameter the controller method parameter (never {@code null})
	 * @param value the argument value (possibly {@code null})
	 * @param builder the builder to update (never {@code null})
	 * @param uriVariables a map to add URI variables to (never {@code null})
	 * @param conversionService a ConversionService to format values as Strings
	 */
	void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
			Map<String, Object> uriVariables, ConversionService conversionService);

}
