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

package org.springframework.web.reactive.result.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;

/**
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractView implements View, ApplicationContextAware {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	private final List<MediaType> mediaTypes = new ArrayList<>(4);

	private ApplicationContext applicationContext;


	public AbstractView() {
		this.mediaTypes.add(ViewResolverSupport.DEFAULT_CONTENT_TYPE);
	}


	/**
	 * Set the supported media types for this view.
	 * Default is "text/html;charset=UTF-8".
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "'supportedMediaTypes' is required.");
		this.mediaTypes.clear();
		if (supportedMediaTypes != null) {
			this.mediaTypes.addAll(supportedMediaTypes);
		}
	}

	/**
	 * Return the configured media types supported by this view.
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}


	/**
	 * Prepare the model to render.
	 * @param result the result from handler execution
	 * @param contentType the content type selected to render with which should
	 * match one of the {@link #getSupportedMediaTypes() supported media types}.
	 * @param exchange the current exchange
	 * @return {@code Mono} to represent when and if rendering succeeds
	 */
	@Override
	public Mono<Void> render(HandlerResult result, MediaType contentType,
			ServerWebExchange exchange) {

		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view with model " + result.getModel());
		}

		if (contentType != null) {
			exchange.getResponse().getHeaders().setContentType(contentType);
		}

		Map<String, Object> mergedModel = getModelAttributes(result, exchange);
		return renderInternal(mergedModel, exchange);
	}

	/**
	 * Prepare the model to use for rendering.
	 * <p>The default implementation creates a combined output Map that includes
	 * model as well as static attributes with the former taking precedence.
	 */
	protected Map<String, Object> getModelAttributes(HandlerResult result, ServerWebExchange exchange) {
		ModelMap model = result.getModel();
		int size = (model != null ? model.size() : 0);

		Map<String, Object> attributes = new LinkedHashMap<>(size);
		if (model != null) {
			attributes.putAll(model);
		}

		return attributes;
	}

	/**
	 * Subclasses must implement this method to actually render the view.
	 * @param renderAttributes combined output Map (never {@code null}),
	 * with dynamic values taking precedence over static attributes
	 * @param exchange current exchange
	 * @return {@code Mono} to represent when and if rendering succeeds
	 */
	protected abstract Mono<Void> renderInternal(Map<String, Object> renderAttributes,
			ServerWebExchange exchange);


	@Override
	public String toString() {
		return getClass().getName();
	}

}
