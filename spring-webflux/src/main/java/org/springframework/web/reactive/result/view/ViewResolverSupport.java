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
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Base class for {@code ViewResolver} implementations with shared properties.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class ViewResolverSupport implements Ordered {

	/**
	 * The default {@link MediaType content-type} for views.
	 */
	public static final MediaType DEFAULT_CONTENT_TYPE = MediaType.parseMediaType("text/html;charset=UTF-8");


	private List<MediaType> mediaTypes = new ArrayList<>(4);

	private Charset defaultCharset = StandardCharsets.UTF_8;

	private int order = Ordered.LOWEST_PRECEDENCE;


	public ViewResolverSupport() {
		this.mediaTypes.add(DEFAULT_CONTENT_TYPE);
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
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * Set the default charset for this view, used when the
	 * {@linkplain #setSupportedMediaTypes(List) content type} does not contain one.
	 * Default is {@linkplain StandardCharsets#UTF_8 UTF 8}.
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "Default Charset must not be null");
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
	 * Specify the order value for this ViewResolver bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
