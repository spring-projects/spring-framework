/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Base class for {@link View} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractView implements View, ApplicationContextAware {

	/** Well-known name for the RequestDataValueProcessor in the bean factory */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";


	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private static final Object NO_VALUE = new Object();


	private final List<MediaType> mediaTypes = new ArrayList<>(4);

	private final ReactiveAdapterRegistry adapterRegistry;

	private Charset defaultCharset = StandardCharsets.UTF_8;

	@Nullable
	private String requestContextAttribute;

	@Nullable
	private ApplicationContext applicationContext;


	public AbstractView() {
		this(ReactiveAdapterRegistry.getSharedInstance());
	}

	public AbstractView(ReactiveAdapterRegistry registry) {
		this.mediaTypes.add(ViewResolverSupport.DEFAULT_CONTENT_TYPE);
		this.adapterRegistry = registry;
	}


	/**
	 * Set the supported media types for this view.
	 * Default is "text/html;charset=UTF-8".
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.mediaTypes.clear();
		this.mediaTypes.addAll(supportedMediaTypes);
	}

	/**
	 * Return the configured media types supported by this view.
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * Set the default charset for this view, used when the
	 * {@linkplain #setSupportedMediaTypes(List) content type} does not contain one.
	 * Default is {@linkplain StandardCharsets#UTF_8 UTF 8}.
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "'defaultCharset' must not be null");
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Return the default charset, used when the
	 * {@linkplain #setSupportedMediaTypes(List) content type} does not contain one.
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}

	/**
	 * Set the name of the RequestContext attribute for this view.
	 * Default is none.
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * Return the name of the RequestContext attribute, if any.
	 */
	@Nullable
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
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
	 * Obtain the ApplicationContext for actual use.
	 * @return the ApplicationContext (never {@code null})
	 * @throws IllegalStateException in case of no ApplicationContext set
	 */
	protected final ApplicationContext obtainApplicationContext() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "No ApplicationContext");
		return applicationContext;
	}


	/**
	 * Prepare the model to render.
	 * @param model Map with name Strings as keys and corresponding model
	 * objects as values (Map can also be {@code null} in case of empty model)
	 * @param contentType the content type selected to render with which should
	 * match one of the {@link #getSupportedMediaTypes() supported media types}.
	 * @param exchange the current exchange
	 * @return {@code Mono} to represent when and if rendering succeeds
	 */
	@Override
	public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType,
			ServerWebExchange exchange) {

		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view with model " + model);
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
	protected Mono<Map<String, Object>> getModelAttributes(@Nullable Map<String, ?> model,
			ServerWebExchange exchange) {

		int size = (model != null ? model.size() : 0);

		Map<String, Object> attributes = new LinkedHashMap<>(size);
		if (model != null) {
			attributes.putAll(model);
		}

		return resolveAsyncAttributes(attributes).then(Mono.just(attributes));
	}

	/**
	 * By default, resolve async attributes supported by the
	 * {@link ReactiveAdapterRegistry} to their blocking counterparts.
	 * <p>View implementations capable of taking advantage of reactive types
	 * can override this method if needed.
	 * @return {@code Mono} for the completion of async attributes resolution
	 */
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model) {

		List<String> names = new ArrayList<>();
		List<Mono<?>> valueMonos = new ArrayList<>();

		for (Map.Entry<String, ?> entry : model.entrySet()) {
			Object value =  entry.getValue();
			if (value == null) {
				continue;
			}
			ReactiveAdapter adapter = this.adapterRegistry.getAdapter(null, value);
			if (adapter != null) {
				names.add(entry.getKey());
				if (adapter.isMultiValue()) {
					Flux<Object> fluxValue = Flux.from(adapter.toPublisher(value));
					valueMonos.add(fluxValue.collectList().defaultIfEmpty(Collections.emptyList()));
				}
				else {
					Mono<Object> monoValue = Mono.from(adapter.toPublisher(value));
					valueMonos.add(monoValue.defaultIfEmpty(NO_VALUE));
				}
			}
		}

		if (names.isEmpty()) {
			return Mono.empty();
		}

		return Mono.zip(valueMonos,
				values -> {
					for (int i=0; i < values.length; i++) {
						if (values[i] != NO_VALUE) {
							model.put(names.get(i), values[i]);
						}
						else {
							model.remove(names.get(i));
						}
					}
					return NO_VALUE;
				})
				.then();
	}

	/**
	 * Create a RequestContext to expose under the specified attribute name.
	 * <p>The default implementation creates a standard RequestContext instance
	 * for the given request and model. Can be overridden in subclasses for
	 * custom instances.
	 * @param exchange current exchange
	 * @param model combined output Map (never {@code null}),
	 * with dynamic values taking precedence over static attributes
	 * @return the RequestContext instance
	 * @see #setRequestContextAttribute
	 */
	protected RequestContext createRequestContext(ServerWebExchange exchange, Map<String, Object> model) {
		return new RequestContext(exchange, model, obtainApplicationContext(), getRequestDataValueProcessor());
	}

	/**
	 * Return the {@link RequestDataValueProcessor} to use.
	 * <p>The default implementation looks in the {@link #getApplicationContext()
	 * Spring configuration} for a {@code RequestDataValueProcessor} bean with
	 * the name {@link #REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME}.
	 * @return the RequestDataValueProcessor, or null if there is none at the
	 * application context.
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
	 * @param contentType the content type selected to render with which should
	 * match one of the {@link #getSupportedMediaTypes() supported media types}.
	 *@param exchange current exchange  @return {@code Mono} to represent when
	 * and if rendering succeeds
	 */
	protected abstract Mono<Void> renderInternal(Map<String, Object> renderAttributes,
			@Nullable MediaType contentType, ServerWebExchange exchange);


	@Override
	public String toString() {
		return getClass().getName();
	}

}
