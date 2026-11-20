/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * The default {@link MediaType content-type} for views: {@code "text/html;charset=UTF-8"}.
	 */
	public static final MediaType DEFAULT_CONTENT_TYPE = MediaType.parseMediaType("text/html;charset=UTF-8");


	private final List<MediaType> mediaTypes = new ArrayList<>(4);

	private Charset defaultCharset = StandardCharsets.UTF_8;

	private int order = Ordered.LOWEST_PRECEDENCE;


	public ViewResolverSupport() {
		this.mediaTypes.add(DEFAULT_CONTENT_TYPE);
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
	 * Return the configured media types supported by this view.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * Set the default charset for this view, used when the
	 * {@linkplain #setSupportedMediaTypes(List) content type} does not contain one.
	 * <p>Default is {@linkplain StandardCharsets#UTF_8 UTF-8}.
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
