/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * Base class for {@link View} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class AbstractView implements View, BeanNameAware, ApplicationContextAware {

	/** Well-known name for the RequestDataValueProcessor in the bean factory. */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";


	/** Logger that is available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private final ReactiveAdapterRegistry adapterRegistry;

	private final List<MediaType> mediaTypes = new ArrayList<>(4);

	private Charset defaultCharset = StandardCharsets.UTF_8;

	@Nullable
	private String requestContextAttribute;

	@Nullable
	private String beanName;

	@Nullable
	private ApplicationContext applicationContext;


	public AbstractView() {
		this(ReactiveAdapterRegistry.getSharedInstance());
	}

	public AbstractView(ReactiveAdapterRegistry reactiveAdapterRegistry) {
		this.adapterRegistry = reactiveAdapterRegistry;
		this.mediaTypes.add(ViewResolverSupport.DEFAULT_CONTENT_TYPE);
	}


	/**
	 * Set the supported media types for this view.
	 * <p>Default is {@code "text/html;charset=UTF-8"}.
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.mediaTypes.clear();
		this.mediaTypes.addAll(supportedMediaTypes);
	}

	/**
	 * Get the configured media types supported by this view.
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * Set the default charset for this view, used when the
	 * {@linkplain #setSupportedMediaTypes(List) content type} does not contain one.
	 * <p>Default is {@linkplain StandardCharsets#UTF_8 UTF 8}.
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "'defaultCharset' must not be null");
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Get the default charset, used when the
	 * {@linkplain #setSupportedMediaTypes(List) content type} does not contain one.
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}

	/**
	 * Set the name of the {@code RequestContext} attribute for this view.
	 * <p>Default is none ({@code null}).
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * Get the name of the {@code RequestContext} attribute for this view, if any.
	 */
	@Nullable
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * Set the view's name. Helpful for traceability.
	 * <p>Framework code must call this when constructing views.
	 */
	@Override
	public void setBeanName(@Nullable String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Get the view's name.
	 * <p>Should never be {@code null} if the view was correctly configured.
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * Obtain the {@link ApplicationContext} for actual use.
	 * @return the {@code ApplicationContext} (never {@code null})
	 * @throws IllegalStateException if the ApplicationContext cannot be obtained
	 * @see #getApplicationContext()
	 */
	protected final ApplicationContext obtainApplicationContext() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "No ApplicationContext");
		return applicationContext;
	}


	/**
	 * Prepare the model to render.
	 * @param model a map with attribute names as keys and corresponding model
	 * objects as values (the map can also be {@code null} in case of an empty model)
	 * @param contentType the content type selected to render with, which should
	 * match one of the {@link #getSupportedMediaTypes() supported media types}
	 * @param exchange the current exchange
	 * @return a {@code Mono} that represents when and if rendering succeeds
	 */
	@Override
	public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType,
			ServerWebExchange exchange) {

		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + "View " + formatViewName() +
					", model " + (model != null ? model : Collections.emptyMap()));
		}

		if (contentType != null) {
			exchange.getResponse().getHeaders().setContentType(contentType);
		}

		return getModelAttributes(model, exchange).flatMap(mergedModel -> {
			// Expose RequestContext?
			if (this.requestContextAttribute != null) {
				mergedModel.put(this.requestContextAttribute, createRequestContext(exchange, mergedModel));
			}
			return renderInternal(mergedModel, contentType, exchange);
		});
	}

	/**
	 * Prepare the model to use for rendering.
	 * <p>The default implementation creates a combined output Map that includes
	 * model as well as static attributes with the former taking precedence.
	 */
	protected Mono<Map<String, Object>> getModelAttributes(
			@Nullable Map<String, ?> model, ServerWebExchange exchange) {

		Map<String, Object> attributes;
		if (model != null) {
			attributes = new ConcurrentHashMap<>(model.size());
			for (Map.Entry<String, ?> entry : model.entrySet()) {
				if (entry.getValue() != null) {
					attributes.put(entry.getKey(), entry.getValue());
				}
			}
		}
		else {
			attributes = new ConcurrentHashMap<>(0);
		}

		return resolveAsyncAttributes(attributes, exchange)
				.doOnTerminate(() -> exchange.getAttributes().remove(BINDING_CONTEXT_ATTRIBUTE))
				.thenReturn(attributes);
	}

	/**
	 * Use the configured {@link ReactiveAdapterRegistry} to adapt asynchronous
	 * attributes to {@code Mono<T>} or {@code Mono<List<T>>} and then wait to
	 * resolve them into actual values. When the returned {@code Mono<Void>}
	 * completes, the asynchronous attributes in the model will have been
	 * replaced with their corresponding resolved values.
	 * @return result a {@code Mono} that completes when the model is ready
	 * @since 5.1.8
	 */
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model, ServerWebExchange exchange) {
		List<Mono<?>> asyncAttributes = null;
		for (Map.Entry<String, ?> entry : model.entrySet()) {
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}
			ReactiveAdapter adapter = this.adapterRegistry.getAdapter(null, value);
			if (adapter != null) {
				if (asyncAttributes == null) {
					asyncAttributes = new ArrayList<>();
				}
				String name = entry.getKey();
				if (adapter.isMultiValue()) {
					asyncAttributes.add(
							Flux.from(adapter.toPublisher(value))
									.collectList()
									.doOnSuccess(result -> model.put(name, result)));
				}
				else {
					asyncAttributes.add(
							Mono.from(adapter.toPublisher(value))
									.doOnSuccess(result -> {
										if (result != null) {
											model.put(name, result);
											addBindingResult(name, result, model, exchange);
										}
										else {
											model.remove(name);
										}
									}));
				}
			}
		}
		return asyncAttributes != null ? Mono.when(asyncAttributes) : Mono.empty();
	}

	private void addBindingResult(String name, Object value, Map<String, Object> model, ServerWebExchange exchange) {
		BindingContext context = exchange.getAttribute(BINDING_CONTEXT_ATTRIBUTE);
		if (context == null || value.getClass().isArray() || value instanceof Collection ||
				value instanceof Map || BeanUtils.isSimpleValueType(value.getClass())) {
			return;
		}
		BindingResult result = context.createDataBinder(exchange, value, name).getBindingResult();
		model.put(BindingResult.MODEL_KEY_PREFIX + name, result);
	}

	/**
	 * Create a {@link RequestContext} to expose under the
	 * {@linkplain #setRequestContextAttribute specified attribute name}.
	 * <p>The default implementation creates a standard {@code RequestContext}
	 * instance for the given exchange and model.
	 * <p>Can be overridden in subclasses to create custom instances.
	 * @param exchange the current exchange
	 * @param model a combined output Map (never {@code null}), with dynamic values
	 * taking precedence over static attributes
	 * @return the {@code RequestContext} instance
	 * @see #setRequestContextAttribute
	 */
	protected RequestContext createRequestContext(ServerWebExchange exchange, Map<String, Object> model) {
		return new RequestContext(exchange, model, obtainApplicationContext(), getRequestDataValueProcessor());
	}

	/**
	 * Get the {@link RequestDataValueProcessor} to use.
	 * <p>The default implementation looks in the {@link #getApplicationContext()
	 * ApplicationContext} for a {@code RequestDataValueProcessor} bean with
	 * the name {@link #REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME}.
	 * @return the {@code RequestDataValueProcessor}, or {@code null} if there is
	 * none in the application context
	 */
	@Nullable
	protected RequestDataValueProcessor getRequestDataValueProcessor() {
		ApplicationContext context = getApplicationContext();
		if (context != null && context.containsBean(REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			return context.getBean(REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
		return null;
	}

	/**
	 * Subclasses must implement this method to actually render the view.
	 * @param renderAttributes combined output Map (never {@code null}),
	 * with dynamic values taking precedence over static attributes
	 * @param contentType the content type selected to render with, which should
	 * match one of the {@linkplain #getSupportedMediaTypes() supported media types}
	 * @param exchange current exchange
	 * @return a {@code Mono} that represents when and if rendering succeeds
	 */
	protected abstract Mono<Void> renderInternal(Map<String, Object> renderAttributes,
			@Nullable MediaType contentType, ServerWebExchange exchange);


	@Override
	public String toString() {
		return getClass().getName() + ": " + formatViewName();
	}

	protected String formatViewName() {
		return (getBeanName() != null ?
				"name '" + getBeanName() + "'" : "[" + getClass().getSimpleName() + "]");
	}

}
