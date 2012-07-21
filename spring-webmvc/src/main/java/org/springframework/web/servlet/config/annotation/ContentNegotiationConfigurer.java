/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;

/**
 * Helps with configuring a {@link ContentNegotiationManager}.
 *
 * <p>By default the extension of the request path extension is checked first and
 * the {@code Accept} is checked second. The path extension check will perform a
 * look up in the media types configured via {@link #setMediaTypes(Map)} and
 * will also fall back to {@link ServletContext} and the Java Activation Framework
 * (if present).
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentNegotiationConfigurer {

	private boolean favorPathExtension = true;

	private boolean favorParameter = false;

	private boolean ignoreAcceptHeader = false;

	private Map<String, MediaType> mediaTypes = new HashMap<String, MediaType>();

	private Boolean useJaf;

	private String parameterName;

	private MediaType defaultContentType;

	/**
	 * Indicate whether the extension of the request path should be used to determine
	 * the requested media type with the <em>highest priority</em>.
	 * <p>By default this value is set to {@code true} in which case a request
	 * for {@code /hotels.pdf} will be interpreted as a request for
	 * {@code "application/pdf"} regardless of the {@code Accept} header.
	 */
	public ContentNegotiationConfigurer setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
		return this;
	}

	/**
	 * Add mappings from file extensions to media types.
	 * <p>If this property is not set, the Java Action Framework, if available, may
	 * still be used in conjunction with {@link #setFavorPathExtension(boolean)}.
	 */
	public ContentNegotiationConfigurer addMediaTypes(Map<String, MediaType> mediaTypes) {
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			for (Map.Entry<String, MediaType> entry : mediaTypes.entrySet()) {
				String extension = entry.getKey().toLowerCase(Locale.ENGLISH);
				this.mediaTypes.put(extension, entry.getValue());
			}
		}
		return this;
	}

	/**
	 * Add mappings from file extensions to media types replacing any previous mappings.
	 * <p>If this property is not set, the Java Action Framework, if available, may
	 * still be used in conjunction with {@link #setFavorPathExtension(boolean)}.
	 */
	public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
		this.mediaTypes.clear();
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			for (Map.Entry<String, MediaType> entry : mediaTypes.entrySet()) {
				String extension = entry.getKey().toLowerCase(Locale.ENGLISH);
				this.mediaTypes.put(extension, entry.getValue());
			}
		}
		return this;
	}

	/**
	 * Indicate whether to use the Java Activation Framework as a fallback option
	 * to map from file extensions to media types. This is used only when
	 * {@link #setFavorPathExtension(boolean)} is set to {@code true}.
	 * <p>The default value is {@code true}.
	 * @see #parameterName
	 * @see #setMediaTypes(Map)
	 */
	public ContentNegotiationConfigurer setUseJaf(boolean useJaf) {
		this.useJaf = useJaf;
		return this;
	}

	/**
	 * Indicate whether a request parameter should be used to determine the
	 * requested media type with the <em>2nd highest priority</em>, i.e.
	 * after path extensions but before the {@code Accept} header.
	 * <p>The default value is {@code false}. If set to to {@code true}, a request
	 * for {@code /hotels?format=pdf} will be interpreted as a request for
	 * {@code "application/pdf"} regardless of the {@code Accept} header.
	 * <p>To use this option effectively you must also configure the MediaType
	 * type mappings via {@link #setMediaTypes(Map)}.
	 * @see #setParameterName(String)
	 */
	public ContentNegotiationConfigurer setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
		return this;
	}

	/**
	 * Set the parameter name that can be used to determine the requested media type
	 * if the {@link #setFavorParameter} property is {@code true}.
	 * <p>The default parameter name is {@code "format"}.
	 */
	public ContentNegotiationConfigurer setParameterName(String parameterName) {
		this.parameterName = parameterName;
		return this;
	}

	/**
	 * Indicate whether the HTTP {@code Accept} header should be ignored altogether.
	 * If set the {@code Accept} header is checked at the
	 * <em>3rd highest priority</em>, i.e. after the request path extension and
	 * possibly a request parameter if configured.
	 * <p>By default this value is set to {@code false}.
	 */
	public ContentNegotiationConfigurer setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
		return this;
	}

	/**
	 * Set the default content type.
	 * <p>This content type will be used when neither the request path extension,
	 * nor a request parameter, nor the {@code Accept} header could help determine
	 * the requested content type.
	 */
	public ContentNegotiationConfigurer setDefaultContentType(MediaType defaultContentType) {
		this.defaultContentType = defaultContentType;
		return this;
	}

	/**
	 * @return the configured {@link ContentNegotiationManager} instance
	 */
	protected ContentNegotiationManager getContentNegotiationManager() {
		List<ContentNegotiationStrategy> strategies = new ArrayList<ContentNegotiationStrategy>();
		if (this.favorPathExtension) {
			PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
			if (this.useJaf != null) {
				strategy.setUseJaf(this.useJaf);
			}
			strategies.add(strategy);
		}
		if (this.favorParameter) {
			ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
			strategy.setParameterName(this.parameterName);
			strategies.add(strategy);
		}
		if (!this.ignoreAcceptHeader) {
			strategies.add(new HeaderContentNegotiationStrategy());
		}
		if (this.defaultContentType != null) {
			strategies.add(new FixedContentNegotiationStrategy(this.defaultContentType));
		}
		ContentNegotiationStrategy[] array = strategies.toArray(new ContentNegotiationStrategy[strategies.size()]);
		return new ContentNegotiationManager(array);
	}

}
