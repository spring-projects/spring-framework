/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

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

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * An extension of {@link RequestMappingInfoHandlerMapping} that creates
 * {@link RequestMappingInfo} instances from type-level and method-level
 * {@link RequestMapping @RequestMapping} and {@link HttpExchange @HttpExchange}
 * annotations.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Olga Maciaszek-Sharma
 * @since 5.0
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements EmbeddedValueResolverAware {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final RequestMethod[] EMPTY_REQUEST_METHOD_ARRAY = new RequestMethod[0];


	private final Map<String, Predicate<Class<?>>> pathPrefixes = new LinkedHashMap<>();

	private RequestedContentTypeResolver contentTypeResolver = new RequestedContentTypeResolverBuilder().build();

	@Nullable
	private StringValueResolver embeddedValueResolver;

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
	 * @see org.springframework.web.method.HandlerTypePredicate
	 */
	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
		this.pathPrefixes.clear();
		prefixes.entrySet().stream()
				.filter(entry -> StringUtils.hasText(entry.getKey()))
				.forEach(entry -> this.pathPrefixes.put(entry.getKey(), entry.getValue()));
	}

	/**
	 * The configured path prefixes as a read-only, possibly empty map.
	 * @since 5.1
	 */
	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return Collections.unmodifiableMap(this.pathPrefixes);
	}

	/**
	 * Set the {@link RequestedContentTypeResolver} to use to determine requested
	 * media types. If not set, the default constructor is used.
	 */
	public void setContentTypeResolver(RequestedContentTypeResolver contentTypeResolver) {
		Assert.notNull(contentTypeResolver, "'contentTypeResolver' must not be null");
		this.contentTypeResolver = contentTypeResolver;
	}

	/**
	 * Return the configured {@link RequestedContentTypeResolver}.
	 */
	public RequestedContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void afterPropertiesSet() {
		this.config = new RequestMappingInfo.BuilderConfiguration();
		this.config.setPatternParser(getPathPatternParser());
		this.config.setContentTypeResolver(getContentTypeResolver());

		super.afterPropertiesSet();
	}


	/**
	 * {@inheritDoc}
	 * <p>Expects a handler to have a type-level @{@link Controller} annotation.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
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
	@Nullable
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMappingInfo info = createRequestMappingInfo(method);
		if (info != null) {
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
			if (typeInfo != null) {
				info = typeInfo.combine(info);
			}
			if (info.getPatternsCondition().isEmptyPathMapping()) {
				info = info.mutate().paths("", "/").options(this.config).build();
			}
			for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
				if (entry.getValue().test(handlerType)) {
					String prefix = entry.getKey();
					if (this.embeddedValueResolver != null) {
						prefix = this.embeddedValueResolver.resolveStringValue(prefix);
					}
					Assert.state(prefix != null, "Prefix must not be null");
					info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
					break;
				}
			}
		}
		return info;
	}

	@Nullable
	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		RequestMappingInfo requestMappingInfo = null;
		RequestCondition<?> customCondition = (element instanceof Class<?> clazz ?
				getCustomTypeCondition(clazz) : getCustomMethodCondition((Method) element));

		List<AnnotationDescriptor> descriptors = getAnnotationDescriptors(element);

		List<AnnotationDescriptor> requestMappings = descriptors.stream()
				.filter(desc -> desc.annotation instanceof RequestMapping).toList();
		if (!requestMappings.isEmpty()) {
			if (requestMappings.size() > 1 && logger.isWarnEnabled()) {
				logger.warn("Multiple @RequestMapping annotations found on %s, but only the first will be used: %s"
						.formatted(element, requestMappings));
			}
			requestMappingInfo = createRequestMappingInfo((RequestMapping) requestMappings.get(0).annotation, customCondition);
		}

		List<AnnotationDescriptor> httpExchanges = descriptors.stream()
				.filter(desc -> desc.annotation instanceof HttpExchange).toList();
		if (!httpExchanges.isEmpty()) {
			Assert.state(requestMappingInfo == null,
					() -> "%s is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed: %s"
							.formatted(element, Stream.of(requestMappings, httpExchanges).flatMap(List::stream).toList()));
			Assert.state(httpExchanges.size() == 1,
					() -> "Multiple @HttpExchange annotations found on %s, but only one is allowed: %s"
							.formatted(element, httpExchanges));
			requestMappingInfo = createRequestMappingInfo((HttpExchange) httpExchanges.get(0).annotation, customCondition);
		}

		return requestMappingInfo;
	}

	/**
	 * Protected method to provide a custom type-level request condition.
	 * <p>The custom {@link RequestCondition} can be of any type so long as the
	 * same condition type is returned from all calls to this method in order
	 * to ensure custom request conditions can be combined and compared.
	 * <p>Consider extending
	 * {@link org.springframework.web.reactive.result.condition.AbstractRequestCondition
	 * AbstractRequestCondition} for custom condition types and using
	 * {@link org.springframework.web.reactive.result.condition.CompositeRequestCondition
	 * CompositeRequestCondition} to provide multiple custom conditions.
	 * @param handlerType the handler type for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@SuppressWarnings("UnusedParameters")
	@Nullable
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * Protected method to provide a custom method-level request condition.
	 * <p>The custom {@link RequestCondition} can be of any type so long as the
	 * same condition type is returned from all calls to this method in order
	 * to ensure custom request conditions can be combined and compared.
	 * <p>Consider extending
	 * {@link org.springframework.web.reactive.result.condition.AbstractRequestCondition
	 * AbstractRequestCondition} for custom condition types and using
	 * {@link org.springframework.web.reactive.result.condition.CompositeRequestCondition
	 * CompositeRequestCondition} to provide multiple custom conditions.
	 * @param method the handler method for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@SuppressWarnings("UnusedParameters")
	@Nullable
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
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
	protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
		if (this.embeddedValueResolver == null) {
			return patterns;
		}
		else {
			String[] resolvedPatterns = new String[patterns.length];
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
	public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
		super.registerMapping(mapping, handler, method);
		updateConsumesCondition(mapping, method);
	}

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

	@Override
	@Nullable
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
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

		if (annotation.maxAge() >= 0) {
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

	private static List<AnnotationDescriptor> getAnnotationDescriptors(AnnotatedElement element) {
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
				.stream()
				.filter(MergedAnnotationPredicates.typeIn(RequestMapping.class, HttpExchange.class))
				.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
				.map(AnnotationDescriptor::new)
				.distinct()
				.toList();
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

}
