/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.condition.ParamsRequestCondition;
import org.springframework.web.server.BadRequestStatusException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.util.WebUtils;

/**
 * Abstract base class for classes for which {@link RequestMappingInfo} defines
 * the mapping between a request and a handler method.
 *
 * @author Rossen Stoyanchev
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	private static final Method HTTP_OPTIONS_HANDLE_METHOD;

	static {
		try {
			HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
		}
		catch (NoSuchMethodException ex) {
			// Should never happen
			throw new IllegalStateException("No handler for HTTP OPTIONS", ex);
		}
	}


	/**
	 * Get the URL path patterns associated with this {@link RequestMappingInfo}.
	 */
	@Override
	protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
		return info.getPatternsCondition().getPatterns();
	}

	/**
	 * Check if the given RequestMappingInfo matches the current request and
	 * return a (potentially new) instance with conditions that match the
	 * current request -- for example with a subset of URL patterns.
	 * @return an info in case of a match; or {@code null} otherwise.
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, ServerWebExchange exchange) {
		return info.getMatchingCondition(exchange);
	}

	/**
	 * Provide a Comparator to sort RequestMappingInfos matched to a request.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final ServerWebExchange exchange) {
		return (info1, info2) -> info1.compareTo(info2, exchange);
	}

	/**
	 * Expose URI template variables, matrix variables, and producible media types in the request.
	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#MATRIX_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, String lookupPath, ServerWebExchange exchange) {
		super.handleMatch(info, lookupPath, exchange);

		String bestPattern;
		Map<String, String> uriVariables;
		Map<String, String> decodedUriVariables;

		Set<String> patterns = info.getPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			bestPattern = lookupPath;
			uriVariables = Collections.emptyMap();
			decodedUriVariables = Collections.emptyMap();
		}
		else {
			bestPattern = patterns.iterator().next();
			uriVariables = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
			decodedUriVariables = getPathHelper().decodePathVariables(exchange, uriVariables);
		}

		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodedUriVariables);

		Map<String, MultiValueMap<String, String>> matrixVars = extractMatrixVariables(exchange, uriVariables);
		exchange.getAttributes().put(MATRIX_VARIABLES_ATTRIBUTE, matrixVars);

		if (!info.getProducesCondition().getProducibleMediaTypes().isEmpty()) {
			Set<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
			exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
		}
	}

	private Map<String, MultiValueMap<String, String>> extractMatrixVariables(
			ServerWebExchange exchange, Map<String, String> uriVariables) {

		Map<String, MultiValueMap<String, String>> result = new LinkedHashMap<>();
		for (Entry<String, String> uriVar : uriVariables.entrySet()) {
			String uriVarValue = uriVar.getValue();

			int equalsIndex = uriVarValue.indexOf('=');
			if (equalsIndex == -1) {
				continue;
			}

			String matrixVariables;

			int semicolonIndex = uriVarValue.indexOf(';');
			if ((semicolonIndex == -1) || (semicolonIndex == 0) || (equalsIndex < semicolonIndex)) {
				matrixVariables = uriVarValue;
			}
			else {
				matrixVariables = uriVarValue.substring(semicolonIndex + 1);
				uriVariables.put(uriVar.getKey(), uriVarValue.substring(0, semicolonIndex));
			}

			MultiValueMap<String, String> vars = WebUtils.parseMatrixVariables(matrixVariables);
			result.put(uriVar.getKey(), getPathHelper().decodeMatrixVariables(exchange, vars));
		}
		return result;
	}

	/**
	 * Iterate all RequestMappingInfos once again, look if any match by URL at
	 * least and raise exceptions accordingly.
	 * @throws MethodNotAllowedException for matches by URL but not by HTTP method
	 * @throws UnsupportedMediaTypeStatusException if there are matches by URL
	 * and HTTP method but not by consumable media types
	 * @throws NotAcceptableStatusException if there are matches by URL and HTTP
	 * method but not by producible media types
	 * @throws BadRequestStatusException if there are matches by URL and HTTP
	 * method but not by query parameter conditions
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos,
			String lookupPath, ServerWebExchange exchange) throws Exception {

		Set<String> allowedMethods = new LinkedHashSet<>(4);

		Set<RequestMappingInfo> patternMatches = new HashSet<>();
		Set<RequestMappingInfo> patternAndMethodMatches = new HashSet<>();

		for (RequestMappingInfo info : requestMappingInfos) {
			if (info.getPatternsCondition().getMatchingCondition(exchange) != null) {
				patternMatches.add(info);
				if (info.getMethodsCondition().getMatchingCondition(exchange) != null) {
					patternAndMethodMatches.add(info);
				}
				else {
					for (RequestMethod method : info.getMethodsCondition().getMethods()) {
						allowedMethods.add(method.name());
					}
				}
			}
		}

		ServerHttpRequest request = exchange.getRequest();
		if (patternMatches.isEmpty()) {
			return null;
		}
		else if (patternAndMethodMatches.isEmpty()) {
			HttpMethod httpMethod = request.getMethod();
			if (HttpMethod.OPTIONS.matches(httpMethod.name())) {
				HttpOptionsHandler handler = new HttpOptionsHandler(allowedMethods);
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}
			else if (!allowedMethods.isEmpty()) {
				throw new MethodNotAllowedException(httpMethod.name(), allowedMethods);
			}
		}

		Set<MediaType> consumableMediaTypes;
		Set<MediaType> producibleMediaTypes;
		List<String[]> paramConditions;

		if (patternAndMethodMatches.isEmpty()) {
			consumableMediaTypes = getConsumableMediaTypes(exchange, patternMatches);
			producibleMediaTypes = getProducibleMediaTypes(exchange, patternMatches);
			paramConditions = getRequestParams(exchange, patternMatches);
		}
		else {
			consumableMediaTypes = getConsumableMediaTypes(exchange, patternAndMethodMatches);
			producibleMediaTypes = getProducibleMediaTypes(exchange, patternAndMethodMatches);
			paramConditions = getRequestParams(exchange, patternAndMethodMatches);
		}

		if (!consumableMediaTypes.isEmpty()) {
			MediaType contentType;
			try {
				contentType = request.getHeaders().getContentType();
			}
			catch (InvalidMediaTypeException ex) {
				throw new UnsupportedMediaTypeStatusException(ex.getMessage());
			}
			throw new UnsupportedMediaTypeStatusException(contentType, new ArrayList<>(consumableMediaTypes));
		}
		else if (!producibleMediaTypes.isEmpty()) {
			throw new NotAcceptableStatusException(new ArrayList<>(producibleMediaTypes));
		}
		else {
			if (!CollectionUtils.isEmpty(paramConditions)) {
				Map<String, String[]> params = request.getQueryParams().entrySet().stream()
						.collect(Collectors.toMap(Entry::getKey,
								entry -> entry.getValue().toArray(new String[entry.getValue().size()]))
				);
				throw new BadRequestStatusException("Unsatisfied query parameter conditions: " +
						paramConditions + ", actual: " + params);
			}
			else {
				return null;
			}
		}
	}

	private Set<MediaType> getConsumableMediaTypes(ServerWebExchange exchange,
			Set<RequestMappingInfo> partialMatches) {

		Set<MediaType> result = new HashSet<>();
		for (RequestMappingInfo partialMatch : partialMatches) {
			if (partialMatch.getConsumesCondition().getMatchingCondition(exchange) == null) {
				result.addAll(partialMatch.getConsumesCondition().getConsumableMediaTypes());
			}
		}
		return result;
	}

	private Set<MediaType> getProducibleMediaTypes(ServerWebExchange exchange,
			Set<RequestMappingInfo> partialMatches) {

		Set<MediaType> result = new HashSet<>();
		for (RequestMappingInfo partialMatch : partialMatches) {
			if (partialMatch.getProducesCondition().getMatchingCondition(exchange) == null) {
				result.addAll(partialMatch.getProducesCondition().getProducibleMediaTypes());
			}
		}
		return result;
	}

	private List<String[]> getRequestParams(ServerWebExchange exchange,
			Set<RequestMappingInfo> partialMatches) {

		List<String[]> result = new ArrayList<>();
		for (RequestMappingInfo partialMatch : partialMatches) {
			ParamsRequestCondition condition = partialMatch.getParamsCondition();
			Set<NameValueExpression<String>> expressions = condition.getExpressions();
			if (!CollectionUtils.isEmpty(expressions) && condition.getMatchingCondition(exchange) == null) {
				int i = 0;
				String[] array = new String[expressions.size()];
				for (NameValueExpression<String> expression : expressions) {
					array[i++] = expression.toString();
				}
				result.add(array);
			}
		}
		return result;
	}


	/**
	 * Default handler for HTTP OPTIONS.
	 */
	private static class HttpOptionsHandler {

		private final HttpHeaders headers = new HttpHeaders();


		public HttpOptionsHandler(Set<String> declaredMethods) {
			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
		}

		private static Set<HttpMethod> initAllowedHttpMethods(Set<String> declaredMethods) {
			Set<HttpMethod> result = new LinkedHashSet<>(declaredMethods.size());
			if (declaredMethods.isEmpty()) {
				for (HttpMethod method : HttpMethod.values()) {
					if (!HttpMethod.TRACE.equals(method)) {
						result.add(method);
					}
				}
			}
			else {
				boolean hasHead = declaredMethods.contains("HEAD");
				for (String method : declaredMethods) {
					result.add(HttpMethod.valueOf(method));
					if (!hasHead && "GET".equals(method)) {
						result.add(HttpMethod.HEAD);
					}
				}
			}
			return result;
		}

		public HttpHeaders handle() {
			return this.headers;
		}
	}

}
