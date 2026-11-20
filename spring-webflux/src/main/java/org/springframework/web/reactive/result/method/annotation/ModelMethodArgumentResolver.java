/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver for a controller method argument of type {@link Model} that can
 * also be resolved as a {@link java.util.Map}.
 *
 * <p>A {@code Map} return value can be interpreted in more than one way depending
 * on the presence of annotations like {@code @ModelAttribute} or
 * {@code @ResponseBody}.
 *
 * <p>As of 5.2 this resolver returns {@code false} if a parameter of type
 * {@code Map} is also annotated. As of 6.2 this resolver returns {@code false}
 * for a parameter of type {@link ModelMap}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.2
 */
public class ModelMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public ModelMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkParameterTypeNoReactiveWrapper(param, type -> Model.class.isAssignableFrom(type) ||
				(Map.class.isAssignableFrom(type) && !ModelMap.class.isAssignableFrom(type) &&
					!param.hasParameterAnnotations()));
	}

	@Override
	public Object resolveArgumentValue(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Class<?> type = parameter.getParameterType();
		if (Model.class.isAssignableFrom(type)) {
			return context.getModel();
		}
		else if (Map.class.isAssignableFrom(type) && !ModelMap.class.isAssignableFrom(type)) {
			return context.getModel().asMap();
		}
		else {
			// Should never happen..
			throw new IllegalStateException("Unexpected method parameter type: " + type.getName());
		}
	}

}
