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
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Base class for {@code ViewResolver} implementations with shared properties.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public abstract class ViewResolverSupport implements ApplicationContextAware, Ordered {

	public static final MediaType DEFAULT_CONTENT_TYPE = MediaType.parseMediaType("text/html;charset=UTF-8");


	private List<MediaType> mediaTypes = new ArrayList<>(4);

	private ApplicationContext applicationContext;

	private int order = Integer.MAX_VALUE;


	public ViewResolverSupport() {
		this.mediaTypes.add(DEFAULT_CONTENT_TYPE);
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
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * Set the order in which this {@link ViewResolver}
	 * is evaluated.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Return the order in which this {@link ViewResolver} is evaluated.
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

}
