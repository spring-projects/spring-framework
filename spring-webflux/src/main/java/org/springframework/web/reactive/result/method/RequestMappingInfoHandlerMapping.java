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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsatisfiedRequestParameterException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Abstract base class for classes for which {@link RequestMappingInfo} defines
 * the mapping between a request and a handler method.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
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


	@Override
	protected Set<String> getDirectPaths(RequestMappingInfo info) {
		return info.getDirectPaths();
	}

	/**
	 * Check if the given RequestMappingInfo matches the current request and
	 * return a (potentially new) instance with conditions that match the
	 * current request -- for example with a subset of URL patterns.
	 * @return an info in case of a match; or {@code null} otherwise.
	 */
	@Override
	protected @Nullable RequestMappingInfo getMatchingMapping(RequestMappingInfo info, ServerWebExchange exchange) {
		return info.getMatchingCondition(exchange);
	}

	/**
	 * Provide a Comparator to sort RequestMappingInfos matched to a request.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final ServerWebExchange exchange) {
		return (info1, info2) -> info1.compareTo(info2, exchange);
	}

	@Override
	public Mono<HandlerMethod> getHandlerInternal(ServerWebExchange exchange) {
		exchange.getAttributes().remove(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		return super.getHandlerInternal(exchange)
				.doOnTerminate(() -> ProducesRequestCondition.clearMediaTypesAttribute(exchange));
	}

	/**
	 * Expose URI template variables, matrix variables, and producible media types in the request.
	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#MATRIX_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
	 */
	@Override
	@SuppressWarnings("removal")
	protected void handleMatch(RequestMappingInfo info, HandlerMethod handlerMethod,
			ServerWebExchange exchange) {

		super.handleMatch(info, handlerMethod, exchange);

		info.getVersionCondition().handleMatch(exchange);

		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();

		PathPattern bestPattern;
		Map<String, String> uriVariables;
		Map<String, MultiValueMap<String, String>> matrixVariables;

		Set<PathPattern> patterns = info.getPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			bestPattern = getPathPatternParser().parse(lookupPath.value());
			uriVariables = Collections.emptyMap();
			matrixVariables = Collections.emptyMap();
		}
		else {
			bestPattern = patterns.iterator().next();
			PathPattern.PathMatchInfo result = bestPattern.matchAndExtract(lookupPath);
			Assert.notNull(result, () ->
					"Expected bestPattern: " + bestPattern + " to match lookupPath " + lookupPath);
			uriVariables = result.getUriVariables();
			matrixVariables = result.getMatrixVariables();
		}

		exchange.getAttributes().put(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerMethod);
		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		ServerRequestObservationContext.findCurrent(exchange.getAttributes())
				.ifPresent(context -> context.setPathPattern(bestPattern.toString()));
		exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
		exchange.getAttributes().put(MATRIX_VARIABLES_ATTRIBUTE, matrixVariables);

		ProducesRequestCondition producesCondition = info.getProducesCondition();
		if (!producesCondition.isEmpty()) {
			Set<MediaType> mediaTypes = producesCondition.getProducibleMediaTypes();
			if (!mediaTypes.isEmpty()) {
				exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
			}
		}
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
	protected @Nullable HandlerMethod handleNoMatch(Set<RequestMappingInfo> infos,
			ServerWebExchange exchange) throws Exception {

		if (CollectionUtils.isEmpty(infos)) {
			return null;
		}

		PartialMatchHelper helper = new PartialMatchHelper(infos, exchange);
		if (helper.isEmpty()) {
			return null;
		}

		ServerHttpRequest request = exchange.getRequest();

		if (helper.hasMethodsMismatch()) {
			HttpMethod httpMethod = request.getMethod();
			Set<HttpMethod> methods = helper.getAllowedMethods();
			if (HttpMethod.OPTIONS.equals(httpMethod)) {
				Set<MediaType> mediaTypes = helper.getConsumablePatchMediaTypes();
				HttpOptionsHandler handler = new HttpOptionsHandler(methods, mediaTypes);
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}
			throw new MethodNotAllowedException(httpMethod, methods);
		}

		if (helper.hasConsumesMismatch()) {
			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
			MediaType contentType;
			try {
				contentType = request.getHeaders().getContentType();
			}
			catch (InvalidMediaTypeException ex) {
				throw new UnsupportedMediaTypeStatusException(ex.getMessage(), new ArrayList<>(mediaTypes));
			}
			throw new UnsupportedMediaTypeStatusException(
					contentType, new ArrayList<>(mediaTypes), exchange.getRequest().getMethod());
		}

		if (helper.hasProducesMismatch()) {
			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
			throw new NotAcceptableStatusException(new ArrayList<>(mediaTypes));
		}

		if (helper.hasParamsMismatch()) {
			throw new UnsatisfiedRequestParameterException(
					helper.getParamConditions().stream().map(Object::toString).toList(),
					request.getQueryParams());
		}

		return null;
	}


	/**
	 * Aggregate all partial matches and expose methods checking across them.
	 */
	private static final class PartialMatchHelper {

		private final List<PartialMatch> partialMatches = new ArrayList<>();


		PartialMatchHelper(Set<RequestMappingInfo> infos, ServerWebExchange exchange) {
			for (RequestMappingInfo info : infos) {
				if (info.getPatternsCondition().getMatchingCondition(exchange) != null) {
					this.partialMatches.add(new PartialMatch(info, exchange));
				}
			}
		}

		/**
		 * Whether there are any partial matches.
		 */
		public boolean isEmpty() {
			return this.partialMatches.isEmpty();
		}

		/**
		 * Any partial matches for "methods"?
		 */
		public boolean hasMethodsMismatch() {
			return this.partialMatches.stream().noneMatch(PartialMatch::hasMethodsMatch);
		}

		/**
		 * Any partial matches for "methods" and "consumes"?
		 */
		public boolean hasConsumesMismatch() {
			return this.partialMatches.stream().noneMatch(PartialMatch::hasConsumesMatch);
		}

		/**
		 * Any partial matches for "methods", "consumes", and "produces"?
		 */
		public boolean hasProducesMismatch() {
			return this.partialMatches.stream().noneMatch(PartialMatch::hasProducesMatch);
		}

		/**
		 * Any partial matches for "methods", "consumes", "produces", and "params"?
		 */
		public boolean hasParamsMismatch() {
			return this.partialMatches.stream().noneMatch(PartialMatch::hasParamsMatch);
		}

		/**
		 * Return declared HTTP methods.
		 */
		public Set<HttpMethod> getAllowedMethods() {
			return this.partialMatches.stream()
					.flatMap(m -> m.getInfo().getMethodsCondition().getMethods().stream())
					.map(requestMethod -> HttpMethod.valueOf(requestMethod.name()))
					.collect(Collectors.toSet());
		}

		/**
		 * Return declared "consumable" types but only among those that also
		 * match the "methods" condition.
		 */
		public Set<MediaType> getConsumableMediaTypes() {
			return this.partialMatches.stream()
					.filter(PartialMatch::hasMethodsMatch)
					.flatMap(m -> m.getInfo().getConsumesCondition().getConsumableMediaTypes().stream())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * Return declared "producible" types but only among those that also
		 * match the "methods" and "consumes" conditions.
		 */
		public Set<MediaType> getProducibleMediaTypes() {
			return this.partialMatches.stream()
					.filter(PartialMatch::hasConsumesMatch)
					.flatMap(m -> m.getInfo().getProducesCondition().getProducibleMediaTypes().stream())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * Return declared "params" conditions but only among those that also
		 * match the "methods", "consumes", and "params" conditions.
		 */
		public List<Set<NameValueExpression<String>>> getParamConditions() {
			return this.partialMatches.stream()
					.filter(PartialMatch::hasProducesMatch)
					.map(match -> match.getInfo().getParamsCondition().getExpressions())
					.toList();
		}

		/**
		 * Return declared "consumable" types but only among those that have
		 * PATCH specified, or that have no methods at all.
		 */
		public Set<MediaType> getConsumablePatchMediaTypes() {
			Set<MediaType> result = new LinkedHashSet<>();
			for (PartialMatch match : this.partialMatches) {
				Set<RequestMethod> methods = match.getInfo().getMethodsCondition().getMethods();
				if (methods.isEmpty() || methods.contains(RequestMethod.PATCH)) {
					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
				}
			}
			return result;
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
			 * Create a new {@link PartialMatch} instance.
			 * @param info the RequestMappingInfo that matches the URL path
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


		public HttpOptionsHandler(Set<HttpMethod> declaredMethods, Set<MediaType> acceptPatch) {
			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
			this.headers.setAcceptPatch(new ArrayList<>(acceptPatch));
		}

		private static Set<HttpMethod> initAllowedHttpMethods(Set<HttpMethod> declaredMethods) {
			if (declaredMethods.isEmpty()) {
				return Stream.of(HttpMethod.values())
						.filter(method -> !HttpMethod.TRACE.equals(method))
						.collect(Collectors.toSet());
			}
			else {
				Set<HttpMethod> result = new LinkedHashSet<>(declaredMethods);
				if (result.contains(HttpMethod.GET)) {
					result.add(HttpMethod.HEAD);
				}
				result.add(HttpMethod.OPTIONS);
				return result;
			}
		}

		@SuppressWarnings("unused")
		public HttpHeaders handle() {
			return this.headers;
		}
	}

}
