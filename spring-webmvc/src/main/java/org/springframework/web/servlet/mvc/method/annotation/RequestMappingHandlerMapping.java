/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
import org.springframework.web.servlet.mvc.condition.CompositeRequestCondition;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * Creates {@link RequestMappingInfo} instances from type and method-level
 * {@link RequestMapping @RequestMapping} annotations in
 * {@link Controller @Controller} classes.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements EmbeddedValueResolverAware {

	private boolean useSuffixPatternMatch = true;

	private boolean useRegisteredSuffixPatternMatch = false;

	private boolean useTrailingSlashMatch = true;

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private final List<String> fileExtensions = new ArrayList<String>();

	private StringValueResolver embeddedValueResolver;


	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>The default value is {@code true}.
	 * <p>Also see {@link #setUseRegisteredSuffixPatternMatch(boolean)} for
	 * more fine-grained control over specific suffixes to allow.
	 */
	public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
	}

	/**
	 * Whether to use suffix pattern match for registered file extensions only
	 * when matching patterns to requests.
	 * <p>If enabled, a controller method mapped to "/users" also matches to
	 * "/users.json" assuming ".json" is a file extension registered with the
	 * provided {@link #setContentNegotiationManager(ContentNegotiationManager)
	 * contentNegotiationManager}. This can be useful for allowing only specific
	 * URL extensions to be used as well as in cases where a "." in the URL path
	 * can lead to ambiguous interpretation of path variable content, (e.g. given
	 * "/users/{user}" and incoming URLs such as "/users/john.j.joe" and
	 * "/users/john.j.joe.json").
	 * <p>If enabled, this flag also enables
	 * {@link #setUseSuffixPatternMatch(boolean) useSuffixPatternMatch}. The
	 * default value is {@code false}.
	 */
	public void setUseRegisteredSuffixPatternMatch(boolean useRegisteredSuffixPatternMatch) {
		this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
		this.useSuffixPatternMatch = (useRegisteredSuffixPatternMatch || this.useSuffixPatternMatch);
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
	}

	/**
	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
	 * If not set, the default constructor is used.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
		this.contentNegotiationManager = contentNegotiationManager;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver  = resolver;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.useRegisteredSuffixPatternMatch) {
			this.fileExtensions.addAll(this.contentNegotiationManager.getAllFileExtensions());
		}
		super.afterPropertiesSet();
	}



	/**
	 * Whether to use suffix pattern matching.
	 */
	public boolean useSuffixPatternMatch() {
		return this.useSuffixPatternMatch;
	}

	/**
	 * Whether to use registered suffixes for pattern matching.
	 */
	public boolean useRegisteredSuffixPatternMatch() {
		return this.useRegisteredSuffixPatternMatch;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * Return the configured {@link ContentNegotiationManager}.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * Return the file extensions to use for suffix pattern matching.
	 */
	public List<String> getFileExtensions() {
		return this.fileExtensions;
	}


	/**
	 * {@inheritDoc}
	 * Expects a handler to have a type-level @{@link Controller} annotation.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return ((AnnotationUtils.findAnnotation(beanType, Controller.class) != null) ||
				(AnnotationUtils.findAnnotation(beanType, RequestMapping.class) != null));
	}

	/**
	 * Uses method and type-level @{@link RequestMapping} annotations to create
	 * the RequestMappingInfo.
	 * @return the created RequestMappingInfo, or {@code null} if the method
	 * does not have a {@code @RequestMapping} annotation.
	 * @see #getCustomMethodCondition(Method)
	 * @see #getCustomTypeCondition(Class)
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMappingInfo info = createRequestMappingInfo(method);
		if (info != null) {
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
			if (typeInfo != null) {
				info = typeInfo.combine(info);
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
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
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
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	/**
	 * Transitional method used to invoke one of two createRequestMappingInfo
	 * variants one of which is deprecated.
	 */
	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement annotatedElement) {
		RequestMapping annotation;
		AnnotationAttributes attributes;
		RequestCondition<?> customCondition;
		String annotationType = RequestMapping.class.getName();
		if (annotatedElement instanceof Class<?>) {
			Class<?> type = (Class<?>) annotatedElement;
			annotation = AnnotationUtils.findAnnotation(type, RequestMapping.class);
			attributes = AnnotatedElementUtils.findAnnotationAttributes(type, annotationType);
			customCondition = getCustomTypeCondition(type);
		}
		else {
			Method method = (Method) annotatedElement;
			annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			attributes = AnnotatedElementUtils.findAnnotationAttributes(method, annotationType);
			customCondition = getCustomMethodCondition(method);
		}
		RequestMappingInfo info = null;
		if (annotation != null) {
			info = createRequestMappingInfo(annotation, customCondition);
			if (info == null) {
				info = createRequestMappingInfo(attributes, customCondition);
			}
		}
		return info;
	}

	/**
	 * Create a RequestMappingInfo from a RequestMapping annotation.
	 * @deprecated as of 4.2 after the introduction of support for
	 * {@code @RequestMapping} as meta-annotation. Please use
	 * {@link #createRequestMappingInfo(AnnotationAttributes, RequestCondition)}.
	 */
	@Deprecated
	protected RequestMappingInfo createRequestMappingInfo(RequestMapping annotation,
			RequestCondition<?> customCondition) {

		return null;
	}

	/**
	 * Create a RequestMappingInfo from the attributes of an
	 * {@code @RequestMapping} annotation or a meta-annotation, i.e. a custom
	 * annotation annotated with {@code @RequestMapping}.
	 * @since 4.2
	 */
	protected RequestMappingInfo createRequestMappingInfo(AnnotationAttributes attributes,
			RequestCondition<?> customCondition) {

		String mappingName = attributes.getString("name");

		String[] paths = attributes.getStringArray("path");
		paths = ObjectUtils.isEmpty(paths) ? attributes.getStringArray("value") : paths;
		PatternsRequestCondition patternsCondition = new PatternsRequestCondition(
				resolveEmbeddedValuesInPatterns(paths), getUrlPathHelper(), getPathMatcher(),
				this.useSuffixPatternMatch, this.useTrailingSlashMatch, this.fileExtensions);

		RequestMethod[] methods = (RequestMethod[]) attributes.get("method");
		RequestMethodsRequestCondition methodsCondition = new RequestMethodsRequestCondition(methods);

		String[] params = attributes.getStringArray("params");
		ParamsRequestCondition paramsCondition = new ParamsRequestCondition(params);

		String[] headers = attributes.getStringArray("headers");
		String[] consumes = attributes.getStringArray("consumes");
		String[] produces = attributes.getStringArray("produces");

		HeadersRequestCondition headersCondition = new HeadersRequestCondition(headers);
		ConsumesRequestCondition consumesCondition = new ConsumesRequestCondition(consumes, headers);
		ProducesRequestCondition producesCondition = new ProducesRequestCondition(produces,
				headers, this.contentNegotiationManager);

		return new RequestMappingInfo(mappingName, patternsCondition, methodsCondition, paramsCondition,
				headersCondition, consumesCondition, producesCondition, customCondition);
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

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		HandlerMethod handlerMethod = createHandlerMethod(handler, method);
		CrossOrigin typeAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), CrossOrigin.class);
		CrossOrigin methodAnnotation = AnnotationUtils.findAnnotation(method, CrossOrigin.class);

		if (typeAnnotation == null && methodAnnotation == null) {
			return null;
		}

		CorsConfiguration config = new CorsConfiguration();
		applyAnnotation(config, typeAnnotation);
		applyAnnotation(config, methodAnnotation);

		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}
		if (CollectionUtils.isEmpty(config.getAllowedHeaders())) {
			for (NameValueExpression<String> headerExpression : mappingInfo.getHeadersCondition().getExpressions()) {
				if (!headerExpression.isNegated()) {
					config.addAllowedHeader(headerExpression.getName());
				}
			}
		}
		return config;
	}

	private void applyAnnotation(CorsConfiguration config, CrossOrigin annotation) {
		if (annotation == null) {
			return;
		}
		for (String origin : annotation.origin()) {
			config.addAllowedOrigin(origin);
		}
		for (RequestMethod method : annotation.method()) {
			config.addAllowedMethod(method.name());
		}
		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(header);
		}
		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(header);
		}
		if (annotation.allowCredentials().equalsIgnoreCase("true")) {
			config.setAllowCredentials(true);
		}
		else if (annotation.allowCredentials().equalsIgnoreCase("false")) {
			config.setAllowCredentials(false);
		}
		else if (!annotation.allowCredentials().isEmpty()) {
			throw new IllegalStateException("AllowCredentials value must be \"true\", \"false\" " +
					"or \"\" (empty string), current value is " + annotation.allowCredentials());
		}
		if (annotation.maxAge() != -1 && config.getMaxAge() == null) {
			config.setMaxAge(annotation.maxAge());
		}
	}

}
