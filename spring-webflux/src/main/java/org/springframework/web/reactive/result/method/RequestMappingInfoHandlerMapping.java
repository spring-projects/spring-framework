/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.util.patterns.PathPattern;

/**
 * Abstract base class for classes for which {@link RequestMappingInfo} defines
 * the mapping between a request and a handler method.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
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
		return info.getPatternsCondition().getPatterns().stream()
				.map(p -> p.getPatternString())
				.collect(Collectors.toSet());
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

		Map<String, String> uriVariables;
		Map<String, String> decodedUriVariables;

		SortedSet<PathPattern> patterns = info.getPatternsCondition().getMatchingPatterns(lookupPath);
		if (patterns.isEmpty()) {
			uriVariables = Collections.emptyMap();
			decodedUriVariables = Collections.emptyMap();
			exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE,
					getPatternRegistry().parsePattern(lookupPath));
		}
		else {
			PathPattern bestPattern = patterns.first();
			uriVariables = bestPattern.matchAndExtract(lookupPath);
			decodedUriVariables = getPathHelper().decodePathVariables(exchange, uriVariables);
			exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		}
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

			MultiValueMap<String, String> vars = parseMatrixVariables(matrixVariables);
			result.put(uriVar.getKey(), getPathHelper().decodeMatrixVariables(exchange, vars));
		}
		return result;
	}

	/**
	 * Parse the given string with matrix variables. An example string would look
	 * like this {@code "q1=a;q1=b;q2=a,b,c"}. The resulting map would contain
	 * keys {@code "q1"} and {@code "q2"} with values {@code ["a","b"]} and
	 * {@code ["a","b","c"]} respectively.
	 * @param matrixVariables the unparsed matrix variables string
	 * @return a map with matrix variable names and values (never {@code null})
	 */
	private static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
		if (!StringUtils.hasText(matrixVariables)) {
			return result;
		}
		StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
		while (pairs.hasMoreTokens()) {
			String pair = pairs.nextToken();
			int index = pair.indexOf('=');
			if (index != -1) {
				String name = pair.substring(0, index);
				String rawValue = pair.substring(index + 1);
				for (String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
					result.add(name, value);
				}
			}
			else {
				result.add(pair, "");
			}
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
	 * @throws ServerWebInputException if there are matches by URL and HTTP
	 * method but not by query parameter conditions
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> infos, String lookupPath,
			ServerWebExchange exchange) throws Exception {

		PartialMatchHelper helper = new PartialMatchHelper(infos, exchange);

		if (helper.isEmpty()) {
			return null;
		}

		ServerHttpRequest request = exchange.getRequest();

		if (helper.hasMethodsMismatch()) {
			HttpMethod httpMethod = request.getMethod();
			Set<String> methods = helper.getAllowedMethods();
			if (HttpMethod.OPTIONS.matches(httpMethod.name())) {
				HttpOptionsHandler handler = new HttpOptionsHandler(methods);
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}
			throw new MethodNotAllowedException(httpMethod.name(), methods);
		}

		if (helper.hasConsumesMismatch()) {
			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
			MediaType contentType;
			try {
				contentType = request.getHeaders().getContentType();
			}
			catch (InvalidMediaTypeException ex) {
				throw new UnsupportedMediaTypeStatusException(ex.getMessage());
			}
			throw new UnsupportedMediaTypeStatusException(contentType, new ArrayList<>(mediaTypes));
		}

		if (helper.hasProducesMismatch()) {
			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
			throw new NotAcceptableStatusException(new ArrayList<>(mediaTypes));
		}

		if (helper.hasParamsMismatch()) {
			throw new ServerWebInputException(
					"Unsatisfied query parameter conditions: " + helper.getParamConditions() +
							", actual parameters: " + request.getQueryParams());
		}

		return null;
	}


	/**
	 * Aggregate all partial matches and expose methods checking across them.
	 */
	private static class PartialMatchHelper {

		private final List<PartialMatch> partialMatches = new ArrayList<>();


		public PartialMatchHelper(Set<RequestMappingInfo> infos, ServerWebExchange exchange) {
			this.partialMatches.addAll(infos.stream().
					filter(info -> info.getPatternsCondition().getMatchingCondition(exchange) != null).
					map(info -> new PartialMatch(info, exchange)).
					collect(Collectors.toList()));
		}


		/**
		 * Whether there any partial matches.
		 */
		public boolean isEmpty() {
			return this.partialMatches.isEmpty();
		}

		/**
		 * Any partial matches for "methods"?
		 */
		public boolean hasMethodsMismatch() {
			return !this.partialMatches.stream().
					filter(PartialMatch::hasMethodsMatch).findAny().isPresent();
		}

		/**
		 * Any partial matches for "methods" and "consumes"?
		 */
		public boolean hasConsumesMismatch() {
			return !this.partialMatches.stream().
					filter(PartialMatch::hasConsumesMatch).findAny().isPresent();
		}

		/**
		 * Any partial matches for "methods", "consumes", and "produces"?
		 */
		public boolean hasProducesMismatch() {
			return !this.partialMatches.stream().
					filter(PartialMatch::hasProducesMatch).findAny().isPresent();
		}

		/**
		 * Any partial matches for "methods", "consumes", "produces", and "params"?
		 */
		public boolean hasParamsMismatch() {
			return !this.partialMatches.stream().
					filter(PartialMatch::hasParamsMatch).findAny().isPresent();
		}

		/**
		 * Return declared HTTP methods.
		 */
		public Set<String> getAllowedMethods() {
			return this.partialMatches.stream().
					flatMap(m -> m.getInfo().getMethodsCondition().getMethods().stream()).
					map(requestMethod -> requestMethod.name()).
					collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * Return declared "consumable" types but only among those that also
		 * match the "methods" condition.
		 */
		public Set<MediaType> getConsumableMediaTypes() {
			return this.partialMatches.stream().filter(PartialMatch::hasMethodsMatch).
					flatMap(m -> m.getInfo().getConsumesCondition().getConsumableMediaTypes().stream()).
					collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * Return declared "producible" types but only among those that also
		 * match the "methods" and "consumes" conditions.
		 */
		public Set<MediaType> getProducibleMediaTypes() {
			return this.partialMatches.stream().filter(PartialMatch::hasConsumesMatch).
					flatMap(m -> m.getInfo().getProducesCondition().getProducibleMediaTypes().stream()).
					collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * Return declared "params" conditions but only among those that also
		 * match the "methods", "consumes", and "params" conditions.
		 */
		public List<Set<NameValueExpression<String>>> getParamConditions() {
			return this.partialMatches.stream().filter(PartialMatch::hasProducesMatch).
					map(match -> match.getInfo().getParamsCondition().getExpressions()).
					collect(Collectors.toList());
		}


		/**
		 * Container for a RequestMappingInfo that matches the URL path at least.
		 */
		private static class PartialMatch {

			private final RequestMappingInfo info;

			private final boolean methodsMatch;

			private final boolean consumesMatch;

			private final boolean producesMatch;

			private final boolean paramsMatch;


			/**
			 * @param info RequestMappingInfo that matches the URL path
			 * @param exchange the current exchange
			 */
			public PartialMatch(RequestMappingInfo info, ServerWebExchange exchange) {
				this.info = info;
				this.methodsMatch = info.getMethodsCondition().getMatchingCondition(exchange) != null;
				this.consumesMatch = info.getConsumesCondition().getMatchingCondition(exchange) != null;
				this.producesMatch = info.getProducesCondition().getMatchingCondition(exchange) != null;
				this.paramsMatch = info.getParamsCondition().getMatchingCondition(exchange) != null;
			}


			public RequestMappingInfo getInfo() {
				return this.info;
			}

			public boolean hasMethodsMatch() {
				return this.methodsMatch;
			}

			public boolean hasConsumesMatch() {
				return hasMethodsMatch() && this.consumesMatch;
			}

			public boolean hasProducesMatch() {
				return hasConsumesMatch() && this.producesMatch;
			}

			public boolean hasParamsMatch() {
				return hasProducesMatch() && this.paramsMatch;
			}

			@Override
			public String toString() {
				return this.info.toString();
			}
		}
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
