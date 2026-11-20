/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver for {@link Map} method arguments annotated with
 * {@link PathVariable @PathVariable} where the annotation does not specify a
 * path variable name. The resulting {@link Map} argument is a copy of all URI
 * template name-value pairs.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see PathVariableMethodArgumentResolver
 */
public class PathVariableMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public PathVariableMapMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkAnnotatedParamNoReactiveWrapper(parameter, PathVariable.class, this::allVariables);
	}

	private boolean allVariables(PathVariable pathVariable, Class<?> type) {
		return (Map.class.isAssignableFrom(type) && !StringUtils.hasText(pathVariable.value()));
	}


	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return exchange.getAttributeOrDefault(name, Collections.emptyMap());
	}

}
