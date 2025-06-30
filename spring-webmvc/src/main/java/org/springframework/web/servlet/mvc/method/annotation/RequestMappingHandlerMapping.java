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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
import org.springframework.web.servlet.mvc.condition.CompositeRequestCondition;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Creates {@link RequestMappingInfo} instances from type-level and method-level
 * {@link RequestMapping @RequestMapping} and {@link HttpExchange @HttpExchange}
 * annotations in {@link Controller @Controller} classes.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Olga Maciaszek-Sharma
 * @since 3.1
 */
@SuppressWarnings("removal")
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements MatchableHandlerMapping, EmbeddedValueResolverAware {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final RequestMethod[] EMPTY_REQUEST_METHOD_ARRAY = new RequestMethod[0];


	private Map<String, Predicate<Class<?>>> pathPrefixes = Collections.emptyMap();

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private @Nullable ApiVersionStrategy apiVersionStrategy;

	private @Nullable StringValueResolver embeddedValueResolver;

	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();


	/**
	 * Configure path prefixes to apply to controller methods.
	 * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
	 * method and {@code @HttpExchange} method whose controller type is matched
	 * by a corresponding {@code Predicate} in the map. The prefix for the first
	 * matching predicate is used, assuming the input map has predictable order.
	 * <p>Consider using {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} to group controllers.
	 * @param prefixes a map with path prefixes as key
	 * @since 5.1
	 */
	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
		this.pathPrefixes = (!prefixes.isEmpty() ?
				Collections.unmodifiableMap(new LinkedHashMap<>(prefixes)) :
				Collections.emptyMap());
	}

	/**
	 * The configured path prefixes as a read-only, possibly empty map.
	 * @since 5.1
	 */
	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}

	/**
	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
	 * If not set, the default constructor is used.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * Return the configured {@link ContentNegotiationManager}.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * Configure a strategy to manage API versioning.
	 * @param strategy the strategy to use
	 * @since 7.0
	 */
	public void setApiVersionStrategy(@Nullable ApiVersionStrategy strategy) {
		this.apiVersionStrategy = strategy;
	}

	/**
	 * Return the configured {@link ApiVersionStrategy} strategy.
	 * @since 7.0
	 */
	public @Nullable ApiVersionStrategy getApiVersionStrategy() {
		return this.apiVersionStrategy;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	@SuppressWarnings("removal")
	public void afterPropertiesSet() {
		this.config = new RequestMappingInfo.BuilderConfiguration();
		this.config.setContentNegotiationManager(getContentNegotiationManager());
		this.config.setApiVersionStrategy(getApiVersionStrategy());

		if (getPatternParser() != null) {
			this.config.setPatternParser(getPatternParser());
		}
		else {
			this.config.setPathMatcher(getPathMatcher());
		}

		super.afterPropertiesSet();
	}


	/**
	 * Obtain a {@link RequestMappingInfo.BuilderConfiguration} that reflects
	 * the internal configuration of this {@code HandlerMapping} and can be used
	 * to set {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}.
	 * <p>This is useful for programmatic registration of request mappings via
	 * {@link #registerHandlerMethod(Object, Method, RequestMappingInfo)}.
	 * @return the builder configuration that reflects the internal state
	 * @since 5.3.14
	 */
	public RequestMappingInfo.BuilderConfiguration getBuilderConfiguration() {
		return this.config;
	}


	/**
	 * {@inheritDoc}
	 * <p>Expects a handler to have a type-level @{@link Controller} annotation.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
	}


	@Override
	protected @Nullable HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		if (this.apiVersionStrategy != null) {
			Comparable<?> version = (Comparable<?>) request.getAttribute(API_VERSION_ATTRIBUTE);
			if (version == null) {
				version = this.apiVersionStrategy.resolveParseAndValidateVersion(request);
				if (version != null) {
					request.setAttribute(API_VERSION_ATTRIBUTE, version);
				}
			}
		}
		return super.getHandlerInternal(request);
	}

	/**
	 * Uses type-level and method-level {@link RequestMapping @RequestMapping}
	 * and {@link HttpExchange @HttpExchange} annotations to create the
	 * {@link RequestMappingInfo}.
	 * @return the created {@code RequestMappingInfo}, or {@code null} if the method
	 * does not have a {@code @RequestMapping} or {@code @HttpExchange} annotation
	 * @see #getCustomMethodCondition(Method)
	 * @see #getCustomTypeCondition(Class)
	 */
	@Override
	protected @Nullable RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMappingInfo info = createRequestMappingInfo(method);
		if (info != null) {
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
			if (typeInfo != null) {
				info = typeInfo.combine(info);
			}
			if (info.isEmptyMapping()) {
				info = info.mutate().paths("", "/").options(this.config).build();
			}
			String prefix = getPathPrefix(handlerType);
			if (prefix != null) {
				info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
			}
		}
		return info;
	}

	@Nullable String getPathPrefix(Class<?> handlerType) {
		for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
			if (entry.getValue().test(handlerType)) {
				String prefix = entry.getKey();
				if (this.embeddedValueResolver != null) {
					prefix = this.embeddedValueResolver.resolveStringValue(prefix);
				}
				return prefix;
			}
		}
		return null;
	}

	private @Nullable RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {

		List<AnnotationDescriptor> descriptors =
				MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
						.stream()
						.filter(MergedAnnotationPredicates.typeIn(RequestMapping.class, HttpExchange.class))
						.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
						.map(AnnotationDescriptor::new)
						.distinct()
						.toList();

		RequestMappingInfo info = null;
		RequestCondition<?> customCondition = (element instanceof Class<?> clazz ?
				getCustomTypeCondition(clazz) : getCustomMethodCondition((Method) element));

		List<AnnotationDescriptor> mappingDescriptors =
				descriptors.stream().filter(desc -> desc.annotation instanceof RequestMapping).toList();

		if (!mappingDescriptors.isEmpty()) {
			checkMultipleAnnotations(element, mappingDescriptors);
			info = createRequestMappingInfo((RequestMapping) mappingDescriptors.get(0).annotation, customCondition);
		}

		List<AnnotationDescriptor> exchangeDescriptors =
				descriptors.stream().filter(desc -> desc.annotation instanceof HttpExchange).toList();

		if (!exchangeDescriptors.isEmpty()) {
			checkMultipleAnnotations(element, info, mappingDescriptors, exchangeDescriptors);
			info = createRequestMappingInfo((HttpExchange) exchangeDescriptors.get(0).annotation, customCondition);
		}

		if (info != null && this.apiVersionStrategy instanceof DefaultApiVersionStrategy davs) {
			String version = info.getVersionCondition().getVersion();
			if (version != null) {
				davs.addMappedVersion(version);
			}
		}

		return info;
	}

	/**
	 * Provide a custom type-level request condition.
	 * The custom {@link RequestCondition} can be of any type so long as the
	 * same condition type is returned from all calls to this method in order
	 * to ensure custom request conditions can be combined and compared.
	 * <p>Consider extending {@link AbstractRequestCondition} for custom
	 * condition types and using {@link CompositeRequestCondition} to provide
	 * multiple custom conditions.
	 * @param handlerType the handler type for which to create the condition
	 * @return the condition, or {@code null}
	 */
	protected @Nullable RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * Provide a custom method-level request condition.
	 * The custom {@link RequestCondition} can be of any type so long as the
	 * same condition type is returned from all calls to this method in order
	 * to ensure custom request conditions can be combined and compared.
	 * <p>Consider extending {@link AbstractRequestCondition} for custom
	 * condition types and using {@link CompositeRequestCondition} to provide
	 * multiple custom conditions.
	 * @param method the handler method for which to create the condition
	 * @return the condition, or {@code null}
	 */
	protected @Nullable RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	private void checkMultipleAnnotations(
			AnnotatedElement element, List<AnnotationDescriptor> mappingDescriptors) {

		if (logger.isWarnEnabled() && mappingDescriptors.size() > 1) {
			logger.warn("Multiple @RequestMapping annotations found on %s, but only the first will be used: %s"
					.formatted(element, mappingDescriptors));
		}
	}

	private static void checkMultipleAnnotations(
			AnnotatedElement element, @Nullable RequestMappingInfo info,
			List<AnnotationDescriptor> mappingDescriptors, List<AnnotationDescriptor> exchangeDescriptors) {

		Assert.state(info == null,
				() -> "%s is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed: %s"
						.formatted(element, Stream.of(mappingDescriptors, exchangeDescriptors).flatMap(List::stream).toList()));

		Assert.state(exchangeDescriptors.size() == 1,
				() -> "Multiple @HttpExchange annotations found on %s, but only one is allowed: %s"
						.formatted(element, exchangeDescriptors));
	}

	/**
	 * Create a {@link RequestMappingInfo} from the supplied
	 * {@link RequestMapping @RequestMapping} annotation, meta-annotation,
	 * or synthesized result of merging annotation attributes within an
	 * annotation hierarchy.
	 */
	protected RequestMappingInfo createRequestMappingInfo(
			RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {

		RequestMappingInfo.Builder builder = RequestMappingInfo
				.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
				.methods(requestMapping.method())
				.params(requestMapping.params())
				.headers(requestMapping.headers())
				.consumes(requestMapping.consumes())
				.produces(requestMapping.produces())
				.version(requestMapping.version())
				.mappingName(requestMapping.name());

		if (customCondition != null) {
			builder.customCondition(customCondition);
		}

		return builder.options(this.config).build();
	}

	/**
	 * Create a {@link RequestMappingInfo} from the supplied
	 * {@link HttpExchange @HttpExchange} annotation, meta-annotation,
	 * or synthesized result of merging annotation attributes within an
	 * annotation hierarchy.
	 * @since 6.1
	 */
	protected RequestMappingInfo createRequestMappingInfo(
			HttpExchange httpExchange, @Nullable RequestCondition<?> customCondition) {

		RequestMappingInfo.Builder builder = RequestMappingInfo
				.paths(resolveEmbeddedValuesInPatterns(toStringArray(httpExchange.value())))
				.methods(toMethodArray(httpExchange.method()))
				.consumes(toStringArray(httpExchange.contentType()))
				.produces(httpExchange.accept())
				.headers(httpExchange.headers());

		if (customCondition != null) {
			builder.customCondition(customCondition);
		}

		return builder.options(this.config).build();
	}

	/**
	 * Resolve placeholder values in the given array of patterns.
	 * @return a new array with updated patterns
	 */
	protected @Nullable String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
		if (this.embeddedValueResolver == null) {
			return patterns;
		}
		else {
			@Nullable String[] resolvedPatterns = new String[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
			}
			return resolvedPatterns;
		}
	}

	private static String[] toStringArray(String value) {
		return (StringUtils.hasText(value) ? new String[] {value} : EMPTY_STRING_ARRAY);
	}

	private static RequestMethod[] toMethodArray(String method) {
		return (StringUtils.hasText(method) ?
				new RequestMethod[] {RequestMethod.valueOf(method)} : EMPTY_REQUEST_METHOD_ARRAY);
	}

	@Override
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain executionChain = super.getHandlerExecutionChain(handler, request);
		Comparable<?> version = (Comparable<?>) request.getAttribute(API_VERSION_ATTRIBUTE);
		if (version != null) {
			executionChain.addInterceptor(new DeprecationInterceptor(version));
		}
		return executionChain;
	}

	@Override
	public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
		super.registerMapping(mapping, handler, method);
		updateConsumesCondition(mapping, method);
	}

	/**
	 * {@inheritDoc}
	 * <p><strong>Note:</strong> To create the {@link RequestMappingInfo},
	 * please use {@link #getBuilderConfiguration()} and set the options on
	 * {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}
	 * to match how this {@code HandlerMapping} is configured. This
	 * is important for example to ensure use of
	 * {@link org.springframework.web.util.pattern.PathPattern} or
	 * {@link org.springframework.util.PathMatcher} based matching.
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 */
	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		super.registerHandlerMethod(handler, method, mapping);
		updateConsumesCondition(mapping, method);
	}

	private void updateConsumesCondition(RequestMappingInfo info, Method method) {
		ConsumesRequestCondition condition = info.getConsumesCondition();
		if (!condition.isEmpty()) {
			for (Parameter parameter : method.getParameters()) {
				MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);
				if (annot.isPresent()) {
					condition.setBodyRequired(annot.getBoolean("required"));
					break;
				}
			}
		}
	}

	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public @Nullable RequestMatchResult match(HttpServletRequest request, String pattern) {
		Assert.state(getPatternParser() == null, "This HandlerMapping uses PathPatterns.");
		RequestMappingInfo info = RequestMappingInfo.paths(pattern).options(this.config).build();
		RequestMappingInfo match = info.getMatchingCondition(request);
		return (match != null && match.getPatternsCondition() != null ?
				new RequestMatchResult(
						match.getPatternsCondition().getPatterns().iterator().next(),
						UrlPathHelper.getResolvedLookupPath(request),
						getPathMatcher()) : null);
	}

	@Override
	protected @Nullable CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		HandlerMethod handlerMethod = createHandlerMethod(handler, method);
		Class<?> beanType = handlerMethod.getBeanType();
		CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
		CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);

		if (typeAnnotation == null && methodAnnotation == null) {
			return null;
		}

		CorsConfiguration config = new CorsConfiguration();
		updateCorsConfig(config, typeAnnotation);
		updateCorsConfig(config, methodAnnotation);

		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}
		return config.applyPermitDefaultValues();
	}

	private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
		if (annotation == null) {
			return;
		}
		for (String origin : annotation.origins()) {
			config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
		}
		for (String patterns : annotation.originPatterns()) {
			config.addAllowedOriginPattern(resolveCorsAnnotationValue(patterns));
		}
		for (RequestMethod method : annotation.methods()) {
			config.addAllowedMethod(method.name());
		}
		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(resolveCorsAnnotationValue(header));
		}
		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(resolveCorsAnnotationValue(header));
		}

		String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
		if ("true".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(true);
		}
		else if ("false".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(false);
		}
		else if (!allowCredentials.isEmpty()) {
			throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
					"or an empty string (\"\"): current value is [" + allowCredentials + "]");
		}

		String allowPrivateNetwork = resolveCorsAnnotationValue(annotation.allowPrivateNetwork());
		if ("true".equalsIgnoreCase(allowPrivateNetwork)) {
			config.setAllowPrivateNetwork(true);
		}
		else if ("false".equalsIgnoreCase(allowPrivateNetwork)) {
			config.setAllowPrivateNetwork(false);
		}
		else if (!allowPrivateNetwork.isEmpty()) {
			throw new IllegalStateException("@CrossOrigin's allowPrivateNetwork value must be \"true\", \"false\", " +
					"or an empty string (\"\"): current value is [" + allowPrivateNetwork + "]");
		}

		if (annotation.maxAge() >= 0 ) {
			config.setMaxAge(annotation.maxAge());
		}
	}

	private String resolveCorsAnnotationValue(String value) {
		if (this.embeddedValueResolver != null) {
			String resolved = this.embeddedValueResolver.resolveStringValue(value);
			return (resolved != null ? resolved : "");
		}
		else {
			return value;
		}
	}


	private static class AnnotationDescriptor {

		private final Annotation annotation;
		private final MergedAnnotation<?> root;

		AnnotationDescriptor(MergedAnnotation<Annotation> mergedAnnotation) {
			this.annotation = mergedAnnotation.synthesize();
			this.root = mergedAnnotation.getRoot();
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof AnnotationDescriptor that && this.annotation.equals(that.annotation));
		}

		@Override
		public int hashCode() {
			return this.annotation.hashCode();
		}

		@Override
		public String toString() {
			return this.root.synthesize().toString();
		}

	}


	private final class DeprecationInterceptor implements HandlerInterceptor {

		private final Comparable<?> version;

		private DeprecationInterceptor(Comparable<?> version) {
			this.version = version;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			Assert.state(apiVersionStrategy != null, "No ApiVersionStrategy");
			apiVersionStrategy.handleDeprecations(this.version, request, response);
			return true;
		}
	}

}
