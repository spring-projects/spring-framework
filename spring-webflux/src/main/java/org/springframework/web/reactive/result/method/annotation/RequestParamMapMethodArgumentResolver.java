/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver for {@link Map} method arguments annotated with
 * {@link RequestParam @RequestParam} where the annotation does not specify a
 * request parameter name. See {@link RequestParamMethodArgumentResolver} for
 * resolving {@link Map} method arguments with a request parameter name.
 *
 * <p>The created {@link Map} contains all request parameter name-value pairs.
 * If the method parameter type is {@link MultiValueMap} instead, the created
 * map contains all request parameters and all there values for cases where
 * request parameters have multiple values.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 * @see RequestParamMethodArgumentResolver
 */
public class RequestParamMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public RequestParamMapMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, RequestParam.class, this::allParams);
	}

	private boolean allParams(RequestParam requestParam, Class<?> type) {
		return (Map.class.isAssignableFrom(type) && !StringUtils.hasText(requestParam.name()));
	}


	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {

		boolean isMultiValueMap = MultiValueMap.class.isAssignableFrom(methodParameter.getParameterType());
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		return (isMultiValueMap ? queryParams : queryParams.toSingleValueMap());
	}

}
